package com.example.audioplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.audioplayer.R
import com.example.audioplayer.config.AudioConfig
import com.example.audioplayer.player.AudioPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 简洁的播放器ViewModel，管理UI状态和音频播放逻辑
 * 支持从外部JSON文件加载音频配置
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val audioPlayer = AudioPlayer(application.applicationContext)
    
    // UI状态
    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying
    
    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _currentConfig = MutableLiveData<AudioConfig>()
    val currentConfig: LiveData<AudioConfig> = _currentConfig
    
    private val _availableConfigs = MutableLiveData<List<AudioConfig>>()
    val availableConfigs: LiveData<List<AudioConfig>> = _availableConfigs

    init {
        setupAudioPlayerListener()
        loadConfigurations()
        _statusMessage.value = getApplication<Application>().getString(R.string.status_ready)
    }

    /**
     * 加载配置文件
     */
    private fun loadConfigurations() {
        viewModelScope.launch(Dispatchers.IO) {
            val configs = AudioConfig.loadConfigs(getApplication())
            updateUI {
                _availableConfigs.value = configs
                if (configs.isNotEmpty()) {
                    // 设置第一个配置为默认配置
                    val defaultConfig = configs[0]
                    audioPlayer.setAudioConfig(defaultConfig)
                    _currentConfig.value = defaultConfig
                    _statusMessage.value = "配置已加载: ${configs.size} 个配置"
                }
            }
        }
    }

    /**
     * 重新加载配置文件
     */
    fun reloadConfigurations() {
        if (_isPlaying.value == true) {
            updateUI {
                _statusMessage.value = "播放中无法重新加载配置"
            }
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            val configs = AudioConfig.reloadConfigs(getApplication())
            updateUI {
                _availableConfigs.value = configs
                _statusMessage.value = "配置已重新加载: ${configs.size} 个配置"
            }
        }
    }

    fun play() {
        _statusMessage.value = getApplication<Application>().getString(R.string.status_preparing)
        
        viewModelScope.launch(Dispatchers.IO) {
            audioPlayer.play()
        }
    }

    fun stop() {
        _statusMessage.value = getApplication<Application>().getString(R.string.status_stopping)
        audioPlayer.stop()
    }
    
    /**
     * 设置音频配置
     */
    fun setAudioConfig(config: AudioConfig) {
        audioPlayer.setAudioConfig(config)
        _currentConfig.value = config
        updateUI {
            _statusMessage.value = "配置已更新: ${config.description}"
        }
    }
    
    /**
     * 获取所有可用的音频配置
     */
    fun getAllAudioConfigs(): List<AudioConfig> {
        return _availableConfigs.value ?: emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }

    private fun setupAudioPlayerListener() {
        audioPlayer.setPlaybackListener(object : AudioPlayer.PlaybackListener {
            override fun onPlaybackStarted() {
                updateUI {
                    _isPlaying.value = true
                    _statusMessage.value = getApplication<Application>().getString(R.string.status_playing)
                }
            }

            override fun onPlaybackStopped() {
                updateUI {
                    _isPlaying.value = false
                    _statusMessage.value = getApplication<Application>().getString(R.string.status_stopped)
                }
            }

            override fun onPlaybackError(error: String) {
                updateUI {
                    _isPlaying.value = false
                    _statusMessage.value = getApplication<Application>().getString(R.string.error_playback_failed)
                    _errorMessage.value = error
                }
            }
        })
    }

    private fun updateUI(block: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            block()
            // 清除错误消息
            _errorMessage.value = null
        }
    }
}