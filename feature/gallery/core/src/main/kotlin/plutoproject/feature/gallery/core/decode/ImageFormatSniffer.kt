package plutoproject.feature.gallery.core.decode

import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi
import javax.imageio.spi.IIORegistry
import javax.imageio.spi.ImageReaderSpi

private val webpReaderSpi = WebPImageReaderSpi()
private val pngReaderSpi: ImageReaderSpi by lazy { findReaderSpi("png") }
private val jpegReaderSpi: ImageReaderSpi by lazy { findReaderSpi("jpeg") }
private val gifReaderSpi: ImageReaderSpi by lazy { findReaderSpi("gif") }

private fun findReaderSpi(format: String): ImageReaderSpi {
    val providers = IIORegistry.getDefaultInstance()
        .getServiceProviders(ImageReaderSpi::class.java, true)

    return providers.asSequence().firstOrNull { spi ->
        spi.formatNames.any { it.equals(format, ignoreCase = true) }
    } ?: error("No ImageReaderSpi found for format: $format")
}

enum class DecodableImageFormat {
    PNG, JPEG, WEBP, GIF;

    val readerSpi: ImageReaderSpi
        get() = when (this) {
            PNG -> pngReaderSpi
            JPEG -> jpegReaderSpi
            WEBP -> webpReaderSpi
            GIF -> gifReaderSpi
        }
}

object ImageFormatSniffer {
    fun sniff(bytes: ByteArray, fileNameHint: String?): DecodableImageFormat? {
        sniffByMagic(bytes)?.let { return it }
        return sniffByFileName(fileNameHint)
    }
}


private fun sniffByMagic(bytes: ByteArray): DecodableImageFormat? {
    return when {
        isPng(bytes) -> DecodableImageFormat.PNG
        isJpeg(bytes) -> DecodableImageFormat.JPEG
        isGif(bytes) -> DecodableImageFormat.GIF
        isWebp(bytes) -> DecodableImageFormat.WEBP
        else -> null
    }
}

private fun sniffByFileName(fileNameHint: String?): DecodableImageFormat? {
    val extension = fileNameHint
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.lowercase()
        .orEmpty()

    return when (extension) {
        "png" -> DecodableImageFormat.PNG
        "jpg", "jpeg" -> DecodableImageFormat.JPEG
        "gif" -> DecodableImageFormat.GIF
        "webp" -> DecodableImageFormat.WEBP
        else -> null
    }
}

private fun isPng(bytes: ByteArray): Boolean {
    if (bytes.size < 8) return false
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
    if (bytes.size < 3) return false
    return bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()
}

private fun isGif(bytes: ByteArray): Boolean {
    if (bytes.size < 6) return false
    return (bytes[0] == 'G'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == '8'.code.toByte() &&
            (bytes[4] == '7'.code.toByte() || bytes[4] == '9'.code.toByte()) &&
            bytes[5] == 'a'.code.toByte())
}

private fun isWebp(bytes: ByteArray): Boolean {
    if (bytes.size < 12) return false
    return bytes[0] == 'R'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() &&
            bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() &&
            bytes[11] == 'P'.code.toByte()
}
