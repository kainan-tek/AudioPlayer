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
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.FileInputStream
import java.io.IOException

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
        private const val RAW_AUDIO_FILE = "/data/48k_2ch_16bit.raw"
        private const val USAGE = AudioAttributes.USAGE_MEDIA
        private const val CONTENT = AudioAttributes.CONTENT_TYPE_MUSIC
        private const val TRANSFER_MODE = AudioTrack.MODE_STREAM
        private const val PERF_MODE = AudioTrack.PERFORMANCE_MODE_NONE
        private const val SAMPLE_RATE = 48000
        private const val CHANNEL_MASK = AudioFormat.CHANNEL_IN_STEREO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        private var isStart = false
        private var numOfMinBuf = 2
        private var bytesPerSample = 0
        private var channelsPerFrame = 0
        private var minBufferSizeInFrames = 0
        private var audioTrack: AudioTrack? = null
        private var audioManager: AudioManager? = null
        private var focusRequest: AudioFocusRequest? = null
    }

    private fun startAudioPlayback() {
        if (isStart){
            Log.i(LOG_TAG,"in playing status, needn't start again")
            return
        }
        Log.i(LOG_TAG,"start AudioPlayback.")
        isStart = true
        initPlayback()
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

    private fun initPlayback() {
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
            isStart = false
            return
        }
        Log.i(LOG_TAG, "audioManager requestAudioFocus success")

        val minBufSizeInBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_MASK,
            ENCODING)
        Log.i(LOG_TAG, "audioTrack getMinBufferSize: $minBufSizeInBytes")

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_MASK)
                    .setEncoding(ENCODING)
                    .build())
            .setPerformanceMode(PERF_MODE)
            .setTransferMode(TRANSFER_MODE)
            .setBufferSizeInBytes(minBufSizeInBytes * numOfMinBuf)
            .build()

        channelsPerFrame = audioTrack!!.channelCount
        bytesPerSample = when (audioTrack!!.audioFormat) {
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_IEC61937, AudioFormat.ENCODING_DEFAULT -> 2
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
            AudioFormat.ENCODING_PCM_FLOAT, AudioFormat.ENCODING_PCM_32BIT -> 4
            else -> 2
        }
        minBufferSizeInFrames = minBufSizeInBytes / channelsPerFrame / bytesPerSample
        minBufferSizeInFrames -= minBufferSizeInFrames % (SAMPLE_RATE / 1000)
        audioTrack!!.setBufferSizeInFrames(numOfMinBuf * minBufferSizeInFrames)
        Log.i(LOG_TAG, "set audioTrack params: " +
                "Usage ${USAGE}， " +
                "ContentType ${CONTENT}， " +
                "SampleRate $SAMPLE_RATE， " +
                "ChannelMask ${CHANNEL_MASK}， " +
                "Encoding $ENCODING, " +
                "BufferSizeInFrames ${audioTrack!!.bufferSizeInFrames}")

//        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
//        for (device in devices) {
//            Log.i(LOG_TAG,"device address: ${device.address}")
//            if (device.address == "bus0_media_out"){
//                audioTrack!!.setPreferredDevice(device)
//                break
//            }
//        }
    }

    private fun startPlayback() {
        class AudioTrackThread: Thread() {
            override fun run() {
                super.run()

                var fileInputStream: FileInputStream? = null
                try {
                    fileInputStream = FileInputStream(RAW_AUDIO_FILE)
                    val dis = DataInputStream(BufferedInputStream(fileInputStream))
                    val bytes = ByteArray(minBufferSizeInFrames * channelsPerFrame * bytesPerSample)

                    if (audioTrack!!.state != AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack!!.play()
                        sleep(5)
                    }
                    while (audioTrack != null) {
                        if (!isStart) {
                            stopPlayback()
                            continue
                        }
                        val len = dis.read(bytes)
                        if (len == -1) {
                            isStart = false
                            continue
                        }
                        audioTrack!!.write(bytes, 0, len)
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Exception: ")
                    e.printStackTrace()
                } finally {
                    try {
                        fileInputStream?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
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
}
