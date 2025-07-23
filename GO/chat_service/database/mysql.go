package database

import (
	"chatservice/data"
	"chatservice/logger"
	"database/sql"
	"fmt"
	"time"

	_ "github.com/go-sql-driver/mysql"
)

type MysqlConnection struct {
	logger        logger.ILogger
	db            *sql.DB
	configuration *data.Configuration
}

func NewConnection(l logger.ILogger, conf *data.Configuration) *MysqlConnection {
	return &MysqlConnection{logger: l, db: nil, configuration: conf}
}

func (conn *MysqlConnection) InitializeConnection() error {
	dbUser, dbPassword, dbHost, dbName := conn.configuration.GetDatabaseConfig()

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

func (conn *MysqlConnection) TestConnection() error {
	return conn.db.Ping()
}

func (conn *MysqlConnection) CloseConnection() error {
	conn.logger.Info("Closed the connection to the database")
	return conn.db.Close()
}

func (conn *MysqlConnection) UIDExists(uid string) (bool, error) {
	query := "SELECT COUNT(*) FROM accounts WHERE uid = ?"
	var count int
	err := conn.db.QueryRow(query, uid).Scan(&count)
	if err != nil {
		conn.logger.Error("UID Exists query failed:", err.Error())
		return false, err
	}
	return count > 0, nil
}

func (conn *MysqlConnection) CreateGroup(groupName string, creatorUID string) (string, error) {
	res, err := conn.db.Exec("INSERT INTO groups (group_name, creator_uid) VALUES (?, ?)", groupName, creatorUID)
	if err != nil {
		conn.logger.Error("Failed to create group:", err.Error())
		return "", err
	}

	lastGID, err := res.LastInsertId()
	if err != nil {
		conn.logger.Error("Failed to get last insert id:", err.Error())
		return "", err
	}

	return fmt.Sprintf("%d", lastGID), nil
}

func (conn *MysqlConnection) InsertPendingMessage(toUID, fromUID, fullName, timestamp, msgType string, data []byte) error {
	query := "INSERT INTO pending_messages (to_uid, from_uid, full_name, timestamp, message_type, data) VALUES (?, ?, ?, ?, ?, ?)"
	stmt, err := conn.db.Prepare(query)
	if err != nil {
		conn.logger.Error("Prepare failed:", err.Error())
		return err
	}
	defer stmt.Close()

	_, err = stmt.Exec(toUID, fromUID, fullName, timestamp, msgType, data)
	if err != nil {
		conn.logger.Error("Insert failed:", err.Error())
	}
	return err
}

func (conn *MysqlConnection) GetPendingMessages(toUID string) ([]data.PendingMessage, error) {
	query := "SELECT id, to_uid, from_uid, full_name, timestamp, message_type, data FROM pending_messages WHERE to_uid = ? ORDER BY id ASC"
	rows, err := conn.db.Query(query, toUID)
	if err != nil {
		conn.logger.Error("Query failed:", err.Error())
		return nil, err
	}
	defer rows.Close()

	var messages []data.PendingMessage
	for rows.Next() {
		var msg data.PendingMessage
		var data []byte
		err := rows.Scan(&msg.ID, &msg.ToUID, &msg.FromUID, &msg.FullName, &msg.Timestamp, &msg.MessageType, &data)
		if err != nil {
			conn.logger.Error("Row scan failed:", err.Error())
			continue
		}
		msg.Data = data
		messages = append(messages, msg)
	}

	return messages, nil
}

func (conn *MysqlConnection) DeletePendingMessages(toUID string) error {
	query := "DELETE FROM pending_messages WHERE to_uid = ?"
	_, err := conn.db.Exec(query, toUID)
	if err != nil {
		conn.logger.Error("Delete failed:", err.Error())
	}
	return err
}

func (conn *MysqlConnection) InsertQRCodeValidation(uid1, uid2, signature1 string) error {
	_, err := conn.db.Exec(`
        INSERT INTO qr_codes_validations (uid1, uid2, signature1)
        VALUES (?, ?, ?)`, uid1, uid2, signature1)
	return err
}

func (conn *MysqlConnection) GetQRCodeValidationSignature1(uid1, uid2 string) (string, error) {
	var signature1 string
	err := conn.db.QueryRow(`
		SELECT signature1 FROM qr_codes_validations 
		WHERE uid1 = ? AND uid2 = ?`,
		uid1, uid2).Scan(&signature1)
	if err != nil {
		return "", err
	}
	return signature1, nil
}

func (conn *MysqlConnection) DeleteQRCodeValidation(uid1, uid2 string) error {
	_, err := conn.db.Exec(`
        DELETE FROM qr_codes_validations 
        WHERE (uid1 = ? AND uid2 = ?) OR (uid1 = ? AND uid2 = ?)`,
		uid1, uid2, uid2, uid1)
	return err
}

func (conn *MysqlConnection) UpdateQRCodeValidationSignature2(uid1, uid2, signature2 string) error {
	_, err := conn.db.Exec(`
        UPDATE qr_codes_validations 
        SET signature2 = ? 
        WHERE uid1 = ? AND uid2 = ?`,
		signature2, uid1, uid2)
	return err
}
