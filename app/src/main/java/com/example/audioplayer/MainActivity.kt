package com.example.audioplayer

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.audioplayer.viewmodel.PlayerViewModel
import com.google.android.material.button.MaterialButton

/**
 * 简洁的音频播放器主界面
 * 支持从外部JSON文件加载音频配置，方便测试不同场景
 * 
 * 使用说明:
 * 1. adb root && adb remount && adb shell setenforce 0
 * 2. adb push 48k_2ch_16bit.wav /data/
 * 3. adb push 96k_8ch_24bit.wav /data/  (可选，用于高质量音频测试)
 * 4. adb push 48k_12ch_16bit.wav /data/ (可选，用于多声道测试)
 * 5. 安装并运行应用 (首次运行会在/data/创建audio_configs.json)
 * 6. 修改 /data/audio_configs.json 文件来自定义配置
 * 7. 在应用中点击"配置"按钮，选择"重新加载配置文件"来应用更改
 * 
 * 系统要求: Android 13 (API 33+)
 * 
 * JSON配置文件格式:
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
 *       "description": "自定义配置名称"
 *     }
 *   ]
 * }
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var viewModel: PlayerViewModel
    private lateinit var playButton: MaterialButton
    private lateinit var stopButton: MaterialButton
    private lateinit var configButton: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var fileInfoText: TextView

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        initViewModel()
        setupClickListeners()
        checkPermissions()
    }

    private fun initViews() {
        playButton = findViewById(R.id.playButton)
        stopButton = findViewById(R.id.stopButton)
        configButton = findViewById(R.id.configButton)
        statusText = findViewById(R.id.statusTextView)
        fileInfoText = findViewById(R.id.fileInfoTextView)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[PlayerViewModel::class.java]
        
        // 观察播放状态
        viewModel.isPlaying.observe(this) { isPlaying ->
            playButton.isEnabled = !isPlaying
            stopButton.isEnabled = isPlaying
            configButton.isEnabled = !isPlaying  // 播放时禁用配置更改
        }
        
        // 观察状态消息
        viewModel.statusMessage.observe(this) { message ->
            statusText.text = message
        }
        
        // 观察错误消息
        viewModel.errorMessage.observe(this) { error ->
            error?.let { showToast("错误: $it") }
        }
        
        // 观察当前配置
        viewModel.currentConfig.observe(this) { config ->
            config?.let {
                configButton.text = getString(R.string.audio_config_format, it.description)
            }
        }
        
        // 观察可用配置
        viewModel.availableConfigs.observe(this) { configs ->
            Log.d("MainActivity", "可用配置数量: ${configs.size}")
        }
    }

    private fun setupClickListeners() {
        playButton.setOnClickListener {
            if (hasAudioPermission()) {
                viewModel.play()
            } else {
                showToast(getString(R.string.error_permission_denied))
                requestAudioPermission()
            }
        }
        
        stopButton.setOnClickListener {
            viewModel.stop()
        }
        
        configButton.setOnClickListener {
            showConfigSelectionDialog()
        }
    }

    /**
     * 显示配置选择对话框
     */
    private fun showConfigSelectionDialog() {
        val configs = viewModel.getAllAudioConfigs()
        if (configs.isEmpty()) {
            showToast("没有可用的配置")
            return
        }
        
        val configNames = configs.map { it.description }.toMutableList()
        configNames.add("🔄 重新加载配置文件")
        
        AlertDialog.Builder(this)
            .setTitle("选择音频配置 (${configs.size} 个配置)")
            .setItems(configNames.toTypedArray()) { _, which ->
                if (which == configs.size) {
                    // 重新加载配置
                    viewModel.reloadConfigurations()
                    showToast("正在重新加载配置文件...")
                } else {
                    // 选择配置
                    val selectedConfig = configs[which]
                    viewModel.setAudioConfig(selectedConfig)
                    showToast("已切换到: ${selectedConfig.description}")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun checkPermissions() {
        if (!hasAudioPermission()) {
            requestAudioPermission()
        }
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast(getString(R.string.permission_granted))
            } else {
                showToast(getString(R.string.permission_required))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stop()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
