import { socket } from "./socket";
let pc;
let localStream;

export async function startPTT(yourId, targetId, remoteAudioRef) {
  socket.emit("register", yourId);

  localStream = await navigator.mediaDevices.getUserMedia({ audio: true });
  pc = new RTCPeerConnection({
    iceServers: [{ urls: "stun:stun.l.google.com:19302" }]
  });

  localStream.getTracks().forEach(track => pc.addTrack(track, localStream));

  pc.ontrack = e => {
    remoteAudioRef.current.srcObject = e.streams[0];
  };


  pc.onicecandidate = event => {
  if (event.candidate) {
    ws.send(JSON.stringify({
      type: "ice",
      target: targetId,
      candidate: event.candidate
    }));
  }
};


  const offer = await pc.createOffer();
  await pc.setLocalDescription(offer);

  socket.emit("webrtc-offer", { to: targetId, from: yourId, sdp: offer.sdp });
}

export function releasePTT() {
  if (!pc) return;
  localStream.getAudioTracks().forEach(track => track.enabled = false);
}

socket.on("webrtc-offer", async data => {
  const remoteDesc = { type: "offer", sdp: data.sdp };
  if (!pc) {
    pc = new RTCPeerConnection({ iceServers: [{ urls: "stun:stun.l.google.com:19302" }] });
    localStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    localStream.getTracks().forEach(track => pc.addTrack(track, localStream));
    pc.ontrack = e => { document.getElementById("remoteAudio").srcObject = e.streams[0]; };
    pc.onicecandidate = e => { if (e.candidate) socket.emit("webrtc-ice", { to: data.from, candidate: e.candidate }); };
  }
  await pc.setRemoteDescription(remoteDesc);
  const answer = await pc.createAnswer();
  await pc.setLocalDescription(answer);
  socket.emit("webrtc-answer", { to: data.from, sdp: answer.sdp });
});

socket.on("webrtc-answer", async data => {
  await pc.setRemoteDescription({ type: "answer", sdp: data.sdp });
});

socket.on("webrtc-ice", async data => {
  if (data.candidate) {
    await pc.addIceCandidate(data.candidate);
  }
});
