package websocket

import (
	"fmt"
	"log"

	"github.com/gorilla/websocket"
)

type Client struct {
	UID  string
	Conn *websocket.Conn
	Pool *Pool
}

type Message struct {
	C    *Client
	Type int    `json:"type"`
	Body string `json:"body"`
}

type ResponseMessage struct {
	Type int    `json:"type"`
	Body string `json:"body"`
}

type SetAccountUIDMessage struct {
	SetUID string `json:"setAccountUID"`
}

func (c *Client) Read() {
	defer func() {
		c.Pool.Unregister <- c
		c.Conn.Close()
	}()

	for {
		messageType, p, err := c.Conn.ReadMessage()
		if err != nil {
			log.Println(err)
			return
		}

		message := Message{Type: messageType, Body: string(p), C: c}

		c.Pool.Broadcast <- message

		fmt.Printf("Message Received: %+v\n", message)
	}
}
