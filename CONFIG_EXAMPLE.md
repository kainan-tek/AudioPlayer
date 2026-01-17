# 音频配置文件说明

## 配置文件位置
- **外部配置文件**: `/data/config/audio_configs.json`
- **默认配置文件**: `app/src/main/assets/config/audio_configs.json`

## 使用方法

1. **首次运行**: 应用会自动从assets复制默认配置到 `/data/config/audio_configs.json`
2. **修改配置**: 直接编辑 `/data/config/audio_configs.json` 文件
3. **应用更改**: 在应用中选择"重新加载配置文件"

## JSON配置格式

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

## 参数说明

### usage (音频用途)
- `MEDIA` - 媒体播放
- `GAME` - 游戏音频
- `NOTIFICATION` - 通知音效
- `RINGTONE` - 铃声
- `ALARM` - 闹钟
- `ACCESSIBILITY` - 辅助功能
- `SYSTEM_SONIFICATION` - 系统音效

### contentType (内容类型)
- `MUSIC` - 音乐
- `MOVIE` - 电影
- `SPEECH` - 语音
- `SONIFICATION` - 音效

### transferMode (传输模式)
- `STREAM` - 流式传输 (推荐)
- `STATIC` - 静态模式 (适合短音效)

### performanceMode (性能模式)
- `LOW_LATENCY` - 低延迟模式
- `POWER_SAVING` - 省电模式
- `NONE` - 无特殊模式

### 其他参数
- `bufferMultiplier`: 缓冲区倍数 (1-8)
- `audioFilePath`: 音频文件路径
- `minBufferSize`: 最小缓冲区大小 (字节)
- `description`: 配置描述

## 示例配置

### 超低延迟游戏音频
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

### 高质量音乐播放
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

### 多声道电影音频
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

## 快速修改配置

使用adb命令快速修改配置文件:

```bash
# 下载当前配置文件
adb pull /data/config/audio_configs.json

# 编辑配置文件 (使用你喜欢的编辑器)
nano audio_configs.json

# 上传修改后的配置文件
adb push audio_configs.json /data/config/

# 在应用中重新加载配置
```

## 注意事项

1. **文件权限**: 确保 `/data/` 目录有写入权限
2. **JSON格式**: 确保JSON格式正确，否则会使用默认配置
3. **文件路径**: 确保音频文件路径存在且可访问
4. **参数范围**: 缓冲区大小建议在120-4096字节之间