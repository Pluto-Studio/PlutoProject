package ink.pmc.essentials.warp

import ink.pmc.advkt.component.text
import ink.pmc.advkt.showTitle
import ink.pmc.advkt.title.*
import ink.pmc.essentials.TELEPORT_SUCCEED_SOUND
import ink.pmc.essentials.api.teleport.TeleportManager
import ink.pmc.essentials.api.warp.Warp
import ink.pmc.essentials.api.warp.WarpCategory
import ink.pmc.essentials.api.warp.WarpTeleportEvent
import ink.pmc.essentials.api.warp.WarpType
import ink.pmc.essentials.home.loadFailed
import ink.pmc.essentials.models.WarpModel
import ink.pmc.essentials.repositories.WarpRepository
import ink.pmc.framework.concurrent.async
import ink.pmc.framework.concurrent.submitAsync
import ink.pmc.framework.player.uuid
import ink.pmc.framework.storage.model
import ink.pmc.framework.time.formatDate
import ink.pmc.framework.time.zoneId
import ink.pmc.framework.chat.mochaText
import ink.pmc.framework.chat.mochaYellow
import kotlinx.coroutines.Deferred
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.util.Ticks
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus.Internal
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

class WarpImpl(private val model: WarpModel) : Warp, KoinComponent {
    private val repo by inject<WarpRepository>()
    private val teleport by inject<TeleportManager>()

    override val id: UUID = model.id
    override val name: String = model.name
    override var alias: String? = model.alias
    override var founderId = model.founder?.uuid
    override val founder: Deferred<OfflinePlayer>?
        get() = founderId?.let { submitAsync<OfflinePlayer> { Bukkit.getOfflinePlayer(it) } }
    override var icon: Material? = model.icon
    override var category: WarpCategory? = model.category
    override var description: Component? =
        model.description?.let { MiniMessage.miniMessage().deserialize(model.description) }
    override var type: WarpType = model.type @Internal set
    override val createdAt: Instant = Instant.ofEpochMilli(model.createdAt)
    override var location: Location =
        requireNotNull(model.location.location) {
            loadFailed(id, "Failed to get location ${model.location}")
        }
    override val isSpawn: Boolean
        get() = type == WarpType.SPAWN || type == WarpType.SPAWN_DEFAULT
    override val isDefaultSpawn: Boolean
        get() = type == WarpType.SPAWN_DEFAULT

    override fun teleport(player: Player, prompt: Boolean) {
        submitAsync {
            teleportSuspend(player, prompt)
        }
    }

    override suspend fun teleportSuspend(player: Player, prompt: Boolean) {
        async {
            val options = teleport.getWorldTeleportOptions(location.world).copy(disableSafeCheck = true)
            // 必须异步触发
            val event = WarpTeleportEvent(player, player.location, this@WarpImpl).apply { callEvent() }
            if (event.isCancelled) return@async
            teleport.teleportSuspend(player, location, options, false)
            if (prompt) {
                val founderName = founder?.await()?.name
                player.showTitle {
                    times {
                        fadeIn(Ticks.duration(5))
                        stay(Ticks.duration(35))
                        fadeOut(Ticks.duration(20))
                    }
                    mainTitle {
                        text(alias ?: name) with mochaYellow
                    }
                    subTitle {
                        if (founderName != null) {
                            text("$founderName ") with mochaText
                        }
                        val time = ZonedDateTime.ofInstant(createdAt, player.zoneId)
                        text("设于 ${time.formatDate()}") with mochaText
                    }
                }
                player.playSound(TELEPORT_SUCCEED_SOUND)
            }
        }
    }

    private fun toModel(): WarpModel = model.copy(
        alias = alias,
        founder = founderId?.toString(),
        icon = icon,
        category = category,
        description = description?.let { MiniMessage.miniMessage().serialize(it) },
        type = type,
        createdAt = createdAt.toEpochMilli(),
        location = location.model,
    )

    override suspend fun update() {
        repo.update(toModel())
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Warp) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}