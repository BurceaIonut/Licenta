package server

import (
	"chatservice/data"
	"chatservice/database"
	"chatservice/handlers"
	"chatservice/logger"
	"chatservice/websocket"
	"context"
	"errors"
	"log"
	"net/http"
	"os"
	"os/signal"
	"time"

	"github.com/gorilla/mux"
)

var serverLogger logger.ILogger
var server *http.Server = nil
var serverDb database.IConnection
var serverConfiguration *data.Configuration = nil

var configurationFile string = "chat.conf"

func InitServer(address string) error {
	if server != nil {
		return errors.New("cannot initialize the server twice")
	}

	file, err := os.OpenFile(configurationFile, os.O_RDWR, 0644)
	if err != nil {
		return err
	}
	defer file.Close()

	configuration := data.Configuration{}
	err = configuration.FromJSON(file)
	if err != nil {
		log.Println("Cannot initialize the configuration settings from file")
		return err
	}
	serverConfiguration = &configuration

	if configuration.DebugEnabled {
		serverLogger = logger.NewDebugLogger(log.New(os.Stdout, "[*] - Chat Service - ", log.LstdFlags), configuration.InfoPrefix, configuration.WarningPrefix, configuration.ErrorPrefix, configuration.DebugPrefix)
	} else {
		serverLogger = logger.NewLogger(log.New(os.Stdout, "[*] - Chat Service - ", log.LstdFlags), configuration.InfoPrefix, configuration.WarningPrefix, configuration.ErrorPrefix)
	}
	serverLogger.Info("Logger has been initialized")

	serverDb = database.NewConnection(serverLogger, serverConfiguration)
	err = serverDb.InitializeConnection()
	if err != nil {
		return err
	}

	serverLogger.Info("Database connection has been initialized")

	pool := websocket.NewPool(serverLogger, serverDb, *serverConfiguration)
	go pool.Start()

	handlerChat := handlers.NewChat(serverDb, serverLogger)

	serveMux := mux.NewRouter()

	serveMux.HandleFunc("/ws", func(rw http.ResponseWriter, r *http.Request) {
		handlerChat.ServeWs(pool, rw, r)
	})

	serverLogger.Info("Handlers have been added to the serve mux")

	server = &http.Server{
		Addr:         configuration.Address,
		Handler:      serveMux,
		IdleTimeout:  120 * time.Second,
		WriteTimeout: 60 * time.Second,
		ReadTimeout:  60 * time.Second,
	}

	serverLogger.Info("Server finished initialization - Listening on", configuration.Address)
	return nil
}

func RunServer() error {
	if server == nil || serverLogger == nil {
		return errors.New("server need to be initialized before running")
	}

	err := serverDb.TestConnection()
	if err != nil {
		return err
	}
	serverLogger.Info("Service is connected to the database")

	go func() {
		err := server.ListenAndServe()
		if err != nil {
			serverLogger.Error(err.Error())
			os.Exit(-1)
		}
	}()

	chanSig := make(chan os.Signal)

	signal.Notify(chanSig, os.Kill)
	signal.Notify(chanSig, os.Interrupt)

	sig := <-chanSig

	serverLogger.Info("Received signal to terminate, exiting gracefully", sig)

	tc, _ := context.WithTimeout(context.Background(), 30*time.Second)
	server.Shutdown(tc)

	return nil
}
