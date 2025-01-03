package ink.pmc.essentials.home

import ink.pmc.essentials.api.home.Home
import ink.pmc.essentials.api.home.HomeManager
import ink.pmc.essentials.api.home.HomeTeleportEvent
import ink.pmc.essentials.api.teleport.TeleportManager
import ink.pmc.essentials.models.HomeModel
import ink.pmc.essentials.repositories.HomeRepository
import ink.pmc.framework.concurrent.async
import ink.pmc.framework.concurrent.submitAsync
import ink.pmc.framework.storage.model
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.*

class HomeImpl(private val model: HomeModel) : Home, KoinComponent {
    private val manager by inject<HomeManager>()
    private val repo by inject<HomeRepository>()
    private val teleport by inject<TeleportManager>()

    override val id: UUID = model.id
    override var name: String = model.name
    override var icon: Material? = model.icon
    override val createdAt: Instant = Instant.ofEpochMilli(model.createdAt)
    override var location: Location =
        requireNotNull(model.location.location) {
            loadFailed(id, "Failed to load location ${model.location}")
        }
    override val owner: OfflinePlayer =
        requireNotNull(Bukkit.getOfflinePlayer(model.owner)) {
            loadFailed(id, "Failed to load OfflinePlayer ${model.owner}")
        }
    override var isStarred: Boolean = model.isStarred
    override var isPreferred: Boolean = model.isPreferred
    override val isLoaded: Boolean
        get() = manager.isLoaded(id)

    override fun teleport(player: Player, prompt: Boolean) {
        submitAsync {
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

        (manager.getPreferredHome(owner) as HomeImpl?)?.let {
            if (it == this) return
            it.isPreferred = false
            it.update()
        }

        isPreferred = true
        update()
    }

    override suspend fun teleportSuspend(player: Player, prompt: Boolean) {
        async {
            val options = teleport.getWorldTeleportOptions(location.world).copy(disableSafeCheck = true)
            // 必须异步触发
            val event = HomeTeleportEvent(player, player.location, this@HomeImpl).apply { callEvent() }
            if (event.isCancelled) return@async
            teleport.teleportSuspend(player, location, options, prompt)
        }
    }

    private fun toModel() = model.copy(
        id = id,
        name = name,
        icon = icon,
        createdAt = createdAt.toEpochMilli(),
        location = location.model,
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