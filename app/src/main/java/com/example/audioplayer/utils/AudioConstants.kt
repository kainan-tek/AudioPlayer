package com.example.audioplayer.utils

/**
 * Audio-related constants
 */
object AudioConstants {
    // Configuration file paths
    const val EXTERNAL_CONFIG_PATH = "/data/audio_player_configs.json"
    const val ASSETS_CONFIG_FILE = "audio_player_configs.json"
    
    // Default audio file path
    const val DEFAULT_AUDIO_FILE = "/data/48k_2ch_16bit.wav"
    
    // Progress log interval (bytes)
    const val PROGRESS_LOG_INTERVAL = 10 * 1024 * 1024L  // 10MB
    
    // Buffer size configuration
    const val BUFFER_SIZE_12CH = 8192
    const val BUFFER_SIZE_8CH = 6144
    const val BUFFER_SIZE_6CH = 4096
    const val BUFFER_SIZE_DEFAULT = 4096
}