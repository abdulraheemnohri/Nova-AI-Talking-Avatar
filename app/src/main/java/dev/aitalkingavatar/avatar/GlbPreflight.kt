package dev.aitalkingavatar.avatar

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Cheap GLB container preflight before handing user-selected files to Filament. */
object GlbPreflight {
    const val DEFAULT_MAX_BYTES: Long = 64L * 1024L * 1024L
    private const val GLB_MAGIC = 0x46546C67
    private const val JSON_CHUNK_TYPE = 0x4E4F534A

    data class Result(val valid: Boolean, val message: String)

    fun validate(file: File, maxBytes: Long = DEFAULT_MAX_BYTES): Result {
        if (!file.isFile || !file.canRead()) return Result(false, "Character file is not readable.")
        val length = file.length()
        if (maxBytes <= 0L || length > maxBytes) {
            return Result(false, "Character file exceeds the ${maxBytes / (1024 * 1024)} MiB import limit.")
        }
        if (length < 20L) return Result(false, "File is too short to contain a GLB header and JSON chunk.")

        return try {
            RandomAccessFile(file, "r").use { input ->
                val headerBytes = ByteArray(12)
                input.readFully(headerBytes)
                val header = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
                val magic = header.int
                val version = header.int
                val declaredLength = header.int.toLong() and 0xFFFFFFFFL

                if (magic != GLB_MAGIC) return Result(false, "File is not a binary glTF (.glb) file.")
                if (version != 2) return Result(false, "Unsupported GLB version $version; expected version 2.")
                if (declaredLength != length) return Result(false, "GLB header length does not match the file size.")

                val chunkHeader = ByteArray(8)
                input.readFully(chunkHeader)
                val chunk = ByteBuffer.wrap(chunkHeader).order(ByteOrder.LITTLE_ENDIAN)
                val jsonLength = chunk.int.toLong() and 0xFFFFFFFFL
                val chunkType = chunk.int
                if (chunkType != JSON_CHUNK_TYPE) return Result(false, "The first GLB chunk is not a JSON chunk.")
                if (jsonLength == 0L || jsonLength % 4L != 0L || jsonLength > length - 20L) {
                    return Result(false, "GLB JSON chunk length is invalid.")
                }
                Result(true, "GLB container header looks valid.")
            }
        } catch (_: Exception) {
            Result(false, "Could not read the GLB container header.")
        }
    }
}
