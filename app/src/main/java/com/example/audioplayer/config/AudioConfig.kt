package com.example.audioplayer.config

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioTrack
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 音频配置数据类
 * 包含配置管理功能，支持从外部JSON文件加载配置
 */
data class AudioConfig(
    val usage: Int = AudioAttributes.USAGE_MEDIA,
    val contentType: Int = AudioAttributes.CONTENT_TYPE_MUSIC,
    val transferMode: Int = AudioTrack.MODE_STREAM,
    val performanceMode: Int = AudioTrack.PERFORMANCE_MODE_POWER_SAVING,
    val bufferMultiplier: Int = 4,
    val audioFilePath: String = "/data/48k_2ch_16bit.wav",
    val minBufferSize: Int = 960,
    val description: String = "默认配置 (省电模式)"
) {
    /**
     * 获取配置的详细信息
     */
    fun getDetailedInfo(): String {
        return buildString {
            appendLine("配置: $description")
            appendLine("Usage: ${getUsageString(usage)}")
            appendLine("Content Type: ${getContentTypeString(contentType)}")
            appendLine("Transfer Mode: ${getTransferModeString(transferMode)}")
            appendLine("Performance Mode: ${getPerformanceModeString(performanceMode)}")
            appendLine("Buffer Multiplier: ${bufferMultiplier}x")
            appendLine("Audio File: $audioFilePath")
            appendLine("Min Buffer Size: $minBufferSize bytes")
        }.trim()
    }
    
    private fun getUsageString(usage: Int): String {
        return when (usage) {
            AudioAttributes.USAGE_UNKNOWN -> "UNKNOWN"
            AudioAttributes.USAGE_MEDIA -> "MEDIA"
            AudioAttributes.USAGE_VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
            AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING -> "VOICE_COMMUNICATION_SIGNALLING"
            AudioAttributes.USAGE_ALARM -> "ALARM"
            AudioAttributes.USAGE_NOTIFICATION -> "NOTIFICATION"
            AudioAttributes.USAGE_NOTIFICATION_RINGTONE -> "RINGTONE"
            AudioAttributes.USAGE_NOTIFICATION_EVENT -> "NOTIFICATION_EVENT"
            AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY -> "ACCESSIBILITY"
            AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE -> "NAVIGATION_GUIDANCE"
            AudioAttributes.USAGE_ASSISTANCE_SONIFICATION -> "SYSTEM_SONIFICATION"
            AudioAttributes.USAGE_GAME -> "GAME"
            AudioAttributes.USAGE_ASSISTANT -> "ASSISTANT"
            else -> "UNKNOWN($usage)"
        }
    }
    
    private fun getContentTypeString(contentType: Int): String {
        return when (contentType) {
            AudioAttributes.CONTENT_TYPE_UNKNOWN -> "UNKNOWN"
            AudioAttributes.CONTENT_TYPE_MUSIC -> "MUSIC"
            AudioAttributes.CONTENT_TYPE_MOVIE -> "MOVIE"
            AudioAttributes.CONTENT_TYPE_SPEECH -> "SPEECH"
            AudioAttributes.CONTENT_TYPE_SONIFICATION -> "SONIFICATION"
            else -> "UNKNOWN($contentType)"
        }
    }
    
    private fun getTransferModeString(transferMode: Int): String {
        return when (transferMode) {
            AudioTrack.MODE_STREAM -> "STREAM"
            AudioTrack.MODE_STATIC -> "STATIC"
            else -> "UNKNOWN($transferMode)"
        }
    }
    
    private fun getPerformanceModeString(performanceMode: Int): String {
        return when (performanceMode) {
            AudioTrack.PERFORMANCE_MODE_LOW_LATENCY -> "LOW_LATENCY"
            AudioTrack.PERFORMANCE_MODE_POWER_SAVING -> "POWER_SAVING"
            AudioTrack.PERFORMANCE_MODE_NONE -> "NONE"
            else -> "UNKNOWN($performanceMode)"
        }
    }

    companion object {
        private const val TAG = "AudioConfig"
        private const val CONFIG_FILE_PATH = "/data/audio_configs.json"
        private const val ASSETS_CONFIG_FILE = "audio_configs.json"

        /**
         * 从外部文件或assets加载配置
         */
        fun loadConfigs(context: Context): List<AudioConfig> {
            return try {
                // 首先尝试从外部文件加载
                val externalConfigs = loadFromExternalFile()
                if (externalConfigs.isNotEmpty()) {
                    Log.i(TAG, "从外部文件加载了 ${externalConfigs.size} 个配置")
                    externalConfigs
                } else {
                    // 如果外部文件不存在或为空，从assets加载并创建外部文件
                    Log.i(TAG, "外部配置文件不存在，从assets加载默认配置")
                    val defaultConfigs = loadFromAssets(context)
                    if (defaultConfigs.isNotEmpty()) {
                        createExternalConfigFile(defaultConfigs)
                    }
                    defaultConfigs
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载配置失败，使用空配置列表", e)
                emptyList()
            }
        }

        /**
         * 重新加载配置（用于配置文件更新后）
         */
        fun reloadConfigs(context: Context): List<AudioConfig> {
            Log.i(TAG, "重新加载配置文件")
            return loadConfigs(context)
        }

        /**
         * 从外部JSON文件加载配置
         */
        private fun loadFromExternalFile(): List<AudioConfig> {
            val file = File(CONFIG_FILE_PATH)
            if (!file.exists()) {
                return emptyList()
            }

            return try {
                val jsonContent = FileInputStream(file).use { it.readBytes().toString(Charsets.UTF_8) }
                parseJsonConfigs(jsonContent)
            } catch (e: Exception) {
                Log.e(TAG, "读取外部配置文件失败", e)
                emptyList()
            }
        }

        /**
         * 从assets文件夹加载默认配置
         */
        private fun loadFromAssets(context: Context): List<AudioConfig> {
            return try {
                val jsonContent = context.assets.open(ASSETS_CONFIG_FILE).use {
                    it.readBytes().toString(Charsets.UTF_8)
                }
                parseJsonConfigs(jsonContent)
            } catch (e: Exception) {
                Log.e(TAG, "从assets加载配置失败，使用空配置列表", e)
                emptyList()
            }
        }

        /**
         * 解析JSON配置
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

                Log.i(TAG, "成功解析 ${configs.size} 个配置")
            } catch (e: Exception) {
                Log.e(TAG, "解析JSON配置失败", e)
            }

            return configs
        }

        /**
         * 解析单个音频配置
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
                description = json.optString("description", "自定义配置")
            )
        }

        /**
         * 创建外部配置文件
         */
        private fun createExternalConfigFile(configs: List<AudioConfig>) {
            try {
                val jsonObject = JSONObject()
                val configsArray = JSONArray()

                configs.forEach { config ->
                    val configJson = JSONObject().apply {
                        put("usage", getUsageString(config.usage))
                        put("contentType", getContentTypeString(config.contentType))
                        put("transferMode", getTransferModeString(config.transferMode))
                        put("performanceMode", getPerformanceModeString(config.performanceMode))
                        put("bufferMultiplier", config.bufferMultiplier)
                        put("audioFilePath", config.audioFilePath)
                        put("minBufferSize", config.minBufferSize)
                        put("description", config.description)
                    }
                    configsArray.put(configJson)
                }

                jsonObject.put("configs", configsArray)

                val file = File(CONFIG_FILE_PATH)
                FileOutputStream(file).use {
                    it.write(jsonObject.toString(2).toByteArray(Charsets.UTF_8))
                }

                Log.i(TAG, "外部配置文件已创建: $CONFIG_FILE_PATH")
            } catch (e: Exception) {
                Log.e(TAG, "创建外部配置文件失败", e)
            }
        }

        // 解析方法
        private fun parseUsage(usage: String): Int {
            return when (usage.uppercase()) {
                "UNKNOWN" -> AudioAttributes.USAGE_UNKNOWN
                "MEDIA" -> AudioAttributes.USAGE_MEDIA
                "VOICE_COMMUNICATION" -> AudioAttributes.USAGE_VOICE_COMMUNICATION
                "VOICE_COMMUNICATION_SIGNALLING" -> AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING
                "ALARM" -> AudioAttributes.USAGE_ALARM
                "NOTIFICATION" -> AudioAttributes.USAGE_NOTIFICATION
                "RINGTONE" -> AudioAttributes.USAGE_NOTIFICATION_RINGTONE
                "NOTIFICATION_EVENT" -> AudioAttributes.USAGE_NOTIFICATION_EVENT
                "ACCESSIBILITY" -> AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
                "NAVIGATION_GUIDANCE" -> AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
                "SYSTEM_SONIFICATION" -> AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
                "GAME" -> AudioAttributes.USAGE_GAME
                "ASSISTANT" -> AudioAttributes.USAGE_ASSISTANT
                else -> AudioAttributes.USAGE_MEDIA
            }
        }

        private fun parseContentType(contentType: String): Int {
            return when (contentType.uppercase()) {
                "UNKNOWN" -> AudioAttributes.CONTENT_TYPE_UNKNOWN
                "MUSIC" -> AudioAttributes.CONTENT_TYPE_MUSIC
                "MOVIE" -> AudioAttributes.CONTENT_TYPE_MOVIE
                "SPEECH" -> AudioAttributes.CONTENT_TYPE_SPEECH
                "SONIFICATION" -> AudioAttributes.CONTENT_TYPE_SONIFICATION
                else -> AudioAttributes.CONTENT_TYPE_MUSIC
            }
        }

        private fun parseTransferMode(transferMode: String): Int {
            return when (transferMode.uppercase()) {
                "STREAM" -> AudioTrack.MODE_STREAM
                "STATIC" -> AudioTrack.MODE_STATIC
                else -> AudioTrack.MODE_STREAM
            }
        }

        private fun parsePerformanceMode(performanceMode: String): Int {
            return when (performanceMode.uppercase()) {
                "LOW_LATENCY" -> AudioTrack.PERFORMANCE_MODE_LOW_LATENCY
                "POWER_SAVING" -> AudioTrack.PERFORMANCE_MODE_POWER_SAVING
                "NONE" -> AudioTrack.PERFORMANCE_MODE_NONE
                else -> AudioTrack.PERFORMANCE_MODE_POWER_SAVING
            }
        }

        // 转换为字符串的方法
        private fun getUsageString(usage: Int): String {
            return when (usage) {
                AudioAttributes.USAGE_UNKNOWN -> "UNKNOWN"
                AudioAttributes.USAGE_MEDIA -> "MEDIA"
                AudioAttributes.USAGE_VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
                AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING -> "VOICE_COMMUNICATION_SIGNALLING"
                AudioAttributes.USAGE_ALARM -> "ALARM"
                AudioAttributes.USAGE_NOTIFICATION -> "NOTIFICATION"
                AudioAttributes.USAGE_NOTIFICATION_RINGTONE -> "RINGTONE"
                AudioAttributes.USAGE_NOTIFICATION_EVENT -> "NOTIFICATION_EVENT"
                AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY -> "ACCESSIBILITY"
                AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE -> "NAVIGATION_GUIDANCE"
                AudioAttributes.USAGE_ASSISTANCE_SONIFICATION -> "SYSTEM_SONIFICATION"
                AudioAttributes.USAGE_GAME -> "GAME"
                AudioAttributes.USAGE_ASSISTANT -> "ASSISTANT"
                else -> "MEDIA"
            }
        }

        private fun getContentTypeString(contentType: Int): String {
            return when (contentType) {
                AudioAttributes.CONTENT_TYPE_UNKNOWN -> "UNKNOWN"
                AudioAttributes.CONTENT_TYPE_MUSIC -> "MUSIC"
                AudioAttributes.CONTENT_TYPE_MOVIE -> "MOVIE"
                AudioAttributes.CONTENT_TYPE_SPEECH -> "SPEECH"
                AudioAttributes.CONTENT_TYPE_SONIFICATION -> "SONIFICATION"
                else -> "MUSIC"
            }
        }

        private fun getTransferModeString(transferMode: Int): String {
            return when (transferMode) {
                AudioTrack.MODE_STREAM -> "STREAM"
                AudioTrack.MODE_STATIC -> "STATIC"
                else -> "STREAM"
            }
        }

        private fun getPerformanceModeString(performanceMode: Int): String {
            return when (performanceMode) {
                AudioTrack.PERFORMANCE_MODE_LOW_LATENCY -> "LOW_LATENCY"
                AudioTrack.PERFORMANCE_MODE_POWER_SAVING -> "POWER_SAVING"
                AudioTrack.PERFORMANCE_MODE_NONE -> "NONE"
                else -> "POWER_SAVING"
            }
        }
    }
}