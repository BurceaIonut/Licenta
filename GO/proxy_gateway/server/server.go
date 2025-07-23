package server

import (
	"context"
	"errors"
	"log"
	"net/http"
	"os"
	"os/signal"
	"proxygateway/data"
	handlers "proxygateway/handlers"
	"proxygateway/logger"
	"time"

	"github.com/gorilla/mux"
	"github.com/joho/godotenv"
)

var serverLogger logger.ILogger
var server *http.Server
var configFile string = "gateway.conf"

func Init(port string) error {
	if server != nil || serverLogger != nil {
		return errors.New("Server already running!")
	}

	file, err := os.OpenFile(configFile, os.O_RDWR, 0644)
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

	serverLogger = logger.NewLogger(log.New(os.Stdout, "[*] - Gateway - ", log.LstdFlags), "[INFO]", "[WARNING]", "[ERROR]")
	serverLogger.Info("Logger running")

	handlerRegister := handlers.NewAccountRegister(serverLogger, configuration)
	handlerFeedback := handlers.NewFeedback(serverLogger, configuration)

	serveMux := mux.NewRouter()

	postRouter := serveMux.Methods(http.MethodPost).Subrouter()
	postRouter.HandleFunc("/account/register", handlerRegister.RegisterAccount)
	postRouter.HandleFunc("/account/update/status", handlerFeedback.UpdateStatus)
	postRouter.HandleFunc("/account/update/picture", handlerFeedback.UpdateProfilePicture)

	getRouter := serveMux.Methods(http.MethodGet).Subrouter()
	getRouter.PathPrefix("/account/static/").HandlerFunc(handlerFeedback.GetProfilePicture)
	getRouter.HandleFunc("/account/profile/{UID:[0-9A-Za-z]+}", handlerFeedback.GetProfile)
	getRouter.HandleFunc("/account/fetch/prekeybundle/{UID:[0-9a-zA-Z]+}", handlerFeedback.GetPreKeyBundle)

	server = &http.Server{
		Addr:         port,
		Handler:      serveMux,
		ReadTimeout:  60 * time.Second,
		WriteTimeout: 60 * time.Second,
		IdleTimeout:  30 * time.Second,
	}

	serverLogger.Info("Proxy Gateway initialized")
	return nil
}

func Run() error {
	if server == nil || serverLogger == nil {
		return errors.New("Proxy Gateway not initialised!")
	}

	err := godotenv.Load(".env")
	if err != nil {
		panic(err.Error())
	}

	certFile := os.Getenv("CERT_FILE")
	keyFile := os.Getenv("KEY_FILE")

	go func() {
		err := server.ListenAndServeTLS(certFile, keyFile)
		if err != nil {
			serverLogger.Error(err.Error())
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
