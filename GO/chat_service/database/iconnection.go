package database

import "chatservice/data"

type IConnection interface {
	InitializeConnection() error
	TestConnection() error
	CloseConnection() error
	InsertPendingMessage(toUID, fromUID, fullName, timestamp, msgType string, data []byte) error
	GetPendingMessages(toUID string) ([]data.PendingMessage, error)
	DeletePendingMessages(toUID string) error
	UIDExists(uid string) (bool, error)
	InsertQRCodeValidation(uid1, uid2, signature1 string) error
	GetQRCodeValidationSignature1(uid1, uid2 string) (string, error)
	DeleteQRCodeValidation(uid1, uid2 string) error
	UpdateQRCodeValidationSignature2(uid1, uid2, signature2 string) error
}
