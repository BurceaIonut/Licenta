package data

import (
	"encoding/json"
	"io"
	"regexp"

	"github.com/go-playground/validator/v10"
)

type RegisterAccount struct {
	FirstName                      string                `json:"first_name" validate:"required,first_name"`
	LastName                       string                `json:"last_name" validate:"required,last_name"`
	IdentityPublicKey              string                `json:"identityPublicKey" validate:"required"`
	SignedPublicKey                string                `json:"signedPublicKey" validate:"required"`
	SignedPreKeySignature          string                `json:"signedPreKeySignature" validate:"required"`
	OneTimePreKeys                 []OneTimePreKeyUpload `json:"oneTimePreKeys" validate:"required,min=1"`
	LastResortPQKey                string                `json:"lastResortPQKey" validate:"required"`
	SignedLastResortPQKeySignature string                `json:"signedLastResortPQKeySignature" validate:"required"`
}

func (regAcc *RegisterAccount) ToJSON(w io.Writer) error {
	e := json.NewEncoder(w)
	return e.Encode(regAcc)
}

func (regAcc *RegisterAccount) FromJSON(r io.Reader) error {
	e := json.NewDecoder(r)
	return e.Decode(regAcc)
}

func validateName(fl validator.FieldLevel) bool {
	username := fl.Field().String()

	r := regexp.MustCompile("^([a-zA-Z]+)([0-9|_|a-z|A-Z]*)$")

	matches := r.FindAllString(username, -1)

	if matches == nil {
		return false
	}

	if len(matches) != 1 {
		return false
	}

	return true
}

func (regAcc *RegisterAccount) Validate() error {
	validate := validator.New()
	validate.RegisterValidation("first_name", validateName)
	validate.RegisterValidation("last_name", validateName)
	return validate.Struct(regAcc)
}
