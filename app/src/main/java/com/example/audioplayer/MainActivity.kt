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
 * steps to use *
adb root
adb remount
adb shell setenforce 0
adb push 48k_2ch_16bit.wav /data/
adb install xxx.apk
 */

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: PlayerViewModel
    private lateinit var playButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusTextView: TextView

    companion object {
        private const val REQUEST_AUDIO_PERMISSIONS = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[PlayerViewModel::class.java]
        
        playButton = findViewById(R.id.button1)
        stopButton = findViewById(R.id.button2)
        statusTextView = findViewById(R.id.statusTextView)

        setupListeners()
        observeViewModel()
        
        checkAndRequestPermissions()
    }

    private fun setupListeners() {
        playButton.setOnClickListener {
            if (hasAudioPermissions()) {
                viewModel.play()
            } else {
                Toast.makeText(this, "Please grant audio permissions", Toast.LENGTH_SHORT).show()
                checkAndRequestPermissions()
            }
        }

        stopButton.setOnClickListener {
            viewModel.stop()
        }
    }

    private fun observeViewModel() {
        viewModel.isPlaying.observe(this) { isPlaying ->
            playButton.isEnabled = !isPlaying
            stopButton.isEnabled = isPlaying
        }

        viewModel.statusMessage.observe(this) { status ->
            statusTextView.text = status
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                // 可以在这里添加Toast提示作为补充
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stop()
    }

    private fun hasAudioPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            REQUEST_AUDIO_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Audio permissions required to play audio files", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
