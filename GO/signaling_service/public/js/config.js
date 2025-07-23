turnConfig = {
    iceServers: [
        { 
            urls: ["stun:fr-turn1.xirsys.com"] 
        },
        { 
            username: "mmG4Vx4Mv-Lvw8Oic-5HVebCy1rcGCj-SSrDv6dQ1McxyNiRjMCKt-mGz-RrmlmsAAAAAGeVLnBpYnVyY2Vh", 
            credential: "da23edb6-db4a-11ef-8d07-0242ac120004", 
            urls: ["turn:fr-turn1.xirsys.com:80?transport=udp", 
				"turn:fr-turn1.xirsys.com:3478?transport=udp", 
				"turn:fr-turn1.xirsys.com:80?transport=tcp", 
				"turn:fr-turn1.xirsys.com:3478?transport=tcp", 
				"turns:fr-turn1.xirsys.com:443?transport=tcp", 
				"turns:fr-turn1.xirsys.com:5349?transport=tcp"]
        }
    ],
}

turnReceiverConfig = {
    encodedInsertableStreams: true,
    iceServers: [
        { 
            urls: ["stun:fr-turn1.xirsys.com"] 
        },
        { 
            username: "mmG4Vx4Mv-Lvw8Oic-5HVebCy1rcGCj-SSrDv6dQ1McxyNiRjMCKt-mGz-RrmlmsAAAAAGeVLnBpYnVyY2Vh", 
            credential: "da23edb6-db4a-11ef-8d07-0242ac120004", 
            urls: ["turn:fr-turn1.xirsys.com:80?transport=udp", 
				"turn:fr-turn1.xirsys.com:3478?transport=udp", 
				"turn:fr-turn1.xirsys.com:80?transport=tcp", 
				"turn:fr-turn1.xirsys.com:3478?transport=tcp", 
				"turns:fr-turn1.xirsys.com:443?transport=tcp", 
				"turns:fr-turn1.xirsys.com:5349?transport=tcp"]
        }
    ],
}