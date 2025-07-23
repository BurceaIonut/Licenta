package server

import (
	"net/http"
	"signalingservice/logger"
	"signalingservice/websocket"
)

type Notifications struct {
	logger logger.ILogger
}

func NewNotifications(l logger.ILogger) *Notifications {
	return &Notifications{logger: l}
}

func (not *Notifications) NotificationsHandler(pool *websocket.Pool, rw http.ResponseWriter, r *http.Request) {
	not.logger.Info("Endpoint /notifications hit")
	ws, err := websocket.Upgrade(rw, r)
	if err != nil {
		not.logger.Error(err.Error())
		return
	}

	client := &websocket.Client{
		Conn: ws,
		Pool: pool,
		UID:  "",
	}

	pool.Register <- client
	client.Read()
}
