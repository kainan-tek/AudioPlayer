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
    }

    private var audioTrack: AudioTrack? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: Any? = null
    private var waveFile: WaveFile? = null
    private var minBufSize: Int = 0
    
    private var isPlaying = false
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var audioTrackLock = Any()

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
        if (isPlaying) {
            Log.i(LOG_TAG, "Already playing, stopping current playback")
            stop()
        }

        waveFile = WaveFile(AUDIO_FILE)
        if (!waveFile!!.open()) {
            Log.e(LOG_TAG, "Failed to open WAV file: ${waveFile!!.filePath}")
            listener?.onPlaybackError("Failed to open WAV file")
            return false
        }
        if (!waveFile!!.isValid()) {
            Log.e(LOG_TAG, "Invalid WAV file: ${waveFile!!.filePath}")
            listener?.onPlaybackError("Invalid WAV file")
            return false
        }

        if (!initializePlayback()) {
            waveFile?.close()
            waveFile = null
            return false
        }

        isPlaying = true
        startPlayback()
        listener?.onPlaybackStarted()
        return true
    }

    fun stop() {
        if (!isPlaying) return

        isPlaying = false
        
        // 使用同步块确保线程安全
        synchronized(audioTrackLock) {
            playbackJob?.cancel()
            playbackJob = null
            
            stopPlayback()
            waveFile?.close()
            waveFile = null
            
            listener?.onPlaybackStopped()
        }
    }

    fun isPlaying(): Boolean = isPlaying

    private fun initializePlayback(): Boolean {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(USAGE)
            .setContentType(CONTENT)
            .build()

        val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    Log.i(LOG_TAG, "Audio focus GAINED")
                    audioTrack?.setVolume(1.0f)
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    Log.i(LOG_TAG, "Audio focus LOSS")
                    stop()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    Log.i(LOG_TAG, "Audio focus LOSS_TRANSIENT")
                    stop()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
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
        val result = audioManager?.requestAudioFocus(focusRequest as AudioFocusRequest)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.e(LOG_TAG, "Failed to get audio focus")
            return false
        }

        val channelMask = when (waveFile!!.channelCount) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            4 -> AudioFormat.CHANNEL_OUT_QUAD
            8 -> AudioFormat.CHANNEL_OUT_7POINT1_SURROUND
            10 -> AudioFormat.CHANNEL_OUT_5POINT1POINT4
            12 -> AudioFormat.CHANNEL_OUT_7POINT1POINT4
            14 -> AudioFormat.CHANNEL_OUT_9POINT1POINT4
            16 -> AudioFormat.CHANNEL_OUT_9POINT1POINT6
            else -> AudioFormat.CHANNEL_OUT_MONO
        }

        val format = when (waveFile!!.bitsPerSample) {
            8 -> AudioFormat.ENCODING_PCM_8BIT
            16 -> AudioFormat.ENCODING_PCM_16BIT
            24 -> AudioFormat.ENCODING_PCM_24BIT_PACKED
            32 -> AudioFormat.ENCODING_PCM_32BIT
            else -> AudioFormat.ENCODING_PCM_16BIT
        }

        minBufSize = AudioTrack.getMinBufferSize(
            waveFile!!.sampleRate, 
            channelMask, 
            format
        )
        if (minBufSize <= 480) {
            minBufSize = 480
            Log.e(LOG_TAG, "Insufficient buffer size: $minBufSize, using 480 instead")
        }

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(waveFile!!.sampleRate)
                    .setChannelMask(channelMask)
                    .setEncoding(format)
                    .build()
            )
            .setPerformanceMode(PERF_MODE)
            .setTransferMode(TRANSFER_MODE)
            .setBufferSizeInBytes(minBufSize * MIN_BUF_MULTIPLIER)
            .build()

        Log.i(LOG_TAG, "AudioTrack initialized - SampleRate: ${waveFile!!.sampleRate}, " +
                "Channels: ${waveFile!!.channelCount}, Bits: ${waveFile!!.bitsPerSample}, " +
                "DataSize: ${waveFile!!.dataSize}, minBufferSize: $minBufSize")

        return true
    }

    private fun startPlayback() {
        playbackJob = scope.launch {
            try {
                val buffer = ByteArray(minBufSize)
                var totalBytesRead = 0

                synchronized(audioTrackLock) {
                    if (!isPlaying) return@launch
                    audioTrack?.play()
                }

                while (isActive && isPlaying) {
                    val bytesRead = waveFile!!.readData(buffer, 0, buffer.size)
                    if (bytesRead == -1 || bytesRead == 0) {
                        Log.i(LOG_TAG, "Reached end of file or read 0 bytes, breaking playback loop")
                        break
                    }
                    
                    synchronized(audioTrackLock) {
                        if (!isPlaying) break
                        audioTrack?.write(buffer, 0, bytesRead)
                    }
                    
                    totalBytesRead += bytesRead
                }
                
                if (isPlaying) {
                    stop()
                }
                Log.i(LOG_TAG, "Playback completed, total bytes read: $totalBytesRead")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Playback error", e)
                if (isPlaying) {
                    listener?.onPlaybackError("Playback error: ${e.message}")
                    stop()
                }
            }
        }
    }

    private fun stopPlayback() {
        synchronized(audioTrackLock) {
            try {
                audioTrack?.let { track ->
                    try {
                        if (track.state == AudioTrack.STATE_INITIALIZED) {
                            track.stop()
                        }
                        track.release()
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "Error stopping AudioTrack", e)
                    }
                }
                audioTrack = null

                focusRequest?.let {
                    audioManager?.abandonAudioFocusRequest(it as AudioFocusRequest)
                }
                focusRequest = null
                
                Log.i(LOG_TAG, "Audio playback stopped")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error stopping playback", e)
            }
        }
    }

    fun release() {
        stop()
        scope.cancel()
    }
}