package plutoproject.feature.gallery.core.image.usecase

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.InMemoryImageDataEntryRepository
import plutoproject.feature.gallery.core.dummyUuid

class ImageDataEntryCrudUseCaseTest {
    @Test
    fun `create should save first and then load into manager`() = runTest {
        val repo = InMemoryImageDataEntryRepository()
        val manager = ImageManager()
        val useCase = CreateImageDataEntryUseCase(repo, manager)
        val belongsTo = dummyUuid(301)

        val result = useCase.execute(
            belongsTo = belongsTo,
            type = ImageType.STATIC,
            data = staticImageData(tileIndexesSize = 1),
        )

        val entry = (result as CreateImageDataEntryUseCase.Result.Success).entry
        assertNotNull(repo.findByImageId(belongsTo))
        assertSame(entry, manager.getLoadedImageDataEntry(belongsTo))
    }

    @Test
    fun `create should return already existed when belongsTo exists`() = runTest {
        val repo = InMemoryImageDataEntryRepository()
        val manager = ImageManager()
        val useCase = CreateImageDataEntryUseCase(repo, manager)
        val belongsTo = dummyUuid(330)
        val existed = ImageDataEntry(
            belongsTo = belongsTo,
            type = ImageType.STATIC,
            data = staticImageData(tileIndexesSize = 2),
        )
        repo.save(existed)

        val result = useCase.execute(
            belongsTo = belongsTo,
            type = ImageType.STATIC,
            data = staticImageData(tileIndexesSize = 1),
        )

        assertEquals(CreateImageDataEntryUseCase.Result.AlreadyExists(existed), result)
    }

    @Test
    fun `get should call manager lifecycle to load and cache entry`() = runTest {
        val repo = InMemoryImageDataEntryRepository()
        val manager = ImageManager()
        val useCase = GetImageDataEntryUseCase(repo, manager)
        val belongsTo = dummyUuid(302)
        val entry = ImageDataEntry(
            belongsTo = belongsTo,
            type = ImageType.ANIMATED,
            data = animatedImageData(frameCount = 2, durationMillis = 100, tileIndexesSize = 2),
        )
        repo.save(entry)

        val first = useCase.execute(belongsTo)
        val second = useCase.execute(belongsTo)

        assertSame((first as GetImageDataEntryUseCase.Result.Success).entry, manager.getLoadedImageDataEntry(belongsTo))
        assertSame((second as GetImageDataEntryUseCase.Result.Success).entry, manager.getLoadedImageDataEntry(belongsTo))
    }

    @Test
    fun `replace should mutate loaded entry and persist it`() = runTest {
        val repo = InMemoryImageDataEntryRepository()
        val manager = ImageManager()
        val belongsTo = dummyUuid(303)
        val entry = ImageDataEntry(
            belongsTo = belongsTo,
            type = ImageType.STATIC,
            data = staticImageData(tileIndexesSize = 1),
        )
        repo.save(entry)
        manager.loadImageDataEntry(entry)
        val useCase = ReplaceImageDataEntryUseCase(repo, manager)

        val newData = staticImageData(tileIndexesSize = 4)
        val result = useCase.execute(belongsTo, newData)

        assertEquals(ReplaceImageDataEntryUseCase.Result.Success, result)
        assertSame(newData, entry.data)
        assertSame(newData, (repo.findByImageId(belongsTo)?.data as ImageData.Static))
    }

    @Test
    fun `replace should fallback to io when entry is not loaded`() = runTest {
        val repo = InMemoryImageDataEntryRepository()
        val manager = ImageManager()
        val belongsTo = dummyUuid(304)
        val entry = ImageDataEntry(
            belongsTo = belongsTo,
            type = ImageType.ANIMATED,
            data = animatedImageData(frameCount = 1, durationMillis = 50, tileIndexesSize = 1),
        )
        repo.save(entry)
        val useCase = ReplaceImageDataEntryUseCase(repo, manager)

        val newData = animatedImageData(frameCount = 2, durationMillis = 100, tileIndexesSize = 2)
        val result = useCase.execute(belongsTo, newData)

        assertEquals(ReplaceImageDataEntryUseCase.Result.Success, result)
        assertSame(newData, (repo.findByImageId(belongsTo)?.data as ImageData.Animated))
        assertNull(manager.getLoadedImageDataEntry(belongsTo))
    }

    @Test
    fun `delete should unload from manager and remove from repository`() = runTest {
        val repo = InMemoryImageDataEntryRepository()
        val manager = ImageManager()
        val belongsTo = dummyUuid(305)
        val entry = ImageDataEntry(
            belongsTo = belongsTo,
            type = ImageType.STATIC,
            data = staticImageData(tileIndexesSize = 1),
        )
        repo.save(entry)
        manager.loadImageDataEntry(entry)
        val useCase = DeleteImageDataEntryUseCase(repo, manager)

        val result = useCase.execute(belongsTo)

        assertEquals(DeleteImageDataEntryUseCase.Result.Success, result)
        assertNull(repo.findByImageId(belongsTo))
        assertNull(manager.getLoadedImageDataEntry(belongsTo))
    }

    @Test
    fun `delete should return not existed when entry is absent`() = runTest {
        val repo = InMemoryImageDataEntryRepository()
        val manager = ImageManager()
        val useCase = DeleteImageDataEntryUseCase(repo, manager)

        val result = useCase.execute(dummyUuid(331))

        assertEquals(DeleteImageDataEntryUseCase.Result.NotFound, result)
    }
}
