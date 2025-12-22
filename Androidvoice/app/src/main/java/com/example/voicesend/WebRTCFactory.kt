//package com.example.voicesend
//
//import android.app.Application
//import android.content.Context
//import android.util.Log
//import org.webrtc.*
//import org.webrtc.audio.AudioDeviceModule
//import org.webrtc.audio.JavaAudioDeviceModule
//
//object WebRTCFactory {
//
//    private var factory: PeerConnectionFactory? = null
//    private var adm: AudioDeviceModule? = null
//
//    fun init(context: Context) {
//        if (factory != null) return
//
//        // 1️⃣ Initialize WebRTC globally
//        val options = PeerConnectionFactory.InitializationOptions.builder(context)
//            .setEnableInternalTracer(true)  // optional: debugging
//            .createInitializationOptions()
//        PeerConnectionFactory.initialize(options)
//
//        // 2️⃣ Safe AudioDeviceModule
//        adm = JavaAudioDeviceModule.builder(context)
//            .setUseHardwareAcousticEchoCanceler(false)  // disable hardware for crash safety
//            .setUseHardwareNoiseSuppressor(false)       // disable hardware for crash safety
//            .createAudioDeviceModule()
//
//        // 3️⃣ Create single PeerConnectionFactory
//        factory = PeerConnectionFactory.builder()
//            .setAudioDeviceModule(adm)
//            .createPeerConnectionFactory()
//
//        Log.d("WebRTCFactory", "✅ WebRTC Factory initialized")
//    }
//
//    fun getFactory(): PeerConnectionFactory? {
//        return factory
//    }
//
////    fun release() {
////        adm?.dispose()
////        factory?.dispose()
////        adm = null
////        factory = null
////        Log.d("WebRTCFactory", "🧹 WebRTC Factory released")
////    }
//}
//
//// ====================== WebRTC Client ======================
//
//class WebRTCClient(private val context: Context) {
//
//    private var peerConnection: PeerConnection? = null
//    private var audioSource: AudioSource? = null
//    private var audioTrack: AudioTrack? = null
//
//    private val pendingIce = mutableListOf<IceCandidate>()
//    private var remoteSdpSet = false
//    private var targetId: String? = null
//    private lateinit var signalingClient: SignalingClient
//
//    fun setSignalingClient(client: SignalingClient) {
//        signalingClient = client
//        WebRTCFactory.init(context)  // ensure factory initialized
//    }
//
//    private fun createPeerConnectionIfNeeded() {
//        if (peerConnection != null) return
//        val f = WebRTCFactory.getFactory() ?: return
//
//        val iceServers = listOf(
//            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
//        )
//
//        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
//        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
//
//        peerConnection = f.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
//            override fun onIceCandidate(candidate: IceCandidate) {
//                targetId?.let { signalingClient.sendIce(candidate, it) }
//            }
//
//            override fun onTrack(transceiver: RtpTransceiver?) {
//                Log.d("WebRTCClient", "🎧 Remote track received")
//            }
//
//            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
//                Log.d("WebRTCClient", "ICE state = $state")
//            }
//
//            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
//            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
//            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
//            override fun onAddStream(p0: MediaStream?) {}
//            override fun onRemoveStream(p0: MediaStream?) {}
//            override fun onDataChannel(p0: DataChannel?) {}
//            override fun onRenegotiationNeeded() {}
//            override fun onConnectionChange(p0: PeerConnection.PeerConnectionState?) {}
//            override fun onIceConnectionReceivingChange(p0: Boolean) {}
//        })
//
//        // Create audio transceiver
//        val init = RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_RECV)
//        peerConnection!!.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO, init)
//
//        // Attach audio track
//        createAudioTrackIfNeeded()
//        Log.d("WebRTCClient", "✅ PeerConnection + Audio track ready")
//    }
//
//    private fun createAudioTrackIfNeeded() {
//        if (audioTrack != null) return
//        val f = WebRTCFactory.getFactory() ?: return
//
//        val constraints = MediaConstraints()
//        audioSource = f.createAudioSource(constraints)
//        audioTrack = f.createAudioTrack("AUDIO_TRACK", audioSource)
//        audioTrack!!.setEnabled(true)
//
//        val sender = peerConnection!!.senders.firstOrNull { it.track()?.kind() == "audio" }
//        sender?.setTrack(audioTrack, false)
//        Log.d("WebRTCClient", "🎤 Audio track attached")
//    }
//
//    fun call(target: String) {
//        targetId = target
//        createPeerConnectionIfNeeded()
//        signalingClient.sendReady(target)
//    }
//
//    fun onOfferReceived(sdp: SessionDescription, from: String) {
//        targetId = from
//        createPeerConnectionIfNeeded()
//        peerConnection!!.setRemoteDescription(SdpObserverAdapter(), sdp)
//        remoteSdpSet = true
//        pendingIce.forEach { peerConnection?.addIceCandidate(it) }
//        pendingIce.clear()
//
//        peerConnection!!.createAnswer(object : SdpObserverAdapter() {
//            override fun onCreateSuccess(answer: SessionDescription?) {
//                answer ?: return
//                peerConnection!!.setLocalDescription(SdpObserverAdapter(), answer)
//                signalingClient.sendAnswer(answer, from)
//            }
//        }, MediaConstraints())
//    }
//
//    fun onAnswerReceived(sdp: SessionDescription) {
//        peerConnection?.setRemoteDescription(SdpObserverAdapter(), sdp)
//        remoteSdpSet = true
//        pendingIce.forEach { peerConnection?.addIceCandidate(it) }
//        pendingIce.clear()
//    }
//
//    fun onIceReceived(candidate: IceCandidate) {
//        createPeerConnectionIfNeeded()
//        if (!remoteSdpSet) {
//            pendingIce.add(candidate)
//        } else {
//            peerConnection?.addIceCandidate(candidate)
//        }
//    }
//
//    fun hangup() {
//        audioTrack?.dispose()
//        audioSource?.dispose()
//        peerConnection?.close()
//        peerConnection?.dispose()
//        audioTrack = null
//        audioSource = null
//        peerConnection = null
//        targetId = null
//    }
//}
//
//// ====================== SdpObserverAdapter ======================
//open class SdpObserverAdapter : SdpObserver {
//    override fun onCreateSuccess(sdp: SessionDescription?) {}
//    override fun onSetSuccess() {}
//    override fun onCreateFailure(error: String?) {}
//    override fun onSetFailure(error: String?) {}
//}
