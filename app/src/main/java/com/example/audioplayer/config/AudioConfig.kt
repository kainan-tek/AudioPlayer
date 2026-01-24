package com.example.audioplayer.config

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioTrack
import android.util.Log
import com.example.audioplayer.utils.AudioConstants
import org.json.JSONObject
import java.io.File

/**
 * Audio configuration data class
 * Includes configuration management functionality, supports loading configuration from external JSON files
 */
data class AudioConfig(
    val usage: Int = AudioAttributes.USAGE_MEDIA,
    val contentType: Int = AudioAttributes.CONTENT_TYPE_MUSIC,
    val transferMode: Int = AudioTrack.MODE_STREAM,
    val performanceMode: Int = AudioTrack.PERFORMANCE_MODE_POWER_SAVING,
    val bufferMultiplier: Int = 4,
    val audioFilePath: String = AudioConstants.DEFAULT_AUDIO_FILE,
    val minBufferSize: Int = 960,
    val description: String = "Default configuration (power saving mode)"
) {
    /**
     * Get detailed configuration information
     */
    fun getDetailedInfo(): String {
        return buildString {
            appendLine("Configuration: $description")
            appendLine("Usage: ${getUsageString(usage)}")
            appendLine("Content Type: ${getContentTypeString(contentType)}")
            appendLine("Transfer Mode: ${getTransferModeString(transferMode)}")
            appendLine("Performance Mode: ${getPerformanceModeString(performanceMode)}")
            appendLine("Buffer Multiplier: ${bufferMultiplier}x")
            appendLine("Audio File: $audioFilePath")
            appendLine("Min Buffer Size: $minBufferSize bytes")
        }.trim()
    }
    
    private fun getUsageString(usage: Int): String = USAGE_MAP[usage] ?: "UNKNOWN($usage)"
    private fun getContentTypeString(contentType: Int): String = CONTENT_TYPE_MAP[contentType] ?: "UNKNOWN($contentType)"
    private fun getTransferModeString(transferMode: Int): String = TRANSFER_MODE_MAP[transferMode] ?: "UNKNOWN($transferMode)"
    private fun getPerformanceModeString(performanceMode: Int): String = PERFORMANCE_MODE_MAP[performanceMode] ?: "UNKNOWN($performanceMode)"

    companion object {
        private const val TAG = "AudioConfig"
        private const val CONFIG_FILE_PATH = AudioConstants.EXTERNAL_CONFIG_PATH
        private const val ASSETS_CONFIG_FILE = AudioConstants.ASSETS_CONFIG_FILE

        // Constant mapping tables to avoid repetitive when expressions
        private val USAGE_MAP = mapOf(
            AudioAttributes.USAGE_UNKNOWN to "USAGE_UNKNOWN",
            AudioAttributes.USAGE_MEDIA to "USAGE_MEDIA",
            AudioAttributes.USAGE_VOICE_COMMUNICATION to "USAGE_VOICE_COMMUNICATION",
            AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING to "USAGE_VOICE_COMMUNICATION_SIGNALLING",
            AudioAttributes.USAGE_ALARM to "USAGE_ALARM",
            AudioAttributes.USAGE_NOTIFICATION to "USAGE_NOTIFICATION",
            AudioAttributes.USAGE_NOTIFICATION_RINGTONE to "USAGE_NOTIFICATION_RINGTONE",
            AudioAttributes.USAGE_NOTIFICATION_EVENT to "USAGE_NOTIFICATION_EVENT",
            AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY to "USAGE_ASSISTANCE_ACCESSIBILITY",
            AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE to "USAGE_ASSISTANCE_NAVIGATION_GUIDANCE",
            AudioAttributes.USAGE_ASSISTANCE_SONIFICATION to "USAGE_ASSISTANCE_SONIFICATION",
            AudioAttributes.USAGE_GAME to "USAGE_GAME",
            AudioAttributes.USAGE_ASSISTANT to "USAGE_ASSISTANT"
        )

        private val CONTENT_TYPE_MAP = mapOf(
            AudioAttributes.CONTENT_TYPE_UNKNOWN to "CONTENT_TYPE_UNKNOWN",
            AudioAttributes.CONTENT_TYPE_MUSIC to "CONTENT_TYPE_MUSIC",
            AudioAttributes.CONTENT_TYPE_MOVIE to "CONTENT_TYPE_MOVIE",
            AudioAttributes.CONTENT_TYPE_SPEECH to "CONTENT_TYPE_SPEECH",
            AudioAttributes.CONTENT_TYPE_SONIFICATION to "CONTENT_TYPE_SONIFICATION"
        )

        private val TRANSFER_MODE_MAP = mapOf(
            AudioTrack.MODE_STREAM to "MODE_STREAM",
            AudioTrack.MODE_STATIC to "MODE_STATIC"
        )

        private val PERFORMANCE_MODE_MAP = mapOf(
            AudioTrack.PERFORMANCE_MODE_LOW_LATENCY to "PERFORMANCE_MODE_LOW_LATENCY",
            AudioTrack.PERFORMANCE_MODE_POWER_SAVING to "PERFORMANCE_MODE_POWER_SAVING",
            AudioTrack.PERFORMANCE_MODE_NONE to "PERFORMANCE_MODE_NONE"
        )

        /**
         * Load configurations from external file or assets
         */
        fun loadConfigs(context: Context): List<AudioConfig> {
            return try {
                // First try to load from external file
                val externalConfigs = loadFromExternalFile()
                if (externalConfigs.isNotEmpty()) {
                    Log.i(TAG, "Loaded ${externalConfigs.size} configurations from external file")
                    externalConfigs
                } else {
                    // If external file doesn't exist or is empty, load from assets
                    Log.i(TAG, "External config file not found, loading default configuration from assets")
                    loadFromAssets(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load configurations, using empty configuration list", e)
                emptyList()
            }
        }

        /**
         * Reload configurations (used after configuration file updates)
         */
        fun reloadConfigs(context: Context): List<AudioConfig> {
            Log.i(TAG, "Reloading configuration file")
            return loadConfigs(context)
        }

        /**
         * Load configuration from external JSON file
         */
        private fun loadFromExternalFile(): List<AudioConfig> {
            val file = File(CONFIG_FILE_PATH)
            return if (file.exists()) {
                try {
                    val content = file.readText(Charsets.UTF_8)
                    parseJsonConfigs(content)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read external configuration file", e)
                    emptyList()
                }
            } else emptyList()
        }

        /**
         * Load default configuration from assets folder
         */
        private fun loadFromAssets(context: Context): List<AudioConfig> {
            return try {
                val content = context.assets.open(ASSETS_CONFIG_FILE).use {
                    it.readBytes().toString(Charsets.UTF_8)
                }
                parseJsonConfigs(content)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load configuration from assets", e)
                emptyList()
            }
        }

        /**
         * Parse JSON configuration
         */
        private fun parseJsonConfigs(jsonContent: String): List<AudioConfig> {
            val configs = mutableListOf<AudioConfig>()

            try {
                val jsonObject = JSONObject(jsonContent)
                val configsArray = jsonObject.getJSONArray("configs")

                for (i in 0 until configsArray.length()) {
                    val configJson = configsArray.getJSONObject(i)
                    val config = parseAudioConfig(configJson)
                    configs.add(config)
                }

                Log.i(TAG, "Successfully parsed ${configs.size} configurations")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse JSON configuration", e)
            }

            return configs
        }

        /**
         * Parse single audio configuration
         */
        private fun parseAudioConfig(json: JSONObject): AudioConfig {
            return AudioConfig(
                usage = parseUsage(json.optString("usage", "MEDIA")),
                contentType = parseContentType(json.optString("contentType", "MUSIC")),
                transferMode = parseTransferMode(json.optString("transferMode", "STREAM")),
                performanceMode = parsePerformanceMode(json.optString("performanceMode", "POWER_SAVING")),
                bufferMultiplier = json.optInt("bufferMultiplier", 4),
                audioFilePath = json.optString("audioFilePath", "/data/48k_2ch_16bit.wav"),
                minBufferSize = json.optInt("minBufferSize", 960),
                description = json.optString("description", "Custom configuration")
            )
        }

        // Parsing methods - direct string matching
        private fun parseUsage(usage: String): Int {
            return USAGE_MAP.entries.find { it.value == usage.uppercase() }?.key ?: AudioAttributes.USAGE_MEDIA
        }

        private fun parseContentType(contentType: String): Int {
            return CONTENT_TYPE_MAP.entries.find { it.value == contentType.uppercase() }?.key ?: AudioAttributes.CONTENT_TYPE_MUSIC
        }

        private fun parseTransferMode(transferMode: String): Int {
            return TRANSFER_MODE_MAP.entries.find { it.value == transferMode.uppercase() }?.key ?: AudioTrack.MODE_STREAM
        }

        private fun parsePerformanceMode(performanceMode: String): Int {
            return PERFORMANCE_MODE_MAP.entries.find { it.value == performanceMode.uppercase() }?.key ?: AudioTrack.PERFORMANCE_MODE_POWER_SAVING
        }
    }
}