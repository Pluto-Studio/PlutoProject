package plutoproject.feature.gallery.core.decode

import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStreamSpi
import java.nio.file.Path
import javax.imageio.spi.ImageInputStreamSpi
import javax.imageio.spi.ImageReaderSpi
import kotlin.io.path.inputStream

class A {

}

sealed interface SupportedImageFormat {
    val inputStreamSpi: ImageInputStreamSpi?
    val readerSpi: ImageReaderSpi?

    companion object {
        val SUPPORTED_FORMAT_NAMES = listOf("PNG", "JPEG", "WebP", "GIF")
        val SUPPORTED_FILE_EXTENSIONS = listOf("png", "jpg", "jpeg", "webp", "gif")
        val SUPPORTED_MIME_TYPES = listOf(
            ContentType("png"),
            ContentType("jpeg"),
            ContentType("webp"),
            ContentType("gif"),
        )
    }

    data object Png : SupportedImageFormat {
        override val inputStreamSpi: ImageInputStreamSpi? = null
        override val readerSpi: ImageReaderSpi? = null
    }

    data object Jpeg : SupportedImageFormat {
        override val inputStreamSpi: ImageInputStreamSpi? = null
        override val readerSpi: ImageReaderSpi? = null
    }

    data object Webp : SupportedImageFormat {
        override val inputStreamSpi: ImageInputStreamSpi = ByteArrayImageInputStreamSpi()
        override val readerSpi: ImageReaderSpi = WebPImageReaderSpi()
    }

    data object Gif : SupportedImageFormat {
        override val inputStreamSpi: ImageInputStreamSpi? = null
        override val readerSpi: ImageReaderSpi? = null
    }
}

data class ContentType(
    val contentSubType: String
) {
    val contentType: String = "image"
}

private const val PNG_MAGIC_SIZE = 8
private const val JPEG_MAGIC_SIZE = 3
private const val WEBP_MAGIC_SIZE = 12
private const val GIF_MAGIC_SIZE = 6

private val SNIFFER_HEADER_SIZE = maxOf(PNG_MAGIC_SIZE, JPEG_MAGIC_SIZE, WEBP_MAGIC_SIZE, GIF_MAGIC_SIZE)


object ImageFormatSniffer {
    fun sniff(bytes: ByteArray): SupportedImageFormat? {
        return when {
            isPng(bytes) -> SupportedImageFormat.Png
            isJpeg(bytes) -> SupportedImageFormat.Jpeg
            isGif(bytes) -> SupportedImageFormat.Gif
            isWebp(bytes) -> SupportedImageFormat.Webp
            else -> null
        }
    }

    fun sniff(filePath: Path): SupportedImageFormat? {
        val header = readHeader(filePath) ?: return null
        return sniff(header)
    }
}

private fun readHeader(tempFile: Path): ByteArray? = runCatching {
    tempFile.inputStream().use { inputStream ->
        inputStream.readNBytes(SNIFFER_HEADER_SIZE)
    }
}.getOrNull()

private fun isPng(bytes: ByteArray): Boolean {
    if (bytes.size < PNG_MAGIC_SIZE) return false
    return bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte() &&
            bytes[4] == 0x0D.toByte() &&
            bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() &&
            bytes[7] == 0x0A.toByte()
}

private fun isJpeg(bytes: ByteArray): Boolean {
    if (bytes.size < JPEG_MAGIC_SIZE) return false
    return bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()
}

private fun isWebp(bytes: ByteArray): Boolean {
    if (bytes.size < WEBP_MAGIC_SIZE) return false
    return bytes[0] == 'R'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() &&
            bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() &&
            bytes[11] == 'P'.code.toByte()
}

private fun isGif(bytes: ByteArray): Boolean {
    if (bytes.size < GIF_MAGIC_SIZE) return false
    return (bytes[0] == 'G'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == '8'.code.toByte() &&
            (bytes[4] == '7'.code.toByte() || bytes[4] == '9'.code.toByte()) &&
            bytes[5] == 'a'.code.toByte())
}
