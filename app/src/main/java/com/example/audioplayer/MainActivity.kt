package com.example.audioplayer

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button1: Button = findViewById(R.id.button1)
        val button2: Button = findViewById(R.id.button2)
        button1.setOnClickListener {
            startAudioPlayback()
        }
        button2.setOnClickListener {
            stopAudioPlayback()
        }
    }

    companion object {
        private const val LOG_TAG = "AudioPlayer"
        private const val AUDIO_FILE = "/data/48k_2ch_16bit.wav"
        private const val USAGE = AudioAttributes.USAGE_MEDIA
        private const val CONTENT = AudioAttributes.CONTENT_TYPE_MUSIC
        private const val TRANSFER_MODE = AudioTrack.MODE_STREAM
        private const val PERF_MODE = AudioTrack.PERFORMANCE_MODE_NONE
        // read from wav header if wav file
        private var sampleRate = 48000
        private var channelCount = 1
        private var bytesPerSample = 2

        private var isStart = false
        private var numOfMinBuf = 2
        private var minBufferSizeInFrames = 0
        private var audioTrack: AudioTrack? = null
        private var audioManager: AudioManager? = null
        private var focusRequest: AudioFocusRequest? = null
        private var fileInputStream: FileInputStream? = null
    }

    private fun startAudioPlayback() {
        if (isStart){
            Log.i(LOG_TAG,"in playing status, needn't start again")
            return
        }
        Log.i(LOG_TAG,"start AudioPlayback.")
        isStart = true
        if (!initPlayback()) {
            isStart = false
            return
        }
        startPlayback()
    }

    private fun stopAudioPlayback() {
        if (!isStart){
            Log.i(LOG_TAG,"in stop status, needn't stop again")
            return
        }
        Log.i(LOG_TAG,"stop AudioPlayback")
        isStart = false
    }

    private fun initPlayback(): Boolean {
        // read params from wav header
        val inPutFile = File(AUDIO_FILE)
        try {
            fileInputStream = FileInputStream(inPutFile)
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "no permission to access the audio file")
            return false
        } catch (e: FileNotFoundException) {
            Log.e(LOG_TAG, "audio file can't be opened, check if it exist")
            return false
        }
        Log.i(LOG_TAG, "audio file: $inPutFile")

        val wavHeader = readWavHeader(fileInputStream)
        // println(wavHeader)
        sampleRate = wavHeader.sampleRate
        channelCount = wavHeader.numChannels.toInt()
        bytesPerSample = wavHeader.bitsPerSample.toInt()/8
        val channelMask = when (channelCount) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            4 -> AudioFormat.CHANNEL_OUT_QUAD
            8 -> AudioFormat.CHANNEL_OUT_7POINT1_SURROUND
            // 16 -> AudioFormat.CHANNEL_OUT_9POINT1POINT6
            else -> AudioFormat.CHANNEL_OUT_MONO
        }
        val format = when (bytesPerSample) {
            1 -> AudioFormat.ENCODING_PCM_8BIT
            2 -> AudioFormat.ENCODING_PCM_16BIT
            3 -> AudioFormat.ENCODING_PCM_24BIT_PACKED
            4 -> AudioFormat.ENCODING_PCM_32BIT
            else -> AudioFormat.ENCODING_PCM_16BIT
        }
        Log.i(LOG_TAG, "wav header params: sampleRate: $sampleRate, channelCount: $channelCount," +
                " channelMask: $channelMask, bytesPerSample: $bytesPerSample, format: $format")

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(USAGE)
            .setContentType(CONTENT)
            .build()

        val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN ->  // recover play
                    audioTrack?.setVolume(1.0f)
                    // startAudioPlayback()  // TBD
                AudioManager.AUDIOFOCUS_LOSS ->  // pause play
                    isStart = false  // stop
                    // stopPlayback()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->  // pause play
                    isStart = false  // stop
                    // stopPlayback()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->  // duck
                    audioTrack?.setVolume(0.5f)
            }
        }

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()

        val result = audioManager!!.requestAudioFocus(focusRequest!!)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.e(LOG_TAG, "audioManager requestAudioFocus failed")
            return false
        }
        Log.i(LOG_TAG, "audioManager requestAudioFocus success")

        // after audio focus, init audio track
        val minBufSizeInBytes = AudioTrack.getMinBufferSize(sampleRate, channelMask, format)
        Log.i(LOG_TAG, "audioTrack getMinBufferSize: $minBufSizeInBytes Bytes")

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .setEncoding(format)
                    .build())
            .setPerformanceMode(PERF_MODE)
            .setTransferMode(TRANSFER_MODE)
            .setBufferSizeInBytes(minBufSizeInBytes * numOfMinBuf)
            .build()

        minBufferSizeInFrames = minBufSizeInBytes / channelCount / (bytesPerSample)
        minBufferSizeInFrames -= minBufferSizeInFrames % (sampleRate / 1000)
        audioTrack!!.setBufferSizeInFrames(numOfMinBuf * minBufferSizeInFrames)
        Log.i(LOG_TAG, "set audioTrack params: " +
                "Usage ${USAGE}， " +
                "ContentType ${CONTENT}， " +
                "SampleRate $sampleRate， " +
                "ChannelMask ${channelMask}， " +
                "format $format, " +
                "BufferSizeInFrames ${audioTrack!!.bufferSizeInFrames}")

//        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
//        for (device in devices) {
//            Log.i(LOG_TAG,"device address: ${device.address}")
//            if (device.address == "bus0_media_out"){
//                audioTrack!!.setPreferredDevice(device)
//                break
//            }
//        }
        return true
    }

    private fun startPlayback() {
        class AudioTrackThread: Thread() {
            override fun run() {
                super.run()

                val bytes = ByteArray(minBufferSizeInFrames * channelCount * bytesPerSample)
                if (audioTrack!!.state != AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack!!.play()
                    sleep(5)
                }
                while (audioTrack != null) {
                    if (!isStart) {
                        stopPlayback()
                        continue
                    }
                    val len = fileInputStream?.read(bytes)
                    if (len == -1) {
                        isStart = false
                        continue
                    }
                    if (len != null) {
                        audioTrack!!.write(bytes, 0, len)
                    }
                }
            }
        }
        AudioTrackThread().start()
    }

    private fun stopPlayback() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        val result = focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.i(LOG_TAG, "audioManager abandonAudioFocusRequest failed")
            return
        }
        Log.i(LOG_TAG, "audioManager abandonAudioFocusRequest success")
    }


/*************** wav header function **********************************/
    data class WavHeader(
        val chunkId: String,
        val chunkSize: Int,
        val format: String,
        val subChunk1Id: String,
        val subChunk1Size: Int,
        val audioFormat: Short,
        val numChannels: Short,
        val sampleRate: Int,
        val byteRate: Int,
        val blockAlign: Short,
        val bitsPerSample: Short,
        val subChunk2Id: String,
        val subChunk2Size: Int
    )

    private fun readWavHeader(inPutStream: FileInputStream?): WavHeader {
        val bufferArr = ByteArray(64)
        inPutStream?.read(bufferArr,0,44)
        val buffer = ByteBuffer.wrap(bufferArr).order(ByteOrder.LITTLE_ENDIAN)

        val chunkId = buffer.getString(4)
        val chunkSize = buffer.int
        val format = buffer.getString(4)
        val subChunk1Id = buffer.getString(4)
        val subChunk1Size = buffer.int
        val audioFormat = buffer.short
        val numChannels = buffer.short
        val sampleRate = buffer.int
        val byteRate = buffer.int
        val blockAlign = buffer.short
        val bitsPerSample = buffer.short
        val subChunk2Id = buffer.getString(4)
        val subChunk2Size = buffer.int

        return WavHeader(
            chunkId, chunkSize, format, subChunk1Id, subChunk1Size, audioFormat,
            numChannels, sampleRate, byteRate, blockAlign, bitsPerSample, subChunk2Id, subChunk2Size
        )
    }

    private fun ByteBuffer.getString(length: Int): String {
        val bytes = ByteArray(length)
        this.get(bytes)
        return String(bytes)
    }
}
