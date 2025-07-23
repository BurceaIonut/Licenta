package main

import (
	"accountservice/server"
)

func main() {
	err := server.Init(":8081")
	if err != nil {
		panic(err.Error())
	}

	err = server.Run()
	if err != nil {
		panic(err.Error())
	}
}
