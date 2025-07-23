'use strict';

var isChannelReady = false;
var isInitiator = false;
var isStarted = false;
var localStream;
var pc;
var remoteStream = null;
var turnReady;

const worker = new Worker('js/worker.js');

if ('MediaStreamTrackProcessor' in window && 'MediaStreamTrackGenerator' in window) {
    console.log("Support insertable streams", true);
}

const transformerSender = new TransformStream({
    async transform(encodedFrame, controller) {

        const newData = new ArrayBuffer(encodedFrame.allocationSize());
        encodedFrame.copyTo(newData);

        const newView = new DataView(newData);

        const xor_value = 0x4b;
        for (let i = 0; i < encodedFrame.allocationSize(); ++i)
             newView.setInt8(i, newView.getInt8(i) ^ xor_value);

        encodedFrame.data = newData;
        controller.enqueue(encodedFrame);
    }
});

const transformerReceiver = new TransformStream({
    async transform(encodedFrame, controller) {
        const newData = new ArrayBuffer(encodedFrame.allocationSize());
        encodedFrame.copyTo(newData);

        const newView = new DataView(newData);

        const xor_value = 0x4b;
        for (let i = 0; i < encodedFrame.allocationSize(); ++i)
            newView.setInt8(i, newView.getInt8(i) ^ xor_value);
        
        encodedFrame.data = newData;
        controller.enqueue(encodedFrame);
    }
})

var audio = document.querySelector("#audioEnabled");
var video = document.querySelector("#videoEnabled");

var roomID = document.querySelector("#roomID");
roomID = roomID.innerHTML;

var platform = document.querySelector("#platform");
platform = platform.innerHTML;

var remoteStreamCopy = null;
var currentFrameRate = 0;
var localStreamCopy = null;
var localCurrentFrameRate = 0;

window.onload = function() {
    console.log("On load called");
    setInterval(function() {

        if(remoteStreamCopy !== null) {
            currentFrameRate = remoteStreamCopy.getVideoTracks()[0].getSettings().frameRate;

            var frameRateLabel = document.querySelector("#labelFrameRate");
            frameRateLabel.innerHTML = currentFrameRate.toFixed(2);
        }
        
        if(localStreamCopy !== null) {
            localCurrentFrameRate = localStreamCopy.getVideoTracks()[0].getSettings().frameRate;

            var localFrameRateLabel = document.querySelector("#labelLocalFrameRate");
            localFrameRateLabel.innerHTML = localCurrentFrameRate.toFixed(2);
        }
    }, 1000);
}

var ws = 0;
ws = new WebSocket("wss://172.29.23.176:8090/join?roomID=" + roomID);

ws.onopen = function(event) {
    let msg = {content: "create or join"};
    console.log(JSON.stringify(msg));
    ws.send(JSON.stringify(msg));
}

var muted = false;
var cameraEnabled = true;

function toggleMicrophone() {
    console.log("Toggle microphone called");
    var muted_button = document.querySelector("#microphone_button");
    if(muted == false) {
        muted = true;

        muted_button.innerHTML = '<i class="fa fa-microphone-slash" style="font-size: 30px; color: whitesmoke;" ></i>'

        console.log(localStream.getAudioTracks());
        localStream.getAudioTracks()[0].enabled = false;
    } else { 
        muted = false;
        muted_button.innerHTML = '<i class="fa fa-microphone" style="font-size: 30px; color: whitesmoke;" ></i>'

        localStream.getAudioTracks()[0].enabled = true;
        console.log(localStream.getAudioTracks());
    }
}

function toggleCamera() {
    var camera_button = document.querySelector("#camera_button");
    if(cameraEnabled == true) {
        cameraEnabled = false;
        camera_button.innerHTML = '<i class="fa fa-video-slash" style="font-size: 30px; color: whitesmoke;"></i>';
        localStream.getVideoTracks()[0].enabled = false;
    } else {
        cameraEnabled = true;
        camera_button.innerHTML = '<i class="fa fa-video" style="font-size: 30px; color: whitesmoke;"></i>';
        localStream.getVideoTracks()[0].enabled = true;
    }
}


function PeerMuted() {
    console.log("The other user is muted!");

    const muted_div = document.querySelector("#remote_muted");
    muted_div.style.display = "block";
}

function PeerUnmuted() {
    console.log("The other user unmuted");
    const muted_div = document.querySelector("#remote_muted");
    muted_div.style.display = "none";
}

function leaveCall() {
    hangup();

    const stream = localVideo.srcObject;
    const tracks = stream.getTracks();
  
    tracks.forEach((track) => {
      track.stop();
    });

    const remote_stream = localVideo.srcObject;
    const remote_tracks = remote_stream.getTracks();
  
    tracks.forEach((track) => {
      track.stop();
    });

    localVideo.srcObject = null;
    remoteVideo.srcObject = null;
}

ws.onmessage = function(event) {
    let message = JSON.parse(event.data)
    console.log('Client received message:', message);
    if(message.content === 'created') {
        isInitiator = true;
    }
    if(message.content === 'join') {
        isChannelReady = true;
        isInitiator = true;
    }
    if(message.content === 'joined') {
        isChannelReady = true;
        isInitiator = false;
    }
    if(message.content === 'bye') {
        console.log(message.content);
        if(isInitiator === true) {
            isStarted = false;
            isChannelReady = false;
        } else {
            isStarted = false;
            isChannelReady = false;
            isInitiator = true;
        }
    }
    if (message.content === 'got user media') {
        maybeStart();
    } else if (message.content.type === 'offer') {
        if (!isInitiator && !isStarted) {
          maybeStart();
        }
        pc.setRemoteDescription(new RTCSessionDescription(message.content));
        doAnswer();
    } else if (message.content.type === 'answer' && isStarted) {
        pc.setRemoteDescription(new RTCSessionDescription(message.content));
    } else if (message.content.type === 'candidate' && isStarted) {
        var candidate = new RTCIceCandidate({
          sdpMLineIndex: message.content.label,
          candidate: message.content.candidate
        });
        pc.addIceCandidate(candidate);
    } else if (message === 'bye' && isStarted) {
        handleRemoteHangup();
    }
}

var pcConfig = turnConfig;

var localStreamConstraints = {
    audio: false,
    video: { facingMode: "user" }
};

if(audio.innerHTML === "true") {
    localStreamConstraints.audio = true;
} else {
    localStreamConstraints.audio = false;
}

if(video.innerHTML === "true") {
    localStreamConstraints.video = {facingMode: "user"};
} else {
    localStreamConstraints.video = false;
}

var localVideo = document.querySelector('#localVideo');
var remoteVideo = document.querySelector('#remoteVideo');

navigator.mediaDevices.getUserMedia(localStreamConstraints)
.then(gotStream)
.catch(function(e) {
  alert('getUserMedia() error: ' + e.name);
});


function sendMessage(message) {
    console.log('Client sending message: ', message);
    let msg = { content: message }
    console.log(JSON.stringify(msg));
    ws.send(JSON.stringify(msg));
}

function createPeerConnection() {
    try {
        pc = new RTCPeerConnection(pcConfig);
        pc.onicecandidate = handleIceCandidate;
        pc.onaddstream = handleRemoteStreamAdded;
        pc.onremovestream = handleRemoteStreamRemoved;
        console.log('Created RTCPeerConnnection');
    } catch (e) {
        console.log('Failed to create PeerConnection, exception: ' + e.message);
        alert('Cannot create RTCPeerConnection object.');
        return;
    }
}

function handleIceCandidate(event) {
    console.log('icecandidate event: ', event);
    if (event.candidate) {
        sendMessage({
        type: 'candidate',
            label: event.candidate.sdpMLineIndex,
            id: event.candidate.sdpMid,
            candidate: event.candidate.candidate
        });
    } else {
        console.log('End of candidates.');
    }
}

function handleCreateOfferError(event) {
    console.log('createOffer() error: ', event);
}

function doCall() {
console.log('Sending offer to peer');
    pc.createOffer(setLocalAndSendMessage, handleCreateOfferError);
}

function doAnswer() {
    console.log('Sending answer to peer.');
    pc.createAnswer().then(
        setLocalAndSendMessage,
        onCreateSessionDescriptionError
    );
}

function setLocalAndSendMessage(sessionDescription) {
    pc.setLocalDescription(sessionDescription);
    console.log('setLocalAndSendMessage sending message', sessionDescription);
    sendMessage(sessionDescription);
}

function onCreateSessionDescriptionError(error) {
    console.log('Failed to create session description: ' + error.toString());
}


function handleRemoteStreamAdded(event) {
    console.log('Remote stream added.');
    remoteStream = event.stream;

    remoteStreamCopy = remoteStream;
    remoteVideo.srcObject = remoteStream;
}

function handleRemoteStreamRemoved(event) {
    console.log('Remote stream removed. Event: ', event);
}

function hangup() {
    console.log('Hanging up.');
    stop();
    sendMessage('bye');
}

function handleRemoteHangup() {
    console.log('Session terminated.');
    stop();
    isInitiator = false;
}

function stop() {
    isStarted = false;
    pc.close();
    pc = null;
}

function maybeStart(){
    console.log('>>>>>>> maybeStart() ', isStarted, localStream, isChannelReady);
    if (!isStarted && typeof localStream !== 'undefined' && isChannelReady) {
      console.log('>>>>>> creating peer connection');
      createPeerConnection();
      pc.addStream(localStream);
      isStarted = true;
      console.log('isInitiator', isInitiator);
      if (isInitiator) {
        doCall();
      }
    }
}

function gotStream(stream) {
    console.log('Adding local stream.');
    localStream = stream;
    localVideo.srcObject = stream;
    localStreamCopy = stream;

    sendMessage('got user media');
    if (isInitiator) {
      maybeStart();
    }
}

window.onbeforeunload = function() {
    sendMessage('bye');
};