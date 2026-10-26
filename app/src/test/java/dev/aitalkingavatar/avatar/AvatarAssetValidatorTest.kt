package dev.aitalkingavatar.avatar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

class AvatarAssetValidatorTest {
    private fun tempGlb(bytes: ByteArray): File {
        val file = File.createTempFile("nova-avatar-test", ".glb")
        file.writeBytes(bytes)
        file.deleteOnExit()
        return file
    }

    private fun header(magic: Long, version: Long, length: Long): ByteArray {
        val bytes = ByteArray(12)
        fun writeUInt32LE(offset: Int, value: Long) {
            for (i in 0..3) bytes[offset + i] = ((value ushr (8 * i)) and 0xff).toByte()
        }
        writeUInt32LE(0, magic)
        writeUInt32LE(4, version)
        writeUInt32LE(8, length)
        return bytes
    }

    @Test fun acceptsGlb2HeaderWithMatchingLength() {
        val report = AvatarAssetValidator.validateGlbFile(tempGlb(header(0x46546C67L, 2L, 12L)))
        assertTrue(report.isValid)
        assertEquals(12L, report.fileSize)
    }

    @Test fun rejectsWrongMagic() {
        val report = AvatarAssetValidator.validateGlbFile(tempGlb(header(0L, 2L, 12L)))
        assertFalse(report.isValid)
        assertTrue(report.message.contains("magic", ignoreCase = true))
    }

    @Test fun rejectsUnsupportedVersion() {
        val report = AvatarAssetValidator.validateGlbFile(tempGlb(header(0x46546C67L, 1L, 12L)))
        assertFalse(report.isValid)
        assertTrue(report.message.contains("version", ignoreCase = true))
    }

    @Test fun rejectsDeclaredLengthMismatch() {
        val report = AvatarAssetValidator.validateGlbFile(tempGlb(header(0x46546C67L, 2L, 16L)))
        assertFalse(report.isValid)
        assertTrue(report.message.contains("length", ignoreCase = true))
    }

    @Test fun rejectsTruncatedHeader() {
        val report = AvatarAssetValidator.validateGlbFile(tempGlb(byteArrayOf(0x67, 0x6c, 0x54, 0x46)))
        assertFalse(report.isValid)
        assertEquals(4L, report.fileSize)
    }

    @Test fun rejectsNullFile() {
        val report = AvatarAssetValidator.validateGlbFile(null)
        assertFalse(report.isValid)
        assertEquals(0L, report.fileSize)
    }
}
