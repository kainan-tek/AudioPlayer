package com.example.audioplayer.config

import android.content.Context
import android.util.Log
import com.example.audioplayer.common.AudioConstants
import org.json.JSONObject
import java.io.File

/**
 * Audio configuration data class
 * Includes configuration management functionality, supports loading configuration from external JSON files
 */
data class AudioConfig(
    val usage: String = "USAGE_MEDIA",
    val contentType: String = "CONTENT_TYPE_MUSIC", 
    val transferMode: String = "MODE_STREAM",
    val performanceMode: String = "PERFORMANCE_MODE_POWER_SAVING",
    val bufferMultiplier: Int = 2,
    val audioFilePath: String = AudioConstants.DEFAULT_AUDIO_FILE,
    val description: String = "Default configuration (power saving mode)"
) {
    companion object {
        private const val TAG = "AudioConfig"

        /**
         * Load configurations from external file or assets
         */
        fun loadConfigs(context: Context): List<AudioConfig> {
            return try {
                val externalFile = File(AudioConstants.CONFIG_FILE_PATH)
                val jsonString = if (externalFile.exists()) {
                    Log.i(TAG, "Loading configuration from external file")
                    externalFile.readText()
                } else {
                    Log.i(TAG, "Loading configuration from assets")
                    context.assets.open(AudioConstants.ASSETS_CONFIG_FILE).bufferedReader().use { it.readText() }
                }
                parseConfigs(jsonString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load configurations", e)
                getDefaultConfigs()
            }
        }

        /**
         * Reload configurations (used after configuration file updates)
         */
        fun reloadConfigs(context: Context): List<AudioConfig> {
            Log.i(TAG, "Reloading configuration file")
            return loadConfigs(context)
        }

        private fun parseConfigs(jsonString: String): List<AudioConfig> {
            val configsArray = JSONObject(jsonString).getJSONArray("configs")
            return (0 until configsArray.length()).map { i ->
                val config = configsArray.getJSONObject(i)
                AudioConfig(
                    usage = config.optString("usage", "USAGE_MEDIA"),
                    contentType = config.optString("contentType", "CONTENT_TYPE_MUSIC"),
                    transferMode = config.optString("transferMode", "MODE_STREAM"),
                    performanceMode = config.optString("performanceMode", "PERFORMANCE_MODE_POWER_SAVING"),
                    bufferMultiplier = config.optInt("bufferMultiplier", 2),
                    audioFilePath = config.optString("audioFilePath", AudioConstants.DEFAULT_AUDIO_FILE),
                    description = config.optString("description", "Custom configuration")
                )
            }
        }

        private fun getDefaultConfigs(): List<AudioConfig> {
            Log.w(TAG, "Using hardcoded emergency configuration")
            return listOf(
                AudioConfig(
                    usage = "USAGE_MEDIA",
                    contentType = "CONTENT_TYPE_MUSIC",
                    transferMode = "MODE_STREAM",
                    performanceMode = "PERFORMANCE_MODE_POWER_SAVING",
                    bufferMultiplier = 2,
                    audioFilePath = AudioConstants.DEFAULT_AUDIO_FILE,
                    description = "Emergency Fallback - Media Playback"
                )
            )
        }
    }
}