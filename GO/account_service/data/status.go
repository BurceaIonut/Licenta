package data

import (
	"encoding/json"
	"io"
)

type UpdateStatus struct {
	UID       string `json:"UID"`
	NewStatus string `json:"NewStatus"`
}

func (ua *UpdateStatus) ToJSON(w io.Writer) error {
	e := json.NewEncoder(w)
	return e.Encode(ua)
}

func (ua *UpdateStatus) FromJSON(r io.Reader) error {
	d := json.NewDecoder(r)
	return d.Decode(ua)
}
