package data

import (
	"encoding/json"
	"errors"
	"io"
)

type ServiceConfiguration struct {
	Name string `json:"name" validate:"required"`
	URL  string `json:"url" validate:"required"`
}

func (config *ServiceConfiguration) ToJSON(w io.Writer) error {
	e := json.NewEncoder(w)
	return e.Encode(config)
}

func (config *ServiceConfiguration) FromJSON(r io.Reader) error {
	d := json.NewDecoder(r)
	return d.Decode(config)
}

type Configuration struct {
	Address string `json:"address" validate:"required"`

	IdleTimeout    int `json:"idleTimeout" validate:"required"`
	WriteTimeout   int `json:"writeTimeout" validate:"required"`
	ReadTimeout    int `json:"readTimeout" validate:"required"`
	ContextTimeout int `json:"contextTimeout" validate:"required"`

	UsesDatabase bool   `json:"usesDatabase" validate:"required"`
	DbUser       string `json:"dbUsername" validate:"required"`
	DbPassword   string `json:"dbPassword" validate:"required"`
	DbHost       string `json:"dbHost" validate:"required"`
	DbName       string `json:"dbName" validate:"required"`

	DebugEnabled bool `json:"debugEnabled" validate:"required"`

	InfoPrefix    string `json:"infoPrefix"`
	WarningPrefix string `json:"warningPrefix"`
	ErrorPrefix   string `json:"errorPrefix"`
	DebugPrefix   string `json:"debugPrefix"`

	Services []ServiceConfiguration `json:"services" validate:"required"`
}

func (config *Configuration) ToJSON(w io.Writer) error {
	e := json.NewEncoder(w)
	return e.Encode(config)
}

func (config *Configuration) FromJSON(r io.Reader) error {
	d := json.NewDecoder(r)
	return d.Decode(config)
}

func (conf *Configuration) GetServiceURL(name string) (string, error) {
	for _, service := range conf.Services {
		if service.Name == name {
			return service.URL, nil
		}
	}
	return "", errors.New("service not found: " + name)
}
