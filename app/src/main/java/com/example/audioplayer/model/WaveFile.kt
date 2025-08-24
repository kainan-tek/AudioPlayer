package com.example.audioplayer.model

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException

private const val LOG_TAG = "AudioPlayer"

class WaveFile(waveFilePath: String) {
    private var _file: File? = null
    private var _filePath: String = waveFilePath
    private var _fileSize: Long = 0
    private var _sampleRate: Int = 0
    private var _channels: Int = 0
    private var _bitsPerSample: Int = 0
    private var _dataSize: Int = 0
    private var isOpen: Boolean = false
    private var fileInputStream: FileInputStream? = null

    fun open(): Boolean {
        return try {
            _file = File(_filePath)
            if (!_file!!.exists()) {
                // throw IOException("File not found: $_filePath")
                Log.e(LOG_TAG, "File not found: $_filePath")
                return false
            }
            
            _fileSize = _file!!.length()
            fileInputStream = FileInputStream(_file)
            
            val header = ByteArray(44)
            fileInputStream!!.read(header)
            
            // 检查RIFF标识
            if (header[0].toInt().toChar() != 'R' || header[1].toInt().toChar() != 'I' ||
                header[2].toInt().toChar() != 'F' || header[3].toInt().toChar() != 'F') {
                Log.e(LOG_TAG, "Invalid WAV file format: $_filePath")
                // throw IOException("Invalid WAV file format")
                return false
            }
            
            // 获取采样率
            _sampleRate = ((header[24].toInt() and 0xFF) or 
                       ((header[25].toInt() and 0xFF) shl 8) or 
                       ((header[26].toInt() and 0xFF) shl 16) or 
                       ((header[27].toInt() and 0xFF) shl 24))
            
            // 获取声道数
            _channels = ((header[22].toInt() and 0xFF) or 
                      ((header[23].toInt() and 0xFF) shl 8))
            
            // 获取位深
            _bitsPerSample = ((header[34].toInt() and 0xFF) or 
                           ((header[35].toInt() and 0xFF) shl 8))
            
            // 获取数据大小
            _dataSize = ((header[40].toInt() and 0xFF) or 
                      ((header[41].toInt() and 0xFF) shl 8) or 
                      ((header[42].toInt() and 0xFF) shl 16) or 
                      ((header[43].toInt() and 0xFF) shl 24))
            
            isOpen = true
            true
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Failed to open file: $_filePath, ${e.message}")
            close()
            false
        }
    }

    fun close() {
        try {
            fileInputStream?.close()
            fileInputStream = null
            isOpen = false
        } catch (_: IOException) {
            // 静默处理关闭错误
        }
    }

    val filePath: String get() = _filePath
    val fileSize: Long get() = _fileSize
    val sampleRate: Int get() = _sampleRate
    val channelCount: Int get() = _channels
    val bitsPerSample: Int get() = _bitsPerSample
    val dataSize: Int get() = _dataSize
    val duration: Float get() = _dataSize.toFloat() / (_sampleRate * _channels * (_bitsPerSample / 8))
    
    fun readData(buffer: ByteArray, offset: Int, size: Int): Int {
        if (!isOpen || fileInputStream == null) {
            return -1
        }
        
        return try {
            fileInputStream!!.read(buffer, offset, size)
        } catch (_: IOException) {
            close()
            -1
        }
    }

    fun seek(position: Int): Boolean {
        if (!isOpen || fileInputStream == null) {
            return false
        }
        try {
            fileInputStream!!.skip(position.toLong())
        } catch (_: IOException) {
            close()
            return false
        }
        return true
    }

    fun isValid(): Boolean {
        return try {
            _file != null && _file!!.exists() && _fileSize > 44 &&
            _sampleRate > 0 && _channels > 0 && _bitsPerSample > 0
        } catch (_: Exception) {
            false
        }
    }

    override fun toString(): String {
        return "WaveFile(path='$_filePath', size=$_fileSize, " +
               "sampleRate=$_sampleRate, channels=$_channels, " +
               "bitsPerSample=$_bitsPerSample, duration=${duration}s, dataSize=$_dataSize)"
    }
}