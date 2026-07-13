package plutoproject.feature.gallery.infra.mongo

import com.github.luben.zstd.Zstd
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.core.image.ImageDataRepository
import plutoproject.feature.gallery.infra.mongo.model.ImageDataChunkDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDataCompressionDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDataManifestDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDataManifestStateDocument
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

private const val MANIFEST_ID_FIELD = "_id"
private const val CURRENT_SCHEMA_VERSION = 1
private const val DEFAULT_MAX_CHUNK_BLOB_BYTES = 10 * 1024 * 1024
private val DEFAULT_COMPRESSION = ImageDataCompressionDocument.ZSTD

class MongoImageDataRepository(
    private val manifestCollection: MongoCollection<ImageDataManifestDocument>,
    private val chunkCollection: MongoCollection<ImageDataChunkDocument>,
    private val logger: Logger,
    private val maxChunkBlobBytes: Int = DEFAULT_MAX_CHUNK_BLOB_BYTES,
) : ImageDataRepository {
    private val manifestUpsert = ReplaceOptions().upsert(true)

    suspend fun ensureIndexes() {
        chunkCollection.createIndex(
            Indexes.ascending(ImageDataChunkDocument::imageId.name, ImageDataChunkDocument::order.name),
            IndexOptions().unique(true),
        )
    }

    override suspend fun findByImageId(imageId: UUID): ImageData? {
        val manifest = manifestCollection.find(eq(MANIFEST_ID_FIELD, imageId))
            .firstOrNull()
            ?: return null
        if (manifest.state != ImageDataManifestStateDocument.READY) {
            return null
        }

        val chunks = chunkCollection.find(eq(ImageDataChunkDocument::imageId.name, imageId))
            .sort(Sorts.ascending(ImageDataChunkDocument::order.name))
            .toList()
        return decodeReadyImageData(manifest, chunks)
    }

    override suspend fun findByImageIds(imageIds: Collection<UUID>): Map<UUID, ImageData> {
        if (imageIds.isEmpty()) {
            return emptyMap()
        }

        val manifests = manifestCollection.find(
            and(
                `in`(MANIFEST_ID_FIELD, imageIds.distinct()),
                eq(ImageDataManifestDocument::state.name, ImageDataManifestStateDocument.READY),
            )
        )
            .toList()
        if (manifests.isEmpty()) {
            return emptyMap()
        }

        val chunksByImageId =
            chunkCollection.find(`in`(ImageDataChunkDocument::imageId.name, manifests.map { it.imageId }))
                .sort(Sorts.ascending(ImageDataChunkDocument::imageId.name, ImageDataChunkDocument::order.name))
                .toList()
                .groupBy(ImageDataChunkDocument::imageId)

        return manifests.mapNotNull { manifest ->
            decodeReadyImageData(manifest, chunksByImageId[manifest.imageId].orEmpty())?.let {
                manifest.imageId to it
            }
        }.toMap()
    }

    override suspend fun save(imageId: UUID, data: ImageData) {
        writeImageData(
            imageId = imageId,
            data = data,
            writeWritingManifest = { manifest ->
                manifestCollection.replaceOne(eq(MANIFEST_ID_FIELD, imageId), manifest, manifestUpsert)
                true
            },
            writeReadyManifest = { manifest ->
                manifestCollection.replaceOne(eq(MANIFEST_ID_FIELD, imageId), manifest, manifestUpsert)
            },
        )
    }

    override suspend fun update(imageId: UUID, data: ImageData): Boolean {
        return writeImageData(
            imageId = imageId,
            data = data,
            writeWritingManifest = { manifest ->
                manifestCollection.replaceOne(eq(MANIFEST_ID_FIELD, imageId), manifest).matchedCount > 0
            },
            writeReadyManifest = { manifest ->
                manifestCollection.replaceOne(eq(MANIFEST_ID_FIELD, imageId), manifest)
            },
        )
    }

    override suspend fun deleteByImageId(imageId: UUID) {
        manifestCollection.deleteOne(eq(MANIFEST_ID_FIELD, imageId))
        chunkCollection.deleteMany(eq(ImageDataChunkDocument::imageId.name, imageId))
    }

    private suspend fun writeImageData(
        imageId: UUID,
        data: ImageData,
        writeWritingManifest: suspend (ImageDataManifestDocument) -> Boolean,
        writeReadyManifest: suspend (ImageDataManifestDocument) -> Unit,
    ): Boolean {
        val encoded = ImageDataBinaryCodec.encode(data)
        val compressed = Zstd.compress(encoded)
        val chunks = compressed.toChunkDocuments(imageId)
        val writingManifest = ImageDataManifestDocument(
            imageId = imageId,
            state = ImageDataManifestStateDocument.WRITING,
            chunkCount = chunks.size,
            schemaVersion = CURRENT_SCHEMA_VERSION,
            compression = DEFAULT_COMPRESSION,
            encodedByteLength = encoded.size,
        )
        if (!writeWritingManifest(writingManifest)) {
            return false
        }

        chunkCollection.deleteMany(eq(ImageDataChunkDocument::imageId.name, imageId))
        chunkCollection.insertMany(chunks)
        writeReadyManifest(writingManifest.copy(state = ImageDataManifestStateDocument.READY))
        return true
    }

    private fun decodeReadyImageData(
        manifest: ImageDataManifestDocument,
        chunks: List<ImageDataChunkDocument>,
    ): ImageData? {
        if (manifest.schemaVersion != CURRENT_SCHEMA_VERSION) {
            logger.warning("Failed to load gallery image data ${manifest.imageId}: unsupported schemaVersion=${manifest.schemaVersion}")
            return null
        }
        if (chunks.size != manifest.chunkCount) {
            logger.warning(
                "Failed to load gallery image data ${manifest.imageId}: chunk count mismatch, expected=${manifest.chunkCount}, actual=${chunks.size}"
            )
            return null
        }
        if (chunks.anyIndexed { index, chunk -> chunk.order != index }) {
            logger.warning("Failed to load gallery image data ${manifest.imageId}: chunk order is not contiguous from 0")
            return null
        }

        return runCatching {
            val compressed = chunks.mergeChunkBlobs()
            val encoded = decompress(manifest, compressed)
            ImageDataBinaryCodec.decode(encoded)
        }.onFailure {
            logger.log(Level.WARNING, "Failed to load gallery image data ${manifest.imageId}", it)
        }.getOrNull()
    }

    private fun decompress(manifest: ImageDataManifestDocument, compressed: ByteArray): ByteArray {
        return when (manifest.compression) {
            ImageDataCompressionDocument.ZSTD -> Zstd.decompress(compressed, manifest.encodedByteLength)
        }.also { encoded ->
            require(encoded.size == manifest.encodedByteLength) {
                "Decoded byte length mismatch: expected=${manifest.encodedByteLength}, actual=${encoded.size}"
            }
        }
    }

    private fun ByteArray.toChunkDocuments(imageId: UUID): List<ImageDataChunkDocument> {
        require(maxChunkBlobBytes > 0) { "maxChunkBlobBytes must be > 0" }
        return chunked(maxChunkBlobBytes).mapIndexed { index, chunk ->
            ImageDataChunkDocument(
                imageId = imageId,
                order = index,
                blob = org.bson.BsonBinary(chunk),
            )
        }
    }

    private fun ByteArray.chunked(chunkSize: Int): List<ByteArray> {
        if (isEmpty()) {
            return listOf(ByteArray(0))
        }

        val chunks = ArrayList<ByteArray>((size + chunkSize - 1) / chunkSize)
        var offset = 0
        while (offset < size) {
            val end = minOf(offset + chunkSize, size)
            chunks.add(copyOfRange(offset, end))
            offset = end
        }
        return chunks
    }

    private fun List<ImageDataChunkDocument>.mergeChunkBlobs(): ByteArray {
        val totalBytes = sumOf { it.blob.data.size }
        val merged = ByteArray(totalBytes)
        var offset = 0
        for (chunk in this) {
            val bytes = chunk.blob.data
            bytes.copyInto(merged, destinationOffset = offset)
            offset += bytes.size
        }
        return merged
    }

    private inline fun <T> Iterable<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
        var index = 0
        for (element in this) {
            if (predicate(index, element)) {
                return true
            }
            index++
        }
        return false
    }
}
