# AudioPlayer - 简洁音频播放器

一个简洁易读的Android音频播放测试应用，采用现代化设计和MVVM架构。

## 特性

- 🎵 支持WAV格式音频播放
- 🔊 **完整多通道支持 (1-12声道)**
- 🌟 **7.1.4 3D音频支持**
- 🎨 现代Material Design界面
- 🏗️ 简洁的MVVM架构
- 🌏 中文本地化界面
- 🔧 完整的错误处理
- 📱 响应式UI设计

## 音频格式支持

### 声道配置
| 声道数 | 配置名称 | 说明 | 声道布局 |
|--------|----------|------|----------|
| 1 | 单声道 | Mono | M |
| 2 | 立体声 | Stereo | L R |
| 4 | 四声道 | Quad | L R Ls Rs |
| 6 | 5.1声道 | 5.1 Surround | L R C LFE Ls Rs |
| 8 | 7.1声道 | 7.1 Surround | L R C LFE Ls Rs Lrs Rrs |
| 10 | 5.1.4声道 | 5.1 + 4天空 | L R C LFE Ls Rs Ltf Rtf Ltb Rtb |
| 12 | **7.1.4声道** | **7.1 + 4天空** | **L R C LFE Ls Rs Lrs Rrs Ltf Rtf Ltb Rtb** |

### 7.1.4声道详解
7.1.4是最新的3D音频标准，包含12个声道：
- **前置层**: L(左前), R(右前), C(中置), LFE(低频)
- **环绕层**: Ls(左环绕), Rs(右环绕), Lrs(左后环绕), Rrs(右后环绕)  
- **天空层**: Ltf(左前天空), Rtf(右前天空), Ltb(左后天空), Rtb(右后天空)

### 音频参数
- **采样率**: 8kHz - 192kHz
- **位深度**: 8/16/24/32 bit
- **格式**: WAV (PCM)
- **最大声道**: 12声道 (7.1.4)
- **自动参数验证和设备兼容性检查**

### 性能优化
- 根据声道数动态调整缓冲区大小
- 12声道音频使用8KB优化缓冲区
- 实时播放进度和性能监控

## 快速开始

### 环境要求
- Android Studio
- Android SDK API 32+
- 测试设备或模拟器 (Android 12L+)

### 安装步骤

1. **准备测试文件**
```bash
adb root
adb remount  
adb shell setenforce 0
adb push 48k_2ch_16bit.wav /data/
```

2. **编译安装**
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

3. **运行应用**
- 启动应用
- 授予存储权限
- 点击"播放"按钮开始测试

## 项目结构

```
app/src/main/java/com/example/audioplayer/
├── MainActivity.kt              # 主界面
├── model/
│   └── WaveFile.kt              # WAV文件处理
├── player/
│   └── AudioPlayer.kt           # 音频播放器
└── viewmodel/
    └── PlayerViewModel.kt       # 视图模型
```

## 架构设计

采用简洁的MVVM架构模式：

- **UI层**: 负责用户交互和界面展示
- **ViewModel层**: 管理UI状态和数据绑定  
- **业务逻辑层**: 处理音频播放核心功能
- **数据层**: 处理WAV文件读取和解析

详细架构说明请参考 [ARCHITECTURE.md](ARCHITECTURE.md)

## 主要改进

### UI设计
- 采用Material Design 3组件
- 卡片式状态显示
- 清晰的按钮状态反馈
- 中文本地化界面

### 代码质量
- 简化复杂逻辑
- 统一错误处理
- 清晰的代码注释
- 易于理解的命名

### 用户体验
- 友好的错误提示
- 实时状态更新
- 自动权限管理
- 响应式交互

## 技术栈

- **语言**: Kotlin
- **架构**: MVVM + LiveData
- **UI**: Material Design 3
- **音频**: AudioTrack + AudioManager
- **并发**: Kotlin Coroutines
- **最低API**: 32 (Android 12L+)

## 系统要求

- Android 12L+ (API 32+)
- 支持多声道音频输出的设备
- 存储权限 (READ_MEDIA_AUDIO)

## 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件
