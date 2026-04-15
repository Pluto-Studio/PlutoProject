package plutoproject.feature.gallery.infra.mongo

import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.render.tile.TilePool
import plutoproject.feature.gallery.core.render.tile.TilePoolSnapshot
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Duration.Companion.nanoseconds

internal object ImageDataBinaryCodec {
    fun encode(data: ImageData): ByteArray {
        val snapshot = data.tilePool.snapshot()
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { stream ->
            stream.writeByte(data.type.id)
            stream.writeInt(snapshot.offsets.size)
            snapshot.offsets.forEach(stream::writeInt)
            stream.writeInt(snapshot.blob.size)
            stream.write(snapshot.blob)
            stream.writeInt(data.tileIndexes.size)
            data.tileIndexes.forEach { stream.writeShort(it.toInt()) }

            when (data) {
                is ImageData.Static -> Unit
                is ImageData.Animated -> {
                    stream.writeInt(data.frameCount)
                    stream.writeLong(data.duration.inWholeNanoseconds)
                }
            }
        }
        return output.toByteArray()
    }

    fun decode(bytes: ByteArray): ImageData {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val type = buffer.readImageDataType()
        val offsets = IntArray(buffer.readElementCount("tilePool.offsets", Int.SIZE_BYTES)) { buffer.getInt() }
        val tilePoolBlob = ByteArray(buffer.readElementCount("tilePool.blob", 1))
        buffer.get(tilePoolBlob)
        val tileIndexes = ShortArray(buffer.readElementCount("tileIndexes", Short.SIZE_BYTES)) { buffer.getShort() }
        val tilePool = TilePool.fromSnapshot(TilePoolSnapshot(offsets, tilePoolBlob))

        val decoded = when (type) {
            EncodedImageDataType.STATIC -> ImageData.Static(tilePool = tilePool, tileIndexes = tileIndexes)
            EncodedImageDataType.ANIMATED -> ImageData.Animated(
                tilePool = tilePool,
                tileIndexes = tileIndexes,
                frameCount = buffer.readAnimatedFrameCount(),
                duration = buffer.readAnimatedDurationNanos().nanoseconds,
            )
        }

        require(!buffer.hasRemaining()) { "Unexpected trailing bytes in encoded ImageData" }
        return decoded
    }

    private fun ByteBuffer.readElementCount(field: String, bytesPerElement: Int): Int {
        val count = getInt()
        require(count >= 0) { "$field length must be >= 0, got $count" }

        val expectedBytes = count.toLong() * bytesPerElement
        require(expectedBytes <= remaining().toLong()) {
            "$field length exceeds remaining bytes: length=$count, bytesPerElement=$bytesPerElement, remaining=${remaining()}"
        }
        return count
    }

    private fun ByteBuffer.readImageDataType(): EncodedImageDataType {
        require(hasRemaining()) { "Encoded ImageData is empty" }
        return EncodedImageDataType.fromId(get().toInt())
    }

    private fun ByteBuffer.readAnimatedFrameCount(): Int {
        require(remaining() >= Int.SIZE_BYTES + Long.SIZE_BYTES) {
            "Encoded animated ImageData is truncated"
        }
        val frameCount = getInt()
        require(frameCount >= 0) { "Animated frameCount must be >= 0, got $frameCount" }
        return frameCount
    }

    private fun ByteBuffer.readAnimatedDurationNanos(): Long {
        return getLong()
    }

    private enum class EncodedImageDataType(val id: Int) {
        STATIC(0), ANIMATED(1);

        companion object {
            fun fromId(id: Int): EncodedImageDataType {
                return entries.firstOrNull { it.id == id }
                    ?: error("Unknown encoded ImageData type id=$id")
            }
        }
    }

    private val ImageType.id: Int
        get() = when (this) {
            ImageType.STATIC -> EncodedImageDataType.STATIC.id
            ImageType.ANIMATED -> EncodedImageDataType.ANIMATED.id
        }
}
