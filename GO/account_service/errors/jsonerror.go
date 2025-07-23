package jsonerrors

import (
	"encoding/json"
	"io"
)

type JsonError struct {
	Message string `json:"error"`
}

func (je *JsonError) ToJSON(w io.Writer) error {
	jEncoder := json.NewEncoder(w)
	return jEncoder.Encode(je)
}

func (je *JsonError) FromJSON(r io.Reader) error {
	jDecoder := json.NewDecoder(r)
	return jDecoder.Decode(je)
}
