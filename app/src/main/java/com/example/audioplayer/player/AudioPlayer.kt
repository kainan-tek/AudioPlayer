package com.example.audioplayer.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.example.audioplayer.model.WaveFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlayer(private val context: Context) {
    companion object {
        private const val LOG_TAG = "AudioPlayer"
        private const val AUDIO_FILE = "/data/48k_2ch_16bit.wav"
        private const val USAGE = AudioAttributes.USAGE_MEDIA
        private const val CONTENT = AudioAttributes.CONTENT_TYPE_MUSIC
        private const val TRANSFER_MODE = AudioTrack.MODE_STREAM
        private const val PERF_MODE = AudioTrack.PERFORMANCE_MODE_LOW_LATENCY
        private const val MIN_BUF_MULTIPLIER = 2
        private const val MIN_BUFFER_SIZE = 480
    }

    private var audioTrack: AudioTrack? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var waveFile: WaveFile? = null
    private var minBufSize: Int = 0
    
    private var isPlaying = false
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val audioTrackLock = Any()

    interface PlaybackListener {
        fun onPlaybackStarted()
        fun onPlaybackStopped()
        fun onPlaybackError(error: String)
    }

    private var listener: PlaybackListener? = null

    fun setPlaybackListener(listener: PlaybackListener) {
        this.listener = listener
    }

    fun play(): Boolean {
        Log.d(LOG_TAG, "Requesting play")
        
        if (isPlaying) {
            Log.i(LOG_TAG, "Already playing, stopping current playback")
            stop()
        }

        return try {
            // 创建并打开WaveFile
            waveFile = WaveFile(AUDIO_FILE)
            if (!waveFile!!.open() || !waveFile!!.isValid()) {
                handlePlaybackError("Failed to open or invalid WAV file: ${waveFile!!.filePath}")
                return false
            }
            
            Log.d(LOG_TAG, "Valid WAV file detected: ${waveFile!!.sampleRate}Hz, ${waveFile!!.bitsPerSample}bit, ${waveFile!!.channelCount}ch")

            // 初始化播放
            if (!initializePlayback()) {
                handlePlaybackError("Failed to initialize playback")
                return false
            }

            // 开始播放
            isPlaying = true
            startPlayback()
            listener?.onPlaybackStarted()
            Log.i(LOG_TAG, "Playback started successfully")
            true
        } catch (e: Exception) {
            handlePlaybackError("Unexpected error during playback setup: ${e.message}")
            false
        }
    }

    fun stop() {
        Log.d(LOG_TAG, "Requesting stop")
        if (!isPlaying) {
            Log.d(LOG_TAG, "Not playing, nothing to stop")
            return
        }

        isPlaying = false
        
        synchronized(audioTrackLock) {
            playbackJob?.cancel()
            playbackJob = null
            
            stopPlayback()
            waveFile?.close()
            waveFile = null
        }
        
        listener?.onPlaybackStopped()
        Log.i(LOG_TAG, "Playback stopped")
    }
    
    private fun handlePlaybackError(errorMessage: String) {
        Log.e(LOG_TAG, errorMessage)
        listener?.onPlaybackError(errorMessage)
        waveFile?.close()
        waveFile = null
        stopPlayback()
    }

    private fun initializePlayback(): Boolean {
        Log.d(LOG_TAG, "Initializing playback")
        try {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val waveFile = waveFile ?: return false

            // 创建音频属性
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(USAGE)
                .setContentType(CONTENT)
                .build()

            // 设置音频焦点监听器
            val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
                Log.d(LOG_TAG, "Audio focus change: $focusChange")
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        Log.i(LOG_TAG, "Audio focus gained - restoring volume")
                        audioTrack?.setVolume(1.0f)
                    }
                    AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        Log.i(LOG_TAG, "Audio focus lost - stopping playback")
                        stop()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        Log.i(LOG_TAG, "Audio focus transient loss (can duck) - lowering volume")
                        audioTrack?.setVolume(0.3f)
                    }
                }
            }

            // 请求音频焦点
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .setWillPauseWhenDucked(false)
                .build()
            
            if (audioManager?.requestAudioFocus(focusRequest!!) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.e(LOG_TAG, "Failed to get audio focus")
                return false
            }
            Log.i(LOG_TAG, "Audio focus granted")

            // 确定声道配置
            val channelMask = getChannelMask(waveFile.channelCount)
            
            // 确定音频编码格式
            val format = getAudioEncoding(waveFile.bitsPerSample)

            // 计算最小缓冲区大小
            minBufSize = AudioTrack.getMinBufferSize(
                waveFile.sampleRate, 
                channelMask, 
                format
            )
            
            // 确保缓冲区大小足够
            if (minBufSize <= MIN_BUFFER_SIZE) {
                minBufSize = MIN_BUFFER_SIZE
                Log.w(LOG_TAG, "Insufficient buffer size ($minBufSize), using minimum recommended size: $MIN_BUFFER_SIZE")
            }
            Log.d(LOG_TAG, "Calculated buffer size: $minBufSize")

            // 创建AudioTrack
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(waveFile.sampleRate)
                        .setChannelMask(channelMask)
                        .setEncoding(format)
                        .build()
                )
                .setPerformanceMode(PERF_MODE)
                .setTransferMode(TRANSFER_MODE)
                .setBufferSizeInBytes(minBufSize * MIN_BUF_MULTIPLIER)
                .build()
                
            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                Log.e(LOG_TAG, "Failed to initialize AudioTrack: invalid state (${audioTrack?.state})")
                return false
            }

            Log.i(LOG_TAG, "AudioTrack initialized - SampleRate: ${waveFile.sampleRate}Hz, " +
                    "Channels: ${waveFile.channelCount}, Bits: ${waveFile.bitsPerSample}, " +
                    "DataSize: ${waveFile.dataSize} bytes, BufferSize: ${minBufSize * MIN_BUF_MULTIPLIER} bytes")
            return true
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error during playback initialization", e)
            stopPlayback()
            return false
        }
    }
    
    private fun getChannelMask(channelCount: Int): Int {
        return when (channelCount) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            4 -> AudioFormat.CHANNEL_OUT_QUAD
            8 -> AudioFormat.CHANNEL_OUT_7POINT1_SURROUND
            10 -> AudioFormat.CHANNEL_OUT_5POINT1POINT4
            12 -> AudioFormat.CHANNEL_OUT_7POINT1POINT4
            14 -> AudioFormat.CHANNEL_OUT_9POINT1POINT4
            16 -> AudioFormat.CHANNEL_OUT_9POINT1POINT6
            else -> {
                Log.w(LOG_TAG, "Unsupported channel count: $channelCount, defaulting to MONO")
                AudioFormat.CHANNEL_OUT_MONO
            }
        }
    }
    
    private fun getAudioEncoding(bitsPerSample: Int): Int {
        return when (bitsPerSample) {
            8 -> AudioFormat.ENCODING_PCM_8BIT
            16 -> AudioFormat.ENCODING_PCM_16BIT
            24 -> AudioFormat.ENCODING_PCM_24BIT_PACKED
            32 -> AudioFormat.ENCODING_PCM_32BIT
            else -> {
                Log.w(LOG_TAG, "Unsupported bits per sample: $bitsPerSample, defaulting to 16BIT")
                AudioFormat.ENCODING_PCM_16BIT
            }
        }
    }

    private fun startPlayback() {
        Log.d(LOG_TAG, "Starting playback in coroutine")
        playbackJob = scope.launch {
            val buffer = ByteArray(minBufSize)
            var totalBytesRead = 0L
            val waveFile = waveFile ?: run {
                Log.w(LOG_TAG, "Wave file is null, aborting playback")
                return@launch
            }
            
            // 开始播放
            synchronized(audioTrackLock) {
                if (!isPlaying) return@launch
                audioTrack?.play()
                Log.d(LOG_TAG, "AudioTrack play() called")
            }

            try {
                // 播放循环
                while (isActive && isPlaying) {
                    val bytesRead = waveFile.readData(buffer, 0, buffer.size)
                    if (bytesRead <= 0) {
                        Log.d(LOG_TAG, "End of file reached or read error (bytesRead: $bytesRead)")
                        break
                    }
                    
                    synchronized(audioTrackLock) {
                        if (!isPlaying) break
                        audioTrack?.write(buffer, 0, bytesRead)
                    }
                    
                    totalBytesRead += bytesRead
                }
                
                // 播放完成
                if (isPlaying) {
                    Log.i(LOG_TAG, "Playback completed normally, total read: ${totalBytesRead}bytes, ${totalBytesRead/1024/1024}MB")
                    stop()
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Playback error", e)
                if (isPlaying) {
                    handlePlaybackError("Playback error: ${e.message ?: "Unknown error"}")
                }
            } finally {
                Log.d(LOG_TAG, "Playback coroutine completed")
            }
        }
    }

    private fun stopPlayback() {
        Log.d(LOG_TAG, "Stopping playback resources")
        try {
            // 释放AudioTrack资源
            audioTrack?.apply {
                try {
                    if (state == AudioTrack.STATE_INITIALIZED) {
                        stop()
                    }
                    release()
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Error stopping AudioTrack", e)
                }
            }
            audioTrack = null

            // 放弃音频焦点
            focusRequest?.let {
                audioManager?.abandonAudioFocusRequest(it)
            }
            focusRequest = null
            audioManager = null
            
            Log.i(LOG_TAG, "Playback resources released")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error stopping playback", e)
        }
    }

    fun release() {
        Log.d(LOG_TAG, "Releasing AudioPlayer resources")
        stop()
        scope.cancel()
        Log.i(LOG_TAG, "AudioPlayer resources fully released")
    }
}