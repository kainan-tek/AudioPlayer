package com.example.audioplayer.utils

import android.media.AudioAttributes
import android.media.AudioTrack

/**
 * Audio-related extension functions for simplifying enum to string conversion
 */

fun Int.usageToString(): String = when(this) {
    AudioAttributes.USAGE_MEDIA -> "USAGE_MEDIA"
    AudioAttributes.USAGE_VOICE_COMMUNICATION -> "USAGE_VOICE_COMMUNICATION"
    AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING -> "USAGE_VOICE_COMMUNICATION_SIGNALLING"
    AudioAttributes.USAGE_ALARM -> "USAGE_ALARM"
    AudioAttributes.USAGE_NOTIFICATION -> "USAGE_NOTIFICATION"
    AudioAttributes.USAGE_NOTIFICATION_RINGTONE -> "USAGE_NOTIFICATION_RINGTONE"
    AudioAttributes.USAGE_NOTIFICATION_EVENT -> "USAGE_NOTIFICATION_EVENT"
    AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY -> "USAGE_ASSISTANCE_ACCESSIBILITY"
    AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE -> "USAGE_ASSISTANCE_NAVIGATION_GUIDANCE"
    AudioAttributes.USAGE_ASSISTANCE_SONIFICATION -> "USAGE_ASSISTANCE_SONIFICATION"
    AudioAttributes.USAGE_GAME -> "USAGE_GAME"
    AudioAttributes.USAGE_ASSISTANT -> "USAGE_ASSISTANT"
    else -> "USAGE_UNKNOWN"
}

fun Int.contentTypeToString(): String = when(this) {
    AudioAttributes.CONTENT_TYPE_MUSIC -> "CONTENT_TYPE_MUSIC"
    AudioAttributes.CONTENT_TYPE_SPEECH -> "CONTENT_TYPE_SPEECH"
    AudioAttributes.CONTENT_TYPE_SONIFICATION -> "CONTENT_TYPE_SONIFICATION"
    AudioAttributes.CONTENT_TYPE_MOVIE -> "CONTENT_TYPE_MOVIE"
    else -> "CONTENT_TYPE_UNKNOWN"
}

fun Int.performanceModeToString(): String = when(this) {
    AudioTrack.PERFORMANCE_MODE_LOW_LATENCY -> "PERFORMANCE_MODE_LOW_LATENCY"
    AudioTrack.PERFORMANCE_MODE_POWER_SAVING -> "PERFORMANCE_MODE_POWER_SAVING"
    else -> "PERFORMANCE_MODE_NONE"
}

fun Int.transferModeToString(): String = when(this) {
    AudioTrack.MODE_STREAM -> "MODE_STREAM"
    AudioTrack.MODE_STATIC -> "MODE_STATIC"
    else -> "MODE_UNKNOWN"
}