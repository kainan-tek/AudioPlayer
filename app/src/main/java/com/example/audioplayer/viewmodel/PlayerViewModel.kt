package com.example.audioplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.audioplayer.R
import com.example.audioplayer.config.AudioConfig
import com.example.audioplayer.player.AudioPlayer
import com.example.audioplayer.player.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Concise player ViewModel, manages UI state and audio playback logic
 * Supports loading audio configuration from external JSON files
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val audioPlayer = AudioPlayer(application.applicationContext)
    
    // UI state
    private val _playerState = MutableLiveData(PlayerState.IDLE)
    val playerState: LiveData<PlayerState> = _playerState
    
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
     * Load configuration files
     */
    private fun loadConfigurations() {
        viewModelScope.launch(Dispatchers.IO) {
            val configs = AudioConfig.loadConfigs(getApplication())
            updateUI {
                _availableConfigs.value = configs
                if (configs.isNotEmpty()) {
                    // Set first configuration as default
                    val defaultConfig = configs[0]
                    audioPlayer.setAudioConfig(defaultConfig)
                    _currentConfig.value = defaultConfig
                    _statusMessage.value = "Configuration loaded: ${configs.size} configs"
                }
            }
        }
    }

    /**
     * Reload configuration files
     */
    fun reloadConfigurations() {
        if (_playerState.value == PlayerState.PLAYING) {
            updateUI {
                _statusMessage.value = "Cannot reload configuration while playing"
                _errorMessage.value = "Please stop playback before reloading configuration"
            }
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val configs = AudioConfig.reloadConfigs(getApplication())
                updateUI {
                    if (configs.isNotEmpty()) {
                        _availableConfigs.value = configs
                        // If current configuration is not in new configuration list, set first one as default
                        val currentConfigDescription = _currentConfig.value?.description
                        val newCurrentConfig = configs.find { it.description == currentConfigDescription } 
                            ?: configs[0]
                        
                        audioPlayer.setAudioConfig(newCurrentConfig)
                        _currentConfig.value = newCurrentConfig
                        _statusMessage.value = "Configuration reloaded successfully: ${configs.size} configs"
                    } else {
                        _statusMessage.value = "Configuration file is empty or format error"
                        _errorMessage.value = "No valid audio configuration found"
                    }
                }
            } catch (e: Exception) {
                updateUI {
                    _statusMessage.value = "Configuration reload failed"
                    _errorMessage.value = "Configuration reload failed: ${e.message}"
                }
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
     * Set audio configuration
     */
    fun setAudioConfig(config: AudioConfig) {
        audioPlayer.setAudioConfig(config)
        updateUI {
            _currentConfig.value = config
            _statusMessage.value = "Configuration updated: ${config.description}"
        }
    }
    
    /**
     * Get all available audio configurations
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
                    _playerState.value = PlayerState.PLAYING
                    _statusMessage.value = getApplication<Application>().getString(R.string.status_playing)
                }
            }

            override fun onPlaybackStopped() {
                updateUI {
                    _playerState.value = PlayerState.IDLE
                    _statusMessage.value = getApplication<Application>().getString(R.string.status_stopped)
                }
            }

            override fun onPlaybackError(error: String) {
                updateUI {
                    _playerState.value = PlayerState.ERROR
                    _statusMessage.value = getApplication<Application>().getString(R.string.error_playback_failed)
                    _errorMessage.value = error
                }
            }
        })
    }

    private fun updateUI(block: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            block()
            // Clear error message
            _errorMessage.value = null
        }
    }
}