package com.example.voicesend

import android.os.Environment
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.Format
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

@UnstableApi
class RemoteAudioFileSink : AudioSink {

    private val file: File
    private val output: FileOutputStream

    init {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (!dir.exists()) dir.mkdirs()

        file = File(dir, "webrtc_remote_audio.pcm")
        output = FileOutputStream(file)

        Log.d("AUDIO_SINK", "🎧 Recording remote audio to: ${file.absolutePath}")
    }

    fun onData(
        audioData: ByteBuffer,
        bitsPerSample: Int,
        sampleRate: Int,
        numberOfChannels: Int,
        numberOfFrames: Int
    ) {
        val bytes = ByteArray(audioData.remaining())
        audioData.get(bytes)
        output.write(bytes)
    }

    fun stop() {
        output.flush()
        output.close()
        Log.d("AUDIO_SINK", "🛑 Audio recording stopped")
    }

    override fun setListener(listener: AudioSink.Listener) {
        TODO("Not yet implemented")
    }

    override fun supportsFormat(format: Format): Boolean {
        TODO("Not yet implemented")
    }

    override fun getFormatSupport(format: Format): Int {
        TODO("Not yet implemented")
    }

    override fun getCurrentPositionUs(sourceEnded: Boolean): Long {
        TODO("Not yet implemented")
    }

    override fun configure(
        inputFormat: Format,
        specifiedBufferSize: Int,
        outputChannels: IntArray?
    ) {
        TODO("Not yet implemented")
    }

    override fun play() {
        TODO("Not yet implemented")
    }

    override fun handleDiscontinuity() {
        TODO("Not yet implemented")
    }

    override fun handleBuffer(
        buffer: ByteBuffer,
        presentationTimeUs: Long,
        encodedAccessUnitCount: Int
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun playToEndOfStream() {
        TODO("Not yet implemented")
    }

    override fun isEnded(): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasPendingData(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        TODO("Not yet implemented")
    }

    override fun getPlaybackParameters(): PlaybackParameters {
        TODO("Not yet implemented")
    }

    override fun setSkipSilenceEnabled(skipSilenceEnabled: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getSkipSilenceEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setAudioAttributes(audioAttributes: AudioAttributes) {
        TODO("Not yet implemented")
    }

    override fun getAudioAttributes(): AudioAttributes? {
        TODO("Not yet implemented")
    }

    override fun setAudioSessionId(audioSessionId: Int) {
        TODO("Not yet implemented")
    }

    override fun setAuxEffectInfo(auxEffectInfo: AuxEffectInfo) {
        TODO("Not yet implemented")
    }

    override fun getAudioTrackBufferSizeUs(): Long {
        TODO("Not yet implemented")
    }

    override fun enableTunnelingV21() {
        TODO("Not yet implemented")
    }

    override fun disableTunneling() {
        TODO("Not yet implemented")
    }

    override fun setVolume(volume: Float) {
        TODO("Not yet implemented")
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun flush() {
        TODO("Not yet implemented")
    }

    override fun reset() {
        TODO("Not yet implemented")
    }
}
