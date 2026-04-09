package plutoproject.feature.gallery.adapter.paper

import org.bukkit.NamespacedKey
import org.bukkit.entity.ItemFrame
import org.bukkit.persistence.PersistentDataType
import plutoproject.feature.gallery.core.display.DisplayInstance
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

private val IMAGE_ITEM_FRAME_DATA_KEY = NamespacedKey("plutoproject_gallery", "image_item_frame_data")
private const val DATA_VERSION = 1

class ImageItemFrameData(
    val imageId: UUID, // 16 bytes
    val displayInstanceId: UUID, // 16 bytes
    val tileId: Int, // 4 bytes
) {
    companion object {
        private const val SIZE = 4 + 16 + 16 + 4

        fun fromBytes(bytes: ByteArray): ImageItemFrameData {
            require(bytes.size == SIZE) {
                "Corrupted image item frame data: expected size to be $SIZE, got ${bytes.size}"
            }

            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

            val version = buffer.int
            val imageId = UUID(buffer.long, buffer.long)
            val displayInstanceId = UUID(buffer.long, buffer.long)
            val tileId = buffer.int

            require(version == DATA_VERSION) {
                "Image item frame version mismatch: expected $DATA_VERSION, got $version"
            }
            require(tileId >= 0) {
                "Corrupted image item frame data: tileId must be >= 0, got $tileId"
            }

            return ImageItemFrameData(imageId, displayInstanceId, tileId)
        }
    }

    init {
        require(tileId >= 0) { "tileId must be >= 0, got $tileId" }
    }

    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(SIZE).order(ByteOrder.BIG_ENDIAN)

        buffer.putInt(DATA_VERSION)
        buffer.putLong(imageId.mostSignificantBits)
        buffer.putLong(imageId.leastSignificantBits)
        buffer.putLong(displayInstanceId.mostSignificantBits)
        buffer.putLong(displayInstanceId.leastSignificantBits)
        buffer.putInt(tileId)

        return buffer.array()
    }
}

fun ItemFrame.imageItemFrameData(): ImageItemFrameData? {
    val bytes = persistentDataContainer.get(
        IMAGE_ITEM_FRAME_DATA_KEY,
        PersistentDataType.BYTE_ARRAY
    ) ?: return null
    return ImageItemFrameData.fromBytes(bytes)
}

fun ItemFrame.setImageItemFrame(displayInstance: DisplayInstance, tileId: Int) {
    val data = ImageItemFrameData(
        imageId = displayInstance.imageId,
        displayInstanceId = displayInstance.id,
        tileId = tileId
    )
    persistentDataContainer.set(
        IMAGE_ITEM_FRAME_DATA_KEY,
        PersistentDataType.BYTE_ARRAY,
        data.toByteArray()
    )
}
