package com.example.voicesend

import android.app.Application
import org.webrtc.PeerConnectionFactory

class App : Application() {
        override fun onCreate() {
            super.onCreate()
            val options = PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(options)
        }
}

