package com.example.audioplayer

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.audioplayer.player.PlayerState
import com.example.audioplayer.viewmodel.PlayerViewModel


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: PlayerViewModel
    private lateinit var playButton: Button
    private lateinit var stopButton: Button
    private lateinit var configSpinner: Spinner
    private lateinit var statusText: TextView
    private lateinit var playbackInfoText: TextView

    private var isSpinnerInitialized = false

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
        configSpinner = findViewById(R.id.configSpinner)
        statusText = findViewById(R.id.statusTextView)
        playbackInfoText = findViewById(R.id.playbackInfoTextView)
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
                updatePlaybackInfo()
                updateSpinnerSelection(it.description)
                // Initialize spinner when config is first loaded
                if (configSpinner.adapter == null) {
                    setupConfigSpinner()
                }
            }
        }
    }

    private fun setupClickListeners() {
        playButton.setOnClickListener {
            if (!hasAudioPermission()) {
                requestAudioPermission()
                return@setOnClickListener
            }
            viewModel.startPlayback()
        }

        stopButton.setOnClickListener {
            viewModel.stopPlayback()
        }
    }

    /**
     * Setup configuration spinner
     */
    private fun setupConfigSpinner() {
        val configs = viewModel.getAllAudioConfigs()
        Log.d(TAG, "Setting up config spinner with ${configs.size} configurations")

        if (configs.isEmpty()) {
            Log.w(TAG, "No configurations available for spinner")
            return
        }

        val configNames = configs.map { it.description }
        Log.d(TAG, "Config names: $configNames")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, configNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        configSpinner.adapter = adapter

        // Set initial selection
        val currentConfig = viewModel.currentConfig.value
        currentConfig?.let {
            val index = configs.indexOfFirst { config -> config.description == it.description }
            if (index >= 0) {
                configSpinner.setSelection(index)
                Log.d(TAG, "Set initial spinner selection to index $index: ${it.description}")
            }
        }

        configSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    Log.d(TAG, "Spinner initialized, skipping first selection")
                    return
                }

                val selectedConfig = configs[position]
                Log.d(TAG, "Config selected: ${selectedConfig.description}")
                viewModel.setAudioConfig(selectedConfig)
                showToast("Switched to: ${selectedConfig.description}")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "Nothing selected in spinner")
            }
        }

        // Add long press listener to reload configurations
        configSpinner.setOnLongClickListener {
            Log.d(TAG, "Long press detected on spinner")
            reloadConfigurations()
            true
        }
    }

    /**
     * Update spinner selection based on config description
     */
    private fun updateSpinnerSelection(description: String) {
        val configs = viewModel.getAllAudioConfigs()
        val index = configs.indexOfFirst { it.description == description }
        if (index >= 0 && index != configSpinner.selectedItemPosition) {
            isSpinnerInitialized = false
            configSpinner.setSelection(index)
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
                configSpinner.isEnabled = true
            }

            PlayerState.PLAYING -> {
                playButton.isEnabled = false
                stopButton.isEnabled = true
                configSpinner.isEnabled = false  // Disable configuration changes during playback
            }

            PlayerState.ERROR -> {
                playButton.isEnabled = true
                stopButton.isEnabled = false
                configSpinner.isEnabled = true
            }
        }
    }

    /**
     * Handle audio playback errors with user-friendly messages
     */
    @SuppressLint("SetTextI18n")
    private fun handleError(error: String) {
        Log.e(TAG, "Audio playback error: $error")

        // Convert technical error to user-friendly message
        val userMessage = getUserFriendlyErrorMessage(error)

        // Show user-friendly dialog
        AlertDialog.Builder(this).setTitle("Playback Error").setMessage(userMessage)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                // Clear error state when user dismisses the dialog
                viewModel.clearError()
            }.setCancelable(true).setOnCancelListener {
                // Also clear error state when dialog is canceled
                viewModel.clearError()
            }.show()

        statusText.text = "Error: $userMessage"

        // Reset playback state
        updateButtonStates(PlayerState.ERROR)
    }

    /**
     * Convert technical error message to user-friendly message
     */
    private fun getUserFriendlyErrorMessage(error: String): String {
        return when {
            error.startsWith(
                "[FILE]", ignoreCase = true
            ) -> "Unable to open audio file. The file may be corrupted or inaccessible."

            error.startsWith(
                "[STREAM]", ignoreCase = true
            ) -> "Audio system initialization failed. Please try again."

            error.startsWith(
                "[PERMISSION]", ignoreCase = true
            ) -> "Audio file access permission is required. Please grant the permission in Settings."

            error.startsWith(
                "[PARAM]", ignoreCase = true
            ) -> "Invalid audio configuration. Please select a different configuration."

            else -> "Playback failed. Please try again."
        }
    }

    /**
     * Reload configuration file
     */
    private fun reloadConfigurations() {
        try {
            viewModel.reloadConfigurations()
            showToast("Configuration reloaded successfully")
            // Refresh spinner after reload
            isSpinnerInitialized = false
            setupConfigSpinner()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reload configurations", e)
            showToast("Configuration reload failed: ${e.message}")
        }
    }

    /**
     * Get required permissions based on Android version
     */
    private fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
            }

            else -> {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    /**
     * Check if all required permissions are granted
     */
    private fun hasAudioPermission(): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request required permissions
     */
    private fun requestAudioPermission() {
        val permissions = getRequiredPermissions()
        val deniedPermissions = permissions.filter {
            ActivityCompat.shouldShowRequestPermissionRationale(this, it)
        }

        if (deniedPermissions.isNotEmpty()) {
            AlertDialog.Builder(this).setTitle("Permission Required")
                .setMessage("This app needs audio file access permission to play audio files.")
                .setPositiveButton("Grant") { _, _ ->
                    ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
                }.setNegativeButton("Cancel", null).show()
        } else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            val message = if (allGranted) {
                getString(R.string.permission_granted)
            } else {
                val deniedCount = grantResults.count { it != PackageManager.PERMISSION_GRANTED }
                "${getString(R.string.permission_required)} ($deniedCount permission(s) denied)"
            }
            showToast(message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            viewModel.stopPlayback()
            Log.d(TAG, "AudioPlayer resources released successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioPlayer resources", e)
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause playback when app goes to background
        if (viewModel.playerState.value == PlayerState.PLAYING) {
            viewModel.stopPlayback()
            Log.d(TAG, "Playback paused due to app going to background")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SetTextI18n")
    private fun updatePlaybackInfo() {
        viewModel.currentConfig.value?.let { config ->
            val configInfo =
                "Current Config: ${config.description}\n" + "Usage: ${config.usage} | ${config.contentType}\n" + "Mode: ${config.performanceMode} | ${config.transferMode}\n" + "File: ${config.audioFilePath}"
            playbackInfoText.text = configInfo
        } ?: run {
            playbackInfoText.text = "Configuration Info"
        }
    }
}
