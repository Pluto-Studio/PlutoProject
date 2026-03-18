package plutoproject.feature.gallery.adapter.paper

import org.bukkit.Chunk
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.nio.ByteBuffer
import java.util.UUID

class PaperChunkDisplayIndexStorage(
    plugin: JavaPlugin,
) {
    private val key = NamespacedKey(plugin, CHUNK_DISPLAY_INDEX_KEY)

    fun readDisplayInstanceIds(chunk: Chunk): List<UUID> {
        val bytes = chunk.persistentDataContainer.get(key, PersistentDataType.BYTE_ARRAY)
            ?: return emptyList()
        return decodeUuids(bytes)
    }

    fun writeDisplayInstanceIds(chunk: Chunk, displayInstanceIds: Collection<UUID>) {
        if (displayInstanceIds.isEmpty()) {
            chunk.persistentDataContainer.remove(key)
            return
        }

        chunk.persistentDataContainer.set(
            key,
            PersistentDataType.BYTE_ARRAY,
            encodeUuids(displayInstanceIds),
        )
    }

    companion object {
        private const val CHUNK_DISPLAY_INDEX_KEY = "display_instance_ids"

        internal fun encodeUuids(displayInstanceIds: Collection<UUID>): ByteArray {
            if (displayInstanceIds.isEmpty()) {
                return ByteArray(0)
            }

            val buffer = ByteBuffer.allocate(displayInstanceIds.size * UUID_BYTES)
            displayInstanceIds.forEach { id ->
                buffer.putLong(id.mostSignificantBits)
                buffer.putLong(id.leastSignificantBits)
            }
            return buffer.array()
        }

        internal fun decodeUuids(bytes: ByteArray): List<UUID> {
            require(bytes.size % UUID_BYTES == 0) {
                "Invalid chunk display index payload length: ${bytes.size}"
            }

            if (bytes.isEmpty()) {
                return emptyList()
            }

            val buffer = ByteBuffer.wrap(bytes)
            val count = bytes.size / UUID_BYTES
            return List(count) {
                UUID(buffer.long, buffer.long)
            }
        }

        private const val UUID_BYTES = 16
    }
}
