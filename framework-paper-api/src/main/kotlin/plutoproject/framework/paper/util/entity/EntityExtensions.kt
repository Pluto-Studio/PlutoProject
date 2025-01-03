package plutoproject.framework.paper.util.entity

import kotlinx.coroutines.future.await
import org.bukkit.Location
import org.bukkit.entity.Entity

suspend fun Entity.teleportSuspend(location: Location) {
    teleportAsync(location).await()
}
