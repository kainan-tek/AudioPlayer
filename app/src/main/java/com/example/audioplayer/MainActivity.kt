package com.example.audioplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.audioplayer.viewmodel.PlayerViewModel

/**
 * MainActivity - The main entry point for the AudioPlayer application
 * 
 * Usage Steps:
 * 1. adb root
 * 2. adb remount
 * 3. adb shell setenforce 0
 * 4. adb push 48k_2ch_16bit.wav /data/
 * 5. adb install xxx.apk
 */
class MainActivity : AppCompatActivity() {
    // View components
    private lateinit var viewModel: PlayerViewModel
    private lateinit var playButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusTextView: TextView

    // Permission constants
    companion object {
        private const val REQUEST_AUDIO_PERMISSIONS = 1001
    }

    /**
     * Initializes the activity, sets up UI components and checks permissions
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[PlayerViewModel::class.java]
        
        // Initialize UI components
        initViews()
        
        // Set up event listeners and observers
        setupListeners()
        observeViewModel()
        
        // Check and request necessary permissions
        checkAndRequestPermissions()
    }

    /**
     * Initializes UI components by finding views from the layout
     */
    private fun initViews() {
        playButton = findViewById(R.id.button1)
        stopButton = findViewById(R.id.button2)
        statusTextView = findViewById(R.id.statusTextView)
    }

    /**
     * Sets up click listeners for UI buttons
     */
    private fun setupListeners() {
        playButton.setOnClickListener {
            if (hasAudioPermissions()) {
                viewModel.play()
            } else {
                Toast.makeText(this, "请授予音频权限", Toast.LENGTH_SHORT).show()
                checkAndRequestPermissions()
            }
        }

        stopButton.setOnClickListener {
            viewModel.stop()
        }
    }

    /**
     * Observes LiveData from ViewModel to update UI state
     */
    private fun observeViewModel() {
        viewModel.isPlaying.observe(this) { isPlaying ->
            // Update button states based on playback status
            playButton.isEnabled = !isPlaying
            stopButton.isEnabled = isPlaying
        }

        viewModel.statusMessage.observe(this) { status ->
            // Update status text
            statusTextView.text = status
        }

        viewModel.errorMessage.observe(this) { error ->
            // Show error toast message
            error?.let {
                Toast.makeText(this, "错误: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Stops playback when activity is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        viewModel.stop()
    }

    /**
     * Checks if the app has the necessary audio permissions
     * @return true if permissions are granted, false otherwise
     */
    private fun hasAudioPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks and requests audio permissions if not already granted
     */
    private fun checkAndRequestPermissions() {
        if (!hasAudioPermissions()) {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            ActivityCompat.requestPermissions(this, permissions, REQUEST_AUDIO_PERMISSIONS)
        }
    }

    /**
     * Handles the result of permission requests
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            REQUEST_AUDIO_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "播放音频文件需要音频权限", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
