package handlers

import (
	"accountservice/data"
	"accountservice/database"
	jsonerrors "accountservice/errors"
	"accountservice/logger"
	"errors"
	"net/http"

	"github.com/go-playground/validator/v10"
)

type Register struct {
	l             logger.ILogger
	dbConn        *database.MYSQLConnection
	configuration data.Configuration
}

func NewRegister(l logger.ILogger, dbConn *database.MYSQLConnection, conf data.Configuration) *Register {
	return &Register{l: l, dbConn: dbConn, configuration: conf}
}

func (register *Register) insertUser(regAcc *data.RegisterAccount) (string, error) {
	newAcc := &data.Account{
		FirstName:                      regAcc.FirstName,
		LastName:                       regAcc.LastName,
		IdentityPublicKey:              regAcc.IdentityPublicKey,
		SignedPublicKey:                regAcc.SignedPublicKey,
		SignedPreKeySignature:          regAcc.SignedPreKeySignature,
		OneTimePreKeys:                 regAcc.OneTimePreKeys,
		LastResortPQKey:                regAcc.LastResortPQKey,
		SignedLastResortPQKeySignature: regAcc.SignedLastResortPQKeySignature,
	}

	uid, err := register.dbConn.CreateAccount(newAcc)
	if err != nil {
		register.l.Error("Error occured during interaction with the database server")
		return "", errors.New("Account registration failed")
	}

	return uid, nil
}

func (register *Register) RegisterAccount(rw http.ResponseWriter, r *http.Request) {
	register.l.Info("Endpoint /register reached (POST method)")

	data := &data.RegisterAccount{}

	err := data.FromJSON(r.Body)
	if err != nil {
		register.l.Error("JSON decode error: ", err)

		rw.WriteHeader(http.StatusBadRequest)
		jsonError := &jsonerrors.JsonError{Message: "Invalid json format"}
		jsonError.ToJSON(rw)
		return
	}

	err = data.Validate()
	if err != nil {
		for _, e := range err.(validator.ValidationErrors) {

			rw.WriteHeader(http.StatusBadRequest)
			jsonError := &jsonerrors.JsonError{Message: e.Field() + " invalid format"}
			jsonError.ToJSON(rw)
			break
		}

		register.l.Error(err.Error())
		return
	}

	register.l.Info("Inserting the account into the database")

	uid, err := register.insertUser(data)
	if err != nil {
		rw.WriteHeader(http.StatusInternalServerError)
		jsonError := &jsonerrors.JsonError{Message: err.Error()}
		jsonError.ToJSON(rw)
		return
	}

	register.l.Info("Account has been added, first_name = " + data.FirstName + ",last_name = " + data.LastName + ", uid = " + uid)

	rw.WriteHeader(http.StatusOK)
	rw.Header().Set("Content-Type", "application/json")
	rw.Write([]byte(`{"uid":"` + uid + `"}`))
}
