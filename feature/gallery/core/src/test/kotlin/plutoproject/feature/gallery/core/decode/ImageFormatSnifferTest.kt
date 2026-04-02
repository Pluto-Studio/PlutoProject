package plutoproject.feature.gallery.core.decode

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ImageFormatSnifferTest {
    @Test
    fun `should detect png by magic bytes`() {
        val bytes = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A,
        )

        val format = ImageFormatSniffer.sniff(bytes, fileNameHint = null)

        assertEquals(DecodableImageFormat.PNG, format)
    }

    @Test
    fun `should detect jpeg by magic bytes`() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

        val format = ImageFormatSniffer.sniff(bytes, fileNameHint = null)

        assertEquals(DecodableImageFormat.JPEG, format)
    }

    @Test
    fun `should detect gif by magic bytes`() {
        val bytes = "GIF89a".encodeToByteArray()

        val format = ImageFormatSniffer.sniff(bytes, fileNameHint = null)

        assertEquals(DecodableImageFormat.GIF, format)
    }

    @Test
    fun `should detect webp by riff and webp signature`() {
        val bytes = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            0, 0, 0, 0,
            'W'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), 'P'.code.toByte(),
        )

        val format = ImageFormatSniffer.sniff(bytes, fileNameHint = null)

        assertEquals(DecodableImageFormat.WEBP, format)
    }

    @Test
    fun `should fallback to jpg file extension when magic bytes are unknown`() {
        val format = ImageFormatSniffer.sniff(byteArrayOf(1, 2, 3), fileNameHint = "poster.jpg")

        assertEquals(DecodableImageFormat.JPEG, format)
    }

    @Test
    fun `should fallback to jpeg file extension when magic bytes are unknown`() {
        val format = ImageFormatSniffer.sniff(byteArrayOf(1, 2, 3), fileNameHint = "poster.jpeg")

        assertEquals(DecodableImageFormat.JPEG, format)
    }

    @Test
    fun `should prefer magic bytes over file extension`() {
        val pngBytes = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A,
        )

        val format = ImageFormatSniffer.sniff(pngBytes, fileNameHint = "wrong.gif")

        assertEquals(DecodableImageFormat.PNG, format)
    }

    @Test
    fun `should return null when no format can be detected`() {
        val format = ImageFormatSniffer.sniff(byteArrayOf(1, 2, 3), fileNameHint = "unknown.bin")

        assertNull(format)
    }
}
