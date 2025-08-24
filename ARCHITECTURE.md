# AudioPlayer 架构重构说明

本项目已经按照MVVM架构模式进行了重构，实现了UI与业务逻辑的完全分离。

## 新架构结构

### 1. 目录结构
```
com.example.audioplayer/
├── MainActivity.kt              // UI层，仅负责界面交互
├── model/
│   └── WaveFile.kt              // 数据模型层，封装WAV文件操作
├── player/
│   └── AudioPlayer.kt           // 业务逻辑层，封装音频播放功能
└── viewmodel/
    └── PlayerViewModel.kt       // 视图模型层，连接UI与业务逻辑
```

### 2. 各层职责

#### UI层 (MainActivity)
- 负责界面展示和用户交互
- 通过ViewModel与业务逻辑通信
- 不包含任何音频播放相关代码

#### ViewModel层 (PlayerViewModel)
- 管理UI状态
- 处理用户操作转发给业务逻辑
- 使用LiveData实现数据驱动UI

#### 业务逻辑层 (AudioPlayer)
- 封装所有音频播放相关操作
- 管理音频焦点、播放线程等
- 提供播放状态回调

#### 数据模型层 (WaveFile)
- 封装WAV文件的读取和解析
- 提供音频格式信息
- 处理文件IO操作

### 3. 关键改进

1. **职责分离**: 每个类都有明确的单一职责
2. **可测试性**: 业务逻辑可以独立测试
3. **可维护性**: 修改某一层不会影响其他层
4. **复用性**: AudioPlayer和WaveFile可以在其他项目中复用
5. **生命周期管理**: 使用ViewModel自动处理生命周期

### 4. 使用示例

```kotlin
// 在Activity中播放音频
viewModel.play("/path/to/audio.wav")

// 停止播放
viewModel.stop()

// 监听播放状态
viewModel.isPlaying.observe(this) { isPlaying ->
    // 更新UI状态
}
```

### 5. 依赖关系

- MainActivity → PlayerViewModel → AudioPlayer → WaveFile
- 单向依赖，降低耦合度
- 使用接口和LiveData实现解耦