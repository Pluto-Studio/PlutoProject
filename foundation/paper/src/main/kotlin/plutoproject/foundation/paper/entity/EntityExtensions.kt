package plutoproject.foundation.paper.entity

import kotlinx.coroutines.future.await
import org.bukkit.Location
import org.bukkit.entity.Entity

suspend fun Entity.teleportSuspend(location: Location) {
    teleportAsync(location).await()
}
