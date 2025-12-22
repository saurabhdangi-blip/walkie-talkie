import { useRef, useState } from "react";

const iceConfig = {
  iceServers: [{ urls: "stun:stun.l.google.com:19302" }],
};

export default function App() {
  const [yourId, setYourId] = useState("");
  const [targetId, setTargetId] = useState("");
  const [audioUnlocked, setAudioUnlocked] = useState(false);
  const [inCall, setInCall] = useState(false);

  const wsRef = useRef(null);
  const pcRef = useRef(null);
  const pendingIceRef = useRef([]);

  /* 🔓 AUDIO UNLOCK (REQUIRED FOR CHROME / ANDROID) */
  const unlockAudio = async () => {
    const audio = document.getElementById("remoteAudio");
    try {
      await audio.play();
      setAudioUnlocked(true);
      console.log("🔊 Speaker unlocked");
    } catch (e) {
      console.error("❌ Audio unlock failed", e);
    }
  };

  /* ▶ START */
  const start = async () => {
    /* WebSocket */
    wsRef.current = new WebSocket("ws://localhost:8080");

    wsRef.current.onopen = () => {
      wsRef.current.send(
        JSON.stringify({ type: "register", id: yourId })
      );
      console.log("✅ WebSocket connected");
    };

    /* PeerConnection */
    pcRef.current = new RTCPeerConnection(iceConfig);

    /* 🎤 MIC (WEB → ANDROID) */
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    stream.getTracks().forEach(track =>
      pcRef.current.addTrack(track, stream)
    );

    /* 🎧 REMOTE AUDIO (ANDROID → WEB) */
    pcRef.current.ontrack = (event) => {
      console.log("🎧 Remote track received");

      const audio = document.getElementById("remoteAudio");

      if (!audio.srcObject) {
        audio.srcObject = new MediaStream();
      }

      audio.srcObject.addTrack(event.track);

      if (audioUnlocked) {
        audio.play().catch(() => {});
      }
    };

    /* ICE */
    pcRef.current.onicecandidate = (e) => {
      if (e.candidate && targetId) {
        wsRef.current.send(JSON.stringify({
          type: "ice",
          target: targetId,
          candidate: e.candidate
        }));
      }
    };

    /* SIGNALING */
    wsRef.current.onmessage = async (event) => {
      const data = JSON.parse(event.data);
      console.log("📥", data);

      /* ANDROID → WEB OFFER */
      if (data.type === "offer") {
        await pcRef.current.setRemoteDescription(data.offer);

        const answer = await pcRef.current.createAnswer();
        await pcRef.current.setLocalDescription(answer);

        wsRef.current.send(JSON.stringify({
          type: "answer",
          target: data.from,
          answer
        }));

        pendingIceRef.current.forEach(c =>
          pcRef.current.addIceCandidate(new RTCIceCandidate(c))
        );
        pendingIceRef.current = [];

        setInCall(true);
      }

      /* WEB → ANDROID ANSWER */
      if (data.type === "answer") {
        await pcRef.current.setRemoteDescription(data.answer);
        setInCall(true);
      }

      /* ICE */
      if (data.type === "ice") {
        const candidate = new RTCIceCandidate(data.candidate);
        if (pcRef.current.remoteDescription)
          pcRef.current.addIceCandidate(candidate);
        else
          pendingIceRef.current.push(data.candidate);
      }

      /* CALL CUT FROM REMOTE */
      if (data.type === "call-ended") {
        console.log("📴 Remote ended call");
        hangup();
      }
    };
  };

  /* 📞 CALL */
  const call = async () => {
    if (!pcRef.current) {
      alert("Click Start first");
      return;
    }

    await unlockAudio(); // 🔊 AUTO ENABLE SPEAKER

    const offer = await pcRef.current.createOffer({
      offerToReceiveAudio: true,
    });

    await pcRef.current.setLocalDescription(offer);

    wsRef.current.send(JSON.stringify({
      type: "offer",
      target: targetId,
      from: yourId,
      offer
    }));

    setInCall(true);
    console.log("📤 Call started");
  };

  /* ❌ HANGUP */
  const hangup = () => {
    console.log("📴 Call ended");

    pcRef.current?.getSenders().forEach(s => s.track?.stop());
    pcRef.current?.close();
    pcRef.current = null;

    const audio = document.getElementById("remoteAudio");
    if (audio) {
      audio.pause();
      audio.srcObject = null;
    }

    wsRef.current?.send(JSON.stringify({
      type: "call-ended",
      target: targetId,
      from: yourId
    }));

    setInCall(false);
  };

  return (
    <div style={{ padding: 20 }}>
      <h3>WebRTC Voice (Android ↔ Web)</h3>

      <input
        placeholder="Your ID"
        value={yourId}
        onChange={e => setYourId(e.target.value)}
      />
      <br /><br />

      <input
        placeholder="Target ID"
        value={targetId}
        onChange={e => setTargetId(e.target.value)}
      />
      <br /><br />

      <button onClick={start}>▶ Start</button>

      <button
        onClick={call}
        disabled={inCall}
        style={{ marginLeft: 10 }}
      >
        📞 Call
      </button>

      <button
        onClick={hangup}
        disabled={!inCall}
        style={{
          marginLeft: 10,
          background: "red",
          color: "white"
        }}
      >
        ❌ End Call
      </button>

      <br /><br />

      <audio
        id="remoteAudio"
        playsInline
        controls
      />
    </div>
  );
}
