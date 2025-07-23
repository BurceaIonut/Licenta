package websocket

import (
	"chatservice/data"
	"chatservice/database"
	"chatservice/logger"
	"chatservice/utils"
	"encoding/json"
	"fmt"
)

type Pool struct {
	Register      chan *Client
	Unregister    chan *Client
	Clients       map[*Client]bool
	Broadcast     chan Message
	logger        logger.ILogger
	dbConn        database.IConnection
	configuration data.Configuration
}

func NewPool(l logger.ILogger, dbConn database.IConnection, conf data.Configuration) *Pool {
	return &Pool{
		Register:      make(chan *Client),
		Unregister:    make(chan *Client),
		Clients:       make(map[*Client]bool),
		Broadcast:     make(chan Message),
		logger:        l,
		dbConn:        dbConn,
		configuration: conf,
	}
}

func (pool *Pool) ClientRegistered(c *Client) {
	pool.logger.Debug("Client connected to websocket")
	messages, err := pool.dbConn.GetPendingMessages(c.UID)
	if err != nil {
		pool.logger.Error("Failed to load pending messages for ", c.UID, ": ", err.Error())
		return
	}

	for _, msg := range messages {
		var payload map[string]interface{}
		json.Unmarshal(msg.Data, &payload)

		if initialRaw, ok := payload["initialMessage"]; ok {
			c.Conn.WriteJSON(InitialMessageResponse{
				Type:           msg.MessageType,
				SenderUID:      msg.FromUID,
				FullName:       msg.FullName,
				Timestamp:      msg.Timestamp,
				InitialMessage: initialRaw,
			})
		} else {
			c.Conn.WriteJSON(PrivateMessageResponse{
				NormalMessage: payload["normalMessage"],
				MessageType:   msg.MessageType,
				SenderUID:     msg.FromUID,
				Timestamp:     msg.Timestamp,
				FullName:      msg.FullName,
			})
		}
	}

	pool.dbConn.DeletePendingMessages(c.UID)
}

func (pool *Pool) ClientUnregistered(c *Client) {
	pool.logger.Debug("Client disconnected from websocket")
}

func (pool *Pool) MessageReceived(message Message) {
	pool.logger.Info("Message received on the websocket", message.Body)

	setUIDData := SetAccountUIDMessage{}

	if err := json.Unmarshal([]byte(message.Body), &setUIDData); err == nil && setUIDData.SetUID != "" {
		pool.logger.Info("Message is SetAccountUID Message, AccountUID = ", setUIDData.SetUID)

		exists, err := pool.dbConn.UIDExists(setUIDData.SetUID)
		if err != nil {
			pool.logger.Error("Database error while checking UID:", err.Error())
			return
		}
		if !exists {
			err := message.C.Conn.WriteJSON(ResponseMessage{
				Type: 3,
				Body: "UID does not exist in the system.",
			})
			if err != nil {
				pool.logger.Error(err.Error())
			}
			return
		}

		accountServiceURL, err := pool.configuration.GetServiceURL("accountservice")
		identityKey, err := utils.FetchIdentityPublicKey(accountServiceURL, setUIDData.SetUID)

		if err != nil {
			pool.logger.Warning("Could not fetch public key for UID:", err.Error())
			_ = message.C.Conn.WriteJSON(ResponseMessage{
				Type: 3,
				Body: "Account not found in system.",
			})
			return
		}

		err = utils.VerifySignature(setUIDData.SetUID, setUIDData.Signature, identityKey)
		if err != nil {
			pool.logger.Error("Signature verification failed:", err.Error())
			_ = message.C.Conn.WriteJSON(ResponseMessage{
				Type: 4,
				Body: "Signature verification failed.",
			})
			return
		} else {
			pool.logger.Info("Signature verified successfully for UID:", setUIDData.SetUID)
		}

		for client := range pool.Clients {
			if client != message.C && client.UID == setUIDData.SetUID {
				pool.logger.Warning("UID already in use by another client:", setUIDData.SetUID)
				err := message.C.Conn.WriteJSON(ResponseMessage{
					Type: 2,
					Body: "This UID is already in use by another connection.",
				})
				if err != nil {
					pool.logger.Error(err.Error())
				}
				return
			}
		}

		message.C.UID = setUIDData.SetUID

		err = message.C.Conn.WriteJSON(ResponseMessage{
			Type: 1,
			Body: "UID has been set",
		})
		if err != nil {
			pool.logger.Error(err.Error())
		}
		pool.ClientRegistered(message.C)
		return
	}

	qrCodeValidationMsg := ValidateQRCodeRequest{}

	if err := json.Unmarshal([]byte(message.Body), &qrCodeValidationMsg); err == nil && qrCodeValidationMsg.UID1 != "" && qrCodeValidationMsg.UID2 != "" {
		pool.logger.Info("Message is ValidateQRCode Message, UID1 = ", qrCodeValidationMsg.UID1, ", UID2 = ", qrCodeValidationMsg.UID2)

		exists1, err := pool.dbConn.UIDExists(qrCodeValidationMsg.UID1)
		if err != nil {
			pool.logger.Error("Database error while checking UID:", err.Error())
			return
		}
		exists2, err := pool.dbConn.UIDExists(qrCodeValidationMsg.UID2)
		if err != nil {
			pool.logger.Error("Database error while checking UID:", err.Error())
			return
		}
		if !exists1 || !exists2 {
			err := message.C.Conn.WriteJSON(ResponseMessage{
				Type: 5,
				Body: "One or both UIDs do not exist in the system.",
			})
			if err != nil {
				pool.logger.Error(err.Error())
			}
			return
		}

		accountServiceURL, err := pool.configuration.GetServiceURL("accountservice")
		identityKey, err := utils.FetchIdentityPublicKey(accountServiceURL, qrCodeValidationMsg.UID1)

		if err != nil {
			pool.logger.Warning("Could not fetch public key for UID:", err.Error())
			_ = message.C.Conn.WriteJSON(ResponseMessage{
				Type: 3,
				Body: "Account not found in system.",
			})
			return
		}

		err = utils.VerifySignature(qrCodeValidationMsg.UID2, qrCodeValidationMsg.Signature, identityKey)
		if err != nil {
			pool.logger.Error("Signature verification failed:", err.Error())
			_ = message.C.Conn.WriteJSON(ResponseMessage{
				Type: 4,
				Body: "Signature verification failed.",
			})
			return
		} else {
			pool.logger.Info("Signature verified successfully for UID:", qrCodeValidationMsg.UID1)
		}

		signature1FromOther, err := pool.dbConn.GetQRCodeValidationSignature1(qrCodeValidationMsg.UID2, qrCodeValidationMsg.UID1)
		if err == nil {
			err = pool.dbConn.UpdateQRCodeValidationSignature2(qrCodeValidationMsg.UID2, qrCodeValidationMsg.UID1, qrCodeValidationMsg.Signature)
			if err != nil {
				pool.logger.Error("Failed to update QR validation with signature2:", err.Error())
				return
			}

			err = pool.dbConn.DeleteQRCodeValidation(qrCodeValidationMsg.UID2, qrCodeValidationMsg.UID1)
			if err != nil {
				pool.logger.Error("Failed to delete completed QR validation:", err.Error())
				return
			}

			resp := ValidateQRCodeResponse{
				UID1:       qrCodeValidationMsg.UID2,
				UID2:       qrCodeValidationMsg.UID1,
				Signature1: signature1FromOther,
				Signature2: qrCodeValidationMsg.Signature,
			}

			for client := range pool.Clients {
				if client.UID == resp.UID1 || client.UID == resp.UID2 {
					err := client.Conn.WriteJSON(resp)
					if err != nil {
						pool.logger.Error("Failed to send QR validation success to UID "+client.UID+":", err.Error())
					}
				}
			}
			return
		}

		err = pool.dbConn.InsertQRCodeValidation(qrCodeValidationMsg.UID1, qrCodeValidationMsg.UID2, qrCodeValidationMsg.Signature)
		if err != nil {
			pool.logger.Error("Failed to insert QR validation:", err.Error())
			return
		}

	}

	privateMsg := PrivateMessage{}

	if err := json.Unmarshal([]byte(message.Body), &privateMsg); err == nil && privateMsg.ToUID != "" {
		if message.C.UID == "" {
			pool.logger.Warning("Sender has no UID set. Ignoring message.")
			return
		}

		pool.logger.Info("Private message received from", privateMsg.UID, "to", privateMsg.ToUID)
		found := false

		var payload map[string]interface{}
		json.Unmarshal([]byte(message.Body), &payload)

		for client := range pool.Clients {
			if client.UID == privateMsg.ToUID {
				if dataRaw, ok := payload["data"]; ok {
					if dataMap, ok := dataRaw.(map[string]interface{}); ok {
						if initialRaw, ok := dataMap["initialMessage"]; ok {
							pool.logger.Info("Detected embedded initialMessage")
							found = true
							response := InitialMessageResponse{
								Type:           "initial",
								SenderUID:      message.C.UID,
								FullName:       privateMsg.FullName,
								Timestamp:      privateMsg.Timestamp,
								InitialMessage: initialRaw,
							}

							pool.logger.Info("Response to be sent:", response)

							err = client.Conn.WriteJSON(response)
							if err != nil {
								pool.logger.Error("Failed to send initial message to", client.UID, ":", err.Error())
							} else {
								pool.logger.Info("Sent initial message to", client.UID)
							}
							break
						} else if normalMessage, ok := dataMap["normalMessage"]; ok {
							pool.logger.Info("Detected embedded normalMessage")
							found = true
							response := PrivateMessageResponse{
								NormalMessage: normalMessage,
								MessageType:   "normal",
								SenderUID:     message.C.UID,
								Timestamp:     privateMsg.Timestamp,
								FullName:      privateMsg.FullName,
							}

							pool.logger.Info("Response to be sent:", response)

							err = client.Conn.WriteJSON(response)
							if err != nil {
								pool.logger.Error("Failed to send message to", client.UID, ":", err.Error())
							} else {
								pool.logger.Info("Sent message to", client.UID)
							}
							break
						} else {
							pool.logger.Warning("No initialMessage or normalMessage found in data")
						}
						break
					}
				}
			}
		}

		exists, err := pool.dbConn.UIDExists(setUIDData.SetUID)
		if !found && exists && err == nil {
			payloadBytes, _ := json.Marshal(privateMsg.Data)
			err := pool.dbConn.InsertPendingMessage(
				privateMsg.ToUID,
				message.C.UID,
				privateMsg.FullName,
				privateMsg.Timestamp,
				privateMsg.MessageType,
				payloadBytes,
			)
			if err != nil {
				pool.logger.Error("Failed to insert pending message: ", err.Error())
			} else {
				pool.logger.Info("Saved message in pending queue for: ", privateMsg.UID)
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
			for client, _ := range pool.Clients {
				fmt.Println(client)
				client.Conn.WriteJSON(Message{Type: 1, Body: "New User Joined..."})
			}
			//pool.ClientRegistered(client)
			break
		case client := <-pool.Unregister:
			for client, _ := range pool.Clients {
				client.Conn.WriteJSON(Message{Type: 1, Body: "User Disconnected..."})
			}
			pool.ClientUnregistered(client)
			delete(pool.Clients, client)
			fmt.Println("Size of Connection Pool: ", len(pool.Clients))
			break
		case message := <-pool.Broadcast:
			pool.MessageReceived(message)
			break
		}
	}
}
