package com.example.audioplayer

import android.media.AudioAttributes
import android.media.AudioFormat
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
    private var isStart = false
    private var numOfMinBuf = 2
    private var bytesPerSample = 0
    private var channelsPerFrame = 0
    private var minBufferSizeInFrames = 0
    private var audioTrack: AudioTrack? = null

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
    }

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

    private fun initAudioPlayback() {
        val minBufSizeInBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_MASK,
            ENCODING)
        Log.i(LOG_TAG, "audioTrack getMinBufferSize: $minBufSizeInBytes")

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(USAGE)
                    .setContentType(CONTENT)
                    .build())
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

        // specify the device address with setPreferredDevice
        /*
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (device in devices) {
            Log.i(LOG_TAG,"device address: ${device.address}")
            if (device.address == "bus0_media_out"){
                audioTrack!!.setPreferredDevice(device)
                break
            }
        }
        */
    }

    private fun startAudioPlayback() {
        Log.i(LOG_TAG,"start AudioPlayback, isStart: $isStart")
        if (isStart){
            Log.i(LOG_TAG,"in playing status, needn't start again")
            return
        }
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
                        if (isStart) {
                            val len = dis.read(bytes)
                            if (len > 0) {
                                audioTrack!!.write(bytes, 0, len)
                            } else if (len == -1) {
                                isStart = false
                            }
                        } else {
                            stopPlayback()
                        }
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
        initAudioPlayback()
        isStart = true
        AudioTrackThread().start()
    }

    private fun stopAudioPlayback() {
        Log.i(LOG_TAG,"stop AudioPlayback, isStart: $isStart")
        if (isStart) {
            isStart = false
        }
    }

    private fun stopPlayback() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
