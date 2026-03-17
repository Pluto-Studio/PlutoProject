package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.DisplayInstance
import plutoproject.feature.gallery.core.DisplayInstanceRepository
import plutoproject.feature.gallery.core.DisplayManager
import plutoproject.feature.gallery.core.ItemFrameFacing
import java.util.UUID

class CreateDisplayInstanceUseCase(
    private val displayInstances: DisplayInstanceRepository,
    private val displayManager: DisplayManager,
) {
    sealed class Result {
        data class Ok(val displayInstance: DisplayInstance) : Result()
        data class AlreadyExisted(val displayInstance: DisplayInstance) : Result()
    }

    suspend fun execute(
        id: UUID,
        belongsTo: UUID,
        world: String,
        chunkX: Int,
        chunkZ: Int,
        facing: ItemFrameFacing,
        widthBlocks: Int,
        heightBlocks: Int,
        originX: Double,
        originY: Double,
        originZ: Double,
        itemFrameIds: List<UUID>,
    ): Result = displayManager.withDisplayInstanceOperationLock(id) {
        val existed = displayManager.getLoadedDisplayInstance(id)
            ?: displayInstances.findById(id)
        if (existed != null) {
            return@withDisplayInstanceOperationLock Result.AlreadyExisted(existed)
        }

        val displayInstance = DisplayInstance(
            id = id,
            belongsTo = belongsTo,
            world = world,
            chunkX = chunkX,
            chunkZ = chunkZ,
            facing = facing,
            widthBlocks = widthBlocks,
            heightBlocks = heightBlocks,
            originX = originX,
            originY = originY,
            originZ = originZ,
            itemFrameIds = itemFrameIds,
        )

        displayManager.loadDisplayInstance(displayInstance)
        displayInstances.save(displayInstance)
        Result.Ok(displayInstance)
    }
}
