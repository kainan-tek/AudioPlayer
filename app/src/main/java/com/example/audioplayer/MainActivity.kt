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
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 使用步骤 *
adb root
adb remount
adb shell setenforce 0
adb push 48k_2ch_16bit.wav /data/
adb install xxx.apk
 */

class MainActivity : AppCompatActivity() {
    private var audioTrack: AudioTrack? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var fileInputStream: FileInputStream? = null

    private var isStart = false
    // read from wav header if wav file
    private var sampleRate = 48000
    private var channelCount = 2
    private var minBufSizeInBytes = 0

    // USAGE_MEDIA USAGE_NOTIFICATION USAGE_ASSISTANCE_ACCESSIBILITY
    // USAGE_ASSISTANCE_NAVIGATION_GUIDANCE USAGE_ALARM
    companion object {
        private const val LOG_TAG = "AudioPlayerDemo"
        private const val AUDIO_FILE = "/data/48k_2ch_16bit.wav"
        private const val USAGE = AudioAttributes.USAGE_MEDIA
        private const val CONTENT = AudioAttributes.CONTENT_TYPE_MUSIC
        private const val TRANSFER_MODE = AudioTrack.MODE_STREAM
        private const val PERF_MODE = AudioTrack.PERFORMANCE_MODE_LOW_LATENCY
        private const val MIN_BUF_MULTIPLIER = 2
        private const val WAV_HEADER_SIZE = 44
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
        /**
         * 步骤 1: 打开音频文件输入流。
         */
        val inPutFile = File(AUDIO_FILE)
        try {
            fileInputStream = FileInputStream(inPutFile)
        } catch (_: SecurityException) {
            Log.e(LOG_TAG, "no permission to access the audio file")
            fileInputStream?.close() // Close if opened before error
            fileInputStream = null
            return false
        } catch (_: FileNotFoundException) {
            Log.e(LOG_TAG, "audio file can't be opened, check if it exist")
            // fileInputStream would be null here if construction failed
            return false
        } catch (e: IOException) { // Catch other IO exceptions during header read
            Log.e(LOG_TAG, "Error reading WAV header", e)
            fileInputStream?.close()
            fileInputStream = null
            return false
        }
        Log.i(LOG_TAG, "audio file: $inPutFile")

        /**
         * 步骤 2: 读取WAV头并设置音频参数。
         */
        val wavHeader = readWavHeader(fileInputStream)
        if (wavHeader.chunkId != "RIFF" || wavHeader.format != "WAVE") {
            Log.e(LOG_TAG, "invalid wav header")
            return false
        }

        // println(wavHeader)
        sampleRate = wavHeader.sampleRate
        channelCount = wavHeader.numChannels.toInt()
        val bytesPerSample = wavHeader.bitsPerSample.toInt()/8
        val channelMask = when (channelCount) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            4 -> AudioFormat.CHANNEL_OUT_QUAD
            8 -> AudioFormat.CHANNEL_OUT_7POINT1_SURROUND
            // 12 -> AudioFormat.CHANNEL_OUT_7POINT1POINT4
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

        /**
         * 步骤 3: 请求音频焦点。
         */
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(USAGE)
            .setContentType(CONTENT)
            .build()

        val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> { // recover play
                    Log.i(LOG_TAG, "Audio focus GAINED")
                    audioTrack?.setVolume(1.0f)
                    // startAudioPlayback()  // TBD
                }
                AudioManager.AUDIOFOCUS_LOSS -> { // stop play
                    Log.i(LOG_TAG, "Audio focus LOSS")
                    isStart = false  // will stop in playback thread
                    // stopAudioPlayback()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> { // pause play
                    Log.i(LOG_TAG, "Audio focus LOSS_TRANSIENT")
                    isStart = false // will stop in playback thread
                    // audioTrack?.pause()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { // duck
                    Log.i(LOG_TAG, "Audio focus LOSS_TRANSIENT_CAN_DUCK")
                    audioTrack?.setVolume(0.3f)
                }
            }
        }

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .setWillPauseWhenDucked(false)
            .build()

        val result = audioManager!!.requestAudioFocus(focusRequest!!)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.e(LOG_TAG, "audioManager requestAudioFocus failed")
            return false
        }
        Log.i(LOG_TAG, "audioManager requestAudioFocus success")

        /**
         * 步骤 4: 创建并初始化 AudioTrack。
         */
        // after audio focus, init audio track
        minBufSizeInBytes = AudioTrack.getMinBufferSize(sampleRate, channelMask, format)
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
            .setBufferSizeInBytes(minBufSizeInBytes * MIN_BUF_MULTIPLIER)
            .build()

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

                val bytes = ByteArray(minBufSizeInBytes)
                if (audioTrack!!.state != AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack!!.play()
                    sleep(2)
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
    val subChunk2Size: Int,
)

    private fun readWavHeader(inputStream: FileInputStream?): WavHeader {
        val bufferArr = ByteArray(WAV_HEADER_SIZE)
        val bytesRead = try {
            inputStream?.read(bufferArr, 0, WAV_HEADER_SIZE)
        } catch (e: IOException) {
            Log.e(LOG_TAG, "IOException while reading WAV header", e)
            return WavHeader("", 0, "", "", 0, 0, 0, 0, 0, 0, 0, "",0)
        }
        if (bytesRead != null) {
            if (bytesRead < WAV_HEADER_SIZE) {
                Log.e(LOG_TAG, "Could not read complete WAV header. Bytes read: $bytesRead")
                return WavHeader("", 0, "", "", 0, 0, 0, 0, 0, 0, 0, "",0)
            }
        }
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
