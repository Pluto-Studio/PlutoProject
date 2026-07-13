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
    val originItemFrame: UUID, // 16 bytes
    val nextItemFrame: UUID?, // 16 bytes
) {
    companion object {
        private const val SIZE_WITHOUT_NEXT = 4 + 16 + 16 + 16
        private const val SIZE_WITH_NEXT = SIZE_WITHOUT_NEXT + 16

        fun fromBytes(bytes: ByteArray): ImageItemFrameData {
            require(bytes.size == SIZE_WITHOUT_NEXT || bytes.size == SIZE_WITH_NEXT) {
                "Corrupted image item frame data: expected size to be $SIZE_WITHOUT_NEXT or $SIZE_WITH_NEXT, got ${bytes.size}"
            }

            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

            val version = buffer.int
            val imageId = UUID(buffer.long, buffer.long)
            val displayInstanceId = UUID(buffer.long, buffer.long)
            val originItemFrame = UUID(buffer.long, buffer.long)
            val nextItemFrame = if (buffer.remaining() >= 16) {
                UUID(buffer.long, buffer.long)
            } else {
                null
            }

            require(version == DATA_VERSION) {
                "Image item frame version mismatch: expected $DATA_VERSION, got $version"
            }

            return ImageItemFrameData(imageId, displayInstanceId, originItemFrame, nextItemFrame)
        }
    }

    fun toByteArray(): ByteArray {
        val size = if (nextItemFrame != null) SIZE_WITH_NEXT else SIZE_WITHOUT_NEXT
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN)

        buffer.putInt(DATA_VERSION)
        buffer.putLong(imageId.mostSignificantBits)
        buffer.putLong(imageId.leastSignificantBits)
        buffer.putLong(displayInstanceId.mostSignificantBits)
        buffer.putLong(displayInstanceId.leastSignificantBits)
        buffer.putLong(originItemFrame.mostSignificantBits)
        buffer.putLong(originItemFrame.leastSignificantBits)

        if (nextItemFrame != null) {
            buffer.putLong(nextItemFrame.mostSignificantBits)
            buffer.putLong(nextItemFrame.leastSignificantBits)
        }

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

fun ItemFrame.setImageItemFrame(displayInstance: DisplayInstance, nextItemFrame: UUID?) {
    val data = ImageItemFrameData(
        imageId = displayInstance.imageId,
        displayInstanceId = displayInstance.id,
        originItemFrame = displayInstance.itemFrameIds.first(),
        nextItemFrame = nextItemFrame
    )
    persistentDataContainer.set(
        IMAGE_ITEM_FRAME_DATA_KEY,
        PersistentDataType.BYTE_ARRAY,
        data.toByteArray()
    )
}

fun ItemFrame.unsetImageItemFrame() {
    persistentDataContainer.remove(IMAGE_ITEM_FRAME_DATA_KEY)
}
