package dev.aitalkingavatar.data

import android.content.Context
import android.net.Uri
import java.io.File

private const val MAX_GLB_BYTES = 150L * 1024L * 1024L

fun importCharacter(context: Context, uri: Uri): File {
    val directory = File(context.filesDir, "characters").apply { mkdirs() }
    val destination = File(directory, "current.glb")
    val temporary = File.createTempFile("character-", ".glb", directory)
    try {
        val input = context.contentResolver.openInputStream(uri) ?: error("Selected character file could not be opened.")
        input.use { source ->
            temporary.outputStream().use { target ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count <= 0) break
                    total += count
                    require(total <= MAX_GLB_BYTES) { "The GLB exceeds the 150 MB mobile asset limit. Choose a smaller optimized model." }
                    target.write(buffer, 0, count)
                }
            }
        }
        require(temporary.length() > 20) { "The selected file is empty or too small to be a GLB." }
        val header = ByteArray(4)
        temporary.inputStream().use { stream ->
            var offset = 0
            while (offset < header.size) {
                val count = stream.read(header, offset, header.size - offset)
                if (count <= 0) break
                offset += count
            }
            require(offset == header.size && header.contentEquals(byteArrayOf(0x67, 0x6c, 0x54, 0x46))) {
                "The selected file is not a glTF binary (.glb)."
            }
        }
        val backup = File(directory, "previous.glb")
        backup.delete()
        val hadPrevious = destination.exists()
        if (hadPrevious && !destination.renameTo(backup)) error("Could not safely replace the current character file.")
        if (!temporary.renameTo(destination)) {
            if (hadPrevious) backup.renameTo(destination)
            error("Could not save the new character file.")
        }
        backup.delete()
        return destination
    } finally {
        temporary.delete()
    }
}
