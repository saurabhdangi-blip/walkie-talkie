package com.example.voicesend.webrtc

import android.content.Context
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule

object RTCFactory {

    fun create(context: Context): PeerConnectionFactory {

        val audioModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        return PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioModule)
            .createPeerConnectionFactory()
    }
}
