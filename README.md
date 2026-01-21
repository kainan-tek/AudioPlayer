# AudioPlayer - 简洁音频播放器

一个简洁易读的Android音频播放测试应用，采用现代化设计和MVVM架构。

## 特性

- 🎵 支持WAV格式音频播放
- 🔊 **完整多通道支持 (1-16声道)**
- 🌟 **7.1.4 3D音频支持**
- 🎨 现代Material Design界面
- 🏗️ 简洁的MVVM架构
- 🌏 中文本地化界面
- 🔧 完整的错误处理
- 📱 响应式UI设计

## 音频格式支持

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

## 快速开始

### 环境要求
- Android Studio
- Android SDK API 33+
- 测试设备或模拟器 (Android 13+)

### 安装步骤

1. **准备测试文件**
   ```bash
   adb push 48k_2ch_16bit.wav /data/
   # 可选：推送自定义配置文件
   adb push audio_configs.json /data/
   ```

2. **编译安装**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **运行测试**
   - 启动应用并授予存储权限
   - 应用会自动检测 `/data/audio_configs.json` 是否存在
   - 如果存在外部配置文件，使用外部配置；否则使用内置配置
   - 点击"配置"按钮选择不同的音频配置进行测试

## 项目结构

```
app/src/main/java/com/example/audioplayer/
├── MainActivity.kt              # 主界面
├── config/
│   └── AudioConfig.kt           # 音频配置管理
├── model/
│   └── WaveFile.kt              # WAV文件处理
├── player/
│   └── AudioPlayer.kt           # 音频播放器
└── viewmodel/
    └── PlayerViewModel.kt       # 视图模型
```

## 架构设计

采用简洁的MVVM架构模式：

- **UI层 (MainActivity)**: 负责用户交互和界面展示，使用Material Design组件
- **ViewModel层 (PlayerViewModel)**: 管理UI状态和数据绑定，使用LiveData  
- **业务逻辑层 (AudioPlayer)**: 处理音频播放核心功能，支持多种配置
- **数据层 (WaveFile + AudioConfig)**: 处理WAV文件读取和音频配置管理

### 主要特点
- **简洁性**: 每个类都有明确的单一职责
- **可读性**: 清晰的命名和代码结构  
- **易维护**: 模块化设计，便于扩展
- **配置化**: 支持外部JSON配置文件

详细配置说明请参考 [CONFIGURATION.md](CONFIGURATION.md)

## 主要改进

### 配置系统
- 支持外部JSON配置文件
- 多种音频用途和性能模式
- 12种预设配置场景
- 运行时配置重载

### UI设计
- Material Design 3组件
- 卡片式状态显示
- 中文本地化界面

### 代码质量
- 统一错误处理
- 清晰的代码注释
- 易于理解的命名

### 用户体验
- 友好的错误提示
- 实时状态更新
- 自动权限管理

## 技术栈

- **语言**: Kotlin
- **架构**: MVVM + LiveData
- **UI**: Material Design 3
- **音频**: AudioTrack + AudioManager
- **并发**: Kotlin Coroutines
- **最低API**: 33 (Android 13+)

## 系统要求

- Android 13+ (API 33+)
- 支持多声道音频输出的设备
- 存储权限 (READ_MEDIA_AUDIO)

## 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件
