package com.example.audioplayer.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.example.audioplayer.config.AudioConfig
import com.example.audioplayer.model.WaveFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 简洁的音频播放器，负责WAV文件播放
 * 支持可配置的音频参数用于测试不同场景
 */
class AudioPlayer(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioPlayer"
    }

    // 播放组件
    private var audioTrack: AudioTrack? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var waveFile: WaveFile? = null
    
    // 播放状态
    private var isPlaying = false
    private var playbackJob: Job? = null
    private val playbackScope = CoroutineScope(Dispatchers.IO)
    
    // 音频配置
    private var currentConfig: AudioConfig = AudioConfig()

    // 播放监听器
    interface PlaybackListener {
        fun onPlaybackStarted()
        fun onPlaybackStopped()
        fun onPlaybackError(error: String)
    }

    private var listener: PlaybackListener? = null

    fun setPlaybackListener(listener: PlaybackListener) {
        this.listener = listener
    }
    
    /**
     * 设置音频配置
     */
    fun setAudioConfig(config: AudioConfig) {
        if (isPlaying) {
            Log.w(TAG, "播放中无法更改配置，请先停止播放")
            return
        }
        currentConfig = config
        Log.i(TAG, "音频配置已更新: ${config.description}")
        Log.d(TAG, config.getDetailedInfo())
    }

    /**
     * 开始播放音频
     */
    fun play(): Boolean {
        Log.d(TAG, "开始播放")
        
        if (isPlaying) {
            Log.i(TAG, "已在播放中，先停止当前播放")
            stop()
        }

        return try {
            // 打开音频文件
            if (!openAudioFile()) {
                return false
            }
            
            // 初始化播放器
            if (!initializeAudioTrack()) {
                return false
            }

            // 开始播放
            isPlaying = true
            startPlaybackLoop()
            listener?.onPlaybackStarted()
            
            Log.i(TAG, "播放开始成功")
            true
        } catch (e: Exception) {
            handleError("播放初始化失败: ${e.message}")
            false
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        Log.d(TAG, "停止播放")
        
        if (!isPlaying) {
            return
        }

        isPlaying = false
        playbackJob?.cancel()
        releaseResources()
        listener?.onPlaybackStopped()
        
        Log.i(TAG, "播放已停止")
    }

    /**
     * 释放所有资源
     */
    fun release() {
        Log.d(TAG, "释放播放器资源")
        stop()
        playbackScope.cancel()
    }

    /**
     * 打开音频文件，使用配置中的文件路径
     */
    private fun openAudioFile(): Boolean {
        waveFile = WaveFile(currentConfig.audioFilePath)
        
        if (!waveFile!!.open() || !waveFile!!.isValid()) {
            handleError("无法打开音频文件: ${currentConfig.audioFilePath}")
            return false
        }
        
        Log.d(TAG, "音频文件已打开: ${waveFile!!.sampleRate}Hz, ${waveFile!!.bitsPerSample}bit, ${waveFile!!.channelCount}ch")
        Log.d(TAG, "文件路径: ${currentConfig.audioFilePath}")
        return true
    }

    /**
     * 初始化AudioTrack，使用当前配置参数
     */
    private fun initializeAudioTrack(): Boolean {
        val waveFile = waveFile ?: return false
        
        try {
            // 获取音频焦点
            if (!requestAudioFocus()) {
                return false
            }

            // 验证音频参数
            if (!validateAudioParameters(waveFile)) {
                return false
            }

            // 创建AudioTrack
            val channelMask = getChannelMask(waveFile.channelCount)
            val audioFormat = getAudioFormat(waveFile.bitsPerSample)
            val minBufferSize = AudioTrack.getMinBufferSize(waveFile.sampleRate, channelMask, audioFormat)
            
            if (minBufferSize == AudioTrack.ERROR_BAD_VALUE) {
                handleError("不支持的音频参数组合: ${waveFile.sampleRate}Hz, ${waveFile.channelCount}ch, ${waveFile.bitsPerSample}bit")
                return false
            }
            
            val bufferSize = maxOf(minBufferSize * currentConfig.bufferMultiplier, currentConfig.minBufferSize)

            // 使用配置参数创建AudioAttributes
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(currentConfig.usage)
                .setContentType(currentConfig.contentType)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(waveFile.sampleRate)
                        .setChannelMask(channelMask)
                        .setEncoding(audioFormat)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(currentConfig.transferMode)
                .setPerformanceMode(currentConfig.performanceMode)
                .build()

            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                handleError("AudioTrack初始化失败，状态: ${audioTrack?.state}")
                return false
            }

            Log.i(TAG, "AudioTrack初始化成功 - ${waveFile.sampleRate}Hz, ${waveFile.channelDescription}, ${waveFile.bitsPerSample}bit, 缓冲区: ${bufferSize}字节")
            Log.i(TAG, "使用配置: ${currentConfig.description}")
            
            // 输出详细的音频信息
            if (waveFile.channelCount >= 10) {
                Log.i(TAG, "3D音频信息:")
                Log.i(TAG, "声道布局: ${waveFile.channelLayout}")
                if (waveFile.channelCount == 12) {
                    Log.i(TAG, "7.1.4格式: 包含4个天空声道 (Ltf Rtf Ltb Rtb)")
                }
            }
            
            return true
        } catch (e: Exception) {
            handleError("AudioTrack创建失败: ${e.message}")
            return false
        }
    }

    /**
     * 验证音频参数是否受支持
     */
    private fun validateAudioParameters(waveFile: WaveFile): Boolean {
        // 检查采样率
        if (waveFile.sampleRate !in 8000..192000) {
            handleError("不支持的采样率: ${waveFile.sampleRate}Hz (支持范围: 8000-192000Hz)")
            return false
        }
        
        // 检查声道数 - 扩展到12通道支持
        if (waveFile.channelCount !in 1..16) {
            handleError("不支持的声道数: ${waveFile.channelCount} (支持范围: 1-16声道)")
            return false
        }
        
        // 特别标识12通道7.1.4配置
        if (waveFile.channelCount == 12) {
            Log.i(TAG, "检测到7.1.4音频配置 (12通道)")
        }
        
        // 检查位深度
        if (waveFile.bitsPerSample !in listOf(8, 16, 24, 32)) {
            handleError("不支持的位深度: ${waveFile.bitsPerSample}bit (支持: 8/16/24/32bit)")
            return false
        }
        
        Log.d(TAG, "音频参数验证通过: ${waveFile.sampleRate}Hz, ${waveFile.channelDescription}, ${waveFile.bitsPerSample}bit")
        return true
    }

    /**
     * 请求音频焦点，使用当前配置参数
     */
    private fun requestAudioFocus(): Boolean {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> audioTrack?.setVolume(1.0f)
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> stop()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> audioTrack?.setVolume(0.3f)
            }
        }

        // 使用配置参数创建AudioAttributes
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(currentConfig.usage)
            .setContentType(currentConfig.contentType)
            .build()

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()

        return audioManager?.requestAudioFocus(focusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    /**
     * 开始播放循环，根据配置和声道数调整缓冲区
     */
    private fun startPlaybackLoop() {
        playbackJob = playbackScope.launch {
            val waveFile = waveFile ?: return@launch
            
            // 根据配置和声道数调整缓冲区大小
            val baseBufferSize = when {
                waveFile.channelCount >= 12 -> 8192  // 12通道需要更大缓冲区
                waveFile.channelCount >= 8 -> 6144   // 8通道
                waveFile.channelCount >= 6 -> 4096   // 6通道
                else -> 4096                         // 默认
            }
            
            // 根据性能模式调整缓冲区
            val bufferSize = when (currentConfig.performanceMode) {
                AudioTrack.PERFORMANCE_MODE_LOW_LATENCY -> baseBufferSize / 2  // 低延迟模式使用较小缓冲区
                AudioTrack.PERFORMANCE_MODE_POWER_SAVING -> baseBufferSize * 2  // 省电模式使用较大缓冲区
                else -> baseBufferSize
            }
            
            val buffer = ByteArray(bufferSize)
            var totalBytes = 0L
            val startTime = System.currentTimeMillis()
            
            audioTrack?.play()
            Log.i(TAG, "开始播放 ${waveFile.channelDescription} 音频，配置: ${currentConfig.description}，缓冲区: ${bufferSize}字节")
            
            try {
                while (isActive && isPlaying) {
                    val bytesRead = waveFile.readData(buffer, 0, buffer.size)
                    
                    if (bytesRead <= 0) {
                        Log.d(TAG, "文件读取完成")
                        break
                    }
                    
                    val bytesWritten = audioTrack?.write(buffer, 0, bytesRead) ?: -1
                    if (bytesWritten < 0) {
                        Log.e(TAG, "AudioTrack写入失败: $bytesWritten")
                        break
                    }
                    
                    totalBytes += bytesRead
                    
                    // 定期输出播放进度（每10MB）
                    if (totalBytes % (10 * 1024 * 1024) == 0L && totalBytes > 0) {
                        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                        val mbPlayed = totalBytes / (1024.0 * 1024.0)
                        Log.d(TAG, "播放进度: ${String.format(java.util.Locale.US, "%.1f", mbPlayed)}MB, 用时: ${String.format(java.util.Locale.US, "%.1f", elapsed)}s")
                    }
                }
                
                if (isPlaying) {
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                    val mbTotal = totalBytes / (1024.0 * 1024.0)
                    Log.i(TAG, "播放完成: ${String.format(java.util.Locale.US, "%.1f", mbTotal)}MB, 总用时: ${String.format(java.util.Locale.US, "%.1f", elapsed)}s")
                    stop()
                }
            } catch (e: Exception) {
                if (isPlaying) {
                    handleError("播放过程出错: ${e.message}")
                }
            }
        }
    }

    /**
     * 释放资源
     */
    private fun releaseResources() {
        try {
            audioTrack?.apply {
                if (state == AudioTrack.STATE_INITIALIZED) {
                    stop()
                }
                release()
            }
            audioTrack = null

            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            focusRequest = null
            audioManager = null
            
            waveFile?.close()
            waveFile = null
        } catch (e: Exception) {
            Log.e(TAG, "资源释放出错", e)
        }
    }

    /**
     * 处理错误
     */
    private fun handleError(message: String) {
        Log.e(TAG, message)
        listener?.onPlaybackError(message)
        releaseResources()
    }

    /**
     * 获取声道掩码，支持多通道音频包括7.1.4 (12通道)
     * 适用于API 33+
     */
    private fun getChannelMask(channelCount: Int): Int {
        return when (channelCount) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            4 -> AudioFormat.CHANNEL_OUT_QUAD
            6 -> AudioFormat.CHANNEL_OUT_5POINT1
            8 -> AudioFormat.CHANNEL_OUT_7POINT1_SURROUND
            10 -> AudioFormat.CHANNEL_OUT_5POINT1POINT4  // 5.1.4配置
            12 -> AudioFormat.CHANNEL_OUT_7POINT1POINT4  // 7.1.4配置
            else -> {
                Log.w(TAG, "不支持的声道数: $channelCount，使用立体声播放")
                AudioFormat.CHANNEL_OUT_STEREO
            }
        }
    }

    /**
     * 获取音频格式，支持多种位深度
     */
    private fun getAudioFormat(bitsPerSample: Int): Int {
        return when (bitsPerSample) {
            8 -> AudioFormat.ENCODING_PCM_8BIT
            16 -> AudioFormat.ENCODING_PCM_16BIT
            24 -> AudioFormat.ENCODING_PCM_24BIT_PACKED
            32 -> AudioFormat.ENCODING_PCM_32BIT
            else -> {
                Log.w(TAG, "不支持的位深度: $bitsPerSample，使用16位")
                AudioFormat.ENCODING_PCM_16BIT
            }
        }
    }
}