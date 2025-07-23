package websocket

import (
	"encoding/json"
	"fmt"
	"signalingservice/logger"
)

type Pool struct {
	Register   chan *Client
	Unregister chan *Client
	Clients    map[*Client]bool
	Broadcast  chan Message
	logger     logger.ILogger
}

func NewPool(l logger.ILogger) *Pool {
	return &Pool{
		Register:   make(chan *Client),
		Unregister: make(chan *Client),
		Clients:    make(map[*Client]bool),
		Broadcast:  make(chan Message),
		logger:     l,
	}
}

func (pool *Pool) ClientRegistered(c *Client) {
	pool.logger.Debug("Client connected to websocket")
}

func (pool *Pool) ClientUnregistered(c *Client) {
	pool.logger.Debug("Client disconnected from websocket")

}

func (pool *Pool) MessageReceived(message Message) {
	pool.logger.Info("Message received on the websocket")
	setUIDData := &SetAccountUIDMessage{}
	err := json.Unmarshal([]byte(message.Body), &setUIDData)
	var isSetId bool = true
	if err != nil {
		pool.logger.Error(err.Error())
		isSetId = false
	}

	pool.logger.Info(setUIDData)
	if isSetId && setUIDData.SetUID != "" {
		pool.logger.Info("Message is SetAccountID Message, AccountID = ", setUIDData.SetUID)
		for client := range pool.Clients {
			if client == message.C {
				client.UID = setUIDData.SetUID
				err = client.Conn.WriteJSON(ResponseMessage{Type: 1, Body: "Account ID has been set"})
				if err != nil {
					pool.logger.Error(err.Error())
				}
			}
		}

		return
	}
}

func (pool *Pool) Start() {
	for {
		select {
		case client := <-pool.Register:
			pool.Clients[client] = true
			fmt.Println("Size of Connection Pool: ", len(pool.Clients))
			for client := range pool.Clients {
				fmt.Println(client)
				client.Conn.WriteJSON(Message{Type: 1, Body: "New User Joined..."})
			}
			pool.ClientRegistered(client)
		case client := <-pool.Unregister:
			for client := range pool.Clients {
				client.Conn.WriteJSON(Message{Type: 1, Body: "User Disconnected..."})
			}
			pool.ClientUnregistered(client)
			delete(pool.Clients, client)
			fmt.Println("Size of Connection Pool: ", len(pool.Clients))
		case message := <-pool.Broadcast:
			pool.MessageReceived(message)
		}
	}
}
