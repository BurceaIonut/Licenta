package handlers

import (
	"chatservice/database"
	"chatservice/logger"
	"chatservice/websocket"
	"net/http"
)

type Chat struct {
	dbConn database.IConnection
	logger logger.ILogger
}

func NewChat(dbConn database.IConnection, logger logger.ILogger) *Chat {
	return &Chat{dbConn: dbConn, logger: logger}
}

func (ch *Chat) ServeWs(pool *websocket.Pool, rw http.ResponseWriter, r *http.Request) {
	ch.logger.Info("Endpoint /ws hit")
	ws, err := websocket.Upgrade(rw, r)
	if err != nil {
		ch.logger.Error(err.Error())
		return
	}

	client := &websocket.Client{
		UID:  "",
		Conn: ws,
		Pool: pool,
	}

	pool.Register <- client
	client.Read()
}
