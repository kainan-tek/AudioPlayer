# 音频配置系统详解

AudioPlayer 应用包含一个完整的配置系统，支持通过外部JSON文件灵活配置音频播放参数。

## 目录
- [配置文件格式](#配置文件格式)
- [配置系统架构](#配置系统架构)  
- [配置参数详解](#配置参数详解)
- [配置示例](#配置示例)
- [预设配置场景](#预设配置场景)
- [配置文件管理](#配置文件管理)
- [最佳实践](#最佳实践)
- [故障排除](#故障排除)

## 配置文件格式

### JSON结构示例
```json
{
  "configs": [
    {
      "usage": "MEDIA",
      "contentType": "MUSIC",
      "transferMode": "STREAM", 
      "performanceMode": "LOW_LATENCY",
      "bufferMultiplier": 2,
      "audioFilePath": "/data/48k_2ch_16bit.wav",
      "minBufferSize": 480,
      "description": "媒体播放 (低延迟)"
    }
  ]
}
```

### 使用方法
1. **首次运行**: 应用会自动从assets复制默认配置到 `/data/audio_configs.json`
2. **修改配置**: 直接编辑 `/data/audio_configs.json` 文件
3. **应用更改**: 在应用中选择"重新加载配置文件"

## 配置系统架构
### 配置文件层次
1. **默认配置**: `app/src/main/assets/audio_configs.json` (内置)
2. **外部配置**: `/data/audio_configs.json` (可修改)

### 加载机制
- 首次运行时，自动从assets复制默认配置到外部路径
- 优先加载外部配置文件
- 外部文件不存在时回退到默认配置
- 支持运行时重新加载配置

## 配置参数详解

### 音频用途 (usage)
控制音频流的系统级用途，影响音量控制和音频焦点：

- `MEDIA` - 媒体播放 (音乐、视频)
- `GAME` - 游戏音频
- `VOICE_COMMUNICATION` - 语音通话
- `VOICE_COMMUNICATION_SIGNALLING` - 通话信令音
- `ALARM` - 闹钟音效
- `NOTIFICATION` - 通知音效
- `RINGTONE` - 铃声播放
- `NOTIFICATION_EVENT` - 通知事件
- `ACCESSIBILITY` - 辅助功能
- `NAVIGATION_GUIDANCE` - 导航语音
- `SYSTEM_SONIFICATION` - 系统提示音
- `ASSISTANT` - 语音助手

### 内容类型 (contentType)
描述音频内容的性质，用于系统优化：

- `MUSIC` - 音乐内容
- `MOVIE` - 电影音频
- `SPEECH` - 语音内容
- `SONIFICATION` - 音效提示

### 传输模式 (transferMode)
控制音频数据传输方式：

- `STREAM` - 流式传输 (推荐，适合长音频)
- `STATIC` - 静态模式 (适合短音效)

### 性能模式 (performanceMode)
平衡延迟和功耗：

- `LOW_LATENCY` - 低延迟模式 (游戏、实时音频)
- `POWER_SAVING` - 省电模式 (音乐播放)
- `NONE` - 无特殊优化

### 缓冲区配置
- `bufferMultiplier` - 缓冲区倍数 (1-8)
- `minBufferSize` - 最小缓冲区大小 (字节)

## 配置示例

### 常用场景配置

#### 标准音乐播放
```json
{
  "usage": "MEDIA",
  "contentType": "MUSIC",
  "transferMode": "STREAM",
  "performanceMode": "POWER_SAVING",
  "bufferMultiplier": 4,
  "audioFilePath": "/data/48k_2ch_16bit.wav",
  "minBufferSize": 960,
  "description": "标准音乐播放"
}
```

#### 低延迟游戏音频
```json
{
  "usage": "GAME",
  "contentType": "SONIFICATION",
  "transferMode": "STREAM",
  "performanceMode": "LOW_LATENCY",
  "bufferMultiplier": 1,
  "audioFilePath": "/data/game_sound.wav",
  "minBufferSize": 120,
  "description": "游戏音频 (超低延迟)"
}
```

#### 高质量音乐播放
```json
{
  "usage": "MEDIA",
  "contentType": "MUSIC", 
  "transferMode": "STREAM",
  "performanceMode": "POWER_SAVING",
  "bufferMultiplier": 6,
  "audioFilePath": "/data/hifi_music.wav",
  "minBufferSize": 1920,
  "description": "高保真音乐播放"
}
```

#### 多声道电影音频
```json
{
  "usage": "MEDIA",
  "contentType": "MOVIE",
  "transferMode": "STREAM", 
  "performanceMode": "LOW_LATENCY",
  "bufferMultiplier": 4,
  "audioFilePath": "/data/movie_7_1_4.wav",
  "minBufferSize": 1440,
  "description": "7.1.4环绕声电影"
}
```

## 预设配置场景

应用包含12种预设配置，覆盖以下场景：

**媒体播放**: 标准音乐、多声道音乐、高质量电影  
**通信音频**: 语音通话、通话信令  
**系统音效**: 系统提示音、通知音效、闹钟音效  
**其他用途**: 游戏音频、语音助手、辅助功能、导航语音

详细配置参数请查看 `app/src/main/assets/audio_configs.json` 文件。

## 配置文件管理

### 基本操作
```bash
# 查看当前配置
adb pull /data/audio_configs.json && cat audio_configs.json

# 修改配置
adb pull /data/audio_configs.json
# 编辑文件后
adb push audio_configs.json /data/

# 恢复默认配置
adb shell rm /data/audio_configs.json
```

## 配置验证

系统会自动验证配置参数：
- 检查JSON格式正确性
- 验证参数值有效性
- 提供详细的错误信息
- 无效配置时回退到默认值

## 最佳实践

### 低延迟场景
- 使用 `LOW_LATENCY` 性能模式
- 设置较小的 `bufferMultiplier` (1-2)
- 选择合适的音频用途 (`GAME`, `SYSTEM_SONIFICATION`)

### 高质量音乐
- 使用 `POWER_SAVING` 性能模式
- 设置较大的 `bufferMultiplier` (4-6)
- 选择 `MEDIA` 用途和 `MUSIC` 内容类型

### 多声道音频
- 确保音频文件路径正确
- 使用适当的缓冲区大小
- 12声道音频建议使用更大的缓冲区

## 故障排除

### 配置加载失败
- 检查JSON格式是否正确
- 确认文件路径和权限
- 查看应用日志获取详细错误信息

### 音频播放问题
- 验证音频文件是否存在
- 检查设备是否支持指定的声道配置
- 尝试使用默认配置测试

### 性能问题
- 调整缓冲区大小
- 选择合适的性能模式
- 根据声道数优化配置参数