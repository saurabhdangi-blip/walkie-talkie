package com.example.voicesend

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class SignalingClient(
    private val yourId: String,
    private val rtcClient: WebRTCClient
) {

    private var ws: WebSocket? = null

    private val socketListener = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            ws = webSocket
            Log.d("Signaling", "✅ WebSocket connected")

            val register = JSONObject()
            register.put("type", "register")
            register.put("id", yourId)

            webSocket.send(register.toString())
            Log.d("Signaling", "📤 Registered as $yourId")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("Signaling", "📥 $text")

            try {
                val json = JSONObject(text)

                when (json.getString("type")) {

                    "offer" -> {
                        val sdp = SessionDescription(
                            SessionDescription.Type.OFFER,
                            json.getJSONObject("offer").getString("sdp")
                        )
                        rtcClient.onOfferReceived(
                            sdp,
                            json.getString("from")
                        )
                    }

                    "answer" -> {
                        val sdp = SessionDescription(
                            SessionDescription.Type.ANSWER,
                            json.getJSONObject("answer").getString("sdp")
                        )
                        rtcClient.onAnswerReceived(sdp)
                    }

                    "ice" -> {
                        val c = json.getJSONObject("candidate")
                        rtcClient.onIceReceived(
                            IceCandidate(
                                c.getString("sdpMid"),
                                c.getInt("sdpMLineIndex"),
                                c.getString("candidate")
                            )
                        )
                    }

                    "ready" -> {
                        Log.d("Signaling", "✅ Target ready")
                    }

                    "call-ended" -> {
                        Log.d("Signaling", "📴 Remote ended call")
                        rtcClient.hangup()
                    }

                }

            } catch (e: Exception) {
                Log.e("Signaling", "❌ Invalid message", e)
            }
        }

        override fun onFailure(
            webSocket: WebSocket,
            t: Throwable,
            response: Response?
        ) {
            Log.e("Signaling", "❌ WS failed", t)
        }
    }

//    init {
//        val client = OkHttpClient()
//        client.newWebSocket(
//            Request.Builder()
//                .url("ws://192.168.1.110:8080")
//                .build(),
//            socketListener
//        )
//    }

    init {
        val client = OkHttpClient()

        client.newWebSocket(
            Request.Builder()
                .url("wss://walkie-talkie-4fn8.onrender.com")
                .build(),
            socketListener
        )
    }


    // ================= SENDERS =================

    fun sendOffer(sdp: SessionDescription, target: String) {
        val json = JSONObject().apply {
            put("type", "offer")
            put("target", target)
            put("from", yourId)
            put("offer", JSONObject().apply {
                put("type", "offer")              // ✅ MUST
                put("sdp", sdp.description)
            })
        }
        ws?.send(json.toString())
    }

    fun sendAnswer(sdp: SessionDescription, target: String) {
        val json = JSONObject().apply {
            put("type", "answer")
            put("target", target)
            put("answer", JSONObject().apply {
                put("type", "answer")             // ✅ MUST
                put("sdp", sdp.description)
            })
        }
        ws?.send(json.toString())
    }


    fun sendIce(c: IceCandidate, target: String) {
        val json = JSONObject()
        json.put("type", "ice")
        json.put("target", target)

        val ice = JSONObject()
        ice.put("sdpMid", c.sdpMid)
        ice.put("sdpMLineIndex", c.sdpMLineIndex)
        ice.put("candidate", c.sdp)

        json.put("candidate", ice)

        ws?.send(json.toString())
    }

    fun sendCallEnded(target: String) {
        val json = JSONObject().apply {
            put("type", "call-ended")
            put("from", yourId)
            put("target", target)
        }
        ws?.send(json.toString())
        Log.d("Signaling", "📴 CALL ENDED → $target")
    }


    fun sendReady(target: String) {
        val json = JSONObject()
        json.put("type", "ready")
        json.put("from", yourId)
        json.put("target", target)

        ws?.send(json.toString())
        Log.d("Signaling", "📤 READY → $target")
    }
}
