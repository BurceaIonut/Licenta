package data

import (
	"encoding/json"
	"io"
)

type IdentityPublicKeyResponse struct {
	UID               string `json:"uid"`
	IdentityPublicKey string `json:"identityPublicKey"`
}

func (acc *IdentityPublicKeyResponse) ToJSON(w io.Writer) error {
	e := json.NewEncoder(w)
	return e.Encode(acc)
}

func (acc *IdentityPublicKeyResponse) FromJSON(r io.Reader) error {
	e := json.NewDecoder(r)
	return e.Decode(acc)
}
