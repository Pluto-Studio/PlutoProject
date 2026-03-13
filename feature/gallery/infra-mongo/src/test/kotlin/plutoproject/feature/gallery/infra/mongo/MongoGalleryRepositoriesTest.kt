package plutoproject.feature.gallery.infra.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
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
import plutoproject.feature.gallery.core.AnimatedImageData
import plutoproject.feature.gallery.core.Image
import plutoproject.feature.gallery.core.ImageDataEntry
import plutoproject.feature.gallery.core.ImageType
import plutoproject.feature.gallery.core.StaticImageData
import plutoproject.feature.gallery.core.TilePool
import plutoproject.feature.gallery.infra.mongo.model.ImageDataEntryDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDocument
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

        repo.deleteByBelongsTo(animatedBelongsTo)
        assertNull(repo.findByBelongsTo(animatedBelongsTo))
        assertNotNull(repo.findByBelongsTo(staticBelongsTo))
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
