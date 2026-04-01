package plutoproject.feature.gallery.infra.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.bson.UuidRepresentation
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import plutoproject.feature.gallery.core.image.AnimatedImageData
import plutoproject.feature.gallery.core.MapIdRange
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.display.ItemFrameFacing
import plutoproject.feature.gallery.core.image.StaticImageData
import plutoproject.feature.gallery.core.image.TilePool
import plutoproject.feature.gallery.infra.mongo.model.DisplayInstanceDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDataEntryDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDocument
import plutoproject.feature.gallery.infra.mongo.model.MapIdSystemInformationDocument
import java.util.UUID

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
            mapWidthBlocks = 2,
            mapHeightBlocks = 1,
            tileMapIds = intArrayOf(1, 2),
        )
        val image2 = Image(
            id = UUID.fromString("00000000-0000-0000-0000-000000000212"),
            type = ImageType.ANIMATED,
            owner = owner,
            ownerName = "Owner_211",
            name = "img-b",
            mapWidthBlocks = 1,
            mapHeightBlocks = 1,
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
        val collection = client.getDatabase("test").getCollection<ImageDataEntryDocument>("gallery_image_data_entries")
        val repo = MongoImageDataEntryRepository(collection)

        val staticBelongsTo = UUID.fromString("00000000-0000-0000-0000-000000000311")
        val staticEntry = ImageDataEntry(
            belongsTo = staticBelongsTo,
            type = ImageType.STATIC,
            data = StaticImageData(
                tilePool = TilePool(
                    offsets = intArrayOf(0, 2),
                    blob = byteArrayOf(7, 9),
                ),
                tileIndexes = shortArrayOf(0, 1),
            ),
        )
        repo.save(staticEntry)

        val loadedStatic = repo.findByBelongsTo(staticBelongsTo)
        assertNotNull(loadedStatic)
        loadedStatic!!
        assertEquals(ImageType.STATIC, loadedStatic.type)
        val loadedStaticData = loadedStatic.data as StaticImageData
        assertArrayEquals(intArrayOf(0, 2), loadedStaticData.tilePool.offsets)
        assertArrayEquals(byteArrayOf(7, 9), loadedStaticData.tilePool.blob)
        assertArrayEquals(shortArrayOf(0, 1), loadedStaticData.tileIndexes)

        val animatedBelongsTo = UUID.fromString("00000000-0000-0000-0000-000000000312")
        val animatedEntry = ImageDataEntry(
            belongsTo = animatedBelongsTo,
            type = ImageType.ANIMATED,
            data = AnimatedImageData(
                frameCount = 2,
                durationMillis = 120,
                tilePool = TilePool(
                    offsets = intArrayOf(0, 1, 3),
                    blob = byteArrayOf(1, 2, 3),
                ),
                tileIndexes = shortArrayOf(0, 1, 1, 0),
            ),
        )
        repo.save(animatedEntry)

        val loadedAnimated = repo.findByBelongsTo(animatedBelongsTo)
        assertNotNull(loadedAnimated)
        loadedAnimated!!
        assertEquals(ImageType.ANIMATED, loadedAnimated.type)
        val loadedAnimatedData = loadedAnimated.data as AnimatedImageData
        assertEquals(2, loadedAnimatedData.frameCount)
        assertEquals(120, loadedAnimatedData.durationMillis)
        assertArrayEquals(intArrayOf(0, 1, 3), loadedAnimatedData.tilePool.offsets)
        assertArrayEquals(byteArrayOf(1, 2, 3), loadedAnimatedData.tilePool.blob)
        assertArrayEquals(shortArrayOf(0, 1, 1, 0), loadedAnimatedData.tileIndexes)

        val byBelongsToIn = repo.findByBelongsToIn(
            listOf(
                staticBelongsTo,
                animatedBelongsTo,
                UUID.fromString("00000000-0000-0000-0000-000000000399"),
            )
        )
        assertEquals(setOf(staticBelongsTo, animatedBelongsTo), byBelongsToIn.keys)

        repo.deleteByBelongsTo(animatedBelongsTo)
        assertNull(repo.findByBelongsTo(animatedBelongsTo))
        assertNotNull(repo.findByBelongsTo(staticBelongsTo))
    }

    @Test
    fun `display instance repository should support save find lookup and delete`() = runTest {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable)

        val client = newClient()
        val collection = client.getDatabase("test").getCollection<DisplayInstanceDocument>("gallery_display_instances")
        val repo = MongoDisplayInstanceRepository(collection)

        val belongsTo = UUID.fromString("00000000-0000-0000-0000-000000000411")
        val first = DisplayInstance(
            id = UUID.fromString("00000000-0000-0000-0000-000000000412"),
            belongsTo = belongsTo,
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
            belongsTo = belongsTo,
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

        val byBelongs = repo.findByBelongsTo(belongsTo)
        assertEquals(2, byBelongs.size)

        val byChunk = repo.findByChunk(12, 34)
        assertEquals(2, byChunk.size)

        val byIds = repo.findByIds(listOf(first.id, second.id, UUID.fromString("00000000-0000-0000-0000-000000000499")))
        assertEquals(setOf(first.id, second.id), byIds.keys)

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
}
