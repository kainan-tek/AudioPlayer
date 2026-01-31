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
import com.example.audioplayer.model.WaveFile
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
    private var waveFile: WaveFile? = null
    
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
    fun play(): Boolean {
        Log.d(TAG, "Starting playback")
        
        if (isPlaying.get()) {
            Log.i(TAG, "Already playing, stopping current playback first")
            stop()
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
            handleError("Permission denied: ${e.message}")
            false
        } catch (e: Exception) {
            handleError("Playback initialization failed: ${e.message}")
            false
        }
    }

    /**
     * Stop playback
     */
    fun stop() {
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
        stop()
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
        waveFile = WaveFile(currentConfig.audioFilePath)
        
        if (!waveFile!!.open() || !waveFile!!.isValid()) {
            handleError("Cannot open audio file: ${currentConfig.audioFilePath}")
            return false
        }
        
        Log.d(TAG, "Audio file opened: ${waveFile!!.sampleRate}Hz, ${waveFile!!.bitsPerSample}bit, ${waveFile!!.channelCount}ch")
        Log.d(TAG, "File path: ${currentConfig.audioFilePath}")
        return true
    }

    /**
     * Initialize AudioTrack using current configuration parameters
     */
    private fun initializeAudioTrack(): Boolean {
        val waveFile = waveFile ?: return false
        
        try {
            // Request audio focus
            if (!requestAudioFocus()) {
                handleError("Cannot obtain audio focus")
                return false
            }

            // Validate audio parameters
            if (!validateAudioParameters(waveFile)) {
                abandonAudioFocus()  // Release focus on validation failure
                return false
            }

            // Create AudioTrack
            val channelMask = AudioConstants.getChannelMask(waveFile.channelCount)
            val audioFormat = AudioConstants.getFormatFromBitDepth(waveFile.bitsPerSample)
            val minBufferSize = AudioTrack.getMinBufferSize(waveFile.sampleRate, channelMask, audioFormat)
            
            if (minBufferSize == AudioTrack.ERROR_BAD_VALUE) {
                abandonAudioFocus()  // Release focus on parameter error
                handleError("Unsupported audio parameter combination: ${waveFile.sampleRate}Hz, ${waveFile.channelCount}ch, ${waveFile.bitsPerSample}bit")
                return false
            }
            
            val bufferSize = minBufferSize * currentConfig.bufferMultiplier
            Log.d(TAG, "Buffer calculation: minBufferSize=$minBufferSize, multiplier=${currentConfig.bufferMultiplier}, final=$bufferSize")

            // Create AudioAttributes using configuration parameters
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioConstants.getUsage(currentConfig.usage))
                .setContentType(AudioConstants.getContentType(currentConfig.contentType))
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(waveFile.sampleRate)
                        .setChannelMask(channelMask)
                        .setEncoding(audioFormat)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioConstants.getTransferMode(currentConfig.transferMode))
                .setPerformanceMode(AudioConstants.getPerformanceMode(currentConfig.performanceMode))
                .build()

            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                abandonAudioFocus()  // Release focus on initialization failure
                handleError("AudioTrack initialization failed, state: ${audioTrack?.state}")
                return false
            }

            Log.i(TAG, "AudioTrack initialized successfully - ${waveFile.sampleRate}Hz, ${waveFile.channelDescription}, ${waveFile.bitsPerSample}bit, buffer: $bufferSize bytes")
            Log.i(TAG, "Using configuration: ${currentConfig.description}")
            
            // Output detailed audio information
            if (waveFile.channelCount >= 10) {
                Log.i(TAG, "3D audio information:")
                Log.i(TAG, "Channel layout: ${waveFile.channelLayout}")
                if (waveFile.channelCount == 12) {
                    Log.i(TAG, "7.1.4 format: includes 4 height channels (Ltf Rtf Ltb Rtb)")
                }
            }
            
            return true
        } catch (e: Exception) {
            abandonAudioFocus()  // Release focus on exception
            handleError("AudioTrack creation failed: ${e.message}")
            return false
        }
    }

    /**
     * Validate if audio parameters are supported
     */
    private fun validateAudioParameters(waveFile: WaveFile): Boolean {
        // Check sample rate
        if (waveFile.sampleRate !in 8000..192000) {
            handleError("Unsupported sample rate: ${waveFile.sampleRate}Hz (supported range: 8000-192000Hz)")
            return false
        }
        
        // Check channel count - extended to 12 channel support
        if (waveFile.channelCount !in 1..16) {
            handleError("Unsupported channel count: ${waveFile.channelCount} (supported range: 1-16 channels)")
            return false
        }
        
        // Special identification for 12-channel 7.1.4 configuration
        if (waveFile.channelCount == 12) {
            Log.i(TAG, "Detected 7.1.4 audio configuration (12 channels)")
        }
        
        // Check bit depth
        if (waveFile.bitsPerSample !in listOf(8, 16, 24, 32)) {
            handleError("Unsupported bit depth: ${waveFile.bitsPerSample}bit (supported: 8/16/24/32bit)")
            return false
        }
        
        Log.d(TAG, "Audio parameter validation passed: ${waveFile.sampleRate}Hz, ${waveFile.channelDescription}, ${waveFile.bitsPerSample}bit")
        return true
    }

    /**
     * Request audio focus using current configuration parameters
     */
    private fun requestAudioFocus(): Boolean {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> audioTrack?.setVolume(1.0f)
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> stop()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> audioTrack?.setVolume(0.3f)
            }
        }

        // Create AudioAttributes using configuration parameters
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioConstants.getUsage(currentConfig.usage))
            .setContentType(AudioConstants.getContentType(currentConfig.contentType))
            .build()

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()

        val result = audioManager?.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        // Only save request object when focus is successfully obtained
        if (result) {
            focusRequest = request
        }
        
        return result
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
            val waveFile = waveFile ?: return@launch
            val audioTrack = audioTrack ?: return@launch
            
            // Use a write buffer that's a fraction of the AudioTrack's internal buffer
            // This ensures smooth playback without underruns
            val audioTrackBufferSize = audioTrack.bufferSizeInFrames * waveFile.channelCount * (waveFile.bitsPerSample / 8)
            val writeBufferSize = when (AudioConstants.getPerformanceMode(currentConfig.performanceMode)) {
                AudioTrack.PERFORMANCE_MODE_LOW_LATENCY -> audioTrackBufferSize / 4  // Smaller chunks for low latency
                AudioTrack.PERFORMANCE_MODE_POWER_SAVING -> audioTrackBufferSize / 2  // Larger chunks for power saving
                else -> audioTrackBufferSize / 3  // Default: 1/3 of AudioTrack buffer
            }
            
            val buffer = ByteArray(writeBufferSize)
            var totalBytes = 0L
            val startTime = System.currentTimeMillis()
            
            audioTrack.play()
            Log.i(TAG, "Started playing ${waveFile.channelDescription} audio, config: ${currentConfig.description}")
            Log.d(TAG, "AudioTrack buffer: $audioTrackBufferSize bytes, Write buffer: $writeBufferSize bytes")
            
            try {
                while (isActive && isPlaying.get()) {
                    val bytesRead = waveFile.readData(buffer, 0, buffer.size)
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
                        Log.d(TAG, "Playback progress: ${String.format(java.util.Locale.US, "%.1f", mbPlayed)}MB, elapsed: ${String.format(java.util.Locale.US, "%.1f", elapsed)}s")
                    }
                }
                
                if (isPlaying.get()) {
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                    val mbTotal = totalBytes / (1024.0 * 1024.0)
                    Log.i(TAG, "Playback completed: ${String.format(java.util.Locale.US, "%.1f", mbTotal)}MB, total time: ${String.format(java.util.Locale.US, "%.1f", elapsed)}s")
                    stop()
                }
            } catch (e: Exception) {
                if (isPlaying.get()) {
                    handleError("Playback error: ${e.message}")
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

            waveFile?.close()
            waveFile = null
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