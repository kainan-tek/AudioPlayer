# AudioPlayer

中文 | [English](README_EN.md)

一个基于Android AudioTrack API的专业音频播放器测试程序，支持17种音频使用场景配置和多格式WAV文件播放。

## 📋 项目概述

AudioPlayer是一个专为Android平台设计的音频播放测试工具，使用Android AudioTrack API。该项目展示了如何在Android应用中实现高质量的音频播放，支持多种音频使用场景和性能模式，是音频开发者和测试工程师的理想工具。

## ✨ 主要特性

- **🎵 多格式WAV播放**: 支持8/16/24/32位PCM格式WAV文件播放
- **🔊 完整多声道支持**: 支持1-16声道音频播放，包括7.1.4 Dolby Atmos
- **🔧 17种使用场景**: 涵盖媒体、通话、游戏、导航、紧急警报等完整音频场景
- **📱 现代化界面**: Material Design 3风格的直观控制界面
- **🛠️ 动态配置系统**: 运行时切换音频配置，支持外部JSON配置文件
- **🎯 智能音频焦点管理**: 自动处理音频焦点申请和释放
- **🏗️ MVVM架构**: 清晰的代码结构和模块化设计

## 🚀 快速开始

### 系统要求

- Android 12 (API 32) 或更高版本
- 支持多声道音频输出的设备
- 开发环境: Android Studio

### 权限要求

- `READ_MEDIA_AUDIO`: 读取音频文件权限 (Android 13+)
- `READ_EXTERNAL_STORAGE`: 读取外部存储权限 (Android 12及以下)

### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/kainan-tek/AudioPlayer.git
   cd AudioPlayer
   ```

2. **准备测试文件**
   ```bash
   adb root && adb remount && adb shell setenforce 0
   adb push 48k_2ch_16bit.wav /data/
   adb push 96k_8ch_24bit.wav /data/  # 可选，用于高质量音频测试
   adb push 48k_12ch_16bit.wav /data/ # 可选，用于多声道测试
   ```

3. **编译安装**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

4. **可选配置文件**
   ```bash
   # 推送自定义配置文件（可选）
   adb push audio_player_configs.json /data/
   ```

## 📖 使用说明

### 基本操作

1. **播放控制**
   - 🎵 **开始播放**: 点击绿色"Start Playback"按钮
   - ⏹️ **停止播放**: 点击红色"Stop Playback"按钮
   - ⚙️ **播放配置**: 通过配置下拉菜单切换音频设置

2. **配置管理**
   - 应用启动时自动加载配置
   - 支持从外部文件动态加载配置
   - 可在运行时通过下拉菜单切换不同的音频场景
   - 长按配置下拉菜单可重新加载外部配置文件

### 配置切换流程

1. 点击配置下拉菜单查看所有可用配置
2. 选择所需的音频场景配置
3. 配置会立即生效，显示在界面上
4. 开始播放测试音频效果
5. 如需重新加载外部配置文件，长按配置下拉菜单即可

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

## 🔧 17种预设配置场景

应用内置17种预设配置，涵盖以下使用场景：

### 媒体播放类
- **媒体播放** (省电模式) - 标准音乐播放
- **游戏音频** (低延迟) - 游戏音效播放

### 通信音频类
- **语音通话** (低延迟) - VoIP通话音频
- **通话信令** (低延迟) - 通话提示音

### 系统音效类
- **闹钟音效** (省电模式) - 闹钟铃声
- **通知音效** (省电模式) - 系统通知
- **铃声播放** (省电模式) - 来电铃声
- **通知事件** (省电模式) - 事件提醒

### 辅助功能类
- **辅助功能** (省电模式) - 无障碍语音
- **导航语音** (省电模式) - GPS导航
- **系统提示音** (低延迟) - 系统警告

### 特殊用途类
- **语音助手** (省电模式) - AI助手语音
- **紧急警报** (低延迟) - 紧急情况
- **安全警告** (低延迟) - 安全提醒
- **车辆状态** (省电模式) - 汽车音频
- **公共广播** (省电模式) - 广播系统
- **扬声器清理** (低延迟) - 硬件维护
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
      "bufferMultiplier": 2,
      "audioFilePath": "/data/48k_2ch_16bit.wav",
      "description": "Media Playback (Power Saving Mode)"
    }
  ]
}
```

### 音频文件位置

- **推荐位置**: `/data/` 目录 (需要root权限)
- **测试文件**: 通过adb push推送到设备
- **支持格式**: 标准WAV文件 (RIFF/WAVE格式)
- **文件要求**: 8/16/24/32位PCM格式，1-16声道

### 支持的常量值

**Usage (使用场景):**
- `USAGE_MEDIA` - 媒体播放
- `USAGE_VOICE_COMMUNICATION` - 语音通话
- `USAGE_ALARM` - 闹钟
- `USAGE_NOTIFICATION` - 通知
- `USAGE_GAME` - 游戏音频
- `USAGE_ASSISTANT` - 语音助手
- `USAGE_EMERGENCY` - 紧急警报
- `USAGE_SAFETY` - 安全警告
- 等17种使用场景

**Content Type (内容类型):**
- `CONTENT_TYPE_MUSIC` - 音乐
- `CONTENT_TYPE_SPEECH` - 语音
- `CONTENT_TYPE_SONIFICATION` - 音效
- `CONTENT_TYPE_MOVIE` - 电影

**Performance Mode:**
- `PERFORMANCE_MODE_LOW_LATENCY` - 低延迟模式
- `PERFORMANCE_MODE_POWER_SAVING` - 省电模式
- `PERFORMANCE_MODE_NONE` - 无特殊优化

**Transfer Mode:**
- `MODE_STREAM` - 流模式
- `MODE_STATIC` - 静态模式

## 🏗️ 技术架构

### 核心组件

- **AudioPlayer**: Kotlin编写的音频播放器封装类，集成音频焦点管理
- **AudioConfig**: 音频配置管理类，支持动态加载配置
- **PlayerViewModel**: MVVM架构的视图模型，管理播放状态
- **MainActivity**: 现代化主界面控制器，提供权限管理和用户交互
- **WavFile**: WAV文件解析器，支持多种格式和多声道

### 技术栈

- **语言**: Kotlin
- **音频API**: Android AudioTrack
- **架构**: MVVM + LiveData
- **UI**: Material Design 3
- **并发**: Kotlin Coroutines
- **最低版本**: Android 12 (API 32)
- **目标版本**: 未指定 (使用 compileSdk)
- **编译版本**: Android 15 (API 36)

### 依赖库

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

## 🔍 技术细节

### AudioTrack集成

- 使用流模式实现音频播放
- 支持多种音频格式 (8/16/24/32位PCM)
- 完整的错误处理机制
- 音频焦点自动管理
- 智能缓冲区管理

### 数据流架构

```
WAV文件 → WavFile解析器 → AudioTrack → 音频输出设备
                                ↓
                           Kotlin协程 → UI状态更新
```

### WAV文件支持

- 标准RIFF/WAVE格式解析
- 支持多声道音频 (1-16声道)
- 采样率范围: 8kHz - 192kHz
- 位深度支持: 8/16/24/32位
- 自动声道映射和验证

### 音频焦点管理

- 自动申请和释放音频焦点
- 支持音频焦点变化处理
- 根据配置选择合适的焦点类型
- 与其他音频应用协调播放

## 📚 API 参考

### AudioPlayer 类
```kotlin
class AudioPlayer(context: Context) {
    fun setAudioConfig(config: AudioConfig)              // 设置配置
    fun startPlayback(): Boolean                         // 开始播放
    fun stopPlayback()                                   // 停止播放
    fun release()                                        // 释放资源
    fun setPlaybackListener(listener: PlaybackListener)  // 设置监听器
}
```

### PlayerViewModel 类
```kotlin
class PlayerViewModel : ViewModel() {
    val playerState: LiveData<PlayerState>               // 播放状态
    val statusMessage: LiveData<String>                  // 状态消息
    val errorMessage: LiveData<String?>                  // 错误消息
    val currentConfig: LiveData<AudioConfig?>            // 当前配置
    
    fun startPlayback()                                  // 开始播放
    fun stopPlayback()                                   // 停止播放
    fun setAudioConfig(config: AudioConfig)              // 设置配置
    fun getAllAudioConfigs(): List<AudioConfig>          // 获取所有配置
    fun reloadConfigurations()                           // 重新加载配置
}
```

### AudioConfig 类
```kotlin
data class AudioConfig(
    val usage: Int,                                      // 音频用途
    val contentType: Int,                                // 内容类型
    val transferMode: Int,                               // 传输模式
    val performanceMode: Int,                            // 性能模式
    val bufferMultiplier: Int,                           // 缓冲区倍数
    val audioFilePath: String,                           // 音频文件路径
    val description: String                              // 配置描述
) {
    companion object {
        fun loadConfigs(context: Context): List<AudioConfig>     // 加载配置
        fun reloadConfigs(context: Context): List<AudioConfig>   // 重新加载配置
    }
}
```

## 🐛 故障排除

### 常见问题

1. **播放失败**
   - 确认WAV文件格式支持 (8/16/24/32位PCM)
   - 验证设备权限设置
   - 检查文件路径是否正确

2. **权限问题**
   - 应用首次运行时会自动请求权限，按照屏幕提示授予
   - 如果权限被拒绝，可在系统设置中手动授予存储权限
   ```bash
   adb root && adb remount && adb shell setenforce 0
   ```

3. **配置加载失败**
   - 检查JSON格式是否正确
   - 验证配置文件路径
   - 查看应用日志获取详细错误信息

4. **多声道播放问题**
   - 确认设备支持指定声道配置
   - 检查音频文件声道数是否正确
   - 尝试使用立体声配置测试

### 调试信息
```bash
# 查看应用日志
adb logcat -s AudioPlayer MainActivity PlayerViewModel

# 检查配置文件
adb shell cat /data/audio_player_configs.json

# 验证音频文件
adb shell ls -la /data/*.wav
```

### 性能优化建议

1. **低延迟场景**
   - 使用 `PERFORMANCE_MODE_LOW_LATENCY`
   - 设置较小的 `bufferMultiplier` (1-2)
   - 选择合适的音频用途 (`GAME`, `EMERGENCY`)

2. **高质量音乐**
   - 使用 `PERFORMANCE_MODE_POWER_SAVING`
   - 设置较大的 `bufferMultiplier` (4-6)
   - 选择 `USAGE_MEDIA` 和 `CONTENT_TYPE_MUSIC`

3. **多声道音频**
   - 确保音频文件路径正确
   - 使用适当的缓冲区大小
   - 12声道音频建议使用更大的缓冲区

## 📊 性能指标

- **延迟性能**: 低延迟模式 ~40-80ms，省电模式 ~80-120ms
- **采样率**: 8kHz - 192kHz
- **声道数**: 1-16声道
- **位深度**: 8/16/24/32位
- **缓冲区**: 可配置缓冲区倍数 (1-8倍)
- **支持格式**: WAV (PCM)
- **最大文件大小**: 受设备存储限制

## 🔗 相关项目

- [**AudioRecorder**](https://github.com/kainan-tek/AudioRecorder) - 配套的音频录制应用，基于AudioRecord API
- [**AAudioPlayer**](https://github.com/kainan-tek/AAudioPlayer) - 基于AAudio API的高性能播放器（独立项目）
- [**AAudioRecorder**](https://github.com/kainan-tek/AAudioRecorder) - 基于AAudio API的录音器（独立项目）

## 📄 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

---

**注意**: 本项目仅用于学习和测试目的，请确保在合适的设备和环境中使用。