package com.example.audioplayer

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.audioplayer.player.PlayerState
import com.example.audioplayer.viewmodel.PlayerViewModel

/**
 * Concise audio player main interface
 * Supports loading audio configurations from external JSON files for convenient testing of different scenarios
 * 
 * Usage instructions:
 * 1. adb root && adb remount && adb shell setenforce 0
 * 2. adb push 48k_2ch_16bit.wav /data/
 * 3. adb push 96k_8ch_24bit.wav /data/  (optional, for high-quality audio testing)
 * 4. adb push 48k_12ch_16bit.wav /data/ (optional, for multi-channel testing)
 * 5. (optional) adb push audio_player_configs.json /data/ (custom configuration file)
 * 6. Install and run the application
 * 7. If /data/audio_player_configs.json exists, the app will use external configuration; otherwise use built-in configuration
 * 8. In the app, click the "Configuration" button to select different audio configurations for testing
 * 
 * System requirements: Android 13 (API 33+)
 * 
 * JSON configuration file format:
 * {
 *   "configs": [
 *     {
 *       "usage": "MEDIA",
 *       "contentType": "MUSIC", 
 *       "transferMode": "STREAM",
 *       "performanceMode": "LOW_LATENCY",
 *       "bufferMultiplier": 2,
 *       "audioFilePath": "/data/your_audio_file.wav",
 *       "minBufferSize": 480,
 *       "description": "Custom configuration name"
 *     }
 *   ]
 * }
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var viewModel: PlayerViewModel
    private lateinit var playButton: Button
    private lateinit var stopButton: Button
    private lateinit var configButton: Button
    private lateinit var statusText: TextView
    private lateinit var fileInfoText: TextView

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        initViewModel()
        setupClickListeners()
        if (!hasAudioPermission()) requestAudioPermission()
    }

    private fun initViews() {
        playButton = findViewById(R.id.playButton)
        stopButton = findViewById(R.id.stopButton)
        configButton = findViewById(R.id.configButton)
        statusText = findViewById(R.id.statusTextView)
        fileInfoText = findViewById(R.id.playbackInfoTextView)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[PlayerViewModel::class.java]
        
        // Observe playback state
        viewModel.playerState.observe(this) { state ->
            updateButtonStates(state)
            updatePlaybackInfo()
        }
        
        // Observe status messages
        viewModel.statusMessage.observe(this) { message ->
            statusText.text = message
        }
        
        // Observe error messages
        viewModel.errorMessage.observe(this) { error ->
            error?.let { handleError(it) }
        }
        
        // Observe current configuration
        viewModel.currentConfig.observe(this) { config ->
            config?.let {
                configButton.text = getString(R.string.audio_config_format, it.description)
                updatePlaybackInfo()
            }
        }
    }

    private fun setupClickListeners() {
        playButton.setOnClickListener {
            if (!hasAudioPermission()) {
                requestAudioPermission()
                return@setOnClickListener
            }
            viewModel.play()
        }
        
        stopButton.setOnClickListener {
            viewModel.stop()
        }
        
        configButton.setOnClickListener {
            showConfigSelectionDialog()
        }
    }

    /**
     * Update button states based on playback state
     */
    private fun updateButtonStates(state: PlayerState) {
        when (state) {
            PlayerState.IDLE -> {
                playButton.isEnabled = true
                stopButton.isEnabled = false
                configButton.isEnabled = true
            }
            PlayerState.PLAYING -> {
                playButton.isEnabled = false
                stopButton.isEnabled = true
                configButton.isEnabled = false  // Disable configuration changes during playback
            }
            PlayerState.ERROR -> {
                playButton.isEnabled = true
                stopButton.isEnabled = false
                configButton.isEnabled = true
            }
        }
    }

    /**
     * Handle audio playback errors
     */
    private fun handleError(error: String) {
        Log.e(TAG, "Audio playback error: $error")
        showToast("Playback error: $error")
        
        // Reset playback state
        resetPlayerState()
    }
    
    /**
     * Reset player state
     */
    private fun resetPlayerState() {
        updateButtonStates(PlayerState.IDLE)
        statusText.text = getString(R.string.status_ready)
    }

    /**
     * Show configuration selection dialog
     */
    private fun showConfigSelectionDialog() {
        val configs = viewModel.getAllAudioConfigs()
        if (configs.isEmpty()) {
            showToast("No available configurations")
            return
        }
        
        val configNames = configs.map { it.description }.toMutableList()
        configNames.add("🔄 Reload configuration file")
        
        AlertDialog.Builder(this)
            .setTitle("Select Audio Configuration (${configs.size} configurations)")
            .setItems(configNames.toTypedArray()) { _, which ->
                if (which == configs.size) {
                    // Reload configurations
                    reloadConfigurations()
                } else {
                    // Select configuration
                    val selectedConfig = configs[which]
                    viewModel.setAudioConfig(selectedConfig)
                    showToast("Switched to: ${selectedConfig.description}")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Reload configuration file
     */
    private fun reloadConfigurations() {
        try {
            viewModel.reloadConfigurations()
            showToast("Reloading configuration file...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reload configurations", e)
            showToast("Configuration reload failed: ${e.message}")
        }
    }

    private fun hasAudioPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) and above use READ_MEDIA_AUDIO
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12 (API 32) and below use READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestAudioPermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            // Show explanation dialog
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app needs audio file access permission to play audio files.")
                .setPositiveButton("Grant") { _, _ ->
                    ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            val message = if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getString(R.string.permission_granted)
            } else {
                getString(R.string.permission_required)
            }
            showToast(message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            viewModel.stop()
            Log.d(TAG, "AudioPlayer resources released successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioPlayer resources", e)
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Pause playback when app goes to background
        if (viewModel.playerState.value == PlayerState.PLAYING) {
            viewModel.stop()
            Log.d(TAG, "Playback paused due to app going to background")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SetTextI18n")
    private fun updatePlaybackInfo() {
        viewModel.currentConfig.value?.let { config ->
            val configInfo = "Current Config: ${config.description}\n" +
                    "Usage: ${config.usage} | ${config.contentType}\n" +
                    "Mode: ${config.performanceMode} | ${config.transferMode}\n" +
                    "File: ${config.audioFilePath}"
            fileInfoText.text = configInfo
        } ?: run {
            fileInfoText.text = "Configuration Info"
        }
    }
}
