package data

import (
	"encoding/json"
	"io"
)

type PendingMessage struct {
	ID          int
	ToUID       string
	FromUID     string
	FullName    string
	Timestamp   string
	MessageType string
	Data        []byte
}

func (ro *PendingMessage) ToJSON(w io.Writer) error {
	e := json.NewEncoder(w)
	return e.Encode(ro)
}

func (ro *PendingMessage) FromJSON(r io.Reader) error {
	d := json.NewDecoder(r)
	return d.Decode(ro)
}
