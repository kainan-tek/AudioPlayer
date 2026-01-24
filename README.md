# Audio Player

一个基于Android AudioTrack API的音频播放器测试程序，支持12种音频使用场景配置和WAV文件播放。

## 📋 项目概述

Audio Player是一个专为Android平台设计的音频播放测试工具，使用Android AudioTrack API。该项目展示了如何在Android应用中实现高质量的音频播放，支持多种音频使用场景和性能模式。

## ✨ 主要特性

- **🎵 WAV文件播放**: 支持多通道PCM格式WAV文件播放
- **🔊 完整多通道支持**: 支持1-16声道音频播放
- **🌟 7.1.4 3D音频支持**: 支持最新的3D音频标准
- **🔧 12种使用场景**: 涵盖媒体、通话、游戏、导航等音频场景
- **📱 现代化界面**: Material Design风格的直观控制界面
- **🛠️ 动态配置**: 运行时切换音频配置，支持JSON配置文件
- **🎯 音频焦点管理**: 自动处理音频焦点申请和释放
- **🏗️ MVVM架构**: 清晰的代码结构和模块化设计

## 🎵 音频格式支持

### 声道配置
| 声道数  | 配置名称        | 说明            | 声道布局                                        |
|------|-------------|---------------|---------------------------------------------|
| 1    | 单声道         | Mono          | M                                           |
| 2    | 立体声         | Stereo        | L R                                         |
| 4    | 四声道         | Quad          | L R Ls Rs                                   |
| 6    | 5.1声道       | 5.1 Surround  | L R C LFE Ls Rs                             |
| 8    | 7.1声道       | 7.1 Surround  | L R C LFE Ls Rs Lrs Rrs                     |
| 10   | 5.1.4声道     | 5.1 + 4天空     | L R C LFE Ls Rs Ltf Rtf Ltb Rtb             |
| 12   | **7.1.4声道** | **7.1 + 4天空** | **L R C LFE Ls Rs Lrs Rrs Ltf Rtf Ltb Rtb** |
| 1-16 | 其他配置        | 自动映射          | 根据声道数自动选择最佳配置                               |

### 7.1.4声道详解
7.1.4是最新的3D音频标准，包含12个声道：前置层(L R C LFE)、环绕层(Ls Rs Lrs Rrs)、天空层(Ltf Rtf Ltb Rtb)。

### 音频参数
- **采样率**: 8kHz - 192kHz
- **位深度**: 8/16/24/32 bit  
- **格式**: WAV (PCM)
- **最大声道**: 16声道
- **配置系统**: 支持多种音频用途和性能模式

## 🚀 快速开始

### 系统要求

- Android 13 (API 33) 或更高版本
- 支持多声道音频输出的设备
- 开发环境: Android Studio

### 权限要求

- `READ_MEDIA_AUDIO` (API 33+): 读取音频文件
- `READ_EXTERNAL_STORAGE` (API ≤32): 读取外部存储

### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/your-repo/AudioPlayer.git
   cd AudioPlayer
   ```

2. **准备测试文件**
   ```bash
   adb push 48k_2ch_16bit.wav /data/
   ```

3. **编译安装**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 📖 使用说明

### 基本操作

1. **播放控制**
   - 🎵 **开始播放**: 点击绿色播放按钮
   - ⏹️ **停止播放**: 点击红色停止按钮
   - ⚙️ **播放配置**: 点击配置按钮切换音频设置

2. **配置管理**
   - 应用启动时自动加载配置
   - 支持从外部文件动态加载配置
   - 可在运行时切换不同的音频场景

## 🔧 配置文件

### 配置位置

- **外部配置**: `/data/audio_player_configs.json` (优先)
- **内置配置**: `app/src/main/assets/audio_player_configs.json`

### 配置格式

```json
{
  "configs": [
    {
      "usage": "USAGE_MEDIA",
      "contentType": "CONTENT_TYPE_MUSIC",
      "transferMode": "MODE_STREAM",
      "performanceMode": "PERFORMANCE_MODE_POWER_SAVING",
      "minBufferSize": 960,
      "bufferMultiplier": 2,
      "audioFilePath": "/data/48k_2ch_16bit.wav",
      "description": "媒体播放配置"
    }
  ]
}
```

### 支持的常量值

**Usage (使用场景):**
- `USAGE_MEDIA` - 媒体播放
- `USAGE_VOICE_COMMUNICATION` - 语音通话
- `USAGE_ALARM` - 闹钟
- `USAGE_NOTIFICATION` - 通知
- `USAGE_GAME` - 游戏音频

**Content Type (内容类型):**
- `CONTENT_TYPE_MUSIC` - 音乐
- `CONTENT_TYPE_SPEECH` - 语音
- `CONTENT_TYPE_SONIFICATION` - 音效

**Performance Mode:**
- `PERFORMANCE_MODE_LOW_LATENCY` - 低延迟模式
- `PERFORMANCE_MODE_POWER_SAVING` - 省电模式

**Transfer Mode:**
- `MODE_STREAM` - 流模式
- `MODE_STATIC` - 静态模式

## 🏗️ 技术架构

### 核心组件

- **AudioPlayer**: Kotlin编写的音频播放器封装类，集成音频焦点管理
- **AudioConfig**: 音频配置管理类，支持动态加载配置
- **PlayerViewModel**: MVVM架构的视图模型，管理播放状态
- **MainActivity**: 现代化主界面控制器，提供权限管理和用户交互
- **WaveFile**: WAV文件解析器，支持多种格式和多声道

### 技术栈

- **语言**: Kotlin
- **音频API**: Android AudioTrack
- **架构**: MVVM + LiveData
- **UI**: Material Design 3
- **并发**: Kotlin Coroutines
- **最低版本**: Android 13 (API 33)
- **目标版本**: Android 15 (API 36)

## 🔍 技术细节

### AudioTrack集成

- 使用流模式实现音频播放
- 支持多种音频格式 (16/24/32位PCM)
- 完整的错误处理机制
- 音频焦点自动管理

### 数据流架构

```
WAV文件 → WaveFile解析器 → AudioTrack → 音频输出设备
                                ↓
                           Kotlin协程 → UI状态更新
```

### WAV文件支持

- 标准RIFF/WAVE格式解析
- 支持多声道音频 (1-16声道)
- 采样率范围: 8kHz - 192kHz
- 位深度支持: 8/16/24/32位

## 📚 API 参考

### AudioPlayer 类
```kotlin
class AudioPlayer {
    fun setConfig(config: AudioConfig)              // 设置配置
    fun play(): Boolean                             // 开始播放
    fun stop(): Boolean                             // 停止播放
    fun isPlaying(): Boolean                        // 检查播放状态
    fun setPlaybackListener(listener: PlaybackListener?) // 设置监听器
}
```

## 🐛 故障排除

### 常见问题
1. **播放失败** - 确认WAV文件格式支持，验证设备权限设置
2. **权限问题** - `adb shell setenforce 0`
3. **配置加载失败** - 检查JSON格式是否正确

### 调试信息
```bash
adb logcat -s AudioPlayer MainActivity
```

## 📊 性能指标

- **低延迟模式**: ~40-80ms
- **省电模式**: ~80-120ms
- **采样率**: 8kHz - 192kHz
- **声道数**: 1-16声道
- **位深度**: 8/16/24/32位

## 📄 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

---

**注意**: 本项目仅用于学习和测试目的，请确保在合适的设备和环境中使用。
