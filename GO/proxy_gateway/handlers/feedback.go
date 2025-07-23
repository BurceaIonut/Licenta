package handlers

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"net/http"
	"proxygateway/data"
	"proxygateway/logger"
)

type Feedback struct {
	logger        logger.ILogger
	configuration data.Configuration
}

func NewFeedback(l logger.ILogger, conf data.Configuration) *Feedback {
	return &Feedback{logger: l, configuration: conf}
}

func (f *Feedback) ForwardRequest(proxyProtocol string, proxyHost string, r *http.Request) ([]byte, error) {
	body, err := ioutil.ReadAll(r.Body)
	if err != nil {
		return nil, err
	}

	r.Body = ioutil.NopCloser(bytes.NewReader(body))

	url := fmt.Sprintf("%s://%s%s", proxyProtocol, proxyHost, r.RequestURI)

	proxyReq, _ := http.NewRequest(r.Method, url, bytes.NewReader(body))

	proxyReq.Header = make(http.Header)
	for h, val := range r.Header {
		proxyReq.Header[h] = val
	}
	f.logger.Info(proxyReq.Header)
	httpClient := &http.Client{}
	resp, err := httpClient.Do(proxyReq)
	if err != nil {
		return nil, err
	}
	body2, _ := ioutil.ReadAll(resp.Body)
	f.logger.Debug(string(body2))
	defer resp.Body.Close()
	return body2, nil
}

func (f *Feedback) UpdateStatus(rw http.ResponseWriter, r *http.Request) {
	f.logger.Info("Endpoint /account/update/status hit (POST method)")
	f.logger.Debug("Forwarding request to Account Service")

	url, err := f.configuration.GetServiceURL("accountservice")
	if err != nil {
		f.logger.Error("Cannot get the account service url from config")
		return
	}

	returnData, err := f.ForwardRequest("http", url, r)
	if err != nil {
		f.logger.Error(err.Error())
		http.Error(rw, err.Error(), http.StatusInternalServerError)
		return
	}

	rw.WriteHeader(http.StatusOK)
	rw.Write(returnData)
}

func (f *Feedback) UpdateProfilePicture(rw http.ResponseWriter, r *http.Request) {
	f.logger.Info("/account/update/picture hit (POST method)")
	f.logger.Debug("Forwarding message to Account service")

	url, err := f.configuration.GetServiceURL("accountservice")
	if err != nil {
		f.logger.Error("Cannot get the account service url from config")
		return
	}

	returnData, err := f.ForwardRequest("http", url, r)
	if err != nil {
		http.Error(rw, err.Error(), http.StatusInternalServerError)
		return
	}
	rw.WriteHeader(http.StatusOK)
	rw.Write(returnData)
}

func (f *Feedback) GetProfilePicture(rw http.ResponseWriter, r *http.Request) {
	f.logger.Info("/account/static hit (GET method)")
	f.logger.Debug("Forwarding message to Account service")

	url, err := f.configuration.GetServiceURL("accountservice")
	if err != nil {
		f.logger.Error("Cannot get the account service url from config")
		return
	}

	returnData, err := f.ForwardRequest("http", url, r)
	if err != nil {
		http.Error(rw, err.Error(), http.StatusInternalServerError)
		return
	}
	rw.WriteHeader(http.StatusOK)
	rw.Write(returnData)
}

func (f *Feedback) GetProfile(rw http.ResponseWriter, r *http.Request) {
	f.logger.Info("/profile/{UID:[0-9A-Za-z]+} hit (GET method)")
	f.logger.Debug("Forwarding message to Account service")

	url, err := f.configuration.GetServiceURL("accountservice")
	if err != nil {
		f.logger.Error("Cannot get the account service url from config")
		return
	}

	returnData, err := f.ForwardRequest("http", url, r)
	if err != nil {
		http.Error(rw, err.Error(), http.StatusInternalServerError)
		return
	}

	rw.WriteHeader(http.StatusOK)
	rw.Write(returnData)
}

func (f *Feedback) GetGroups(rw http.ResponseWriter, r *http.Request) {
	f.logger.Info("/chat/groups/{GID:[0-9a-zA-Z]+} hit (GET method)")
	f.logger.Debug("Forwarding message to Account service")

	url, err := f.configuration.GetServiceURL("accountservice")
	if err != nil {
		f.logger.Error("Cannot get the account service url from config")
		return
	}

	returnData, err := f.ForwardRequest("http", url, r)
	if err != nil {
		http.Error(rw, err.Error(), http.StatusInternalServerError)
		return
	}
	rw.WriteHeader(http.StatusOK)
	rw.Write(returnData)
}

func (f *Feedback) GetGroupPicture(rw http.ResponseWriter, r *http.Request) {
	f.logger.Info("/chat/groups/static/ hit (GET method)")
	f.logger.Debug("Forwarding message to Account Service")

	url, err := f.configuration.GetServiceURL("accountservice")
	if err != nil {
		f.logger.Error("Cannot get the account service url from config")
		return
	}

	returnData, err := f.ForwardRequest("http", url, r)
	if err != nil {
		f.logger.Error(err.Error())
		http.Error(rw, err.Error(), http.StatusInternalServerError)
		return
	}

	f.logger.Info(returnData)

	rw.WriteHeader(http.StatusOK)
	rw.Write(returnData)
}

func (f *Feedback) UpdateGroupPicture(rw http.ResponseWriter, r *http.Request) {
	f.logger.Info("Endpoint /chat/group/updatepicture hit (GET method)")
	f.logger.Debug("Forwarding message to Account Service")

	url, err := f.configuration.GetServiceURL("accountservice")
	if err != nil {
		f.logger.Error("Cannot get the account service url from config")
		return
	}

	returnData, err := f.ForwardRequest("http", url, r)
	if err != nil {
		f.logger.Error(err.Error())
		http.Error(rw, err.Error(), http.StatusInternalServerError)
		return
	}

	rw.WriteHeader(http.StatusOK)
	rw.Write(returnData)
}

func (f *Feedback) GetGroupsWithId(rw http.ResponseWriter, r *http.Request) {
	f.logger.Info("Endpoint /chat/groups/{GID:[0-9a-zA-Z]+}/{lastGID:[0-9a-zA-Z]+} hit (GET method)")
	f.logger.Debug("Forwarding request to Account Service")

	url, err := f.configuration.GetServiceURL("accountservice")
	if err != nil {
		f.logger.Error("Cannot get the account service url from config")
		return
	}

	returnData, err := f.ForwardRequest("http", url, r)
	if err != nil {
		f.logger.Error(err.Error())
		http.Error(rw, err.Error(), http.StatusInternalServerError)
		return
	}

	rw.WriteHeader(http.StatusOK)
	rw.Write(returnData)
}

func (f *Feedback) GetPreKeyBundle(rw http.ResponseWriter, r *http.Request) {
	f.logger.Info("Endpoint /account/fetch/prekeybundle/{UID:[0-9a-zA-Z]+} hit (GET method)")
	f.logger.Debug("Forwarding request to Account Service")

	url, err := f.configuration.GetServiceURL("accountservice")

	if err != nil {
		f.logger.Error("Cannot get the account service URL from config")
		return
	}

	returnData, err := f.ForwardRequest("http", url, r)
	if err != nil {
		f.logger.Error(err.Error())
		http.Error(rw, err.Error(), http.StatusInternalServerError)
		return
	}
	rw.WriteHeader(http.StatusOK)
	rw.Write(returnData)
}
