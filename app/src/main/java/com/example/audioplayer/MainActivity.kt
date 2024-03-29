package com.example.audioplayer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import android.widget.Button
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.FileInputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private var isStart = false
    private var bufferSizeInBytes = 0
    private var audioTrack: AudioTrack? = null

    companion object {
        private const val LOG_TAG = "AudioPlayer"
        private const val RAW_AUDIO_FILE = "/data/16k_2ch_16bit.raw"
        private const val USAGE = AudioAttributes.USAGE_MEDIA
        private const val CONTENT = AudioAttributes.CONTENT_TYPE_MUSIC
        private const val TRANSFER_MODE = AudioTrack.MODE_STREAM
        private const val PERF_MODE = AudioTrack.PERFORMANCE_MODE_NONE
        private const val SAMPLE_RATE = 16000
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
        bufferSizeInBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_MASK,
            ENCODING
        )
        Log.i(LOG_TAG, "audioTrack getMinBufferSize: $bufferSizeInBytes")

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(USAGE)
                    .setContentType(CONTENT)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_MASK)
                    .setEncoding(ENCODING)
                    .build()
            )
            .setPerformanceMode(PERF_MODE)
            .setTransferMode(TRANSFER_MODE)
            .setBufferSizeInBytes(bufferSizeInBytes*2)
            .build()
        Log.i(LOG_TAG, "set audioTrack params: " +
                "Usage ${USAGE}， " +
                "ContentType ${CONTENT}， " +
                "SampleRate $SAMPLE_RATE， " +
                "ChannelMask ${CHANNEL_MASK}， " +
                "Encoding $ENCODING")
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
                    val bytes = ByteArray(bufferSizeInBytes*2)

                    audioTrack?.play()
                    while (audioTrack != null) {
                        val len = dis.read(bytes)
                        if (len > 0) {
                            audioTrack!!.write(bytes, 0, len)
                        } else if(len == -1){
                            stopAudioPlayback()
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
        AudioTrackThread().start()
        isStart = true
    }

    private fun stopAudioPlayback() {
        Log.i(LOG_TAG,"stop AudioPlayback, isStart: $isStart")
        if(isStart) {
            audioTrack?.stop()
            audioTrack?.flush()
            audioTrack?.release()
            audioTrack = null
            isStart = false
        }
    }
}
