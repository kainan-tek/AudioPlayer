package com.example.audioplayer.util

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * WAV file reader for audio playback
 * Handles WAV file parsing and audio data reading with comprehensive validation
 */
class WavFile(private val filePath: String) {

    companion object {
        private const val TAG = "WavFile"
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

    // Audio properties
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
            1 -> "Mono"
            2 -> "Stereo"
            4 -> "Quad"
            6 -> "5.1 Surround"
            8 -> "7.1 Surround"
            10 -> "5.1.4 Surround"
            12 -> "7.1.4 Surround"
            else -> "$channelCount channels (playback as stereo)"
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
            else -> "$channelCount channels → Stereo (L R)"
        }

    /**
     * Open WAV file and parse header information
     */
    fun open(): Boolean {
        Log.d(TAG, "Opening WAV file: $filePath")

        try {
            close() // Ensure previous resources are released

            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: $filePath")
                return false
            }

            if (file.length() < WAV_HEADER_SIZE) {
                Log.e(TAG, "File too small, not a valid WAV file: ${file.length()} bytes")
                return false
            }

            fileInputStream = FileInputStream(file)

            // Read WAV header
            val header = ByteArray(WAV_HEADER_SIZE)
            val bytesRead = fileInputStream!!.read(header)

            if (bytesRead != WAV_HEADER_SIZE) {
                Log.e(TAG, "Cannot read complete WAV header")
                return false
            }

            // Validate RIFF identifier
            if (!isValidRiffHeader(header)) {
                Log.e(TAG, "Not a valid WAV file format")
                return false
            }

            // Parse audio parameters
            sampleRate = readLittleEndianInt(header, SAMPLE_RATE_OFFSET)
            channelCount = readLittleEndianShort(header, CHANNEL_COUNT_OFFSET)
            bitsPerSample = readLittleEndianShort(header, BITS_PER_SAMPLE_OFFSET)
            dataSize = readLittleEndianInt(header, DATA_SIZE_OFFSET)
            byteRate = readLittleEndianInt(header, BYTE_RATE_OFFSET)
            blockAlign = readLittleEndianShort(header, BLOCK_ALIGN_OFFSET)

            // Validate parameter validity and consistency
            if (!validateAudioParameters()) {
                return false
            }

            isFileOpen = true
            Log.i(
                TAG,
                "WAV file opened successfully: ${sampleRate}Hz, $channelDescription, ${bitsPerSample}bit, duration ${
                    String.format(
                        java.util.Locale.US, "%.2f", duration
                    )
                }s"
            )
            return true

        } catch (_: SecurityException) {
            Log.e(TAG, "Permission denied when opening file: $filePath")
            close()
            return false
        } catch (e: IOException) {
            Log.e(TAG, "Failed to open file: $filePath", e)
            close()
            return false
        }
    }

    /**
     * Read audio data with improved error handling
     */
    fun readData(buffer: ByteArray, offset: Int, length: Int): Int {
        if (!isFileOpen || fileInputStream == null) {
            Log.w(TAG, "File not open for reading")
            return -1
        }

        if (offset < 0 || length < 0 || offset + length > buffer.size) {
            Log.w(
                TAG,
                "Invalid read parameters: offset=$offset, length=$length, bufferSize=${buffer.size}"
            )
            return -1
        }

        return try {
            fileInputStream!!.read(buffer, offset, length)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read data", e)
            close()
            -1
        }
    }

    /**
     * Close file
     */
    fun close() {
        if (isFileOpen || fileInputStream != null) {
            Log.d(TAG, "Closing WAV file")
            try {
                fileInputStream?.close()
            } catch (_: IOException) {
                Log.w(TAG, "Error closing file")
            } finally {
                fileInputStream = null
                isFileOpen = false
            }
        }
    }

    /**
     * Check if file is valid
     */
    fun isValid(): Boolean {
        return isFileOpen && sampleRate > 0 && channelCount > 0 && bitsPerSample > 0
    }

    override fun toString(): String {
        return "WavFile(path='$filePath', ${sampleRate}Hz, $channelDescription, ${bitsPerSample}bit, ${
            String.format(
                java.util.Locale.US, "%.2f", duration
            )
        }s)"
    }

    /**
     * Validate audio parameter validity and consistency
     */
    private fun validateAudioParameters(): Boolean {
        // Basic parameter check
        if (sampleRate <= 0 || channelCount <= 0 || bitsPerSample <= 0) {
            Log.e(
                TAG,
                "Invalid audio parameters: ${sampleRate}Hz, ${channelCount}ch, ${bitsPerSample}bit"
            )
            return false
        }

        // Check parameter ranges
        if (sampleRate !in 8000..192000) {
            Log.w(TAG, "Sample rate outside common range: ${sampleRate}Hz")
        }

        if (channelCount > 16) {
            Log.w(TAG, "Channel count exceeds supported range: $channelCount channels")
            return false
        } else if (channelCount > 12) {
            Log.w(
                TAG,
                "High channel count: $channelCount channels, may not be supported by all devices"
            )
        } else if (channelCount == 12) {
            Log.i(TAG, "Detected 7.1.4 audio format (12 channels)")
        }

        if (bitsPerSample !in listOf(8, 16, 24, 32)) {
            Log.w(TAG, "Uncommon bit depth: ${bitsPerSample}bit")
        }

        // Validate calculation consistency
        val expectedByteRate = sampleRate * channelCount * (bitsPerSample / 8)
        val expectedBlockAlign = channelCount * (bitsPerSample / 8)

        if (byteRate != expectedByteRate || blockAlign != expectedBlockAlign) {
            Log.w(
                TAG,
                "Header mismatch - ByteRate: expected=$expectedByteRate, actual=$byteRate; BlockAlign: expected=$expectedBlockAlign, actual=$blockAlign"
            )
        }

        return true
    }

    // Helper methods - consistent with AudioRecorder implementation
    private fun isValidRiffHeader(header: ByteArray): Boolean {
        return header[RIFF_OFFSET].toInt().toChar() == 'R' && header[RIFF_OFFSET + 1].toInt()
            .toChar() == 'I' && header[RIFF_OFFSET + 2].toInt()
            .toChar() == 'F' && header[RIFF_OFFSET + 3].toInt().toChar() == 'F'
    }

    private fun readLittleEndianInt(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8) or ((bytes[offset + 2].toInt() and 0xFF) shl 16) or ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun readLittleEndianShort(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }
}