package plutoproject.feature.paper.warp

import ink.pmc.advkt.component.text
import ink.pmc.advkt.showTitle
import ink.pmc.advkt.title.*
import kotlinx.coroutines.Deferred
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.util.Ticks
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus.Internal
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.teleport.TeleportManager
import plutoproject.feature.paper.api.warp.Warp
import plutoproject.feature.paper.api.warp.WarpCategory
import plutoproject.feature.paper.api.warp.WarpTeleportEvent
import plutoproject.feature.paper.api.warp.WarpType
import plutoproject.feature.paper.home.loadFailed
import plutoproject.feature.paper.teleport.TELEPORT_SUCCEED_SOUND
import plutoproject.framework.common.api.profile.Profile
import plutoproject.framework.common.api.profile.ProfileLookup
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.common.util.chat.palettes.mochaYellow
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.common.util.coroutine.runAsyncIO
import plutoproject.framework.common.util.coroutine.withDefault
import plutoproject.framework.common.util.data.convertToUuid
import plutoproject.framework.common.util.time.formatDate
import plutoproject.framework.paper.api.provider.timezone
import plutoproject.framework.paper.util.data.models.toModel
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

class WarpImpl(private val model: WarpModel) : Warp, KoinComponent {
    private val repo by inject<WarpRepository>()

    override val id: UUID = model.id
    override val name: String = model.name
    override var alias: String? = model.alias
    override var founderId = model.founder?.convertToUuid()
    override val founder: Deferred<Profile>?
        get() = founderId?.let { runAsyncIO { ProfileLookup.lookupByUuid(it)!! } }
    override var icon: Material? = model.icon
    override var category: WarpCategory? = model.category
    override var description: Component? =
        model.description?.let { MiniMessage.miniMessage().deserialize(model.description) }
    override var type: WarpType = model.type @Internal set
    override val createdAt: Instant = Instant.ofEpochMilli(model.createdAt)
    override var location: Location =
        requireNotNull(model.location.toLocation()) {
            loadFailed(id, "Failed to get location ${model.location}")
        }
    override val isSpawn: Boolean
        get() = type == WarpType.SPAWN || type == WarpType.SPAWN_DEFAULT
    override val isDefaultSpawn: Boolean
        get() = type == WarpType.SPAWN_DEFAULT

    override fun teleport(player: Player, prompt: Boolean) {
        runAsync {
            teleportSuspend(player, prompt)
        }
    }

    override suspend fun teleportSuspend(player: Player, prompt: Boolean) {
        withDefault {
            val options = TeleportManager.getWorldTeleportOptions(location.world).copy(disableSafeCheck = true)
            // 必须异步触发
            val event = WarpTeleportEvent(player, player.location, this@WarpImpl).apply { callEvent() }
            if (event.isCancelled) return@withDefault
            TeleportManager.teleportSuspend(player, location, options, false)
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
                        val time = ZonedDateTime.ofInstant(createdAt, player.timezone.toZoneId())
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
        location = location.toModel(),
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
