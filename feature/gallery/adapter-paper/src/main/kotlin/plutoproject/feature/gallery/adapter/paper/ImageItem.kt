package plutoproject.feature.gallery.adapter.paper

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import plutoproject.feature.gallery.core.image.Image
import plutoproject.framework.common.util.chat.component.replace
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

val IMAGE_ITEM_MATERIAL = Material.PAPER

private val IMAGE_ITEM_DATA_KEY = NamespacedKey("plutoproject_gallery", "image_item_data")
private const val DATA_VERSION = 1

class ImageItemData(
    val imageId: UUID, // 16 bytes
    val widthBlocks: Int, // 4 bytes
    val heightBlocks: Int, // 4 bytes
    val tileMapIds: IntArray, // 4*n bytes
) {
    companion object {
        private const val HEADER_SIZE = 4 + 16 + 4 + 4

        fun fromBytes(bytes: ByteArray): ImageItemData {
            require(bytes.size >= HEADER_SIZE) {
                "Corrupted image item data: expected at least $HEADER_SIZE bytes, got ${bytes.size}"
            }

            val mapIdsLength = bytes.size - HEADER_SIZE

            require(mapIdsLength % Int.SIZE_BYTES == 0) {
                "Corrupted image item data: tileMapIds length $mapIdsLength is not divisible by ${Int.SIZE_BYTES}"
            }

            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

            val version = buffer.int
            val imageId = UUID(buffer.long, buffer.long)
            val widthBlocks = buffer.int
            val heightBlocks = buffer.int

            require(version == DATA_VERSION) {
                "Image item data version mismatch: expected $DATA_VERSION, got $version"
            }
            require(widthBlocks > 0) {
                "Corrupted image item data: widthBlocks must be > 0, got $widthBlocks"
            }
            require(heightBlocks > 0) {
                "Corrupted image item data: heightBlocks must be > 0, got $heightBlocks"
            }

            val tileMapIds = IntArray(mapIdsLength / Int.SIZE_BYTES) { buffer.int }
            val expectedTileCount = Math.multiplyExact(widthBlocks, heightBlocks)

            require(tileMapIds.size == expectedTileCount) {
                "Corrupted image item data: expected tileMapIds.size to be $expectedTileCount, got ${tileMapIds.size}"
            }

            return ImageItemData(imageId, widthBlocks, heightBlocks, tileMapIds)
        }
    }

    init {
        require(widthBlocks > 0) { "widthBlocks must be > 0, got $widthBlocks" }
        require(heightBlocks > 0) { "heightBlocks must be > 0, got $heightBlocks" }
        require(widthBlocks.toLong() * heightBlocks.toLong() <= Int.MAX_VALUE) {
            "Map size overflow (> ${Int.MAX_VALUE})"
        }
        require(tileMapIds.size == widthBlocks * heightBlocks) {
            "tileMapIds size must be equal to widthBlocks * heightBlocks"
        }
    }

    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(HEADER_SIZE + Int.SIZE_BYTES * tileMapIds.size)
            .order(ByteOrder.BIG_ENDIAN)

        buffer.putInt(DATA_VERSION)
        buffer.putLong(imageId.mostSignificantBits)
        buffer.putLong(imageId.leastSignificantBits)
        buffer.putInt(widthBlocks)
        buffer.putInt(heightBlocks)

        for (id in tileMapIds) {
            buffer.putInt(id)
        }

        return buffer.array()
    }
}

@Suppress("UnstableApiUsage")
fun createImageItem(image: Image): ItemStack {
    val itemStack = ItemStack(Material.PAPER)

    itemStack.setData(DataComponentTypes.ITEM_NAME, IMAGE_ITEM_NAME.resolveImagePlaceholders(image))
    itemStack.setData(
        DataComponentTypes.LORE,
        ItemLore.lore(IMAGE_ITEM_LORE.map { it.resolveImagePlaceholders(image) })
    )
    itemStack.itemMeta = itemStack.itemMeta.apply { setImageItemData(image) } // getItemMeta 怎么拿的是 copy。。

    return itemStack
}

private fun Component.resolveImagePlaceholders(image: Image): Component {
    return replace("<name>", image.name)
        .replace("<creator>", image.ownerName)
        .replace("<time>", "TODO")
        .replace("<width>", image.widthBlocks)
        .replace("<height>", image.heightBlocks)
}

fun ItemStack.imageItemData(): ImageItemData? {
    val bytes = persistentDataContainer.get(
        IMAGE_ITEM_DATA_KEY,
        PersistentDataType.BYTE_ARRAY
    ) ?: return null
    return ImageItemData.fromBytes(bytes)
}

private fun ItemMeta.setImageItemData(image: Image) {
    val data = ImageItemData(
        imageId = image.id,
        widthBlocks = image.widthBlocks,
        heightBlocks = image.heightBlocks,
        tileMapIds = image.tileMapIds
    )
    persistentDataContainer.set(
        IMAGE_ITEM_DATA_KEY,
        PersistentDataType.BYTE_ARRAY,
        data.toByteArray()
    )
}
