package com.example.audioplayer.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.example.audioplayer.common.AudioConstants
import com.example.audioplayer.config.AudioConfig
import com.example.audioplayer.util.WavFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Player state enumeration
 */
enum class PlayerState {
    IDLE,       // Idle state
    PLAYING,    // Playing
    ERROR       // Error state
}

/**
 * Concise audio player responsible for WAV file playback
 * Supports configurable audio parameters for testing different scenarios
 */
class AudioPlayer(private val context: Context) {

    companion object {
        private const val TAG = "AudioPlayer"
    }

    // Playback components
    private var audioTrack: AudioTrack? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var wavFile: WavFile? = null

    // Playback state - using simple AtomicBoolean, consistent with AudioRecorder
    private val isPlaying = AtomicBoolean(false)
    private var playbackJob: Job? = null
    private val playbackScope = CoroutineScope(Dispatchers.IO)

    // Audio configuration
    private var currentConfig: AudioConfig = AudioConfig()

    // Playback listener
    interface PlaybackListener {
        fun onPlaybackStarted()
        fun onPlaybackStopped()
        fun onPlaybackError(error: String)
    }

    private var listener: PlaybackListener? = null

    fun setPlaybackListener(listener: PlaybackListener) {
        this.listener = listener
    }

    /**
     * Set audio configuration
     */
    fun setAudioConfig(config: AudioConfig) {
        if (isPlaying.get()) {
            Log.w(TAG, "Cannot change configuration while playing, please stop playback first")
            return
        }
        currentConfig = config
        Log.i(TAG, "Audio configuration updated: ${config.description}")
        Log.d(TAG, getDetailedInfo())
    }

    /**
     * Start audio playback
     */
    fun startPlayback(): Boolean {
        Log.d(TAG, "Starting playback")

        if (isPlaying.get()) {
            Log.i(TAG, "Already playing, stopping current playback first")
            stopPlayback()
        }

        return try {
            // Open audio file
            if (!openAudioFile()) {
                return false
            }

            // Initialize player
            if (!initializeAudioTrack()) {
                return false
            }

            // Start playback
            isPlaying.set(true)
            startPlaybackLoop()
            listener?.onPlaybackStarted()

            Log.i(TAG, "Playback started successfully")
            true
        } catch (e: SecurityException) {
            handleError("[PERMISSION] Permission denied: ${e.message}")
            false
        } catch (e: Exception) {
            handleError("[STREAM] Playback initialization failed: ${e.message}")
            false
        }
    }

    /**
     * Stop playback
     */
    fun stopPlayback() {
        Log.d(TAG, "Stopping playback")

        if (!isPlaying.get()) {
            return
        }

        isPlaying.set(false)
        playbackJob?.cancel()
        releaseResources()
        listener?.onPlaybackStopped()

        Log.i(TAG, "Playback stopped")
    }

    /**
     * Release all resources
     */
    fun release() {
        Log.d(TAG, "Releasing player resources")
        stopPlayback()
        listener = null  // Clear listener reference to prevent memory leaks
        try {
            playbackScope.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error canceling playback scope", e)
        }
    }

    /**
     * Open audio file using the file path from configuration
     */
    private fun openAudioFile(): Boolean {
        wavFile = WavFile(currentConfig.audioFilePath)

        if (!wavFile!!.open() || !wavFile!!.isValid()) {
            handleError("[FILE] Cannot open audio file: ${currentConfig.audioFilePath}")
            return false
        }

        Log.d(
            TAG,
            "Audio file opened: ${wavFile!!.sampleRate}Hz, ${wavFile!!.bitsPerSample}bit, ${wavFile!!.channelCount}ch"
        )
        Log.d(TAG, "File path: ${currentConfig.audioFilePath}")
        return true
    }

    /**
     * Initialize AudioTrack using current configuration parameters
     */
    private fun initializeAudioTrack(): Boolean {
        val wavFile = wavFile ?: return false

        try {
            // Request audio focus
            if (!requestAudioFocus()) {
                handleError("[STREAM] Cannot obtain audio focus")
                return false
            }

            // Validate audio parameters
            if (!validateAudioParameters(wavFile)) {
                abandonAudioFocus()  // Release focus on validation failure
                return false
            }

            // Create AudioTrack
            val channelMask = AudioConstants.getChannelMask(wavFile.channelCount)
            val audioFormat = AudioConstants.getFormatFromBitDepth(wavFile.bitsPerSample)
            val minBufferSize =
                AudioTrack.getMinBufferSize(wavFile.sampleRate, channelMask, audioFormat)

            if (minBufferSize == AudioTrack.ERROR_BAD_VALUE) {
                abandonAudioFocus()  // Release focus on parameter error
                handleError("[PARAM] Unsupported audio parameter combination: ${wavFile.sampleRate}Hz, ${wavFile.channelCount}ch, ${wavFile.bitsPerSample}bit")
                return false
            }

            val bufferSize = minBufferSize * currentConfig.bufferMultiplier
            Log.d(
                TAG,
                "Buffer calculation: minBufferSize=$minBufferSize, multiplier=${currentConfig.bufferMultiplier}, final=$bufferSize"
            )

            // Create AudioAttributes using configuration parameters
            val audioAttributes =
                AudioAttributes.Builder().setUsage(AudioConstants.getUsage(currentConfig.usage))
                    .setContentType(AudioConstants.getContentType(currentConfig.contentType))
                    .build()

            audioTrack = AudioTrack.Builder().setAudioAttributes(audioAttributes).setAudioFormat(
                AudioFormat.Builder().setSampleRate(wavFile.sampleRate).setChannelMask(channelMask)
                    .setEncoding(audioFormat).build()
            ).setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioConstants.getTransferMode(currentConfig.transferMode))
                .setPerformanceMode(AudioConstants.getPerformanceMode(currentConfig.performanceMode))
                .build()

            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                abandonAudioFocus()  // Release focus on initialization failure
                handleError("[STREAM] AudioTrack initialization failed, state: ${audioTrack?.state}")
                return false
            }

            Log.i(
                TAG,
                "AudioTrack initialized successfully - ${wavFile.sampleRate}Hz, ${wavFile.channelDescription}, ${wavFile.bitsPerSample}bit, buffer: $bufferSize bytes"
            )
            Log.i(TAG, "Using configuration: ${currentConfig.description}")

            // Output detailed audio information
            if (wavFile.channelCount >= 10) {
                Log.i(TAG, "3D audio information:")
                Log.i(TAG, "Channel layout: ${wavFile.channelLayout}")
                if (wavFile.channelCount == 12) {
                    Log.i(TAG, "7.1.4 format: includes 4 height channels (Ltf Rtf Ltb Rtb)")
                }
            }

            return true
        } catch (e: Exception) {
            abandonAudioFocus()  // Release focus on exception
            handleError("[STREAM] AudioTrack creation failed: ${e.message}")
            return false
        }
    }

    /**
     * Validate if audio parameters are supported
     */
    private fun validateAudioParameters(wavFile: WavFile): Boolean {
        // Check sample rate
        if (wavFile.sampleRate !in 8000..192000) {
            handleError("[PARAM] Unsupported sample rate: ${wavFile.sampleRate}Hz (supported range: 8000-192000Hz)")
            return false
        }

        // Check channel count - extended to 12 channel support
        if (wavFile.channelCount !in 1..16) {
            handleError("[PARAM] Unsupported channel count: ${wavFile.channelCount} (supported range: 1-16 channels)")
            return false
        }

        // Special identification for 12-channel 7.1.4 configuration
        if (wavFile.channelCount == 12) {
            Log.i(TAG, "Detected 7.1.4 audio configuration (12 channels)")
        }

        // Check bit depth
        if (wavFile.bitsPerSample !in listOf(8, 16, 24, 32)) {
            handleError("[PARAM] Unsupported bit depth: ${wavFile.bitsPerSample}bit (supported: 8/16/24/32bit)")
            return false
        }

        Log.d(
            TAG,
            "Audio parameter validation passed: ${wavFile.sampleRate}Hz, ${wavFile.channelDescription}, ${wavFile.bitsPerSample}bit"
        )
        return true
    }

    /**
     * Request audio focus using current configuration parameters
     */
    private fun requestAudioFocus(): Boolean {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Determine appropriate focus type based on usage scenario
        val focusType = determineFocusType()

        val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            handleFocusChange(focusChange)
        }

        // Create AudioAttributes using configuration parameters
        val audioAttributes =
            AudioAttributes.Builder().setUsage(AudioConstants.getUsage(currentConfig.usage))
                .setContentType(AudioConstants.getContentType(currentConfig.contentType)).build()

        val request = AudioFocusRequest.Builder(focusType).setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(focusChangeListener).setWillPauseWhenDucked(false)
            .setAcceptsDelayedFocusGain(true).build()

        val result =
            audioManager?.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        // Only save request object when focus is successfully obtained
        if (result) {
            focusRequest = request
        }

        return result
    }

    /**
     * Determine appropriate focus type based on usage scenario
     */
    private fun determineFocusType(): Int {
        return when {
            currentConfig.usage.contains("EMERGENCY") || currentConfig.usage.contains("SAFETY") -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT

            currentConfig.usage.contains("NAVIGATION") || currentConfig.usage.contains("ANNOUNCEMENT") -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

            currentConfig.usage.contains("VOICE_COMMUNICATION") -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT

            else -> AudioManager.AUDIOFOCUS_GAIN
        }
    }

    /**
     * Handle audio focus changes with proper recovery
     */
    private fun handleFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus gained, restoring playback")
                audioTrack?.setVolume(1.0f)
                // Resume playback if it was paused due to transient focus loss
                if (isPlaying.get() && audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack?.play()
                }
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                Log.d(TAG, "Audio focus gained transiently")
                audioTrack?.setVolume(1.0f)
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                Log.d(TAG, "Audio focus gained with ducking allowed")
                audioTrack?.setVolume(1.0f)
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "Audio focus lost, stopping playback")
                stopPlayback()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus lost transiently, pausing playback")
                // Pause playback instead of stopping for transient focus loss
                audioTrack?.pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus lost with ducking, reducing volume")
                audioTrack?.setVolume(0.3f)
            }
        }
    }

    /**
     * Release audio focus
     */
    private fun abandonAudioFocus() {
        focusRequest?.let { request ->
            audioManager?.abandonAudioFocusRequest(request)
            focusRequest = null
        }
    }

    /**
     * Start playback loop, using consistent buffer sizing
     */
    private fun startPlaybackLoop() {
        playbackJob = playbackScope.launch {
            val wavFile = wavFile ?: return@launch
            val audioTrack = audioTrack ?: return@launch

            // Use a write buffer that's a fraction of the AudioTrack's internal buffer
            // This ensures smooth playback without underruns
            val audioTrackBufferSize =
                audioTrack.bufferSizeInFrames * wavFile.channelCount * (wavFile.bitsPerSample / 8)
            val writeBufferSize =
                when (AudioConstants.getPerformanceMode(currentConfig.performanceMode)) {
                    AudioTrack.PERFORMANCE_MODE_LOW_LATENCY -> audioTrackBufferSize / 4  // Smaller chunks for low latency
                    AudioTrack.PERFORMANCE_MODE_POWER_SAVING -> audioTrackBufferSize / 2  // Larger chunks for power saving
                    else -> audioTrackBufferSize / 3  // Default: 1/3 of AudioTrack buffer
                }

            val buffer = ByteArray(writeBufferSize)
            var totalBytes = 0L
            val startTime = System.currentTimeMillis()

            audioTrack.play()
            Log.i(
                TAG,
                "Started playing ${wavFile.channelDescription} audio, config: ${currentConfig.description}"
            )
            Log.d(
                TAG,
                "AudioTrack buffer: $audioTrackBufferSize bytes, Write buffer: $writeBufferSize bytes"
            )

            try {
                while (isActive && isPlaying.get()) {
                    val bytesRead = wavFile.readData(buffer, 0, buffer.size)
                    if (bytesRead <= 0) {
                        Log.d(TAG, "File reading completed")
                        break
                    }

                    val bytesWritten = audioTrack.write(buffer, 0, bytesRead)
                    if (bytesWritten < 0) {
                        Log.e(TAG, "AudioTrack write failed: $bytesWritten")
                        break
                    }

                    totalBytes += bytesRead

                    // Periodically output playback progress (every 1MB)
                    if (totalBytes % (1024 * 1024L) == 0L && totalBytes > 0) {
                        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                        val mbPlayed = totalBytes / (1024.0 * 1024.0)
                        Log.d(
                            TAG, "Playback progress: ${
                                String.format(
                                    java.util.Locale.US, "%.1f", mbPlayed
                                )
                            }MB, elapsed: ${String.format(java.util.Locale.US, "%.1f", elapsed)}s"
                        )
                    }
                }

                if (isPlaying.get()) {
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                    val mbTotal = totalBytes / (1024.0 * 1024.0)
                    Log.i(
                        TAG, "Playback completed: ${
                            String.format(
                                java.util.Locale.US, "%.1f", mbTotal
                            )
                        }MB, total time: ${String.format(java.util.Locale.US, "%.1f", elapsed)}s"
                    )
                    stopPlayback()
                }
            } catch (e: Exception) {
                if (isPlaying.get()) {
                    handleError("[STREAM] Playback error: ${e.message}")
                }
            }
        }
    }

    /**
     * Release audio resources consistently
     */
    private fun releaseResources() {
        try {
            audioTrack?.apply {
                if (state == AudioTrack.STATE_INITIALIZED) {
                    stop()
                }
                release()
            }
            audioTrack = null

            abandonAudioFocus()  // Use dedicated method to release audio focus
            audioManager = null

            wavFile?.close()
            wavFile = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources", e)
        }
    }

    /**
     * Handle errors consistently
     */
    private fun handleError(message: String) {
        isPlaying.set(false)  // Stop playback on error
        Log.e(TAG, "Error: $message")
        listener?.onPlaybackError(message)
        releaseResources()
    }

    /**
     * Get detailed configuration information
     */
    fun getDetailedInfo(): String {
        return buildString {
            appendLine("Configuration: ${currentConfig.description}")
            appendLine("Usage: ${currentConfig.usage}")
            appendLine("Content Type: ${currentConfig.contentType}")
            appendLine("Transfer Mode: ${currentConfig.transferMode}")
            appendLine("Performance Mode: ${currentConfig.performanceMode}")
            appendLine("Buffer Multiplier: ${currentConfig.bufferMultiplier}x")
            appendLine("Audio File: ${currentConfig.audioFilePath}")
        }.trim()
    }
}