package main

import (
	"log"
	"net/http"
	"os"
	"signalingservice/logger"
	"signalingservice/server"
	"signalingservice/websocket"

	"github.com/joho/godotenv"
)

func main() {
	server.AllRooms.Init()
	serverLogger := logger.NewDebugLogger(log.New(os.Stdout, "[*] - Signaling Service - ", log.LstdFlags), "[INFO]", "[WARNING]", "[ERROR]", "[DEBUG]")

	pool := websocket.NewPool(serverLogger)
	go pool.Start()
	not := server.NewNotifications(serverLogger)

	http.Handle("/", http.FileServer(http.Dir("./public")))
	http.Handle("/join", http.HandlerFunc(server.JoinRoomRequestHandler))
	http.Handle("/joingroup", http.HandlerFunc(server.JoinGroupRequestHandler))
	http.Handle("/room", http.HandlerFunc(server.RoomRequestHandler))
	http.Handle("/group", http.HandlerFunc(server.GroupRequestHandler))
	http.Handle("/notifications", http.HandlerFunc(func(rw http.ResponseWriter, r *http.Request) {
		not.NotificationsHandler(pool, rw, r)
	}))

	log.Println("Starting server on port 8090")

	err := godotenv.Load(".env")
	if err != nil {
		panic(err.Error())
	}

	certFile := os.Getenv("CERT_FILE")
	keyFile := os.Getenv("KEY_FILE")

	err = http.ListenAndServeTLS(":8090", certFile, keyFile, nil)
	if err != nil {
		log.Fatal(err.Error())
	}
}
