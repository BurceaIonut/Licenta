package data

import (
	"encoding/json"
	"io"
)

type Account struct {
	UID                            string                `json:"UID"`
	FirstName                      string                `json:"first_name"`
	LastName                       string                `json:"last_name"`
	IdentityPublicKey              string                `json:"identityPublicKey"`
	SignedPublicKey                string                `json:"signedPublicKey"`
	SignedPreKeySignature          string                `json:"signedPreKeySignature"`
	OneTimePreKeys                 []OneTimePreKeyUpload `json:"oneTimePreKeys" validate:"required,min=1"`
	LastResortPQKey                string                `json:"lastResortPQKey"`
	SignedLastResortPQKeySignature string                `json:"signedLastResortPQKeySignature"`
}

func (acc *Account) ToJSON(w io.Writer) error {
	e := json.NewEncoder(w)
	return e.Encode(acc)
}

func (acc *Account) FromJSON(r io.Reader) error {
	e := json.NewDecoder(r)
	return e.Decode(acc)
}

type Accounts []Account

func (accs *Accounts) ToJSON(w io.Writer) error {
	e := json.NewEncoder(w)
	return e.Encode(accs)
}

func (accs *Accounts) FromJSON(r io.Reader) error {
	d := json.NewDecoder(r)
	return d.Decode(accs)
}
