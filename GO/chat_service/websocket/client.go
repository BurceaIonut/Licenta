package websocket

import (
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
	SetUID    string `json:"setAccountUID"`
	Signature string `json:"signature"`
}

type UpdateProfilePicture struct {
	UID      string `json:"UID"`
	NewPhoto string `json:"newPhoto"`
}

type PrivateMessage struct {
	FullName    string                 `json:"fullName"`
	UID         string                 `json:"UID"`
	ToUID       string                 `json:"toUID"`
	Data        map[string]interface{} `json:"data"`
	MessageType string                 `json:"messageType"`
	Timestamp   string                 `json:"timestamp"`
}

type PrivateMessageResponse struct {
	FullName      string      `json:"fullName"`
	NormalMessage interface{} `json:"normalMessage"`
	MessageType   string      `json:"messageType"`
	SenderUID     string      `json:"senderUID"`
	Timestamp     string      `json:"timestamp"`
}

type InitialMessageResponse struct {
	Type           string      `json:"type"`
	SenderUID      string      `json:"senderUID"`
	FullName       string      `json:"fullName"`
	Timestamp      string      `json:"timestamp"`
	InitialMessage interface{} `json:"initialMessage"`
}

type ValidateQRCodeRequest struct {
	UID1      string `json:"uid1"`
	UID2      string `json:"uid2"`
	Signature string `json:"signature"`
}

type ValidateQRCodeResponse struct {
	UID1       string `json:"uid1"`
	UID2       string `json:"uid2"`
	Signature1 string `json:"signature1"`
	Signature2 string `json:"signature2"`
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
	}
}
