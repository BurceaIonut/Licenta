package server

import (
	"log"
	"sync"

	"github.com/gorilla/websocket"
)

type Participant struct {
	Host bool
	Conn *websocket.Conn
}

type RoomMap struct {
	Mutex sync.RWMutex
	Map   map[int64][]Participant
}

func (r *RoomMap) Init() {
	r.Map = make(map[int64][]Participant, 0)
}

func (r *RoomMap) Get(roomID int64) []Participant {
	r.Mutex.Lock()
	defer r.Mutex.Unlock()

	return r.Map[roomID]
}

func (r *RoomMap) InsertIntoRoom(roomId int64, host bool, conn *websocket.Conn) {
	r.Mutex.Lock()
	defer r.Mutex.Unlock()

	p := Participant{host, conn}
	log.Println("Inserting into Room with roomID: ", roomId)
	r.Map[roomId] = append(r.Map[roomId], p)
	log.Println(r.Map[roomId])

}

func (r *RoomMap) DeleteRoom(roomID int64) {
	delete(r.Map, roomID)
}
