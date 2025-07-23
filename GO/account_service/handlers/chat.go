package handlers

import (
	"accountservice/data"
	"accountservice/database"
	"accountservice/logger"
	"bytes"
	"fmt"
	"io/ioutil"
	"net/http"
)

type Chat struct {
	logger        logger.ILogger
	dbConn        *database.MYSQLConnection
	configuration data.Configuration
}

func NewChat(logger logger.ILogger, dbConn *database.MYSQLConnection, conf data.Configuration) *Chat {
	return &Chat{logger: logger, dbConn: dbConn, configuration: conf}
}

func (chat *Chat) ForwardRequest(proxyProtocol string, proxyHost string, r *http.Request) ([]byte, error) {
	body, err := ioutil.ReadAll(r.Body)
	if err != nil {
		return nil, err
	}

	r.Body = ioutil.NopCloser(bytes.NewReader(body))

	url := fmt.Sprintf("%s://%s%s", proxyProtocol, proxyHost, r.RequestURI)

	proxyReq, err := http.NewRequest(r.Method, url, bytes.NewReader(body))

	proxyReq.Header = make(http.Header)
	for h, val := range r.Header {
		proxyReq.Header[h] = val
	}
	chat.logger.Debug(proxyReq.Header)

	httpClient := &http.Client{}
	resp, err := httpClient.Do(proxyReq)
	if err != nil {
		return nil, err
	}
	body2, err := ioutil.ReadAll(resp.Body)
	chat.logger.Debug(resp.Status)
	defer resp.Body.Close()
	return body2, nil
}
