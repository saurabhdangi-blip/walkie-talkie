package com.example.voicesend

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.util.Log
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule

class WebRTCClient(private val context: Context) {

    private lateinit var signalingClient: SignalingClient

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var localStream: MediaStream? = null

    private var targetId: String? = null
    private var remoteSdpSet = false
    private val pendingIce = mutableListOf<IceCandidate>()

    // ================= INIT =================

    fun setSignalingClient(client: SignalingClient) {
        signalingClient = client
        initFactory()
    }

    private fun initFactory() {
        if (factory != null) return

        val adm = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        factory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(adm)
            .createPeerConnectionFactory()

        Log.d("WebRTC", "✅ Factory initialized")
    }

    // ================= PEER =================

    @SuppressLint("ServiceCast")
    private fun createPeerConnectionIfNeeded() {
        if (peerConnection != null) return
        val f = factory ?: return

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        val config = PeerConnection.RTCConfiguration(iceServers)
        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnection = f.createPeerConnection(config, object : PeerConnection.Observer {

            override fun onIceCandidate(candidate: IceCandidate) {
                targetId?.let { signalingClient.sendIce(candidate, it) }
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                val track = transceiver?.receiver?.track()
                if (track is AudioTrack) {
                    track.setEnabled(true)
                    Log.d("WebRTC", "🎧 Remote audio playing")
                }
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d("WebRTC", "ICE = $state")
            }

            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onConnectionChange(p0: PeerConnection.PeerConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
        })

        createAudioTrack()
        Log.d("WebRTC", "✅ PeerConnection ready")
    }

    private fun createAudioTrack() {
        if (audioTrack != null) return
        val f = factory ?: return

        val constraints = MediaConstraints()
        audioSource = f.createAudioSource(constraints)
        audioTrack = f.createAudioTrack("AUDIO_TRACK", audioSource)
        audioTrack!!.setEnabled(true)

        // Unified Plan: Add track if no sender exists
        if (peerConnection != null) {
            peerConnection!!.addTrack(audioTrack)
            Log.d("WebRTC", "🎤 Audio track attached via addTrack")
        } else {
            Log.e("WebRTC", "❌ PeerConnection not ready")
        }
    }



    // ================= CALL FLOW =================

    fun call(target: String) {
        targetId = target
        createPeerConnectionIfNeeded()

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection!!.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(offer: SessionDescription?) {
                if (offer != null) {
                    peerConnection!!.setLocalDescription(SdpObserverAdapter(), offer)
                    signalingClient.sendOffer(offer, target)
                    Log.d("WebRTC", "📤 OFFER sent")
                }
            }
        }, constraints)
    }

    fun onOfferReceived(sdp: SessionDescription, from: String) {
        targetId = from
        createPeerConnectionIfNeeded()

        peerConnection!!.setRemoteDescription(SdpObserverAdapter(), sdp)
        remoteSdpSet = true

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection!!.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(answer: SessionDescription?) {
                if (answer != null) {
                    peerConnection!!.setLocalDescription(SdpObserverAdapter(), answer)
                    signalingClient.sendAnswer(answer, from)
                    Log.d("WebRTC", "📤 ANSWER sent")
                }
            }
        }, constraints)
    }

    fun onAnswerReceived(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(SdpObserverAdapter(), sdp)
        remoteSdpSet = true

        pendingIce.forEach { peerConnection?.addIceCandidate(it) }
        pendingIce.clear()

        Log.d("WebRTC", "📥 ANSWER received")
    }

    fun onIceReceived(candidate: IceCandidate) {
        if (!remoteSdpSet) pendingIce.add(candidate)
        else peerConnection?.addIceCandidate(candidate)
    }


    fun hangup() {
        try {
            audioTrack?.dispose()
            audioSource?.dispose()

            peerConnection?.close()
            peerConnection?.dispose()
        } catch (e: Exception) {
            Log.e("WebRTC", "Hangup error", e)
        }

        audioTrack = null
        audioSource = null
        peerConnection = null
        targetId = null

        pendingIce.clear()
        remoteSdpSet = false

        Log.d("WebRTC", "🧹 Call fully ended")
    }

}
