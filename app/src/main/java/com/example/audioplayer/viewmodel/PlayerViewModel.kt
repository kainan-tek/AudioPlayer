package com.example.audioplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.audioplayer.player.AudioPlayer

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val audioPlayer = AudioPlayer(application.applicationContext)
    
    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _statusMessage = MutableLiveData("Ready")
    val statusMessage: LiveData<String> = _statusMessage

    init {
        audioPlayer.setPlaybackListener(object : AudioPlayer.PlaybackListener {
            override fun onPlaybackStarted() {
                _isPlaying.postValue(true)
                _statusMessage.postValue("Playing...")
            }

            override fun onPlaybackStopped() {
                _isPlaying.postValue(false)
                _statusMessage.postValue("Playback stopped")
            }

            override fun onPlaybackError(error: String) {
                _errorMessage.postValue(error)
                _isPlaying.postValue(false)
                _statusMessage.postValue("Playback error: $error")
            }
        })
    }

    fun play() {
        _statusMessage.value = "Preparing to play..."
        if (!audioPlayer.play()) {
            _statusMessage.value = "Failed to start playback"
        }
    }

    fun stop() {
        _statusMessage.value = "Stopping playback..."
        audioPlayer.stop()
    }

    fun isPlaying(): Boolean {
        return audioPlayer.isPlaying()
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}