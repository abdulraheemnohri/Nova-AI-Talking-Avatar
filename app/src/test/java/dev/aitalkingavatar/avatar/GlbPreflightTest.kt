package dev.aitalkingavatar.avatar

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlbPreflightTest {
    private fun makeGlb(): ByteArray {
        val json = """{"asset":{"version":"2.0"},"meshes":[]}""".toByteArray(Charsets.UTF_8)
        val paddedSize = (json.size + 3) / 4 * 4
        val total = 12 + 8 + paddedSize
        val buffer = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(0x46546C67)
        buffer.putInt(2)
        buffer.putInt(total)
        buffer.putInt(paddedSize)
        buffer.putInt(0x4E4F534A)
        buffer.put(json)
        repeat(paddedSize - json.size) { buffer.put(0x20) }
        return buffer.array()
    }

    private fun withTempFile(bytes: ByteArray, block: (File) -> Unit) {
        val file = File.createTempFile("nova-preflight-", ".glb")
        try {
            file.writeBytes(bytes)
            block(file)
        } finally {
            file.delete()
        }
    }

    @Test fun acceptsWellFormedContainerHeader() = withTempFile(makeGlb()) {
        assertTrue(GlbPreflight.validate(it).valid)
    }

    @Test fun rejectsWrongMagic() = withTempFile(makeGlb().also {
        ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).putInt(0)
    }) {
        assertFalse(GlbPreflight.validate(it).valid)
    }

    @Test fun rejectsDeclaredLengthMismatch() = withTempFile(makeGlb().also {
        ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).putInt(8, it.size + 4)
    }) {
        assertFalse(GlbPreflight.validate(it).valid)
    }

    @Test fun rejectsFilesOverLimit() = withTempFile(makeGlb()) {
        assertFalse(GlbPreflight.validate(it, maxBytes = 10L).valid)
    }

    @Test fun rejectsTooShortFile() = withTempFile(byteArrayOf(0x67, 0x6C, 0x54, 0x46)) {
        assertFalse(GlbPreflight.validate(it).valid)
    }
}
