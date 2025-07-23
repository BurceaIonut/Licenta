package database

import (
	"accountservice/data"
	"accountservice/logger"
	"crypto/rand"
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"encoding/json"
	"log"
	"time"

	_ "github.com/go-sql-driver/mysql"
)

type MYSQLConnection struct {
	logger logger.ILogger
	conf   data.Configuration
	db     *sql.DB
}

func NewConnection(l logger.ILogger, c data.Configuration) *MYSQLConnection {
	return &MYSQLConnection{logger: l, conf: c, db: nil}
}

func (conn *MYSQLConnection) MYSQLConnect() error {
	if !conn.conf.UsesDatabase {
		conn.logger.Info("Database connection not used, skipping...")
		return nil
	}

	dbUser, dbPassword, dbHost, dbName := conn.conf.GetDatabaseConfig()

	dbConn, err := sql.Open("mysql", dbUser+":"+dbPassword+"@tcp("+dbHost+":3306)/"+dbName)

	if err != nil {
		conn.logger.Error(err.Error())
		return err
	}
	conn.db = dbConn

	conn.db.SetConnMaxLifetime(time.Minute * 3)
	conn.db.SetMaxIdleConns(10)
	conn.db.SetMaxOpenConns(10)

	return err
}

func (conn *MYSQLConnection) MYSQLTest() error {
	return conn.db.Ping()
}

func (conn *MYSQLConnection) MYSQLClose() error {
	conn.logger.Info("MYSQL Connection closed!")
	return conn.db.Close()
}

func generateHashUID(input string) string {
	randBytes := make([]byte, 16)
	_, err := rand.Read(randBytes)
	if err != nil {
		log.Fatalf("Failed to generate random bytes for UID: %v", err)
	}
	fullInput := input + hex.EncodeToString(randBytes)
	hash := sha256.Sum256([]byte(fullInput))
	return hex.EncodeToString(hash[:])
}

func (conn *MYSQLConnection) CreateAccount(acc *data.Account) (string, error) {
	uid := generateHashUID(acc.FirstName + acc.LastName + time.Now().String())

	tx, err := conn.db.Begin()
	if err != nil {
		conn.logger.Error("TX begin error:", err.Error())
		return "", err
	}
	defer tx.Rollback()

	_, err = tx.Exec(`
		INSERT INTO accounts 
		(UID, FirstName, LastName, IdentityPublicKey, SignedPublicKey, SignedPreKeySignature, LastResortPQKey, SignedLastResortPQKeySignature)
		VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
		uid, acc.FirstName, acc.LastName, acc.IdentityPublicKey, acc.SignedPublicKey, acc.SignedPreKeySignature, acc.LastResortPQKey, acc.SignedLastResortPQKeySignature)
	if err != nil {
		conn.logger.Error("Error inserting account:", err.Error())
		return "", err
	}

	stmtOTPK, err := tx.Prepare(`
		INSERT INTO one_time_pre_keys (ID, AccountUID, OneTimePreKey)
    	VALUES (?, ?, ?)`)
	if err != nil {
		conn.logger.Error("Error preparing OTPK insert:", err.Error())
		return "", err
	}
	defer stmtOTPK.Close()

	for _, otpk := range acc.OneTimePreKeys {
		_, err := stmtOTPK.Exec(otpk.ID, uid, otpk.PublicKey)
		if err != nil {
			conn.logger.Error("Error inserting OTPK:", err.Error())
			return "", err
		}
	}

	if err := tx.Commit(); err != nil {
		conn.logger.Error("TX commit error:", err.Error())
		return "", err
	}

	return uid, nil
}

func (conn *MYSQLConnection) GetPreKeyBundle(UID string) ([]byte, error) {
	tx, err := conn.db.Begin()
	if err != nil {
		conn.logger.Error("Begin TX error:", err.Error())
		return nil, err
	}
	defer tx.Rollback()

	var identityPub, signedPub, signature, lastResortPQKey, signedLastResortPQKeySignature string
	err = tx.QueryRow(`
		SELECT IdentityPublicKey, SignedPublicKey, SignedPreKeySignature, LastResortPQKey, SignedLastResortPQKeySignature
		FROM accounts WHERE UID = ? LIMIT 1`, UID).
		Scan(&identityPub, &signedPub, &signature, &lastResortPQKey, &signedLastResortPQKeySignature)
	if err != nil {
		conn.logger.Error("QueryRow scan error (accounts):", err.Error())
		return nil, err
	}

	var otpkPub, otpkHash string
	err = tx.QueryRow(`
		SELECT OneTimePreKey, ID FROM one_time_pre_keys
		WHERE AccountUID = ? AND Used = FALSE
		ORDER BY ID LIMIT 1 FOR UPDATE SKIP LOCKED`, UID).
		Scan(&otpkPub, &otpkHash)

	useOTPK := false
	if err == nil {
		_, err = tx.Exec(`UPDATE one_time_pre_keys SET Used = TRUE WHERE ID = ?`, otpkHash)
		if err != nil {
			conn.logger.Error("Failed to mark OTPK as used:", err.Error())
			return nil, err
		}

		_, err = tx.Exec(`DELETE FROM one_time_pre_keys WHERE ID = ?`, otpkHash)
		if err != nil {
			conn.logger.Error("Failed to delete used OTPK:", err.Error())
			return nil, err
		}

		useOTPK = true
	} else if err != sql.ErrNoRows {
		conn.logger.Error("OTPK fetch error:", err.Error())
		return nil, err
	}

	if err := tx.Commit(); err != nil {
		conn.logger.Error("TX commit error:", err.Error())
		return nil, err
	}

	response := data.PreKeyBundleResponse{
		IdentityPublicKey:              identityPub,
		SignedPublicKey:                signedPub,
		SignedPreKeySignature:          signature,
		LastResortPQKey:                lastResortPQKey,
		SignedLastResortPQKeySignature: signedLastResortPQKeySignature,
	}

	if useOTPK {
		response.OneTimePreKey = otpkPub
		response.OneTimePreKeyID = otpkHash
	}

	jsonData, err := json.Marshal(response)
	if err != nil {
		conn.logger.Error("JSON marshal error:", err.Error())
		return nil, err
	}
	conn.logger.Debug("PreKeyBundle JSON data:", string(jsonData))
	return jsonData, nil
}

func (conn *MYSQLConnection) UpdateStatus(UID string, newStatus string) (bool, error) {
	stmtUpdate, err := conn.db.Prepare("UPDATE accounts SET Status = ? WHERE UID = ?")
	if err != nil {
		conn.logger.Error("Error occured when preparing the update statement", err.Error())
		return false, err
	}
	_, err = stmtUpdate.Exec(newStatus, UID)
	if err != nil {
		conn.logger.Error("Error occured when executing the update statement", err.Error())
		return false, err
	}
	return true, nil
}

func (conn *MYSQLConnection) UpdateProfilePictureURL(UID string, url string) (bool, error) {
	conn.logger.Debug("New url: ", url)
	stmtUpdate, err := conn.db.Prepare("UPDATE accounts SET ProfilePictureUrl = ? WHERE UID = ?")
	if err != nil {
		conn.logger.Error("Error occured when updating the profile picture URL", err.Error())
		return false, err
	}

	_, err = stmtUpdate.Exec(url, UID)
	if err != nil {
		conn.logger.Error("Error occured when executing the update profile picture URL query", err.Error())
		return false, err
	}

	return true, nil
}

func (conn *MYSQLConnection) IsUIDRegistered(uid string) (bool, error) {
	stmt, err := conn.db.Prepare("SELECT EXISTS(SELECT 1 FROM accounts WHERE UID = ?)")
	if err != nil {
		return false, err
	}
	defer stmt.Close()

	var exists int
	err = stmt.QueryRow(uid).Scan(&exists)
	if err != nil {
		return false, err
	}
	return exists == 1, nil
}

func (conn *MYSQLConnection) GetIdentityPublicKeyByUID(uid string) (string, error) {
	query := "SELECT IdentityPublicKey FROM accounts WHERE UID = ?"
	var pubKey string
	err := conn.db.QueryRow(query, uid).Scan(&pubKey)
	if err != nil {
		return "", err
	}
	return pubKey, nil
}
