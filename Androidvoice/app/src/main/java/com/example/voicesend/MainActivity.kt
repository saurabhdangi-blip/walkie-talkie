package com.example.voicesend

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {

    private lateinit var rtcClient: WebRTCClient
    private lateinit var signalingClient: SignalingClient
    private lateinit var audioTest: AudioRecordTest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioTest = AudioRecordTest(this)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE
            ),
            0
        )


        setContent {
            var yourId by remember { mutableStateOf("") }
            var targetId by remember { mutableStateOf("") }
            var inCall by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(yourId, { yourId = it }, label = { Text("Your ID") })
                TextField(targetId, { targetId = it }, label = { Text("Target ID") })

//                Button(onClick = {
//                    rtcClient = WebRTCClient(this@MainActivity)
//                    signalingClient = SignalingClient(yourId, rtcClient)
//                    rtcClient.setSignalingClient(signalingClient)
//                }) { Text("Start") }
//
//                Button(onClick = { rtcClient.call(targetId) }) { Text("Call") }

                Button(onClick = {
                    rtcClient = WebRTCClient(this@MainActivity)
                    signalingClient = SignalingClient(yourId, rtcClient)
                    rtcClient.setSignalingClient(signalingClient)
                }) {
                    Text("Start")
                }

                Button(
                    onClick = {
                        rtcClient.call(targetId)
                        inCall = true
                    },
                    enabled = !inCall
                ) {
                    Text("📞 Call")
                }

                Button(
                    onClick = {
                        rtcClient.hangup()
                        signalingClient.sendCallEnded(targetId)
                        inCall = false
                    },
                    enabled = inCall,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("❌ End Call")
                }
            }
        }
    }
}
