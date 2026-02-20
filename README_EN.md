# AudioPlayer

[中文文档](README.md) | English

A professional audio player test application based on Android AudioTrack API, supporting 17 audio usage scenario configurations and multi-format WAV file playback.

## 📋 Overview

AudioPlayer is an audio playback test tool designed for the Android platform, using the Android AudioTrack API. This project demonstrates how to implement high-quality audio playback in Android applications, supporting various audio usage scenarios and performance modes. It's an ideal tool for audio developers and test engineers.

## ✨ Key Features

- **🎵 Multi-Format WAV Playback**: Supports 8/16/24/32-bit PCM format WAV file playback
- **🔊 Complete Multi-Channel Support**: Supports 1-16 channel audio playback, including 7.1.4 Dolby Atmos
- **🔧 17 Usage Scenarios**: Covers complete audio scenarios including media, calls, games, navigation, emergency alerts
- **📱 Modern UI**: Intuitive control interface with Material Design 3 style
- **🛠️ Dynamic Configuration System**: Runtime switching of audio configurations, external JSON config file support
- **🎯 Smart Audio Focus Management**: Automatic audio focus request and release handling
- **🏗️ MVVM Architecture**: Clear code structure and modular design

## 🚀 Quick Start

### System Requirements

- Android 12 (API 32) or higher
- Device with multi-channel audio output support
- Development Environment: Android Studio

### Permission Requirements

- `READ_MEDIA_AUDIO`: Read audio files permission (Android 13+)
- `READ_EXTERNAL_STORAGE`: Read external storage permission (Android 12 and below)

### Installation Steps

1. **Clone Project**
   ```bash
   git clone https://github.com/kainan-tek/AudioPlayer.git
   cd AudioPlayer
   ```

2. **Prepare Test Files**
   ```bash
   adb root && adb remount && adb shell setenforce 0
   adb push 48k_2ch_16bit.wav /data/
   adb push 96k_8ch_24bit.wav /data/  # Optional, for high-quality audio testing
   adb push 48k_12ch_16bit.wav /data/ # Optional, for multi-channel testing
   ```

3. **Build and Install**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Optional Config File**
   ```bash
   # Push custom config file (optional)
   adb push audio_player_configs.json /data/
   ```

## 📖 Usage Guide

### Basic Operations

1. **Playback Control**
   - 🎵 **Start Playback**: Tap the green "Start Playback" button
   - ⏹️ **Stop Playback**: Tap the red "Stop Playback" button
   - ⚙️ **Playback Config**: Switch audio settings via config dropdown menu

2. **Configuration Management**
   - Auto-load configurations on app startup
   - Support dynamic loading from external files
   - Switch between different audio scenarios via dropdown menu at runtime
   - Long-press config dropdown to reload external config file

### Configuration Switching Flow

1. Tap config dropdown menu to view all available configurations
2. Select desired audio scenario configuration
3. Configuration takes effect immediately and displays on interface
4. Start playback to test audio effect
5. To reload external config file, long-press config dropdown

## 🎵 Audio Format Support

### Channel Configurations
| Channels | Config Name    | Description   | Channel Layout                                  |
|----------|----------------|---------------|-------------------------------------------------|
| 1        | Mono           | Mono          | M                                               |
| 2        | Stereo         | Stereo        | L R                                             |
| 4        | Quad           | Quad          | L R Ls Rs                                       |
| 6        | 5.1 Surround   | 5.1 Surround  | L R C LFE Ls Rs                                 |
| 8        | 7.1 Surround   | 7.1 Surround  | L R C LFE Ls Rs Lrs Rrs                         |
| 10       | 5.1.4          | 5.1 + 4 Sky   | L R C LFE Ls Rs Ltf Rtf Ltb Rtb                 |
| 12       | **7.1.4**      | **7.1 + 4 Sky** | **L R C LFE Ls Rs Lrs Rrs Ltf Rtf Ltb Rtb**   |
| 1-16     | Other Configs  | Auto-mapping  | Automatically selects best config based on channel count |

### 7.1.4 Channel Details
7.1.4 is the latest 3D audio standard, containing 12 channels: Front layer (L R C LFE), Surround layer (Ls Rs Lrs Rrs), Sky layer (Ltf Rtf Ltb Rtb).

### Audio Parameters
- **Sample Rate**: 8kHz - 192kHz
- **Bit Depth**: 8/16/24/32 bit
- **Format**: WAV (PCM)
- **Maximum Channels**: 16 channels
- **Configuration System**: Supports various audio usages and performance modes

## 🔧 17 Preset Configuration Scenarios

The app includes 17 preset configurations covering the following usage scenarios:

### Media Playback
- **Media Playback** (Power Saving) - Standard music playback
- **Game Audio** (Low Latency) - Game sound effects playback

### Communication Audio
- **Voice Call** (Low Latency) - VoIP call audio
- **Call Signaling** (Low Latency) - Call prompt tones

### System Sound Effects
- **Alarm Sound** (Power Saving) - Alarm ringtone
- **Notification Sound** (Power Saving) - System notification
- **Ringtone Playback** (Power Saving) - Incoming call ringtone
- **Notification Event** (Power Saving) - Event reminder

### Assistive Features
- **Accessibility** (Power Saving) - Accessibility voice
- **Navigation Voice** (Power Saving) - GPS navigation
- **System Alert** (Low Latency) - System warning

### Special Purposes
- **Voice Assistant** (Power Saving) - AI assistant voice
- **Emergency Alert** (Low Latency) - Emergency situations
- **Safety Warning** (Low Latency) - Safety alert
- **Vehicle Status** (Power Saving) - Car audio
- **Public Announcement** (Power Saving) - Broadcast system
- **Speaker Cleaning** (Low Latency) - Hardware maintenance

## 🔧 Configuration File

### Configuration Location

- **External Config**: `/data/audio_player_configs.json` (priority)
- **Built-in Config**: `app/src/main/assets/audio_player_configs.json`

### Configuration Format

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
      "description": "Media playback configuration"
    }
  ]
}
```

### Audio File Location

- **Recommended Location**: `/data/` directory (requires root permission)
- **Test Files**: Push to device via adb push
- **Supported Format**: Standard WAV files (RIFF/WAVE format)
- **File Requirements**: 8/16/24/32-bit PCM format, 1-16 channels

### Supported Constant Values

**Usage (Usage Scenarios):**
- `USAGE_MEDIA` - Media playback
- `USAGE_VOICE_COMMUNICATION` - Voice call
- `USAGE_ALARM` - Alarm
- `USAGE_NOTIFICATION` - Notification
- `USAGE_GAME` - Game audio
- `USAGE_ASSISTANT` - Voice assistant
- `USAGE_EMERGENCY` - Emergency alert
- `USAGE_SAFETY` - Safety warning
- And 17 usage scenarios in total

**Content Type:**
- `CONTENT_TYPE_MUSIC` - Music
- `CONTENT_TYPE_SPEECH` - Speech
- `CONTENT_TYPE_SONIFICATION` - Sound effects
- `CONTENT_TYPE_MOVIE` - Movie

**Performance Mode:**
- `PERFORMANCE_MODE_LOW_LATENCY` - Low latency mode
- `PERFORMANCE_MODE_POWER_SAVING` - Power saving mode
- `PERFORMANCE_MODE_NONE` - No special optimization

**Transfer Mode:**
- `MODE_STREAM` - Stream mode
- `MODE_STATIC` - Static mode

## 🏗️ Technical Architecture

### Core Components

- **AudioPlayer**: Kotlin-written audio player wrapper class with audio focus management
- **AudioConfig**: Audio configuration management class with dynamic config loading
- **PlayerViewModel**: MVVM architecture view model managing playback state
- **MainActivity**: Modern main interface controller with permission management and user interaction
- **WaveFile**: WAV file parser supporting various formats and multi-channel

### Technology Stack

- **Language**: Kotlin
- **Audio API**: Android AudioTrack
- **Architecture**: MVVM + LiveData
- **UI**: Material Design 3
- **Concurrency**: Kotlin Coroutines
- **Minimum Version**: Android 12 (API 32)
- **Target Version**: Not specified (uses compileSdk)
- **Compile Version**: Android 15 (API 36)

### Dependencies

```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
```

## 🔍 Technical Details

### AudioTrack Integration

- Stream mode for audio playback
- Multiple audio format support (8/16/24/32-bit PCM)
- Complete error handling mechanism
- Automatic audio focus management
- Smart buffer management

### Data Flow Architecture

```
WAV File → WaveFile Parser → AudioTrack → Audio Output Device
                                ↓
                         Kotlin Coroutine → UI State Update
```

### WAV File Support

- Standard RIFF/WAVE format parsing
- Multi-channel audio support (1-16 channels)
- Sample rate range: 8kHz - 192kHz
- Bit depth support: 8/16/24/32-bit
- Automatic channel mapping and validation

### Audio Focus Management

- Automatic audio focus request and release
- Audio focus change handling support
- Select appropriate focus type based on configuration
- Coordinate playback with other audio apps

## 📚 API Reference

### AudioPlayer Class
```kotlin
class AudioPlayer(context: Context) {
    fun setAudioConfig(config: AudioConfig)              // Set configuration
    fun startPlayback(): Boolean                         // Start playback
    fun stopPlayback()                                   // Stop playback
    fun release()                                        // Release resources
    fun setPlaybackListener(listener: PlaybackListener)  // Set listener
}
```

### PlayerViewModel Class
```kotlin
class PlayerViewModel : ViewModel() {
    val playerState: LiveData<PlayerState>               // Playback state
    val statusMessage: LiveData<String>                  // Status message
    val errorMessage: LiveData<String?>                  // Error message
    val currentConfig: LiveData<AudioConfig?>            // Current config
    
    fun startPlayback()                                  // Start playback
    fun stopPlayback()                                   // Stop playback
    fun setAudioConfig(config: AudioConfig)              // Set configuration
    fun getAllAudioConfigs(): List<AudioConfig>          // Get all configs
    fun reloadConfigurations()                           // Reload configurations
}
```

### AudioConfig Class
```kotlin
data class AudioConfig(
    val usage: Int,                                      // Audio usage
    val contentType: Int,                                // Content type
    val transferMode: Int,                               // Transfer mode
    val performanceMode: Int,                            // Performance mode
    val bufferMultiplier: Int,                           // Buffer multiplier
    val audioFilePath: String,                           // Audio file path
    val description: String                              // Config description
) {
    companion object {
        fun loadConfigs(context: Context): List<AudioConfig>     // Load configs
        fun reloadConfigs(context: Context): List<AudioConfig>   // Reload configs
    }
}
```

## 🐛 Troubleshooting

### Common Issues

1. **Playback Failure**
   - Confirm WAV file format support (8/16/24/32-bit PCM)
   - Verify device permission settings
   - Check file path correctness

2. **Permission Issues**
   ```bash
   adb root && adb remount && adb shell setenforce 0
   ```

3. **Config Loading Failure**
   - Check JSON format correctness
   - Verify config file path
   - View app logs for detailed error information

4. **Multi-Channel Playback Issues**
   - Confirm device supports specified channel configuration
   - Check audio file channel count correctness
   - Try testing with stereo configuration

### Debug Information
```bash
# View app logs
adb logcat -s AudioPlayer MainActivity PlayerViewModel

# Check config file
adb shell cat /data/audio_player_configs.json

# Verify audio files
adb shell ls -la /data/*.wav
```

### Performance Optimization Tips

1. **Low Latency Scenarios**
   - Use `PERFORMANCE_MODE_LOW_LATENCY`
   - Set smaller `bufferMultiplier` (1-2)
   - Choose appropriate audio usage (`GAME`, `EMERGENCY`)

2. **High-Quality Music**
   - Use `PERFORMANCE_MODE_POWER_SAVING`
   - Set larger `bufferMultiplier` (4-6)
   - Choose `USAGE_MEDIA` and `CONTENT_TYPE_MUSIC`

3. **Multi-Channel Audio**
   - Ensure audio file path is correct
   - Use appropriate buffer size
   - 12-channel audio recommended to use larger buffer

## 📊 Performance Metrics

- **Latency Performance**: Low latency mode ~40-80ms, power saving mode ~80-120ms
- **Sample Rate**: 8kHz - 192kHz
- **Channel Count**: 1-16 channels
- **Bit Depth**: 8/16/24/32-bit
- **Buffer**: Configurable buffer multiplier (1-8x)
- **Supported Format**: WAV (PCM)
- **Maximum File Size**: Limited by device storage

## 🔗 Related Projects

- [**AudioRecorder**](https://github.com/kainan-tek/AudioRecorder) - Companion audio recording app based on AudioRecord API
- [**AAudioPlayer**](https://github.com/kainan-tek/AAudioPlayer) - High-performance player based on AAudio API (standalone project)
- [**AAudioRecorder**](https://github.com/kainan-tek/AAudioRecorder) - Recorder based on AAudio API (standalone project)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Note**: This project is for learning and testing purposes only. Please ensure use in appropriate devices and environments.
