# AudioPlayer

中文 | [English](README_EN.md)

基于 Android AudioTrack API 的音频播放器，支持多种音频场景配置和音频焦点管理。

## 目录

- [项目简介](#项目简介)
- [快速开始](#快速开始)
- [安装部署](#安装部署)
- [配置说明](#配置说明)
- [API 参考](#api-参考)
- [故障排除](#故障排除)
- [许可证](#许可证)

## 项目简介

AudioPlayer 是一个 Android 音频播放器，基于 Android AudioTrack API 开发，支持音频焦点管理和多种音频场景。

### 核心特性

- **17 种音频场景**: 媒体播放、语音通话、闹钟、通知、铃声、游戏、辅助功能、导航语音、系统提示音、语音助手、紧急警报、安全警告、车辆状态、广播、扬声器清理
- **完整音频支持**: 1-16 声道，8kHz-192kHz 采样率，8/16/24/32 位 PCM
- **WAV 文件支持**: 自动解析 WAV 文件头，支持多种 PCM 格式
- **音频焦点管理**: 完整的音频焦点请求和处理机制
- **灵活配置**: JSON 配置文件，支持外部热更新

### 音频场景

| 场景    | Usage                          | 音频焦点           | 典型用途    |
|-------|--------------------------------|----------------|---------|
| 音乐播放  | MEDIA                          | GAIN           | 音乐、视频播放 |
| 语音通话  | VOICE_COMMUNICATION            | GAIN           | VoIP 应用 |
| 通话信令  | VOICE_COMMUNICATION_SIGNALLING | GAIN           | 通话信令音频  |
| 闹钟    | ALARM                          | GAIN           | 系统闹钟    |
| 通知    | NOTIFICATION                   | GAIN_TRANSIENT | 系统通知    |
| 铃声    | RINGTONE                       | GAIN           | 来电铃声    |
| 通知事件  | NOTIFICATION_EVENT             | GAIN_TRANSIENT | 通知事件    |
| 辅助功能  | ASSISTANCE_ACCESSIBILITY       | GAIN           | 辅助功能    |
| 导航语音  | ASSISTANCE_NAVIGATION_GUIDANCE | GAIN           | 导航语音    |
| 系统提示音 | ASSISTANCE_SONIFICATION        | GAIN_TRANSIENT | 系统提示音   |
| 游戏    | GAME                           | GAIN           | 游戏音效    |
| 语音助手  | ASSISTANT                      | GAIN           | 语音助手    |
| 紧急警报  | EMERGENCY                      | GAIN           | 紧急警报    |
| 安全警告  | SAFETY                         | GAIN           | 安全警告    |
| 车辆状态  | VEHICLE_STATUS                 | GAIN           | 车辆状态提示  |
| 广播    | ANNOUNCEMENT                   | GAIN           | 公共广播    |
| 扬声器清理 | SPEAKER_CLEANUP                | GAIN           | 扬声器清理信号 |

## 快速开始

### 基本使用

1. **选择配置** - 通过下拉菜单选择音频场景
2. **开始播放** - 点击绿色播放按钮
3. **停止播放** - 点击红色停止按钮
4. **重载配置** - 长按下拉菜单重新加载外部配置

### 常用操作

```bash
# 推送测试文件到设备
adb push 48k_2ch_16bit.wav /data/

# 查看播放日志
adb logcat -s AudioPlayer MainActivity AudioConfig

# 检查配置文件
adb shell cat /data/audio_player_configs.json
```

## 安装部署

### 环境要求

- **Android 版本**: Android 12L (API 32) 或更高
- **开发环境**: Android Studio
- **构建系统**: Gradle

### 编译安装

```bash
git clone https://github.com/kainan-tek/AudioPlayer.git
cd AudioPlayer
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
adb push 48k_2ch_16bit.wav /data/
```

### 权限配置

| 权限                      | 用途     | 版本要求        |
|-------------------------|--------|-------------|
| `MODIFY_AUDIO_SETTINGS` | 音频控制   | 全部          |
| `READ_MEDIA_AUDIO`      | 读取音频文件 | Android 13+ |
| `READ_EXTERNAL_STORAGE` | 读取外部存储 | Android 12- |

```bash
# 手动授予权限
adb shell pm grant com.example.audioplayer android.permission.READ_EXTERNAL_STORAGE
```

## 配置说明

### 配置文件位置

- **外部配置**: `/data/audio_player_configs.json`（优先加载）
- **内置配置**: `app/src/main/assets/audio_player_configs.json`

### 配置文件格式

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

### 配置参数

#### Usage（使用场景）

| 值                                      | 说明          |
|----------------------------------------|-------------|
| `USAGE_MEDIA`                          | 媒体播放（音乐、视频） |
| `USAGE_VOICE_COMMUNICATION`            | 语音通话（VoIP）  |
| `USAGE_VOICE_COMMUNICATION_SIGNALLING` | 通话信令        |
| `USAGE_ALARM`                          | 闹钟          |
| `USAGE_NOTIFICATION`                   | 通知          |
| `USAGE_NOTIFICATION_RINGTONE`          | 铃声          |
| `USAGE_NOTIFICATION_EVENT`             | 通知事件        |
| `USAGE_ASSISTANCE_ACCESSIBILITY`       | 辅助功能        |
| `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` | 导航语音        |
| `USAGE_ASSISTANCE_SONIFICATION`        | 系统提示音       |
| `USAGE_GAME`                           | 游戏          |
| `USAGE_ASSISTANT`                      | 语音助手        |
| `USAGE_EMERGENCY`                      | 紧急警报        |
| `USAGE_SAFETY`                         | 安全警告        |
| `USAGE_VEHICLE_STATUS`                 | 车辆状态        |
| `USAGE_ANNOUNCEMENT`                   | 广播          |
| `USAGE_SPEAKER_CLEANUP`                | 扬声器清理       |

#### Content Type（内容类型）

| 值                           | 说明 |
|-----------------------------|----|
| `CONTENT_TYPE_MUSIC`        | 音乐 |
| `CONTENT_TYPE_SPEECH`       | 语音 |
| `CONTENT_TYPE_SONIFICATION` | 音效 |
| `CONTENT_TYPE_MOVIE`        | 电影 |
| `CONTENT_TYPE_UNKNOWN`      | 未知 |

#### Transfer Mode（传输模式）

| 值             | 说明      |
|---------------|---------|
| `MODE_STREAM` | 流模式（推荐） |
| `MODE_STATIC` | 静态模式    |

#### Performance Mode（性能模式）

| 值                               | 说明   |
|---------------------------------|------|
| `PERFORMANCE_MODE_LOW_LATENCY`  | 低延迟  |
| `PERFORMANCE_MODE_POWER_SAVING` | 省电模式 |
| `PERFORMANCE_MODE_NONE`         | 默认   |

#### Buffer Multiplier（缓冲区倍数）

| 值  | 说明              |
|----|-----------------|
| 1  | 最小缓冲区           |
| 2  | 推荐（平衡延迟和稳定性）    |
| 4+ | 更大缓冲区（适合高采样率音频） |

## 音频焦点管理

### 焦点变化处理

| 焦点事件                                 | 处理方式 |
|--------------------------------------|------|
| `AUDIOFOCUS_GAIN`                    | 恢复音量 |
| `AUDIOFOCUS_LOSS`                    | 停止播放 |
| `AUDIOFOCUS_LOSS_TRANSIENT`          | 停止播放 |
| `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK` | 停止播放 |

> **注意**: 当前 UI 不支持暂停功能，所有焦点丢失场景均停止播放。

## API 参考

### AudioPlayer 类

```kotlin
class AudioPlayer(context: Context) {
    fun setAudioConfig(config: AudioConfig)     // 设置音频配置
    fun startPlayback(): Boolean                // 开始播放
    fun stopPlayback()                          // 停止播放（幂等）
    fun isPlaying(): Boolean                   // 检查是否正在播放
    fun release()                               // 释放资源
    fun setPlaybackListener(listener: PlaybackListener?)  // 设置监听器
}
```

### PlaybackListener 接口

```kotlin
interface PlaybackListener {
    fun onPlaybackStarted()                     // 播放开始回调
    fun onPlaybackStopped()                     // 播放停止回调
    fun onPlaybackError(error: String)          // 播放错误回调
}
```

### 错误前缀

| 前缀             | 说明     |
|----------------|--------|
| `[FILE]`       | 文件操作错误 |
| `[STREAM]`     | 音频流错误  |
| `[PERMISSION]` | 权限错误   |
| `[PARAM]`      | 参数验证错误 |
| `[FOCUS]`      | 音频焦点错误 |

## 故障排除

### 常见问题

#### 1. 播放失败

```bash
# 检查文件是否存在
adb shell ls -la /data/*.wav

# 查看详细日志
adb logcat -s AudioPlayer
```

#### 2. 权限问题

```bash
adb shell pm grant com.example.audioplayer android.permission.READ_EXTERNAL_STORAGE
adb shell setenforce 0
```

#### 3. 音频焦点问题

```bash
# 查看音频焦点变化
adb logcat -s AudioFocus AudioPlayer

# 检查其他应用占用
adb shell dumpsys audio | grep "Audio Focus"
```

### 调试命令

```bash
adb logcat -s AudioPlayer MainActivity AudioConfig
adb logcat -s AudioTrack AudioFlinger
```

## 相关项目

- [AAudioPlayer](https://github.com/kainan-tek/AAudioPlayer) - 基于 AAudio API 的高性能播放器
- [AAudioRecorder](https://github.com/kainan-tek/AAudioRecorder) - 基于 AAudio API 的高性能录音器
- [AudioRecorder](https://github.com/kainan-tek/AudioRecorder) - 基于 AudioRecord API 的音频录制器
- [audio_test_client](https://github.com/kainan-tek/audio_test_client) - Android 系统级音频测试工具

## 许可证

本项目采用 MIT License 许可证。详细信息请参阅 [LICENSE](LICENSE) 文件。

---

**注意**: 本项目仅供学习和测试使用。
