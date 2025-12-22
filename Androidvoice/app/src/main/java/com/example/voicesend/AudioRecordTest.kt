package com.example.voicesend

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File

class AudioRecordTest(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private lateinit var outputFile: File

    fun startRecording() {
        try {
            val dir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            outputFile = File(dir, "Audio3.m4a")

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                MediaRecorder(context)
            else
                MediaRecorder()

            recorder!!.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            Log.d("AUDIO_TEST", "🎤 Recording started → ${outputFile.absolutePath}")

        } catch (e: Exception) {
            Log.e("AUDIO_TEST", "❌ Start failed", e)
        }
    }

    fun stopRecording() {
        try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            Log.d("AUDIO_TEST", "✅ Saved → ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("AUDIO_TEST", "❌ Stop failed", e)
        }
    }
}
