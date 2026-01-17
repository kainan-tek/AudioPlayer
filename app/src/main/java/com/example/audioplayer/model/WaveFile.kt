package com.example.audioplayer.model

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * 简洁的WAV文件读取类
 */
class WaveFile(private val filePath: String) {
    
    companion object {
        private const val TAG = "WaveFile"
        private const val WAV_HEADER_SIZE = 44
        private const val RIFF_OFFSET = 0
        private const val SAMPLE_RATE_OFFSET = 24
        private const val CHANNEL_COUNT_OFFSET = 22
        private const val BITS_PER_SAMPLE_OFFSET = 34
        private const val DATA_SIZE_OFFSET = 40
        private const val BYTE_RATE_OFFSET = 28
        private const val BLOCK_ALIGN_OFFSET = 32
    }

    private var fileInputStream: FileInputStream? = null
    private var isFileOpen = false
    
    // 音频属性
    var sampleRate: Int = 0
        private set
    var channelCount: Int = 0
        private set
    var bitsPerSample: Int = 0
        private set
    var dataSize: Int = 0
        private set
    var byteRate: Int = 0
        private set
    var blockAlign: Int = 0
        private set
    
    val duration: Float 
        get() = if (sampleRate > 0 && channelCount > 0 && bitsPerSample > 0) {
            dataSize.toFloat() / (sampleRate * channelCount * (bitsPerSample / 8))
        } else 0f
    
    val channelDescription: String
        get() = when (channelCount) {
            1 -> "单声道"
            2 -> "立体声"
            4 -> "四声道"
            6 -> "5.1声道"
            8 -> "7.1声道"
            10 -> "5.1.4声道"
            12 -> "7.1.4声道"
            else -> "${channelCount}声道 (播放为立体声)"
        }

    val channelLayout: String
        get() = when (channelCount) {
            1 -> "M"
            2 -> "L R"
            4 -> "L R Ls Rs"
            6 -> "L R C LFE Ls Rs"
            8 -> "L R C LFE Ls Rs Lrs Rrs"
            10 -> "L R C LFE Ls Rs Ltf Rtf Ltb Rtb"
            12 -> "L R C LFE Ls Rs Lrs Rrs Ltf Rtf Ltb Rtb"
            else -> "${channelCount}声道 → 立体声 (L R)"
        }

    /**
     * 打开WAV文件并解析头部信息
     */
    fun open(): Boolean {
        Log.d(TAG, "打开WAV文件: $filePath")
        
        try {
            close() // 确保之前的资源已释放
            
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "文件不存在: $filePath")
                return false
            }
            
            if (file.length() < WAV_HEADER_SIZE) {
                Log.e(TAG, "文件太小，不是有效的WAV文件: ${file.length()} bytes")
                return false
            }
            
            fileInputStream = FileInputStream(file)
            
            // 读取WAV头部
            val header = ByteArray(WAV_HEADER_SIZE)
            val bytesRead = fileInputStream!!.read(header)
            
            if (bytesRead != WAV_HEADER_SIZE) {
                Log.e(TAG, "无法读取完整的WAV头部")
                return false
            }
            
            // 验证RIFF标识
            if (!isValidRiffHeader(header)) {
                Log.e(TAG, "不是有效的WAV文件格式")
                return false
            }
            
            // 解析音频参数
            sampleRate = readLittleEndianInt(header, SAMPLE_RATE_OFFSET)
            channelCount = readLittleEndianShort(header, CHANNEL_COUNT_OFFSET)
            bitsPerSample = readLittleEndianShort(header, BITS_PER_SAMPLE_OFFSET)
            dataSize = readLittleEndianInt(header, DATA_SIZE_OFFSET)
            byteRate = readLittleEndianInt(header, BYTE_RATE_OFFSET)
            blockAlign = readLittleEndianShort(header, BLOCK_ALIGN_OFFSET)
            
            // 验证参数有效性和一致性
            if (!validateAudioParameters()) {
                return false
            }
            
            isFileOpen = true
            Log.i(TAG, "WAV文件打开成功: ${sampleRate}Hz, $channelDescription, ${bitsPerSample}bit, 时长${String.format(java.util.Locale.US, "%.2f", duration)}s")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "打开文件失败: $filePath", e)
            close()
            return false
        }
    }

    /**
     * 读取音频数据
     */
    fun readData(buffer: ByteArray, offset: Int, length: Int): Int {
        if (!isFileOpen || fileInputStream == null) {
            return -1
        }
        
        return try {
            fileInputStream!!.read(buffer, offset, length)
        } catch (e: IOException) {
            Log.e(TAG, "读取数据失败", e)
            close()
            -1
        }
    }

    /**
     * 关闭文件
     */
    fun close() {
        if (isFileOpen || fileInputStream != null) {
            Log.d(TAG, "关闭WAV文件")
            try {
                fileInputStream?.close()
            } catch (e: IOException) {
                Log.w(TAG, "关闭文件时出错", e)
            } finally {
                fileInputStream = null
                isFileOpen = false
            }
        }
    }

    /**
     * 检查文件是否有效
     */
    fun isValid(): Boolean {
        return isFileOpen && sampleRate > 0 && channelCount > 0 && bitsPerSample > 0
    }

    override fun toString(): String {
        return "WaveFile(path='$filePath', ${sampleRate}Hz, $channelDescription, ${bitsPerSample}bit, ${String.format(java.util.Locale.US, "%.2f", duration)}s)"
    }

    /**
     * 验证音频参数的有效性和一致性
     */
    private fun validateAudioParameters(): Boolean {
        // 基本参数检查
        if (sampleRate <= 0 || channelCount <= 0 || bitsPerSample <= 0) {
            Log.e(TAG, "无效的音频参数: ${sampleRate}Hz, ${channelCount}ch, ${bitsPerSample}bit")
            return false
        }
        
        // 检查参数范围
        if (sampleRate !in 8000..192000) {
            Log.w(TAG, "采样率超出常见范围: ${sampleRate}Hz")
        }
        
        if (channelCount > 16) {
            Log.w(TAG, "声道数超出支持范围: ${channelCount}声道")
            return false
        } else if (channelCount > 12) {
            Log.w(TAG, "声道数较多: ${channelCount}声道，可能不被所有设备支持")
        } else if (channelCount == 12) {
            Log.i(TAG, "检测到7.1.4音频格式 (12通道)")
        }
        
        if (bitsPerSample !in listOf(8, 16, 24, 32)) {
            Log.w(TAG, "不常见的位深度: ${bitsPerSample}bit")
        }
        
        // 验证计算一致性
        val expectedByteRate = sampleRate * channelCount * (bitsPerSample / 8)
        val expectedBlockAlign = channelCount * (bitsPerSample / 8)
        
        if (byteRate != expectedByteRate) {
            Log.w(TAG, "字节率不匹配: 期望$expectedByteRate，实际$byteRate")
        }
        
        if (blockAlign != expectedBlockAlign) {
            Log.w(TAG, "块对齐不匹配: 期望$expectedBlockAlign，实际$blockAlign")
        }
        
        // 特殊格式信息
        if (channelCount >= 10) {
            Log.i(TAG, "3D音频格式: $channelDescription, 声道布局: $channelLayout")
        }
        
        Log.d(TAG, "音频参数验证完成: ${sampleRate}Hz, $channelDescription, ${bitsPerSample}bit")
        return true
    }

    // 辅助方法
    private fun isValidRiffHeader(header: ByteArray): Boolean {
        return header[RIFF_OFFSET].toInt().toChar() == 'R' &&
               header[RIFF_OFFSET + 1].toInt().toChar() == 'I' &&
               header[RIFF_OFFSET + 2].toInt().toChar() == 'F' &&
               header[RIFF_OFFSET + 3].toInt().toChar() == 'F'
    }

    private fun readLittleEndianInt(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or
               ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
               ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
               ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun readLittleEndianShort(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or
               ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }
}