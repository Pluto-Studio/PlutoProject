package plutoproject.feature.home.paper

import plutoproject.foundation.paper.world.toModel

import plutoproject.feature.home.paper.homeManager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import plutoproject.kernel.api.koinInject
import plutoproject.feature.home.api.paper.Home
import plutoproject.feature.home.api.paper.HomeManager
import plutoproject.feature.home.api.paper.HomeTeleportEvent
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.feature.home.paper.moduleScope
import java.time.Instant
import java.util.*

class HomeImpl(private val model: HomeModel) : Home {
    private val repo by koinInject<HomeRepository>()

    override val id: UUID = model.id
    override var name: String = model.name
    override var icon: Material? = model.icon
    override val createdAt: Instant = Instant.ofEpochMilli(model.createdAt)
    override var location: Location =
        requireNotNull(model.location.toLocation()) {
            loadFailed(id, "Failed to load location ${model.location}")
        }
    override val owner: OfflinePlayer =
        requireNotNull(Bukkit.getOfflinePlayer(model.owner)) {
            loadFailed(id, "Failed to load OfflinePlayer ${model.owner}")
        }
    override var isStarred: Boolean = model.isStarred
    override var isPreferred: Boolean = model.isPreferred
    override val isLoaded: Boolean get() = homeManager.isLoaded(id)

    override fun teleport(player: Player, prompt: Boolean) {
        moduleScope.launch {
            teleportSuspend(player, prompt)
        }
    }

    override suspend fun setPreferred(state: Boolean) {
        if (!state) {
            if (!isPreferred) return
            isPreferred = false
            update()
            return
        }

        (homeManager.getPreferredHome(owner) as HomeImpl?)?.let {
            if (it == this) return
            it.isPreferred = false
            it.update()
        }

        isPreferred = true
        update()
    }

    override suspend fun teleportSuspend(player: Player, prompt: Boolean) {
        withContext(Dispatchers.Default) {
            val options = teleportManager.getWorldTeleportOptions(location.world).copy(disableSafeCheck = true)
            // 必须异步触发
            val event = HomeTeleportEvent(player, player.location, this@HomeImpl).apply { callEvent() }
            if (event.isCancelled) return@withContext
            teleportManager.teleportSuspend(player, location, options, prompt)
        }
    }

    private fun toModel() = model.copy(
        id = id,
        name = name,
        icon = icon,
        createdAt = createdAt.toEpochMilli(),
        location = location.toModel(),
        owner = owner.uniqueId,
        isStarred = isStarred,
        isPreferred = isPreferred,
    )

    override fun equals(other: Any?): Boolean {
        if (other !is Home) return false
        return other.id == this.id
    }

    override suspend fun update() {
        repo.update(toModel())
    }

    override fun hashCode(): Int {
        var result = model.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + isStarred.hashCode()
        result = 31 * result + isPreferred.hashCode()
        return result
    }
}
