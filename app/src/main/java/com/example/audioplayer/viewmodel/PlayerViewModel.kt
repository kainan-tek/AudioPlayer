package com.example.audioplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.audioplayer.player.AudioPlayer
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/**
 * ViewModel that manages the audio player state and handles UI interactions
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    // Audio player instance
    private val audioPlayer = AudioPlayer(application.applicationContext)
    
    // UI state LiveData observables
    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _statusMessage = MutableLiveData("Ready")
    val statusMessage: LiveData<String> = _statusMessage

    init {
        // Initialize playback listener to update UI state
        setupPlaybackListener()
    }

    /**
     * Sets up the playback listener to update UI state based on audio player events
     */
    private fun setupPlaybackListener() {
        audioPlayer.setPlaybackListener(object : AudioPlayer.PlaybackListener {
            override fun onPlaybackStarted() {
                updateUiState {
                    _isPlaying.value = true
                    _statusMessage.value = "Playing..."
                }
            }

            override fun onPlaybackStopped() {
                updateUiState {
                    _isPlaying.value = false
                    _statusMessage.value = "Playback stopped"
                }
            }

            override fun onPlaybackError(error: String) {
                updateUiState {
                    _errorMessage.value = error
                    _isPlaying.value = false
                    _statusMessage.value = "Playback error: $error"
                }
            }
        })
    }

    /**
     * Starts audio playback
     */
    fun play() {
        _statusMessage.value = "Preparing to play..."
        
        // Execute potentially long-running operation in background
        viewModelScope.launch(Dispatchers.IO) {
            val success = audioPlayer.play()
            
            if (!success) {
                updateUiState {
                    _statusMessage.value = "Failed to start playback"
                }
            }
        }
    }

    /**
     * Stops audio playback
     */
    fun stop() {
        _statusMessage.value = "Stopping playback..."
        audioPlayer.stop()
    }

    /**
     * Cleans up resources when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
    
    /**
     * Helper method to update UI state safely
     * @param updateBlock Block of code that updates UI state
     */
    private fun updateUiState(updateBlock: () -> Unit) {
        // Use main dispatcher to ensure UI updates on main thread
        viewModelScope.launch(Dispatchers.Main) {
            updateBlock()
        }
    }
}