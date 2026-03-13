package plutoproject.feature.gallery.core.usecase

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.ImageManager
import plutoproject.feature.gallery.core.ImageType
import plutoproject.feature.gallery.core.InMemoryImageRepository
import plutoproject.feature.gallery.core.dummyUuid
import plutoproject.feature.gallery.core.sampleImage

class ImageCrudUseCaseTest {
    @Test
    fun `create should save first and then load into manager`() = runTest {
        val repo = InMemoryImageRepository()
        val manager = ImageManager()
        val useCase = CreateImageUseCase(repo, manager)

        val id = dummyUuid(100)
        val owner = dummyUuid(200)
        val result = useCase.execute(
            id = id,
            type = ImageType.STATIC,
            owner = owner,
            ownerName = "Owner_456",
            name = "gallery",
            mapWidthBlocks = 1,
            mapHeightBlocks = 1,
            tileMapIds = intArrayOf(1),
        )

        val image = (result as CreateImageUseCase.Result.Ok).image
        assertNotNull(repo.findById(id))
        assertSame(image, manager.getLoadedImage(id))
    }

    @Test
    fun `get should call manager lifecycle to load and cache image`() = runTest {
        val repo = InMemoryImageRepository()
        val manager = ImageManager()
        val useCase = GetImageUseCase(repo, manager)
        val image = sampleImage(id = dummyUuid(101))
        repo.save(image)

        val first = useCase.execute(image.id)
        val second = useCase.execute(image.id)

        assertSame((first as GetImageUseCase.Result.Ok).image, manager.getLoadedImage(image.id))
        assertSame((second as GetImageUseCase.Result.Ok).image, manager.getLoadedImage(image.id))
    }

    @Test
    fun `rename should mutate loaded image and persist it`() = runTest {
        val repo = InMemoryImageRepository()
        val manager = ImageManager()
        val image = sampleImage(id = dummyUuid(102), name = "old")
        repo.save(image)
        manager.loadImage(image)
        val useCase = RenameImageUseCase(repo, manager)

        val result = useCase.execute(image.id, "new")

        assertEquals(RenameImageUseCase.Result.Ok, result)
        assertEquals("new", image.name)
        assertEquals("new", repo.findById(image.id)?.name)
    }

    @Test
    fun `change owner name should fallback to io when image is not loaded`() = runTest {
        val repo = InMemoryImageRepository()
        val manager = ImageManager()
        val image = sampleImage(id = dummyUuid(103), ownerName = "OldName")
        repo.save(image)
        val useCase = ChangeImageOwnerNameUseCase(repo, manager)

        val result = useCase.execute(image.id, "NewName")

        assertEquals(ChangeImageOwnerNameUseCase.Result.Ok, result)
        assertEquals("NewName", repo.findById(image.id)?.ownerName)
        assertNull(manager.getLoadedImage(image.id))
    }

    @Test
    fun `delete and lookup by owner should operate on repository`() = runTest {
        val repo = InMemoryImageRepository()
        val owner = dummyUuid(201)
        val other = dummyUuid(202)
        val first = sampleImage(id = dummyUuid(104), owner = owner)
        val second = sampleImage(id = dummyUuid(105), owner = owner)
        val third = sampleImage(id = dummyUuid(106), owner = other)
        repo.save(first)
        repo.save(second)
        repo.save(third)

        val lookup = LookupImageByOwnerUseCase(repo)
        val delete = DeleteImageUseCase(repo)

        val ownerImages = (lookup.execute(owner) as LookupImageByOwnerUseCase.Result.Ok).images
        assertEquals(2, ownerImages.size)

        delete.execute(first.id)
        assertNull(repo.findById(first.id))
    }
}
