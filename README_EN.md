# AudioPlayer

[中文文档](README.md) | English

An audio player based on Android AudioTrack API, supporting multiple audio scenario configurations
and audio focus management.

## Table of Contents

- [Introduction](#introduction)
- [Quick Start](#quick-start)
- [Installation](#installation)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Troubleshooting](#troubleshooting)
- [License](#license)

## Introduction

AudioPlayer is an Android audio player based on Android AudioTrack API, supporting audio focus
management and multiple audio scenarios.

### Key Features

- **17 Audio Scenarios**: Media playback, voice call, alarm, notification, ringtone, game,
  accessibility, navigation guidance, system alert, voice assistant, emergency, safety, vehicle
  status, announcement, speaker cleanup
- **Complete Audio Support**: 1-16 channels, 8kHz-192kHz sample rates, 8/16/24/32-bit PCM
- **WAV File Support**: Automatic WAV header parsing, multiple PCM formats supported
- **Audio Focus Management**: Complete audio focus request and handling mechanism
- **Flexible Configuration**: JSON configuration file with external hot-reload support

### Audio Scenarios

| Scenario            | Usage                          | Audio Focus    | Typical Use            |
|---------------------|--------------------------------|----------------|------------------------|
| Media Playback      | MEDIA                          | GAIN           | Music, video playback  |
| Voice Call          | VOICE_COMMUNICATION            | GAIN           | VoIP applications      |
| Call Signaling      | VOICE_COMMUNICATION_SIGNALLING | GAIN           | Call signaling audio   |
| Alarm               | ALARM                          | GAIN           | System alarm           |
| Notification        | NOTIFICATION                   | GAIN_TRANSIENT | System notification    |
| Ringtone            | RINGTONE                       | GAIN           | Incoming call          |
| Notification Event  | NOTIFICATION_EVENT             | GAIN_TRANSIENT | Notification events    |
| Accessibility       | ASSISTANCE_ACCESSIBILITY       | GAIN           | Accessibility features |
| Navigation Guidance | ASSISTANCE_NAVIGATION_GUIDANCE | GAIN           | Navigation voice       |
| System Alert        | ASSISTANCE_SONIFICATION        | GAIN_TRANSIENT | System alert sounds    |
| Game                | GAME                           | GAIN           | Game sound effects     |
| Voice Assistant     | ASSISTANT                      | GAIN           | Voice assistant        |
| Emergency           | EMERGENCY                      | GAIN           | Emergency alerts       |
| Safety              | SAFETY                         | GAIN           | Safety warnings        |
| Vehicle Status      | VEHICLE_STATUS                 | GAIN           | Vehicle status alerts  |
| Announcement        | ANNOUNCEMENT                   | GAIN           | Public announcement    |
| Speaker Cleanup     | SPEAKER_CLEANUP                | GAIN           | Speaker cleanup signal |

## Quick Start

### Basic Usage

1. **Select Config** - Choose audio scenario via dropdown menu
2. **Start Playback** - Tap green play button
3. **Stop Playback** - Tap red stop button
4. **Reload Config** - Long-press dropdown to reload external config

### Common Operations

```bash
# Push test file to device
adb push 48k_2ch_16bit.wav /data/

# View playback logs
adb logcat -s AudioPlayer MainActivity AudioConfig

# Check config file
adb shell cat /data/audio_player_configs.json
```

## Installation

### Requirements

- **Android Version**: Android 12L (API 32) or higher
- **Development Environment**: Android Studio
- **Build System**: Gradle

### Build and Install

```bash
git clone https://github.com/kainan-tek/AudioPlayer.git
cd AudioPlayer
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
adb push 48k_2ch_16bit.wav /data/
```

### Permissions

| Permission              | Purpose               | Version     |
|-------------------------|-----------------------|-------------|
| `MODIFY_AUDIO_SETTINGS` | Audio control         | All         |
| `READ_MEDIA_AUDIO`      | Read audio files      | Android 13+ |
| `READ_EXTERNAL_STORAGE` | Read external storage | Android 12- |

```bash
# Grant permission manually
adb shell pm grant com.example.audioplayer android.permission.READ_EXTERNAL_STORAGE
```

## Configuration

### Config File Location

- **External Config**: `/data/audio_player_configs.json` (loaded first)
- **Built-in Config**: `app/src/main/assets/audio_player_configs.json`

### Config File Format

```json
{
  "configs": [
    {
      "usage": "USAGE_MEDIA",
      "contentType": "CONTENT_TYPE_MUSIC",
      "transferMode": "MODE_STREAM",
      "performanceMode": "PERFORMANCE_MODE_POWER_SAVING",
      "bufferMultiplier": 2,
      "audioFilePath": "/data/48k_2ch_16bit.wav",
      "description": "Media Playback (Power Saving Mode)"
    }
  ]
}
```

### Configuration Parameters

#### Usage

| Value                                  | Description                   |
|----------------------------------------|-------------------------------|
| `USAGE_MEDIA`                          | Media playback (music, video) |
| `USAGE_VOICE_COMMUNICATION`            | Voice call (VoIP)             |
| `USAGE_VOICE_COMMUNICATION_SIGNALLING` | Call signaling audio          |
| `USAGE_ALARM`                          | Alarm                         |
| `USAGE_NOTIFICATION`                   | Notification                  |
| `USAGE_NOTIFICATION_RINGTONE`          | Ringtone                      |
| `USAGE_NOTIFICATION_EVENT`             | Notification event            |
| `USAGE_ASSISTANCE_ACCESSIBILITY`       | Accessibility features        |
| `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` | Navigation voice guidance     |
| `USAGE_ASSISTANCE_SONIFICATION`        | System alert sounds           |
| `USAGE_GAME`                           | Game                          |
| `USAGE_ASSISTANT`                      | Voice assistant               |
| `USAGE_EMERGENCY`                      | Emergency alerts              |
| `USAGE_SAFETY`                         | Safety warnings               |
| `USAGE_VEHICLE_STATUS`                 | Vehicle status alerts         |
| `USAGE_ANNOUNCEMENT`                   | Public announcement           |
| `USAGE_SPEAKER_CLEANUP`                | Speaker cleanup signal        |

#### Content Type

| Value                       | Description   |
|-----------------------------|---------------|
| `CONTENT_TYPE_MUSIC`        | Music         |
| `CONTENT_TYPE_SPEECH`       | Speech        |
| `CONTENT_TYPE_SONIFICATION` | Sound effects |
| `CONTENT_TYPE_MOVIE`        | Movie         |
| `CONTENT_TYPE_UNKNOWN`      | Unknown       |

#### Transfer Mode

| Value         | Description               |
|---------------|---------------------------|
| `MODE_STREAM` | Stream mode (recommended) |
| `MODE_STATIC` | Static mode               |

#### Performance Mode

| Value                           | Description  |
|---------------------------------|--------------|
| `PERFORMANCE_MODE_LOW_LATENCY`  | Low latency  |
| `PERFORMANCE_MODE_POWER_SAVING` | Power saving |
| `PERFORMANCE_MODE_NONE`         | Default      |

#### Buffer Multiplier

| Value | Description                                    |
|-------|------------------------------------------------|
| 1     | Minimum buffer size                            |
| 2     | Recommended (balance latency and stability)    |
| 4+    | Larger buffer (suitable for high sample rates) |

## Audio Focus Management

### Focus Change Handling

| Focus Event                          | Action         |
|--------------------------------------|----------------|
| `AUDIOFOCUS_GAIN`                    | Restore volume |
| `AUDIOFOCUS_LOSS`                    | Stop playback  |
| `AUDIOFOCUS_LOSS_TRANSIENT`          | Stop playback  |
| `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK` | Stop playback  |

> **Note**: Current UI does not support pause functionality, all focus loss scenarios result in
> playback stop.

## API Reference

### AudioPlayer Class

```kotlin
class AudioPlayer(context: Context) {
    fun setAudioConfig(config: AudioConfig)     // Set audio configuration
    fun startPlayback(): Boolean                // Start playback
    fun stopPlayback()                          // Stop playback (idempotent)
    fun isPlaying(): Boolean                   // Check if playing
    fun release()                               // Release resources
    fun setPlaybackListener(listener: PlaybackListener?)  // Set listener
}
```

### PlaybackListener Interface

```kotlin
interface PlaybackListener {
    fun onPlaybackStarted()                     // Playback started callback
    fun onPlaybackStopped()                     // Playback stopped callback
    fun onPlaybackError(error: String)          // Playback error callback
}
```

### Error Prefixes

| Prefix         | Description                |
|----------------|----------------------------|
| `[FILE]`       | File operation error       |
| `[STREAM]`     | Audio stream error         |
| `[PERMISSION]` | Permission error           |
| `[PARAM]`      | Parameter validation error |
| `[FOCUS]`      | Audio focus error          |

## Troubleshooting

### Common Issues

#### 1. Playback Failed

```bash
# Check if file exists
adb shell ls -la /data/*.wav

# View detailed logs
adb logcat -s AudioPlayer
```

#### 2. Permission Issues

```bash
adb shell pm grant com.example.audioplayer android.permission.READ_EXTERNAL_STORAGE
adb shell setenforce 0
```

#### 3. Audio Focus Issues

```bash
# View audio focus changes
adb logcat -s AudioFocus AudioPlayer

# Check other app occupation
adb shell dumpsys audio | grep "Audio Focus"
```

### Debug Commands

```bash
adb logcat -s AudioPlayer MainActivity AudioConfig
adb logcat -s AudioTrack AudioFlinger
```

## Related Projects

- [AAudioPlayer](https://github.com/kainan-tek/AAudioPlayer) - High-performance player based on
  AAudio API
- [AAudioRecorder](https://github.com/kainan-tek/AAudioRecorder) - High-performance recorder based
  on AAudio API
- [AudioRecorder](https://github.com/kainan-tek/AudioRecorder) - Audio recorder based on AudioRecord
  API
- [audio_test_client](https://github.com/kainan-tek/audio_test_client) - Android system-level audio
  testing tool

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

**Note**: This project is for learning and testing purposes only.
