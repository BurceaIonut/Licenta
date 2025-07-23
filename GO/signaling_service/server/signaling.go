package server

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"
	"strings"
	"text/template"

	"github.com/gorilla/websocket"
)

var AllRooms RoomMap

var upgrader = websocket.Upgrader{
	ReadBufferSize:  4096,
	WriteBufferSize: 4096,
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

type broadcastMsg struct {
	Message string
	RoomID  int64
	Client  *websocket.Conn
}

type groupBroadcastMsg struct {
	Message     string
	RoomID      int64
	Client      *websocket.Conn
	DisplayName string
}

var broadcast = make(chan broadcastMsg)
var groupBroadcast = make(chan groupBroadcastMsg)

type response struct {
	AccountUID string `json:"accountUID"`
	Content    string `json:"content"`
}

type responsePrivate struct {
	Content string `json:"content"`
}

func RemoveIndex(s []Participant, index int) []Participant {
	return append(s[:index], s[index+1:]...)
}

func broadcaster() {
	for {
		msg := <-broadcast
		log.Println(msg.Message)
		if strings.Contains(msg.Message, "create or join") {
			newResp := responsePrivate{}
			_ = json.Unmarshal([]byte(msg.Message), &newResp)
			if _, ok := AllRooms.Map[msg.RoomID]; ok {
				for _, client := range AllRooms.Map[msg.RoomID] {
					if client.Conn != msg.Client {
						resp := responsePrivate{Content: "join"}
						AllRooms.Mutex.Lock()
						err := client.Conn.WriteJSON(resp)
						AllRooms.Mutex.Unlock()

						if err != nil {
							log.Println(err.Error())
							client.Conn.Close()
						}
					}
				}

				log.Println("Here!")
				resp := responsePrivate{Content: "joined"}
				AllRooms.InsertIntoRoom(msg.RoomID, false, msg.Client)
				msg.Client.WriteJSON(resp)
			} else {
				resp := responsePrivate{Content: "created"}
				AllRooms.InsertIntoRoom(msg.RoomID, false, msg.Client)
				msg.Client.WriteJSON(resp)
			}

			continue
		} else {
			if strings.Contains(msg.Message, "bye") {
				newResp := response{}
				_ = json.Unmarshal([]byte(msg.Message), &newResp)
				i := -1
				for index, client := range AllRooms.Map[msg.RoomID] {
					if client.Conn == msg.Client {
						i = index
						break
					}
				}

				log.Println(i)
				AllRooms.Mutex.Lock()
				if i != -1 {
					AllRooms.Map[msg.RoomID] = RemoveIndex(AllRooms.Map[msg.RoomID], i)
				}
				log.Println(AllRooms.Map[msg.RoomID])
				AllRooms.DeleteRoom(msg.RoomID)
				AllRooms.Mutex.Unlock()
			} else {
				for _, client := range AllRooms.Map[msg.RoomID] {
					if client.Conn != msg.Client {
						AllRooms.Mutex.Lock()
						err := client.Conn.WriteMessage(1, []byte(msg.Message))
						AllRooms.Mutex.Unlock()

						if err != nil {
							log.Println(err.Error())
							client.Conn.Close()
						}
					}
				}
			}
		}
	}
}

func JoinRoomRequestHandler(rw http.ResponseWriter, r *http.Request) {
	rw.Header().Set("Access-Control-Allow-Origin", "*")
	roomID, ok := r.URL.Query()["roomID"]
	if !ok {
		log.Println("Room id missing in URL query")
		rw.WriteHeader(http.StatusBadRequest)
		rw.Write([]byte("RoomId needs to be specified in the GET parameters"))
		return
	}

	intRoomId, _ := strconv.Atoi(roomID[0])

	ws, err := upgrader.Upgrade(rw, r, nil)
	if err != nil {
		log.Println("Web socket upgrade error", err.Error())
		return
	}

	go broadcaster()

	for {
		var msg broadcastMsg
		msgType, data, err := ws.ReadMessage()
		log.Println(msgType)
		if msgType == -1 {
			i := -1
			for id, participants := range AllRooms.Map {
				for index, participant := range participants {
					if participant.Conn == ws {
						i = index
						break
					}
				}
				if i != -1 {
					AllRooms.Mutex.Lock()
					if len(participants) == 0 {
						AllRooms.DeleteRoom(id)
					}
					AllRooms.Mutex.Unlock()
					if i != -1 {
						break
					}
				}
			}
			return
		}
		if err != nil {
			log.Println("Read error", err.Error())
			return
		}
		msg.Message = string(data)
		msg.Client = ws
		msg.RoomID = int64(intRoomId)

		broadcast <- msg
	}
}

func groupBroadcaster() {
	for {
		msg := <-groupBroadcast
		log.Println(msg.Message)
		newResp := response{}
		_ = json.Unmarshal([]byte(msg.Message), &newResp)
		if strings.Contains(msg.Message, "create or join") {
			if _, ok := AllRooms.Map[msg.RoomID]; ok {
				log.Println(AllRooms.Map[msg.RoomID])
				for _, client := range AllRooms.Map[msg.RoomID] {
					if client.Conn != msg.Client {
						resp := response{AccountUID: newResp.AccountUID, Content: "join"}
						AllRooms.Mutex.Lock()
						err := client.Conn.WriteJSON(resp)
						AllRooms.Mutex.Unlock()

						if err != nil {
							log.Println(err.Error())
							client.Conn.Close()
						}
					}
				}

				log.Println("Here!")
				resp := response{AccountUID: newResp.AccountUID, Content: "joined"}
				AllRooms.InsertIntoRoom(msg.RoomID, false, msg.Client)
				msg.Client.WriteJSON(resp)
				return
			} else {
				resp := response{Content: "created"}
				AllRooms.InsertIntoRoom(msg.RoomID, false, msg.Client)
				msg.Client.WriteJSON(resp)
			}

			continue
		} else {
			if strings.Contains(msg.Message, "bye") {
				i := -1
				for index, client := range AllRooms.Map[msg.RoomID] {
					if client.Conn == msg.Client {
						i = index
						break
					}
				}

				log.Println(i)
				AllRooms.Mutex.Lock()
				if i != -1 {
					AllRooms.Map[msg.RoomID] = RemoveIndex(AllRooms.Map[msg.RoomID], i)
				}
				log.Println(AllRooms.Map[msg.RoomID])
				AllRooms.DeleteRoom(msg.RoomID)
				AllRooms.Mutex.Unlock()
			} else {
				for _, client := range AllRooms.Map[msg.RoomID] {
					if client.Conn != msg.Client {
						AllRooms.Mutex.Lock()
						err := client.Conn.WriteMessage(1, []byte(msg.Message))
						AllRooms.Mutex.Unlock()

						if err != nil {
							log.Println(err.Error())
							client.Conn.Close()
						}
					}
				}
			}
		}
	}
}

func JoinGroupRequestHandler(rw http.ResponseWriter, r *http.Request) {
	log.Println("/joingroup endpoint hit")
	rw.Header().Set("Access-Control-Allow-Origin", "*")
	roomID, ok := r.URL.Query()["roomID"]
	if !ok {
		log.Println("Room id missing in URL query")
		rw.WriteHeader(http.StatusBadRequest)
		rw.Write([]byte("RoomId needs to be specified in the GET parameters"))
		return
	}

	intRoomId, _ := strconv.Atoi(roomID[0])

	ws, err := upgrader.Upgrade(rw, r, nil)
	if err != nil {
		log.Println("Web socket upgrade error", err.Error())
		return
	}

	go groupBroadcaster()

	for {
		var msg groupBroadcastMsg
		msgType, data, err := ws.ReadMessage()
		log.Println(msgType)
		if msgType == -1 {
			i := -1
			for id, participants := range AllRooms.Map {
				for index, participant := range participants {
					if participant.Conn == ws {
						i = index
						break
					}
				}
				if i != -1 {
					AllRooms.Mutex.Lock()
					if len(participants) == 0 {
						AllRooms.DeleteRoom(id)
					}
					AllRooms.Mutex.Unlock()
					if i != -1 {
						break
					}
				}
			}
			return
		}
		if err != nil {
			log.Println("Read error", err.Error())
			return
		}
		msg.Message = string(data)
		msg.Client = ws
		msg.RoomID = int64(intRoomId)
		msg.DisplayName = "Test"

		groupBroadcast <- msg
	}
}

func RoomRequestHandler(rw http.ResponseWriter, r *http.Request) {
	log.Println("/room endpoint hit")
	rw.Header().Set("Access-Control-Allow-Origin", "*")
	roomID, ok := r.URL.Query()["roomID"]
	if !ok {
		log.Println("Room id missing in URL query")
		rw.WriteHeader(http.StatusBadRequest)
		rw.Write([]byte("RoomId needs to be specified in the GET parameters"))
		return
	}
	log.Println("Room id = ", roomID)

	audio, ok := r.URL.Query()["audio"]
	if !ok {
		log.Println("Audio missing in URL query")
		rw.WriteHeader(http.StatusBadRequest)
		rw.Write([]byte("audio needs to be specified in the GET parameters"))
		return
	}

	video, ok := r.URL.Query()["video"]
	if !ok {
		log.Println("Video missing in URL query")
		rw.WriteHeader(http.StatusBadRequest)
		rw.Write([]byte("video needs to be specified in the GET parameters"))
		return
	}

	platform, ok := r.URL.Query()["platform"]
	if !ok {
		log.Println("Platform missing in URL query")
		rw.WriteHeader(http.StatusBadRequest)
		rw.Write([]byte("platform needs to be specified in the GET parameters"))
		return
	}

	audioEnabled, err := strconv.ParseBool(audio[0])
	if err != nil {
		log.Println("Cannot parse audio value")
		rw.WriteHeader(http.StatusInternalServerError)
		rw.Write([]byte("Cannot parse audio value"))
		return
	}

	videoEnabled, err := strconv.ParseBool(video[0])
	if err != nil {
		log.Println("Cannot parse video value")
		rw.WriteHeader(http.StatusInternalServerError)
		rw.Write([]byte("Cannot parse video value"))
		return
	}

	type room struct {
		RoomID       string
		AudioEnabled bool
		VideoEnabled bool
		Platform     string
	}

	t, err := template.ParseFiles("./templates/index.html")
	if err != nil {
		log.Println("Cannot parse template", err.Error())
		rw.WriteHeader(http.StatusInternalServerError)
		rw.Write([]byte(err.Error()))
		return
	}
	rw.WriteHeader(http.StatusOK)
	t.Execute(rw, room{RoomID: roomID[0], AudioEnabled: audioEnabled, VideoEnabled: videoEnabled, Platform: platform[0]})
}

func GroupRequestHandler(rw http.ResponseWriter, r *http.Request) {
	log.Println("/group endpoint hit")
	rw.Header().Set("Access-Control-Allow-Origin", "*")
	roomID, ok := r.URL.Query()["roomID"]
	if !ok {
		log.Println("Room id missing in URL query")
		rw.WriteHeader(http.StatusBadRequest)
		rw.Write([]byte("RoomId needs to be specified in the GET parameters"))
		return
	}
	log.Println("Room id = ", roomID)

	accountId, ok := r.URL.Query()["accountID"]
	if !ok {
		log.Println("account id missing in URL query")
		rw.WriteHeader(http.StatusBadRequest)
		rw.Write([]byte("accountID needs to be specified in the GET parameters"))
		return
	}
	log.Println("Account id = ", accountId)

	audio, ok := r.URL.Query()["audio"]
	if !ok {
		log.Println("Audio missing in URL query")
		rw.WriteHeader(http.StatusBadRequest)
		rw.Write([]byte("audio needs to be specified in the GET parameters"))
		return
	}

	video, ok := r.URL.Query()["video"]
	if !ok {
		log.Println("Video missing in URL query")
		rw.WriteHeader(http.StatusBadRequest)
		rw.Write([]byte("video needs to be specified in the GET parameters"))
		return
	}

	platform, ok := r.URL.Query()["platform"]
	if !ok {
		log.Println("Platform missing in URL query")
		rw.WriteHeader(http.StatusBadRequest)
		rw.Write([]byte("platform needs to be specified in the GET parameters"))
		return
	}

	audioEnabled, err := strconv.ParseBool(audio[0])
	if err != nil {
		log.Println("Cannot parse audio value")
		rw.WriteHeader(http.StatusInternalServerError)
		rw.Write([]byte("Cannot parse audio value"))
		return
	}

	videoEnabled, err := strconv.ParseBool(video[0])
	if err != nil {
		log.Println("Cannot parse video value")
		rw.WriteHeader(http.StatusInternalServerError)
		rw.Write([]byte("Cannot parse video value"))
		return
	}

	type room struct {
		RoomID       string
		AccountID    string
		AudioEnabled bool
		VideoEnabled bool
		Platform     string
	}

	t, err := template.ParseFiles("./templates/group.html")
	if err != nil {
		log.Println("Cannot parse template", err.Error())
		rw.WriteHeader(http.StatusInternalServerError)
		rw.Write([]byte(err.Error()))
		return
	}
	rw.WriteHeader(http.StatusOK)
	t.Execute(rw, room{RoomID: roomID[0], AccountID: accountId[0], AudioEnabled: audioEnabled, VideoEnabled: videoEnabled, Platform: platform[0]})
}
