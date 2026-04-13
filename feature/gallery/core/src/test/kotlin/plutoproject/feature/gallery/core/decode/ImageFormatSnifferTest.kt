package plutoproject.feature.gallery.core.decode

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ImageFormatSnifferTest {
    @Test
    fun `should detect png by magic bytes`() = runTest {
        val format = withTempImageFile(byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A,
        )) { path ->
            ImageFormatSniffer.sniff(path)
        }

        assertEquals(SupportedImageFormat.Png, format)
    }

    @Test
    fun `should detect jpeg by magic bytes`() = runTest {
        val format = withTempImageFile(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())) { path ->
            ImageFormatSniffer.sniff(path)
        }

        assertEquals(SupportedImageFormat.Jpeg, format)
    }

    @Test
    fun `should detect gif by magic bytes`() = runTest {
        val format = withTempImageFile("GIF89a".encodeToByteArray()) { path ->
            ImageFormatSniffer.sniff(path)
        }

        assertEquals(SupportedImageFormat.Gif, format)
    }

    @Test
    fun `should detect webp by riff and webp signature`() = runTest {
        val format = withTempImageFile(byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            0, 0, 0, 0,
            'W'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), 'P'.code.toByte(),
        )) { path ->
            ImageFormatSniffer.sniff(path)
        }

        assertEquals(SupportedImageFormat.Webp, format)
    }

    @Test
    fun `should detect png when enough header bytes are available in file`() = runTest {
        val format = withTempImageFile(byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A,
        )) { path ->
            ImageFormatSniffer.sniff(path)
        }

        assertEquals(SupportedImageFormat.Png, format)
    }

    @Test
    fun `should return null when no format can be detected`() = runTest {
        val format = withTempImageFile(byteArrayOf(1, 2, 3)) { path ->
            ImageFormatSniffer.sniff(path)
        }

        assertNull(format)
    }
}
