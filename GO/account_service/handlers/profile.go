package handlers

import (
	"accountservice/data"
	"accountservice/database"
	"accountservice/logger"
	"net/http"

	"github.com/gorilla/mux"
)

type Profile struct {
	l             logger.ILogger
	dbConn        *database.MYSQLConnection
	configuration data.Configuration
}

func NewProfile(l logger.ILogger, db *database.MYSQLConnection, conf data.Configuration) *Profile {
	return &Profile{l: l, dbConn: db, configuration: conf}
}
func (prof *Profile) GetPreKeyBundle(rw http.ResponseWriter, r *http.Request) {
	prof.l.Info("Endpoint /account/fetch/prekeybundle/{UID} hit (GET Method)")

	vars := mux.Vars(r)
	uid := vars["UID"]
	if uid == "" {
		prof.l.Error("UID not provided in the request path")
		http.Error(rw, "Bad Request: UID is required", http.StatusBadRequest)
		return
	}

	exists, err := prof.dbConn.IsUIDRegistered(uid)
	if err != nil {
		prof.l.Error("Database error when checking UID existence", err.Error())
		http.Error(rw, "Internal server error", http.StatusInternalServerError)
		return
	}
	if !exists {
		prof.l.Warning("UID does not exist in the database: ", uid)
		http.Error(rw, "UID not found", http.StatusNotFound)
		return
	}

	preKeyBundle, err := prof.dbConn.GetPreKeyBundle(uid)
	if err != nil {
		prof.l.Error("Error occurred when fetching pre-key bundle from the database", err.Error())
		http.Error(rw, "Internal server error", http.StatusInternalServerError)
		return
	}

	rw.Header().Set("Content-Type", "application/json")
	rw.WriteHeader(http.StatusOK)
	rw.Write([]byte(preKeyBundle))
}

func (prof *Profile) UpdateStatus(rw http.ResponseWriter, r *http.Request) {
	prof.l.Info("Endpoint /account/update/status hit (POST Method)")
	updateAbout := data.UpdateStatus{}
	err := updateAbout.FromJSON(r.Body)
	if err != nil {
		prof.l.Error("Error occured when parsing the update about json body", err.Error())
		rw.WriteHeader(http.StatusInternalServerError)
		rw.Write([]byte("Internal server error"))
		return
	}

	res, err := prof.dbConn.UpdateStatus(updateAbout.UID, updateAbout.NewStatus)
	if !res {
		prof.l.Error("Error occured when updating the about in the database", err.Error())
		rw.WriteHeader(http.StatusInternalServerError)
		rw.Write([]byte("Internal server error"))
		return
	}

	rw.WriteHeader(http.StatusOK)
	rw.Write([]byte("Status updated"))
}

func (prof *Profile) FetchIdentityPublicKey(rw http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	uid := vars["UID"]

	prof.l.Info("Fetch identity public key for UID: ", uid)

	publicKey, err := prof.dbConn.GetIdentityPublicKeyByUID(uid)
	if err != nil || publicKey == "" {
		prof.l.Warning("UID not found or database error: ", err)
		rw.WriteHeader(http.StatusNotFound)
		return
	}

	response := data.IdentityPublicKeyResponse{
		UID:               uid,
		IdentityPublicKey: publicKey,
	}

	rw.WriteHeader(http.StatusOK)
	rw.Header().Set("Content-Type", "application/json")
	response.ToJSON(rw)
}
