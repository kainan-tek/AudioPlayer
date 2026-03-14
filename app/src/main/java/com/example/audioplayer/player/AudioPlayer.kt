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

enum class PlayerState {
    IDLE, PLAYING, ERROR
}

/**
 * Audio player using AudioTrack API
 */
class AudioPlayer(private val context: Context) {

    companion object {
        private const val TAG = "AudioPlayer"
    }

    private var audioTrack: AudioTrack? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wavFile: WavFile? = null

    @Volatile
    private var state = PlayerState.IDLE
    private var playbackJob: Job? = null
    private val playbackScope = CoroutineScope(Dispatchers.IO)
    private var currentConfig: AudioConfig = AudioConfig()

    // Playback listener
    interface PlaybackListener {
        fun onPlaybackStarted()
        fun onPlaybackStopped()
        fun onPlaybackError(error: String)
    }

    private var listener: PlaybackListener? = null

    fun setPlaybackListener(listener: PlaybackListener?) {
        this.listener = listener
    }

    fun setAudioConfig(config: AudioConfig) {
        if (state == PlayerState.PLAYING) {
            Log.w(TAG, "Cannot change configuration while playing")
            return
        }
        currentConfig = config
        Log.i(TAG, "Configuration updated: ${config.description}")
    }

    fun startPlayback(): Boolean {
        Log.d(TAG, "Starting playback")

        if (state == PlayerState.PLAYING) {
            Log.w(TAG, "Already playing")
            listener?.onPlaybackError("Already playing")
            return false
        }
        if (state == PlayerState.ERROR) {
            state = PlayerState.IDLE
        }

        return try {
            if (!openAudioFile()) {
                return false
            }
            if (!initializeAudioTrack()) {
                return false
            }

            state = PlayerState.PLAYING
            startPlaybackLoop()
            listener?.onPlaybackStarted()

            Log.i(TAG, "Playback started successfully")
            true
        } catch (e: SecurityException) {
            handleError("${AudioConstants.ErrorTypes.PERMISSION} Permission denied: ${e.message}")
            false
        } catch (e: Exception) {
            handleError("${AudioConstants.ErrorTypes.STREAM} Playback initialization failed: ${e.message}")
            false
        }
    }

    fun stopPlayback() {
        Log.d(TAG, "Stopping playback")

        if (state != PlayerState.PLAYING) {
            return
        }

        state = PlayerState.IDLE
        playbackJob?.cancel()
        releaseResources()
        listener?.onPlaybackStopped()

        Log.i(TAG, "Playback stopped")
    }

    fun release() {
        stopPlayback()
        listener = null
        try {
            playbackScope.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error canceling playback scope", e)
        }
        Log.d(TAG, "AudioPlayer resources released")
    }

    fun isPlaying(): Boolean {
        return state == PlayerState.PLAYING
    }

    /**
     * Open audio file using the file path from configuration
     */
    private fun openAudioFile(): Boolean {
        wavFile = WavFile(currentConfig.audioFilePath)

        if (!wavFile!!.open() || !wavFile!!.isValid()) {
            handleError("${AudioConstants.ErrorTypes.FILE} Cannot open audio file: ${currentConfig.audioFilePath}")
            return false
        }

        Log.d(
            TAG,
            "Audio file opened: ${wavFile!!.sampleRate}Hz, ${wavFile!!.bitsPerSample}bit, ${wavFile!!.channelCount}ch"
        )
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
                handleError("${AudioConstants.ErrorTypes.FOCUS} Cannot obtain audio focus")
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
                handleError("${AudioConstants.ErrorTypes.PARAM} Unsupported audio parameter combination: ${wavFile.sampleRate}Hz, ${wavFile.channelCount}ch, ${wavFile.bitsPerSample}bit")
                return false
            }

            val bufferSize = minBufferSize * currentConfig.bufferMultiplier

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
                handleError("${AudioConstants.ErrorTypes.STREAM} AudioTrack initialization failed, state: ${audioTrack?.state}")
                return false
            }

            Log.i(
                TAG,
                "AudioTrack initialized - ${wavFile.sampleRate}Hz, ${wavFile.channelDescription}, ${wavFile.bitsPerSample}bit"
            )

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
            handleError("${AudioConstants.ErrorTypes.STREAM} AudioTrack creation failed: ${e.message}")
            return false
        }
    }

    private fun validateAudioParameters(wavFile: WavFile): Boolean {
        if (!AudioConstants.isValidSampleRate(wavFile.sampleRate)) {
            handleError("${AudioConstants.ErrorTypes.PARAM} Unsupported sample rate: ${wavFile.sampleRate}Hz (supported range: 8000-192000Hz)")
            return false
        }

        if (!AudioConstants.isValidChannelCount(wavFile.channelCount)) {
            handleError("${AudioConstants.ErrorTypes.PARAM} Unsupported channel count: ${wavFile.channelCount} (supported range: 1-16 channels)")
            return false
        }

        if (wavFile.channelCount == 12) {
            Log.i(TAG, "Detected 7.1.4 audio configuration (12 channels)")
        }

        if (!AudioConstants.isValidBitDepth(wavFile.bitsPerSample)) {
            handleError("${AudioConstants.ErrorTypes.PARAM} Unsupported bit depth: ${wavFile.bitsPerSample}bit (supported: 8/16/24/32bit)")
            return false
        }

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
            .setAcceptsDelayedFocusGain(false).build()

        val result =
            audioManager?.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        // Only save request object when focus is successfully obtained
        if (result) {
            audioFocusRequest = request
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
     * Note: UI doesn't support pause, so all focus loss results in stop
     */
    private fun handleFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "Audio focus lost, stopping playback")
                stopPlayback()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus lost transiently, stopping playback")
                stopPlayback()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus lost with ducking, stopping playback")
                stopPlayback()
            }
        }
    }

    /**
     * Release audio focus
     */
    private fun abandonAudioFocus() {
        audioFocusRequest?.let { request ->
            audioManager?.abandonAudioFocusRequest(request)
            audioFocusRequest = null
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

            audioTrack.play()

            try {
                while (isActive && state == PlayerState.PLAYING) {
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

                    // Log progress every 5MB
                    if (totalBytes % (5 * 1024 * 1024L) == 0L && totalBytes > 0) {
                        val mbPlayed = totalBytes / (1024.0 * 1024.0)
                        Log.v(TAG, "Progress: %.1fMB".format(mbPlayed))
                    }
                }

                if (state == PlayerState.PLAYING) {
                    val mbTotal = totalBytes / (1024.0 * 1024.0)
                    Log.i(TAG, "Playback completed: %.1fMB".format(mbTotal))
                    stopPlayback()
                }
            } catch (e: Exception) {
                if (state == PlayerState.PLAYING) {
                    handleError("${AudioConstants.ErrorTypes.STREAM} Playback error: ${e.message}")
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
                if (this.state == AudioTrack.STATE_INITIALIZED) {
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
        state = PlayerState.ERROR
        Log.e(TAG, "Error: $message")
        listener?.onPlaybackError(message)
        releaseResources()
    }
}