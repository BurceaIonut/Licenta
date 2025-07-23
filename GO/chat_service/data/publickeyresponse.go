package data

import (
	"encoding/json"
	"io"
)

type PublicKeyResponse struct {
	UID               string `json:"uid"`
	IdentityPublicKey string `json:"identityPublicKey"`
}

func (ro *PublicKeyResponse) ToJSON(w io.Writer) error {
	e := json.NewEncoder(w)
	return e.Encode(ro)
}

func (ro *PublicKeyResponse) FromJSON(r io.Reader) error {
	d := json.NewDecoder(r)
	return d.Decode(ro)
}
