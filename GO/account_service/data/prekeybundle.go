package data

import (
	"encoding/json"
	"io"
)

type PreKeyBundleResponse struct {
	IdentityPublicKey              string `json:"identityPublicKey"`
	SignedPublicKey                string `json:"signedPublicKey"`
	SignedPreKeySignature          string `json:"signedPreKeySignature"`
	OneTimePreKey                  string `json:"oneTimePreKey,omitempty"`
	OneTimePreKeyID                string `json:"oneTimePreKeyID,omitempty"`
	LastResortPQKey                string `json:"lastResortPQKey"`
	SignedLastResortPQKeySignature string `json:"signedLastResortPQKeySignature"`
}

type OneTimePreKey struct {
	ID            int    `json:"id"`
	AccountUID    string `json:"accountUID"`
	OneTimePreKey string `json:"oneTimePreKey"`
	Used          bool   `json:"used"`
}

type OneTimePreKeyUpload struct {
	PublicKey string `json:"pubKey" validate:"required"`
	ID        string `json:"id" validate:"required"`
}

type OTPKRegenerationRequest struct {
	UID            string   `json:"uid" validate:"required"`
	OneTimePreKeys []string `json:"oneTimePreKeys" validate:"required,min=1"`
}

func (otpk *OneTimePreKey) ToJSON(w io.Writer) error {
	e := json.NewEncoder(w)
	return e.Encode(otpk)
}

func (otpk *OneTimePreKey) FromJSON(r io.Reader) error {
	e := json.NewDecoder(r)
	return e.Decode(otpk)
}

func (pkb *PreKeyBundleResponse) ToJSON(w io.Writer) error {
	e := json.NewEncoder(w)
	return e.Encode(pkb)
}

func (pkb *PreKeyBundleResponse) FromJSON(r io.Reader) error {
	e := json.NewDecoder(r)
	return e.Decode(pkb)
}
