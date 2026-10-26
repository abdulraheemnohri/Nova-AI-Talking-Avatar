package dev.aitalkingavatar.avatar

import java.io.File
import java.io.RandomAccessFile

data class AssetValidationReport(
    val isValid: Boolean,
    val fileSize: Long,
    val message: String
)

/**
 * Performs a lightweight GLB 2.0 container check before Filament attempts to load it.
 * This does not replace Khronos glTF Validator or a runtime rendering test.
 */
object AvatarAssetValidator {
    private const val GLB_HEADER_SIZE = 12L
    private const val GLB_MAGIC = 0x46546C67L // ASCII "glTF" interpreted little-endian
    private const val GLB_VERSION_2 = 2L

    fun validateGlbFile(file: File?): AssetValidationReport {
        if (file == null || !file.isFile) {
            return AssetValidationReport(false, 0L, "Asset file does not exist.")
        }
        val size = file.length()
        if (!file.canRead()) {
            return AssetValidationReport(false, size, "Asset file is not readable.")
        }
        if (size < GLB_HEADER_SIZE) {
            return AssetValidationReport(false, size, "File is shorter than the 12-byte GLB header.")
        }

        return try {
            RandomAccessFile(file, "r").use { input ->
                val magic = readUInt32LE(input)
                val version = readUInt32LE(input)
                val declaredLength = readUInt32LE(input)
                when {
                    magic != GLB_MAGIC ->
                        AssetValidationReport(false, size, "Invalid GLB magic; expected glTF.")
                    version != GLB_VERSION_2 ->
                        AssetValidationReport(false, size, "Unsupported GLB version $version; expected 2.")
                    declaredLength != size ->
                        AssetValidationReport(false, size, "Declared GLB length ($declaredLength) does not match file size ($size).")
                    else ->
                        AssetValidationReport(true, size, "GLB 2.0 header and declared file length are valid.")
                }
            }
        } catch (e: Exception) {
            AssetValidationReport(false, size, "Could not read GLB header: ${e.message ?: "unknown error"}.")
        }
    }

    private fun readUInt32LE(input: RandomAccessFile): Long {
        val b0 = input.readUnsignedByte().toLong()
        val b1 = input.readUnsignedByte().toLong()
        val b2 = input.readUnsignedByte().toLong()
        val b3 = input.readUnsignedByte().toLong()
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }
}
