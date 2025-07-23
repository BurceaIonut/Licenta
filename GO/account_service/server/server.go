package server

import (
	"accountservice/data"
	"accountservice/database"
	"accountservice/handlers"
	"accountservice/logger"
	"context"
	"errors"
	"log"
	"net/http"
	"os"
	"os/signal"
	"time"

	"github.com/gorilla/mux"
)

var server *http.Server = nil
var serverLogger logger.ILogger = nil
var serverDbConn *database.MYSQLConnection = nil
var configurationFile string = "account.conf"

func Init(port string) error {
	if server != nil || serverLogger != nil {
		return errors.New("Server already running!")
	}

	file, err := os.OpenFile(configurationFile, os.O_RDWR, 0644)
	if err != nil {
		return err
	}
	defer file.Close()

	configuration := data.Configuration{}
	err = configuration.FromJSON(file)
	if err != nil {
		log.Println("Config file error!")
		return err
	}

	serverLogger = logger.NewDebugLogger(log.New(os.Stdout, "[*] - Account Service - ", log.LstdFlags), "[INFO]", "[WARNING]", "[ERROR]", "[DEBUG]")
	serverLogger.Info("Logger running")

	serverDbConn = database.NewConnection(serverLogger, configuration)
	err = serverDbConn.MYSQLConnect()
	if err != nil {
		serverLogger.Info("Database connection initialization failed: " + err.Error())
		return err
	}

	handlerRegister := handlers.NewRegister(serverLogger, serverDbConn, configuration)
	handlerProfile := handlers.NewProfile(serverLogger, serverDbConn, configuration)

	serveMuxServer := mux.NewRouter()

	serveMuxServer.HandleFunc("/register", handlerRegister.RegisterAccount).Methods("POST")
	serveMuxServer.HandleFunc("/account/fetch/prekeybundle/{UID:[0-9a-zA-Z]+}", handlerProfile.GetPreKeyBundle).Methods("GET")
	serveMuxServer.HandleFunc("/account/fetch/identitykey/{UID:[0-9a-zA-Z]+}", handlerProfile.FetchIdentityPublicKey).Methods("GET")

	serverLogger.Info("Handlers added to the server mutex of the account service")

	server = &http.Server{
		Addr:         port,
		Handler:      serveMuxServer,
		ReadTimeout:  60 * time.Second,
		WriteTimeout: 60 * time.Second,
		IdleTimeout:  120 * time.Second,
	}

	serverLogger.Info("Account Service initialized")
	return nil
}

func Run() error {
	if server == nil || serverLogger == nil {
		return errors.New("Account Service not initialized")
	}

	serverLogger.Info("Testing the connection with the database")

	errDb := serverDbConn.MYSQLTest()
	if errDb != nil {
		serverLogger.Error(errDb.Error())
		return errors.New("Account Service cannot connect to the database")
	}

	defer serverDbConn.MYSQLClose()

	serverLogger.Info("Database connection has been established")

	go func() {
		err := server.ListenAndServe()
		if err != nil {
			serverLogger.Error(err)
		}
	}()

	chanSig := make(chan os.Signal)

	signal.Notify(chanSig, os.Kill)
	signal.Notify(chanSig, os.Interrupt)

	sig := <-chanSig

	serverLogger.Info("Received signal: ", sig)

	tc, _ := context.WithTimeout(context.Background(), 30*time.Second)
	server.Shutdown(tc)

	return nil
}
