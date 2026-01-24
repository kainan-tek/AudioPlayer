package com.example.audioplayer.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.audioplayer.config.AudioConfig
import com.example.audioplayer.model.WaveFile
import com.example.audioplayer.utils.AudioConstants
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
        Log.d(TAG, config.getDetailedInfo())
    }

    /**
     * Start audio playback
     */
    fun play(): Boolean {
        Log.d(TAG, "Starting playback")
        
        // Check permissions
        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) and above use READ_MEDIA_AUDIO
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12 (API 32) and below use READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        
        if (!hasPermission) {
            handleError("Audio file read permission not granted")
            return false
        }
        
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
        playbackScope.cancel()
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
            val channelMask = getChannelMask(waveFile.channelCount)
            val audioFormat = getAudioFormat(waveFile.bitsPerSample)
            val minBufferSize = AudioTrack.getMinBufferSize(waveFile.sampleRate, channelMask, audioFormat)
            
            if (minBufferSize == AudioTrack.ERROR_BAD_VALUE) {
                abandonAudioFocus()  // Release focus on parameter error
                handleError("Unsupported audio parameter combination: ${waveFile.sampleRate}Hz, ${waveFile.channelCount}ch, ${waveFile.bitsPerSample}bit")
                return false
            }
            
            val bufferSize = maxOf(minBufferSize * currentConfig.bufferMultiplier, currentConfig.minBufferSize)

            // Create AudioAttributes using configuration parameters
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(currentConfig.usage)
                .setContentType(currentConfig.contentType)
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
                .setTransferMode(currentConfig.transferMode)
                .setPerformanceMode(currentConfig.performanceMode)
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
            .setUsage(currentConfig.usage)
            .setContentType(currentConfig.contentType)
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
     * Start playback loop, adjusting buffer based on configuration and channel count
     */
    private fun startPlaybackLoop() {
        playbackJob = playbackScope.launch {
            val waveFile = waveFile ?: return@launch
            
            // Adjust buffer size based on configuration and channel count
            val baseBufferSize = when {
                waveFile.channelCount >= 12 -> AudioConstants.BUFFER_SIZE_12CH
                waveFile.channelCount >= 8 -> AudioConstants.BUFFER_SIZE_8CH
                waveFile.channelCount >= 6 -> AudioConstants.BUFFER_SIZE_6CH
                else -> AudioConstants.BUFFER_SIZE_DEFAULT
            }
            
            // Adjust buffer based on performance mode
            val bufferSize = when (currentConfig.performanceMode) {
                AudioTrack.PERFORMANCE_MODE_LOW_LATENCY -> baseBufferSize / 2  // Low latency mode uses smaller buffer
                AudioTrack.PERFORMANCE_MODE_POWER_SAVING -> baseBufferSize * 2  // Power saving mode uses larger buffer
                else -> baseBufferSize
            }
            
            val buffer = ByteArray(bufferSize)
            var totalBytes = 0L
            val startTime = System.currentTimeMillis()
            
            audioTrack?.play()
            Log.i(TAG, "Started playing ${waveFile.channelDescription} audio, config: ${currentConfig.description}, buffer: $bufferSize bytes")
            
            try {
                while (isActive && isPlaying.get()) {
                    val bytesRead = waveFile.readData(buffer, 0, buffer.size)
                    
                    if (bytesRead <= 0) {
                        Log.d(TAG, "File reading completed")
                        break
                    }
                    
                    val bytesWritten = audioTrack?.write(buffer, 0, bytesRead) ?: -1
                    if (bytesWritten < 0) {
                        Log.e(TAG, "AudioTrack write failed: $bytesWritten")
                        break
                    }
                    
                    totalBytes += bytesRead
                    
                    // Periodically output playback progress
                    if (totalBytes % AudioConstants.PROGRESS_LOG_INTERVAL == 0L && totalBytes > 0) {
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
     * Release resources
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
     * Handle errors
     */
    private fun handleError(message: String) {
        isPlaying.set(false)  // Stop playback on error
        Log.e(TAG, message)
        listener?.onPlaybackError(message)
        releaseResources()
    }

    /**
     * Get channel mask, supporting multi-channel audio including 7.1.4 (12 channels)
     */
    private fun getChannelMask(channelCount: Int): Int {
        val channelMasks = mapOf(
            1 to AudioFormat.CHANNEL_OUT_MONO,
            2 to AudioFormat.CHANNEL_OUT_STEREO,
            4 to AudioFormat.CHANNEL_OUT_QUAD,
            6 to AudioFormat.CHANNEL_OUT_5POINT1,
            8 to AudioFormat.CHANNEL_OUT_7POINT1_SURROUND,
            10 to AudioFormat.CHANNEL_OUT_5POINT1POINT4,
            12 to AudioFormat.CHANNEL_OUT_7POINT1POINT4
        )
        
        return channelMasks[channelCount] ?: run {
            Log.w(TAG, "Unsupported channel count: $channelCount, using stereo playback")
            AudioFormat.CHANNEL_OUT_STEREO
        }
    }

    /**
     * Get audio format, supporting multiple bit depths
     */
    private fun getAudioFormat(bitsPerSample: Int): Int {
        val audioFormats = mapOf(
            8 to AudioFormat.ENCODING_PCM_8BIT,
            16 to AudioFormat.ENCODING_PCM_16BIT,
            24 to AudioFormat.ENCODING_PCM_24BIT_PACKED,
            32 to AudioFormat.ENCODING_PCM_32BIT
        )
        
        return audioFormats[bitsPerSample] ?: run {
            Log.w(TAG, "Unsupported bit depth: $bitsPerSample, using 16-bit")
            AudioFormat.ENCODING_PCM_16BIT
        }
    }
}