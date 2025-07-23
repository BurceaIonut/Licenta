package handlers

import (
	"io"
	"net/http"
	"proxygateway/data"
	"proxygateway/logger"
)

type AccountRegister struct {
	logger        logger.ILogger
	configuration data.Configuration
}

func NewAccountRegister(l logger.ILogger, conf data.Configuration) *AccountRegister {
	return &AccountRegister{logger: l, configuration: conf}
}

func (reg *AccountRegister) RegisterAccount(rw http.ResponseWriter, r *http.Request) {
	reg.logger.Info("Endpoint /account/register hit, url: ", r.URL.Path)
	reg.logger.Debug("Sending data to Account Service")

	url, err := reg.configuration.GetServiceURL("accountservice")
	if err != nil {
		reg.logger.Error("Cannot get the account service url from config")
		return
	}

	response, err := http.Post("http://"+url+"/register", "application/json", r.Body)
	if err != nil {
		reg.logger.Warning("POST request to Account Service /register failed", err.Error())
		rw.WriteHeader(http.StatusInternalServerError)
		rw.Write([]byte("Bad request"))
		return
	}

	respBody, err := io.ReadAll(response.Body)
	if err != nil {
		reg.logger.Warning("Cannot read data from response body from Account Service /register", err.Error())
		rw.WriteHeader(http.StatusInternalServerError)
		rw.Write([]byte("Bad request"))
		return
	}

	reg.logger.Info("Received response from Account Service")
	rw.WriteHeader(response.StatusCode)
	rw.Write(respBody)
}
