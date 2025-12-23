import { useRef, useState } from "react";

const iceConfig = {
  iceServers: [{ urls: "stun:stun.l.google.com:19302" }]
};

export default function App() {

  const [yourId, setYourId] = useState("");
  const [androidUsers, setAndroidUsers] = useState([]);
  const [targetId, setTargetId] = useState("");
  const [started, setStarted] = useState(false);
  const [inCall, setInCall] = useState(false);

  const wsRef = useRef(null);
  const pcRef = useRef(null);
  const pendingIce = useRef([]);

  /* START */
  const start = async () => {
    if (!yourId) return alert("Enter Web ID");

    const wsUrl =
      (window.location.protocol === "https:" ? "wss://" : "ws://") +
      window.location.host;

    wsRef.current = new WebSocket(wsUrl);

    wsRef.current.onopen = () => {
      wsRef.current.send(JSON.stringify({
        type: "register",
        id: yourId,
        role: "web"
      }));
      console.log("✅ Web registered");
      setStarted(true);
    };

    pcRef.current = new RTCPeerConnection(iceConfig);

    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    stream.getTracks().forEach(t => pcRef.current.addTrack(t, stream));

    pcRef.current.ontrack = (e) => {
      const audio = document.getElementById("remoteAudio");
      if (!audio.srcObject) audio.srcObject = new MediaStream();
      audio.srcObject.addTrack(e.track);
      audio.play().catch(()=>{});
    };

    pcRef.current.onicecandidate = (e) => {
      if (e.candidate && targetId) {
        wsRef.current.send(JSON.stringify({
          type: "ice",
          target: targetId,
          candidate: e.candidate
        }));
      }
    };

    wsRef.current.onmessage = async (event) => {
      const data = JSON.parse(event.data);

      if (data.type === "android-users") {
        setAndroidUsers(data.users);
      }

      if (data.type === "answer") {
        await pcRef.current.setRemoteDescription(data.answer);
        pendingIce.current.forEach(c => pcRef.current.addIceCandidate(c));
        pendingIce.current = [];
        setInCall(true);
      }

      if (data.type === "ice") {
        const c = new RTCIceCandidate(data.candidate);
        if (pcRef.current.remoteDescription)
          pcRef.current.addIceCandidate(c);
        else
          pendingIce.current.push(c);
      }

      if (data.type === "call-ended") {
        hangup();
      }
    };
  };


  const createPeer = async () => {
  pcRef.current = new RTCPeerConnection(iceConfig);

  const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
  stream.getTracks().forEach(t => pcRef.current.addTrack(t, stream));

  pcRef.current.ontrack = (e) => {
    const audio = document.getElementById("remoteAudio");
    if (!audio.srcObject) audio.srcObject = new MediaStream();
    audio.srcObject.addTrack(e.track);
    audio.play().catch(()=>{});
  };

  pcRef.current.onicecandidate = (e) => {
    if (e.candidate && targetId) {
      wsRef.current.send(JSON.stringify({
        type: "ice",
        target: targetId,
        candidate: e.candidate
      }));
    }
  };
};


  /* CALL */
  const call = async () => {
  if (!pcRef.current) {
    await createPeer();   // 🔥 recreate PC for every call
  }

  const offer = await pcRef.current.createOffer();
  await pcRef.current.setLocalDescription(offer);

  wsRef.current.send(JSON.stringify({
    type: "offer",
    from: yourId,
    target: targetId,
    offer
  }));

  console.log("📞 Calling", targetId);
};

  /* HANGUP */
  // const hangup = () => {
  //   pcRef.current?.close();
  //   pcRef.current = null;
  //   setInCall(false);
  // };

  const hangup = () => {
  console.log("📴 Call ended");

  wsRef.current?.send(JSON.stringify({
    type: "call-ended",
    target: targetId,
    from: yourId
  }));

  pcRef.current?.getSenders().forEach(s => s.track?.stop());
  pcRef.current?.close();
  pcRef.current = null;

  const audio = document.getElementById("remoteAudio");
  if (audio) {
    audio.pause();
    audio.srcObject = null;
  }

  setInCall(false);
};


  return (
    <div style={{ padding: 20 }}>
      <h2>Web → Android Walkie-Talkie</h2>

      <input
        placeholder="Web ID"
        value={yourId}
        onChange={e => setYourId(e.target.value)}
      />

      <br /><br />

      <button onClick={start} disabled={started}>
        ▶ Start
      </button>

      <br /><br />

      <select
        value={targetId}
        onChange={e => setTargetId(e.target.value)}
      >
        <option value="">Select Android Device</option>
        {androidUsers.map(id => (
          <option key={id} value={id}>{id}</option>
        ))}
      </select>

      <br /><br />

      <button
        onClick={call}
        disabled={!started || !targetId || inCall}
      >
        📞 Call
      </button>

      <button
  onClick={hangup}
  disabled={!inCall}
  style={{
    marginLeft: 10,
    backgroundColor: "red",
    color: "white"
  }}
>
  ❌ End Call
</button>


      <br /><br />

      <audio id="remoteAudio" controls playsInline />
    </div>
  );
}
