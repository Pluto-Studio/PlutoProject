package plutoproject.feature.gallery.infra.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.bson.UuidRepresentation
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import plutoproject.feature.gallery.core.MapIdRange
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.ItemFrameFacing
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.render.tile.TilePool
import plutoproject.feature.gallery.core.render.tile.TilePoolSnapshot
import plutoproject.feature.gallery.infra.mongo.model.DisplayInstanceDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDataChunkDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDataManifestDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDataManifestStateDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDataDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDocument
import plutoproject.feature.gallery.infra.mongo.model.MapIdSystemInformationDocument
import java.util.UUID
import java.util.logging.Logger
import kotlin.time.Duration.Companion.milliseconds

@Testcontainers(disabledWithoutDocker = true)
class MongoGalleryRepositoriesTest {
    @Container
    private val mongo = MongoDBContainer("mongo:7.0.14")

    @Test
    fun `image repository should support save find lookup and delete`() = runTest {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable)

        val client = newClient()
        val collection = client.getDatabase("test").getCollection<ImageDocument>("gallery_images")
        val repo = MongoImageRepository(collection)

        val owner = UUID.fromString("00000000-0000-0000-0000-000000000111")
        val image = Image(
            id = UUID.fromString("00000000-0000-0000-0000-000000000211"),
            type = ImageType.STATIC,
            owner = owner,
            ownerName = "Owner_211",
            name = "img-a",
            widthBlocks = 2,
            heightBlocks = 1,
            tileMapIds = intArrayOf(1, 2),
        )
        val image2 = Image(
            id = UUID.fromString("00000000-0000-0000-0000-000000000212"),
            type = ImageType.ANIMATED,
            owner = owner,
            ownerName = "Owner_211",
            name = "img-b",
            widthBlocks = 1,
            heightBlocks = 1,
            tileMapIds = intArrayOf(3),
        )

        repo.save(image)
        repo.save(image2)

        val loaded = repo.findById(image.id)
        assertNotNull(loaded)
        loaded!!
        assertEquals(ImageType.STATIC, loaded.type)
        assertEquals("img-a", loaded.name)
        assertArrayEquals(intArrayOf(1, 2), loaded.tileMapIds)

        val owned = repo.findByOwner(owner)
        assertEquals(2, owned.size)

        val byIds = repo.findByIds(listOf(image.id, image2.id, UUID.fromString("00000000-0000-0000-0000-000000000299")))
        assertEquals(setOf(image.id, image2.id), byIds.keys)

        repo.deleteById(image.id)
        assertNull(repo.findById(image.id))
        assertNotNull(repo.findById(image2.id))
    }

    @Test
    fun `image data entry repository should support static and animated crud`() = runTest {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable)

        val client = newClient()
        val manifestCollection = client.getDatabase("test")
            .getCollection<ImageDataManifestDocument>("gallery_image_data_manifests")
        val chunkCollection = client.getDatabase("test")
            .getCollection<ImageDataChunkDocument>("gallery_image_data_chunks")
        val repo = MongoImageDataRepository(
            manifestCollection = manifestCollection,
            chunkCollection = chunkCollection,
            logger = Logger.getAnonymousLogger(),
            maxChunkBlobBytes = 4,
        )
        repo.ensureIndexes()

        val staticBelongsTo = UUID.fromString("00000000-0000-0000-0000-000000000311")
        val staticData = ImageData.Static(
            tilePool = TilePool.fromSnapshot(
                TilePoolSnapshot(
                    offsets = intArrayOf(0, 2),
                    blob = byteArrayOf(7, 9),
                )
            ),
            tileIndexes = shortArrayOf(0, 1),
        )
        repo.save(staticBelongsTo, staticData)

        val loadedStatic = repo.findByImageId(staticBelongsTo)
        assertNotNull(loadedStatic)
        loadedStatic!!
        assertEquals(ImageType.STATIC, loadedStatic.type)
        val loadedStaticData = loadedStatic as ImageData.Static
        assertTilePoolEquals(
            expectedOffsets = intArrayOf(0, 2),
            expectedBlob = byteArrayOf(7, 9),
            tilePool = loadedStaticData.tilePool,
        )
        assertTrue(loadedStaticData.tileIndexes.contentEquals(shortArrayOf(0, 1)))

        val animatedBelongsTo = UUID.fromString("00000000-0000-0000-0000-000000000312")
        val animatedData = ImageData.Animated(
            tilePool = TilePool.fromSnapshot(
                TilePoolSnapshot(
                    offsets = intArrayOf(0, 1, 3),
                    blob = byteArrayOf(1, 2, 3),
                )
            ),
            tileIndexes = shortArrayOf(0, 1, 1, 0),
            frameCount = 2,
            duration = 120.milliseconds,
        )
        repo.save(animatedBelongsTo, animatedData)

        val loadedAnimated = repo.findByImageId(animatedBelongsTo)
        assertNotNull(loadedAnimated)
        loadedAnimated!!
        assertEquals(ImageType.ANIMATED, loadedAnimated.type)
        val loadedAnimatedData = loadedAnimated as ImageData.Animated
        assertEquals(2, loadedAnimatedData.frameCount)
        assertEquals(120.milliseconds, loadedAnimatedData.duration)
        assertTilePoolEquals(
            expectedOffsets = intArrayOf(0, 1, 3),
            expectedBlob = byteArrayOf(1, 2, 3),
            tilePool = loadedAnimatedData.tilePool,
        )
        assertTrue(loadedAnimatedData.tileIndexes.contentEquals(shortArrayOf(0, 1, 1, 0)))

        val byBelongsToIn = repo.findByImageIds(
            listOf(
                staticBelongsTo,
                animatedBelongsTo,
                UUID.fromString("00000000-0000-0000-0000-000000000399"),
            )
        )
        assertEquals(setOf(staticBelongsTo, animatedBelongsTo), byBelongsToIn.keys)

        val updatedStaticData = ImageData.Static(
            tilePool = TilePool.fromSnapshot(
                TilePoolSnapshot(
                    offsets = intArrayOf(0, 3),
                    blob = byteArrayOf(8, 9, 10),
                )
            ),
            tileIndexes = shortArrayOf(0, 0, 0),
        )
        assertTrue(repo.update(staticBelongsTo, updatedStaticData))
        val reloadedStatic = repo.findByImageId(staticBelongsTo) as ImageData.Static
        assertTilePoolEquals(
            expectedOffsets = intArrayOf(0, 3),
            expectedBlob = byteArrayOf(8, 9, 10),
            tilePool = reloadedStatic.tilePool,
        )
        assertTrue(reloadedStatic.tileIndexes.contentEquals(shortArrayOf(0, 0, 0)))
        assertEquals(false, repo.update(UUID.fromString("00000000-0000-0000-0000-000000000398"), updatedStaticData))

        repo.deleteByImageId(animatedBelongsTo)
        assertNull(repo.findByImageId(animatedBelongsTo))
        assertNotNull(repo.findByImageId(staticBelongsTo))
    }

    @Test
    fun `image data entry repository should hide writing and incomplete data`() = runTest {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable)

        val client = newClient()
        val manifestCollection = client.getDatabase("test")
            .getCollection<ImageDataManifestDocument>("gallery_image_data_manifests_integrity")
        val chunkCollection = client.getDatabase("test")
            .getCollection<ImageDataChunkDocument>("gallery_image_data_chunks_integrity")
        val repo = MongoImageDataRepository(
            manifestCollection = manifestCollection,
            chunkCollection = chunkCollection,
            logger = Logger.getAnonymousLogger(),
            maxChunkBlobBytes = 4,
        )

        val imageId = UUID.fromString("00000000-0000-0000-0000-000000000321")
        val data = ImageData.Static(
            tilePool = TilePool.fromSnapshot(
                TilePoolSnapshot(
                    offsets = intArrayOf(0, 2),
                    blob = byteArrayOf(1, 2),
                )
            ),
            tileIndexes = shortArrayOf(0, 1),
        )
        repo.save(imageId, data)

        val readyManifest = manifestCollection.find(eq("_id", imageId)).firstOrNull()
        assertNotNull(readyManifest)
        manifestCollection.replaceOne(
            eq("_id", imageId),
            readyManifest!!.copy(state = ImageDataManifestStateDocument.WRITING),
        )
        assertNull(repo.findByImageId(imageId))

        manifestCollection.replaceOne(eq("_id", imageId), readyManifest)
        chunkCollection.deleteOne(and(eq(ImageDataChunkDocument::imageId.name, imageId), eq(ImageDataChunkDocument::order.name, 0)))
        assertNull(repo.findByImageId(imageId))
        assertTrue(repo.findByImageIds(listOf(imageId)).isEmpty())
    }

    @Test
    fun `image data migration path should convert legacy document into chunked storage`() = runTest {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable)

        val client = newClient()
        val manifestCollection = client.getDatabase("test")
            .getCollection<ImageDataManifestDocument>("gallery_image_data_manifests_migration")
        val chunkCollection = client.getDatabase("test")
            .getCollection<ImageDataChunkDocument>("gallery_image_data_chunks_migration")
        val repo = MongoImageDataRepository(
            manifestCollection = manifestCollection,
            chunkCollection = chunkCollection,
            logger = Logger.getAnonymousLogger(),
            maxChunkBlobBytes = 4,
        )

        val legacyDocument = ImageDataDocument(
            imageId = UUID.fromString("00000000-0000-0000-0000-000000000331"),
            type = plutoproject.feature.gallery.infra.mongo.model.ImageTypeDocument.ANIMATED,
            tilePool = plutoproject.feature.gallery.infra.mongo.model.TilePoolDocument(
                offset = intArrayOf(0, 2, 5),
                blob = org.bson.BsonBinary(byteArrayOf(3, 4, 5, 6, 7)),
            ),
            tileIndexes = shortArrayOf(0, 1, 1, 0),
            frameCount = 2,
            duration = 90.milliseconds,
        )

        val expected = legacyDocument.toDomain()
        repo.save(legacyDocument.imageId, expected)

        assertEquals(expected, repo.findByImageId(legacyDocument.imageId))
    }

    @Test
    fun `display instance repository should support save find lookup and delete`() = runTest {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable)

        val client = newClient()
        val collection = client.getDatabase("test").getCollection<DisplayInstanceDocument>("gallery_display_instances")
        val repo = MongoDisplayInstanceRepository(collection)

        val imageId = UUID.fromString("00000000-0000-0000-0000-000000000411")
        val first = DisplayInstance(
            id = UUID.fromString("00000000-0000-0000-0000-000000000412"),
            imageId = imageId,
            world = "world",
            chunkX = 12,
            chunkZ = 34,
            facing = ItemFrameFacing.NORTH,
            widthBlocks = 2,
            heightBlocks = 2,
            originX = 100.5,
            originY = 64.0,
            originZ = -20.25,
            itemFrameIds = listOf(
                UUID.fromString("00000000-0000-0000-0000-000000000413"),
                UUID.fromString("00000000-0000-0000-0000-000000000414"),
            ),
        )
        val second = DisplayInstance(
            id = UUID.fromString("00000000-0000-0000-0000-000000000415"),
            imageId = imageId,
            world = "world_nether",
            chunkX = 12,
            chunkZ = 34,
            facing = ItemFrameFacing.WEST,
            widthBlocks = 1,
            heightBlocks = 1,
            originX = 10.0,
            originY = 70.0,
            originZ = 10.0,
            itemFrameIds = listOf(UUID.fromString("00000000-0000-0000-0000-000000000416")),
        )

        repo.save(first)
        repo.save(second)

        val loaded = repo.findById(first.id)
        assertNotNull(loaded)
        loaded!!
        assertEquals(ItemFrameFacing.NORTH, loaded.facing)
        assertEquals(12, loaded.chunkX)
        assertEquals(34, loaded.chunkZ)
        assertEquals(2, loaded.itemFrameIds.size)

        val byIds = repo.findByIds(
            listOf(
                first.id,
                second.id,
                first.id,
                UUID.fromString("00000000-0000-0000-0000-000000000499"),
            )
        )
        assertEquals(setOf(first.id, second.id), byIds.keys)

        val byImageId = repo.findByImageId(imageId)
        assertEquals(2, byImageId.size)

        val byChunk = repo.findByChunk(12, 34)
        assertEquals(2, byChunk.size)

        repo.deleteById(first.id)
        assertNull(repo.findById(first.id))
        assertNotNull(repo.findById(second.id))
    }

    @Test
    fun `system information repository should allocate contiguous range and reject overflow`() = runTest {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable)

        val client = newClient()
        val collection = client.getDatabase("test").getCollection<MapIdSystemInformationDocument>("gallery_system_information")
        val repo = MongoSystemInformationRepository(collection)
        val range = MapIdRange(start = 100, end = 110)

        val first = repo.allocateMapIds(count = 5, mapIdRange = range)
        val second = repo.allocateMapIds(count = 3, mapIdRange = range)
        val overflow = repo.allocateMapIds(count = 4, mapIdRange = range)

        assertEquals(104, first)
        assertEquals(107, second)
        assertNull(overflow)
    }

    @Test
    fun `system information repository should be concurrency safe`() = runTest {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable)

        val client = newClient()
        val collection = client.getDatabase("test").getCollection<MapIdSystemInformationDocument>("gallery_system_information_concurrent")
        val repo = MongoSystemInformationRepository(collection)
        val range = MapIdRange(start = 1_000, end = 2_000)

        val results = (1..50)
            .map {
                async {
                    repo.allocateMapIds(count = 1, mapIdRange = range)
                }
            }
            .awaitAll()

        assertEquals(50, results.filterNotNull().size)
        assertEquals(50, results.filterNotNull().distinct().size)
        assertEquals((1_000..1_049).toList(), results.filterNotNull().sorted())
    }

    private fun newClient(): MongoClient {
        return MongoClient.create(
            MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(mongo.replicaSetUrl))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build()
        )
    }

    private fun assertTilePoolEquals(
        expectedOffsets: IntArray,
        expectedBlob: ByteArray,
        tilePool: TilePool,
    ) {
        val snapshot = tilePool.snapshot()
        assertArrayEquals(expectedOffsets, snapshot.offsets)
        assertArrayEquals(expectedBlob, snapshot.blob)
    }
}
