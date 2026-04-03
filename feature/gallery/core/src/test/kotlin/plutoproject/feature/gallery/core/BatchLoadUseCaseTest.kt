package plutoproject.feature.gallery.core

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.usecase.GetDisplayInstancesByIdsUseCase

class BatchLoadUseCaseTest {
    @Test
    fun `get display instances by ids should merge loaded and repository results into cache`() = runTest {
        val manager = DisplayManager()
        val loaded = sampleDisplayInstance(id = dummyUuid(5001), belongsTo = dummyUuid(5002))
        val missing = sampleDisplayInstance(id = dummyUuid(5003), belongsTo = dummyUuid(5004))
        val repository = InMemoryDisplayInstanceRepository(mutableMapOf(missing.id to missing))
        manager.loadDisplayInstance(loaded)

        val result = GetDisplayInstancesByIdsUseCase(repository, manager).execute(listOf(loaded.id, missing.id, dummyUuid(5005)))

        val found = (result as GetDisplayInstancesByIdsUseCase.Result.Ok).displayInstances
        assertEquals(setOf(loaded.id, missing.id), found.keys)
        assertSame(loaded, found[loaded.id])
        assertSame(missing, manager.getLoadedDisplayInstance(missing.id))
    }

    @Test
    fun `get images by ids should merge loaded and repository results into cache`() = runTest {
        val manager = ImageManager()
        val loaded = sampleImage(id = dummyUuid(5101))
        val missing = sampleImage(id = dummyUuid(5102))
        val repository = InMemoryImageRepository(mutableMapOf(missing.id to missing))
        manager.loadImage(loaded)

        val result = GetImagesByIdsUseCase(repository, manager).execute(listOf(loaded.id, missing.id, dummyUuid(5103)))

        val found = (result as GetImagesByIdsUseCase.Result.Success).images
        assertEquals(setOf(loaded.id, missing.id), found.keys)
        assertSame(loaded, found[loaded.id])
        assertSame(missing, manager.getLoadedImage(missing.id))
    }

    @Test
    fun `get image data entries by belongsTo should merge loaded and repository results into cache`() = runTest {
        val manager = ImageManager()
        val loaded = sampleStaticImageDataEntry(dummyUuid(5201))
        val missing = sampleStaticImageDataEntry(dummyUuid(5202))
        val repository = InMemoryImageDataEntryRepository(mutableMapOf(missing.imageId to missing))
        manager.loadImageDataEntry(loaded)

        val result = GetImageDataEntriesByBelongsToUseCase(repository, manager).execute(listOf(loaded.imageId, missing.imageId, dummyUuid(5203)))

        val found = (result as GetImageDataEntriesByBelongsToUseCase.Result.Success).entries
        assertEquals(setOf(loaded.imageId, missing.imageId), found.keys)
        assertSame(loaded, found[loaded.imageId])
        assertSame(missing, manager.getLoadedImageDataEntry(missing.imageId))
    }
}
