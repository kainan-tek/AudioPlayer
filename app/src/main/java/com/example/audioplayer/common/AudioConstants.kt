package com.example.audioplayer.common

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/**
 * Unified audio constants definition
 * For constants and utility class management in AudioPlayer project
 */
object AudioConstants {

    // ============ File Paths ============
    const val CONFIG_FILE_PATH = "/data/audio_player_configs.json"
    const val ASSETS_CONFIG_FILE = "audio_player_configs.json"

    const val DEFAULT_AUDIO_FILE = "/data/48k_2ch_16bit.wav"

    // ============ AudioTrack Usage Constants ============
    object Usage {
        const val UNKNOWN = AudioAttributes.USAGE_UNKNOWN
        const val MEDIA = AudioAttributes.USAGE_MEDIA
        const val VOICE_COMMUNICATION = AudioAttributes.USAGE_VOICE_COMMUNICATION
        const val VOICE_COMMUNICATION_SIGNALLING =
            AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING
        const val ALARM = AudioAttributes.USAGE_ALARM
        const val NOTIFICATION = AudioAttributes.USAGE_NOTIFICATION
        const val NOTIFICATION_RINGTONE = AudioAttributes.USAGE_NOTIFICATION_RINGTONE
        const val NOTIFICATION_EVENT = AudioAttributes.USAGE_NOTIFICATION_EVENT
        const val ASSISTANCE_ACCESSIBILITY = AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
        const val ASSISTANCE_NAVIGATION_GUIDANCE =
            AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
        const val ASSISTANCE_SONIFICATION = AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
        const val GAME = AudioAttributes.USAGE_GAME
        const val ASSISTANT = AudioAttributes.USAGE_ASSISTANT

        // Android Automotive OS (AAOS) special usage scenarios
        const val EMERGENCY = 1000
        const val SAFETY = 1001
        const val VEHICLE_STATUS = 1002
        const val ANNOUNCEMENT = 1003
        const val SPEAKER_CLEANUP = 1004

        // Usage scenario mapping table
        val MAP = mapOf(
            UNKNOWN to "USAGE_UNKNOWN",
            MEDIA to "USAGE_MEDIA",
            VOICE_COMMUNICATION to "USAGE_VOICE_COMMUNICATION",
            VOICE_COMMUNICATION_SIGNALLING to "USAGE_VOICE_COMMUNICATION_SIGNALLING",
            ALARM to "USAGE_ALARM",
            NOTIFICATION to "USAGE_NOTIFICATION",
            NOTIFICATION_RINGTONE to "USAGE_NOTIFICATION_RINGTONE",
            NOTIFICATION_EVENT to "USAGE_NOTIFICATION_EVENT",
            ASSISTANCE_ACCESSIBILITY to "USAGE_ASSISTANCE_ACCESSIBILITY",
            ASSISTANCE_NAVIGATION_GUIDANCE to "USAGE_ASSISTANCE_NAVIGATION_GUIDANCE",
            ASSISTANCE_SONIFICATION to "USAGE_ASSISTANCE_SONIFICATION",
            GAME to "USAGE_GAME",
            ASSISTANT to "USAGE_ASSISTANT",
            EMERGENCY to "USAGE_EMERGENCY",
            SAFETY to "USAGE_SAFETY",
            VEHICLE_STATUS to "USAGE_VEHICLE_STATUS",
            ANNOUNCEMENT to "USAGE_ANNOUNCEMENT",
            SPEAKER_CLEANUP to "USAGE_SPEAKER_CLEANUP"
        )
    }

    // ============ AudioTrack Content Type Constants ============
    object ContentType {
        const val UNKNOWN = AudioAttributes.CONTENT_TYPE_UNKNOWN
        const val MUSIC = AudioAttributes.CONTENT_TYPE_MUSIC
        const val MOVIE = AudioAttributes.CONTENT_TYPE_MOVIE
        const val SPEECH = AudioAttributes.CONTENT_TYPE_SPEECH
        const val SONIFICATION = AudioAttributes.CONTENT_TYPE_SONIFICATION

        // Content type mapping table
        val MAP = mapOf(
            UNKNOWN to "CONTENT_TYPE_UNKNOWN",
            MUSIC to "CONTENT_TYPE_MUSIC",
            MOVIE to "CONTENT_TYPE_MOVIE",
            SPEECH to "CONTENT_TYPE_SPEECH",
            SONIFICATION to "CONTENT_TYPE_SONIFICATION"
        )
    }

    // ============ AudioTrack Transfer Mode Constants ============
    object TransferMode {
        const val STREAM = AudioTrack.MODE_STREAM
        const val STATIC = AudioTrack.MODE_STATIC

        // Transfer mode mapping table
        val MAP = mapOf(
            STREAM to "MODE_STREAM", STATIC to "MODE_STATIC"
        )
    }

    // ============ AudioTrack Performance Mode Constants ============
    object PerformanceMode {
        const val LOW_LATENCY = AudioTrack.PERFORMANCE_MODE_LOW_LATENCY
        const val POWER_SAVING = AudioTrack.PERFORMANCE_MODE_POWER_SAVING
        const val NONE = AudioTrack.PERFORMANCE_MODE_NONE

        // Performance mode mapping table
        val MAP = mapOf(
            LOW_LATENCY to "PERFORMANCE_MODE_LOW_LATENCY",
            POWER_SAVING to "PERFORMANCE_MODE_POWER_SAVING",
            NONE to "PERFORMANCE_MODE_NONE"
        )
    }

    // ============ Utility Functions ============

    /**
     * Get usage integer value from string
     */
    fun getUsage(usage: String): Int =
        parseEnumValue(Usage.MAP, usage, AudioAttributes.USAGE_MEDIA, "Usage")

    /**
     * Get content type integer value from string
     */
    fun getContentType(contentType: String): Int = parseEnumValue(
        ContentType.MAP, contentType, AudioAttributes.CONTENT_TYPE_MUSIC, "ContentType"
    )

    /**
     * Get transfer mode integer value from string
     */
    fun getTransferMode(transferMode: String): Int =
        parseEnumValue(TransferMode.MAP, transferMode, AudioTrack.MODE_STREAM, "TransferMode")

    /**
     * Get performance mode integer value from string
     */
    fun getPerformanceMode(performanceMode: String): Int = parseEnumValue(
        PerformanceMode.MAP,
        performanceMode,
        AudioTrack.PERFORMANCE_MODE_POWER_SAVING,
        "PerformanceMode"
    )

    /**
     * Generic enum value parser with error handling
     */
    private fun parseEnumValue(
        map: Map<Int, String>,
        value: String,
        default: Int,
        typeName: String = "",
    ): Int {
        val result = map.entries.find { it.value == value.uppercase() }?.key ?: default
        if (result == default && value.isNotEmpty()) {
            android.util.Log.w("AudioConstants", "Unknown $typeName value: $value, using default")
        }
        return result
    }

    /**
     * Get channel mask for playback, supporting multi-channel audio including 7.1.4 (12 channels)
     */
    fun getChannelMask(channelCount: Int): Int {
        val channelMasks = mapOf(
            1 to AudioFormat.CHANNEL_OUT_MONO,
            2 to AudioFormat.CHANNEL_OUT_STEREO,
            4 to AudioFormat.CHANNEL_OUT_QUAD,
            6 to AudioFormat.CHANNEL_OUT_5POINT1,
            8 to AudioFormat.CHANNEL_OUT_7POINT1_SURROUND,
            10 to AudioFormat.CHANNEL_OUT_5POINT1POINT4,
            12 to AudioFormat.CHANNEL_OUT_7POINT1POINT4,
            16 to AudioFormat.CHANNEL_OUT_9POINT1POINT6
        )

        return channelMasks[channelCount] ?: run {
            android.util.Log.w(
                "AudioConstants", "Unsupported channel count: $channelCount, using stereo playback"
            )
            AudioFormat.CHANNEL_OUT_STEREO
        }
    }

    /**
     * Get audio format from bit depth, supporting multiple bit depths
     */
    fun getFormatFromBitDepth(bitsPerSample: Int): Int {
        val audioFormats = mapOf(
            8 to AudioFormat.ENCODING_PCM_8BIT,
            16 to AudioFormat.ENCODING_PCM_16BIT,
            24 to AudioFormat.ENCODING_PCM_24BIT_PACKED,
            32 to AudioFormat.ENCODING_PCM_32BIT
        )

        return audioFormats[bitsPerSample] ?: run {
            android.util.Log.w(
                "AudioConstants", "Unsupported bit depth: $bitsPerSample, using 16-bit"
            )
            AudioFormat.ENCODING_PCM_16BIT
        }
    }
}