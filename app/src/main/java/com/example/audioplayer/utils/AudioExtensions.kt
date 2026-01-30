package com.example.audioplayer.utils

import android.media.AudioAttributes
import android.media.AudioTrack

/**
 * Extension functions for audio-related enums and values
 */

fun Int.usageToString(): String = when (this) {
    AudioAttributes.USAGE_UNKNOWN -> "UNKNOWN"
    AudioAttributes.USAGE_MEDIA -> "MEDIA"
    AudioAttributes.USAGE_VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
    AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING -> "VOICE_COMMUNICATION_SIGNALLING"
    AudioAttributes.USAGE_ALARM -> "ALARM"
    AudioAttributes.USAGE_NOTIFICATION -> "NOTIFICATION"
    AudioAttributes.USAGE_NOTIFICATION_RINGTONE -> "NOTIFICATION_RINGTONE"
    AudioAttributes.USAGE_NOTIFICATION_EVENT -> "NOTIFICATION_EVENT"
    AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY -> "ASSISTANCE_ACCESSIBILITY"
    AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE -> "ASSISTANCE_NAVIGATION_GUIDANCE"
    AudioAttributes.USAGE_ASSISTANCE_SONIFICATION -> "ASSISTANCE_SONIFICATION"
    AudioAttributes.USAGE_GAME -> "GAME"
    AudioAttributes.USAGE_ASSISTANT -> "ASSISTANT"
    else -> "UNKNOWN($this)"
}

fun Int.contentTypeToString(): String = when (this) {
    AudioAttributes.CONTENT_TYPE_UNKNOWN -> "UNKNOWN"
    AudioAttributes.CONTENT_TYPE_MUSIC -> "MUSIC"
    AudioAttributes.CONTENT_TYPE_MOVIE -> "MOVIE"
    AudioAttributes.CONTENT_TYPE_SPEECH -> "SPEECH"
    AudioAttributes.CONTENT_TYPE_SONIFICATION -> "SONIFICATION"
    else -> "UNKNOWN($this)"
}

fun Int.transferModeToString(): String = when (this) {
    AudioTrack.MODE_STREAM -> "STREAM"
    AudioTrack.MODE_STATIC -> "STATIC"
    else -> "UNKNOWN($this)"
}

fun Int.performanceModeToString(): String = when (this) {
    AudioTrack.PERFORMANCE_MODE_LOW_LATENCY -> "LOW_LATENCY"
    AudioTrack.PERFORMANCE_MODE_POWER_SAVING -> "POWER_SAVING"
    AudioTrack.PERFORMANCE_MODE_NONE -> "NONE"
    else -> "UNKNOWN($this)"
}