package com.example.audioplayer.model

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

private const val LOG_TAG = "WaveFile"
// WAV file constants
private const val MIN_WAV_HEADER_SIZE = 44
private const val RIFF_HEADER_OFFSET = 0
private const val CHANNEL_COUNT_OFFSET = 22
private const val SAMPLE_RATE_OFFSET = 24
private const val BITS_PER_SAMPLE_OFFSET = 34
private const val DATA_SIZE_OFFSET = 40

class WaveFile(waveFilePath: String) {
    private var file: File? = null
    private var fileInputStream: FileInputStream? = null
    private var isOpen = false
    
    // Audio properties
    val filePath: String = waveFilePath
    var fileSize: Long = 0
        private set
    var sampleRate: Int = 0
        private set
    var channelCount: Int = 0
        private set
    var bitsPerSample: Int = 0
        private set
    var dataSize: Int = 0
        private set
    
    // Calculated properties
    val duration: Float get() = dataSize.toFloat() / (sampleRate * channelCount * (bitsPerSample / 8))
    private val isFileValid: Boolean get() = file != null && file!!.exists() && fileSize > MIN_WAV_HEADER_SIZE
    private val areAudioParamsValid: Boolean get() = sampleRate > 0 && channelCount > 0 && bitsPerSample > 0

    /**
     * Opens the WAV file, reads and validates its header
     * @return true if the file was successfully opened and is a valid WAV file
     */
    fun open(): Boolean {
        Log.d(LOG_TAG, "Opening WAV file: $filePath")
        return try {
            // Reset state
            close()
            
            // Initialize file object
            file = File(filePath)
            
            // Create input stream
            try {
                fileInputStream = FileInputStream(file)
                Log.d(LOG_TAG, "FileInputStream created successfully")
            } catch (e: FileNotFoundException) {
                Log.e(LOG_TAG, "File not found: $filePath, ${e.message ?: "Unknown reason"}")
                return false
            } catch (e: SecurityException) {
                Log.e(LOG_TAG, "Security exception: $filePath, ${e.message ?: "Permission denied"}")
                return false
            }
            
            // Check file size
            fileSize = file?.length() ?: 0
            Log.d(LOG_TAG, "File size: $fileSize bytes")
            if (fileSize < MIN_WAV_HEADER_SIZE) {
                Log.e(LOG_TAG, "Invalid file size: $filePath, size: $fileSize bytes (minimum 44 bytes required)")
                return false
            }
            
            // Read and parse WAV header
            val header = ByteArray(MIN_WAV_HEADER_SIZE)
            val bytesRead = fileInputStream?.read(header) ?: -1
            if (bytesRead != MIN_WAV_HEADER_SIZE) {
                Log.e(LOG_TAG, "Failed to read WAV header: read $bytesRead of $MIN_WAV_HEADER_SIZE bytes")
                return false
            }
            
            // Validate RIFF header
            if (!isValidRiffHeader(header)) {
                Log.e(LOG_TAG, "Invalid WAV file format: Missing RIFF header in $filePath")
                return false
            }
            
            // Parse audio parameters
            sampleRate = readIntFromLittleEndian(header, SAMPLE_RATE_OFFSET)
            channelCount = readIntFromLittleEndian(header, CHANNEL_COUNT_OFFSET, 2)
            bitsPerSample = readIntFromLittleEndian(header, BITS_PER_SAMPLE_OFFSET, 2)
            dataSize = readIntFromLittleEndian(header, DATA_SIZE_OFFSET)
            
            // Validate audio parameters
            if (!areAudioParamsValid) {
                Log.e(LOG_TAG, "Invalid audio parameters: sampleRate=$sampleRate, channels=$channelCount, bits=$bitsPerSample")
                return false
            }
            
            isOpen = true
            Log.i(LOG_TAG, "Successfully opened WAV file: $filePath - " +
                    "${sampleRate}Hz, ${bitsPerSample}bit, ${channelCount}ch, " +
                    "duration=${duration}s, dataSize=${dataSize} bytes")
            true
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error opening file: $filePath", e)
            close()
            false
        }
    }

    /**
     * Closes the WAV file and releases resources
     */
    fun close() {
        if (isOpen || fileInputStream != null) {
            Log.d(LOG_TAG, "Closing WAV file: $filePath")
            try {
                fileInputStream?.close()
            } catch (e: IOException) {
                Log.w(LOG_TAG, "Error closing file: $filePath", e)
            } finally {
                fileInputStream = null
                isOpen = false
                Log.d(LOG_TAG, "WAV file resources released")
            }
        }
    }

    /**
     * Reads audio data from the file into the provided buffer
     * @param buffer The buffer to read data into
     * @param offset The starting offset in the buffer
     * @param size The number of bytes to read
     * @return The number of bytes read, or -1 if there was an error
     */
    fun readData(buffer: ByteArray, offset: Int, size: Int): Int {
        // Validate state and parameters
        if (!isOpen || fileInputStream == null) {
            Log.w(LOG_TAG, "Attempting to read from closed or invalid file: $filePath")
            return -1
        }
        
        if (offset < 0 || size < 0 || offset + size > buffer.size) {
            Log.w(LOG_TAG, "Invalid read parameters: offset=$offset, size=$size, bufferSize=${buffer.size}")
            return -1
        }
        
        // Perform read operation
        return try {
            val bytesRead = fileInputStream!!.read(buffer, offset, size)
            // Log.d(LOG_TAG, "Read $bytesRead bytes from $filePath")
            bytesRead
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Error reading from file: $filePath", e)
            close()
            -1
        }
    }

    /**
     * Checks if the WAV file is valid
     * @return true if the file is valid
     */
    fun isValid(): Boolean {
        return try {
            val isValid = isFileValid && areAudioParamsValid
            Log.d(LOG_TAG, "File validation for $filePath: $isValid")
            
            if (!isValid) {
                Log.d(LOG_TAG, "Invalid reason: fileExists=${file?.exists()}, " +
                        "fileSize=${fileSize} (>${MIN_WAV_HEADER_SIZE}: ${fileSize > MIN_WAV_HEADER_SIZE}), " +
                        "sampleRate=${sampleRate} (>0: ${sampleRate > 0}), " +
                        "channelCount=${channelCount} (>0: ${channelCount > 0}), " +
                        "bitsPerSample=${bitsPerSample} (>0: ${bitsPerSample > 0})")
            }
            isValid
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Exception during file validation: $filePath", e)
            false
        }
    }

    /**
     * Returns a string representation of the WaveFile
     */
    override fun toString(): String {
        return "WaveFile(path='$filePath', size=$fileSize, " +
               "sampleRate=$sampleRate, channels=$channelCount, " +
               "bitsPerSample=$bitsPerSample, duration=${duration}s, dataSize=$dataSize)"
    }
    
    // Helper methods
    private fun isValidRiffHeader(header: ByteArray): Boolean {
        return header[RIFF_HEADER_OFFSET + 0].toInt().toChar() == 'R' && 
               header[RIFF_HEADER_OFFSET + 1].toInt().toChar() == 'I' &&
               header[RIFF_HEADER_OFFSET + 2].toInt().toChar() == 'F' && 
               header[RIFF_HEADER_OFFSET + 3].toInt().toChar() == 'F'
    }
    
    private fun readIntFromLittleEndian(bytes: ByteArray, offset: Int, length: Int = 4): Int {
        var result = 0
        for (i in 0 until length) {
            if (offset + i < bytes.size) {
                result = result or ((bytes[offset + i].toInt() and 0xFF) shl (i * 8))
            }
        }
        return result
    }
}