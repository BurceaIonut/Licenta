package main

import (
	"proxygateway/server"
)

func main() {
	err := server.Init(":8443")
	if err != nil {
		panic(err.Error())
	}

	err = server.Run()
	if err != nil {
		panic(err.Error())
	}
}
