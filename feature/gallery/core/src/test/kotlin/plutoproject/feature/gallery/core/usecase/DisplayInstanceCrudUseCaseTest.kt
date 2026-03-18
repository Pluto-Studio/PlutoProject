package plutoproject.feature.gallery.core.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.InMemoryDisplayInstanceRepository
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.usecase.CreateDisplayInstanceUseCase
import plutoproject.feature.gallery.core.display.usecase.DeleteDisplayInstanceUseCase
import plutoproject.feature.gallery.core.display.usecase.GetDisplayInstanceUseCase
import plutoproject.feature.gallery.core.display.usecase.LookupDisplayInstanceByBelongsUseCase
import plutoproject.feature.gallery.core.display.usecase.LookupDisplayInstanceByChunkUseCase
import plutoproject.feature.gallery.core.dummyUuid
import plutoproject.feature.gallery.core.sampleDisplayInstance
import java.util.UUID

class DisplayInstanceCrudUseCaseTest {
    @Test
    fun `create should load manager first and then persist`() = runTest {
        val manager = DisplayManager()
        val repo = object : InMemoryDisplayInstanceRepository() {
            override suspend fun save(displayInstance: DisplayInstance) {
                assertNotNull(manager.getLoadedDisplayInstance(displayInstance.id))
                super.save(displayInstance)
            }
        }
        val useCase = CreateDisplayInstanceUseCase(repo, manager)
        val display = sampleDisplayInstance(id = dummyUuid(1001))

        val result = useCase.execute(
            id = display.id,
            belongsTo = display.belongsTo,
            world = display.world,
            chunkX = display.chunkX,
            chunkZ = display.chunkZ,
            facing = display.facing,
            widthBlocks = display.widthBlocks,
            heightBlocks = display.heightBlocks,
            originX = display.originX,
            originY = display.originY,
            originZ = display.originZ,
            itemFrameIds = display.itemFrameIds,
        )

        val created = (result as CreateDisplayInstanceUseCase.Result.Ok).displayInstance
        assertNotNull(repo.findById(created.id))
        assertSame(created, manager.getLoadedDisplayInstance(created.id))
    }

    @Test
    fun `create should return already existed when manager or repository already has same id`() = runTest {
        val manager = DisplayManager()
        val repo = InMemoryDisplayInstanceRepository()
        val useCase = CreateDisplayInstanceUseCase(repo, manager)
        val existed = sampleDisplayInstance(id = dummyUuid(1002))
        manager.loadDisplayInstance(existed)

        val resultFromManager = useCase.execute(
            id = existed.id,
            belongsTo = existed.belongsTo,
            world = existed.world,
            chunkX = existed.chunkX,
            chunkZ = existed.chunkZ,
            facing = existed.facing,
            widthBlocks = existed.widthBlocks,
            heightBlocks = existed.heightBlocks,
            originX = existed.originX,
            originY = existed.originY,
            originZ = existed.originZ,
            itemFrameIds = existed.itemFrameIds,
        )
        assertEquals(CreateDisplayInstanceUseCase.Result.AlreadyExisted(existed), resultFromManager)

        manager.unloadDisplayInstance(existed.id)
        repo.save(existed)
        val resultFromRepository = useCase.execute(
            id = existed.id,
            belongsTo = existed.belongsTo,
            world = existed.world,
            chunkX = existed.chunkX,
            chunkZ = existed.chunkZ,
            facing = existed.facing,
            widthBlocks = existed.widthBlocks,
            heightBlocks = existed.heightBlocks,
            originX = existed.originX,
            originY = existed.originY,
            originZ = existed.originZ,
            itemFrameIds = existed.itemFrameIds,
        )
        assertEquals(CreateDisplayInstanceUseCase.Result.AlreadyExisted(existed), resultFromRepository)
    }

    @Test
    fun `delete should unload manager first and then delete from repository`() = runTest {
        val manager = DisplayManager()
        val display = sampleDisplayInstance(id = dummyUuid(1003))
        val repo = object : InMemoryDisplayInstanceRepository(mutableMapOf(display.id to display)) {
            override suspend fun deleteById(id: UUID) {
                assertNull(manager.getLoadedDisplayInstance(id))
                super.deleteById(id)
            }
        }
        manager.loadDisplayInstance(display)
        val useCase = DeleteDisplayInstanceUseCase(repo, manager)

        val result = useCase.execute(display.id)

        assertEquals(DeleteDisplayInstanceUseCase.Result.Ok, result)
        assertNull(manager.getLoadedDisplayInstance(display.id))
        assertNull(repo.findById(display.id))
    }

    @Test
    fun `delete should return not existed when target is absent`() = runTest {
        val manager = DisplayManager()
        val repo = InMemoryDisplayInstanceRepository()
        val useCase = DeleteDisplayInstanceUseCase(repo, manager)

        val result = useCase.execute(dummyUuid(1004))

        assertEquals(DeleteDisplayInstanceUseCase.Result.NotExisted, result)
    }

    @Test
    fun `get and lookups should load from repository into manager cache`() = runTest {
        val manager = DisplayManager()
        val repo = InMemoryDisplayInstanceRepository()
        val first = sampleDisplayInstance(id = dummyUuid(1005), belongsTo = dummyUuid(2001), chunkX = 33, chunkZ = 44)
        val second = sampleDisplayInstance(id = dummyUuid(1006), belongsTo = dummyUuid(2001), chunkX = 33, chunkZ = 44)
        val third = sampleDisplayInstance(id = dummyUuid(1007), belongsTo = dummyUuid(2002), chunkX = 99, chunkZ = 11)
        repo.save(first)
        repo.save(second)
        repo.save(third)

        val get = GetDisplayInstanceUseCase(repo, manager)
        val lookupByBelongs = LookupDisplayInstanceByBelongsUseCase(repo, manager)
        val lookupByChunk = LookupDisplayInstanceByChunkUseCase(repo, manager)

        val belongsFound = (lookupByBelongs.execute(first.belongsTo) as LookupDisplayInstanceByBelongsUseCase.Result.Ok).displayInstances
        val chunkFound = (lookupByChunk.execute(33, 44) as LookupDisplayInstanceByChunkUseCase.Result.Ok).displayInstances
        val got = (get.execute(first.id) as GetDisplayInstanceUseCase.Result.Ok).displayInstance

        assertSame(got, manager.getLoadedDisplayInstance(first.id))
        assertEquals(2, belongsFound.size)
        assertEquals(2, chunkFound.size)
        assertNotNull(manager.getLoadedDisplayInstance(second.id))
    }

    @Test
    fun `create and delete should be concurrency safe and debounced by id`() = runTest {
        val manager = DisplayManager()
        val repo = InMemoryDisplayInstanceRepository()
        val display = sampleDisplayInstance(id = dummyUuid(1008))
        val create = CreateDisplayInstanceUseCase(repo, manager)
        val delete = DeleteDisplayInstanceUseCase(repo, manager)

        val createResults = (1..20)
            .map {
                async {
                    create.execute(
                        id = display.id,
                        belongsTo = display.belongsTo,
                        world = display.world,
                        chunkX = display.chunkX,
                        chunkZ = display.chunkZ,
                        facing = display.facing,
                        widthBlocks = display.widthBlocks,
                        heightBlocks = display.heightBlocks,
                        originX = display.originX,
                        originY = display.originY,
                        originZ = display.originZ,
                        itemFrameIds = display.itemFrameIds,
                    )
                }
            }
            .awaitAll()

        assertEquals(1, createResults.count { it is CreateDisplayInstanceUseCase.Result.Ok })
        assertEquals(19, createResults.count { it is CreateDisplayInstanceUseCase.Result.AlreadyExisted })
        assertNotNull(repo.findById(display.id))

        val deleteResults = (1..20)
            .map {
                async { delete.execute(display.id) }
            }
            .awaitAll()

        assertEquals(1, deleteResults.count { it is DeleteDisplayInstanceUseCase.Result.Ok })
        assertEquals(19, deleteResults.count { it is DeleteDisplayInstanceUseCase.Result.NotExisted })
        assertNull(repo.findById(display.id))
        assertNull(manager.getLoadedDisplayInstance(display.id))
    }
}
