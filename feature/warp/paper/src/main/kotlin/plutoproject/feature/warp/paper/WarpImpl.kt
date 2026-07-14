package plutoproject.feature.warp.paper

import plutoproject.foundation.paper.world.toModel

import plutoproject.feature.warp.paper.profileLookup

import ink.pmc.advkt.component.text
import ink.pmc.advkt.showTitle
import ink.pmc.advkt.title.*
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.util.Ticks
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus.Internal
import plutoproject.kernel.api.koinInject
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.feature.warp.api.paper.Warp
import plutoproject.feature.warp.api.paper.WarpCategory
import plutoproject.feature.warp.api.paper.WarpTeleportEvent
import plutoproject.feature.warp.api.paper.WarpType
import plutoproject.capability.profile.api.Profile
import plutoproject.capability.profile.api.ProfileLookup
import plutoproject.foundation.common.text.mochaText
import plutoproject.foundation.common.text.mochaYellow
import plutoproject.foundation.common.serialization.uuid
import plutoproject.foundation.common.time.formatDate
import plutoproject.feature.warp.paper.timezone
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

class WarpImpl(private val model: WarpModel) : Warp {
    private val repo by koinInject<WarpRepository>()

    override val id: UUID = model.id
    override val name: String = model.name
    override var alias: String? = model.alias
    override var founderId = model.founder?.uuid()
    override val founder: Deferred<Profile>?
        get() = founderId?.let {
            moduleScope.async(Dispatchers.IO) {
                profileLookup.lookupByUuid(it)!!
            }
        }
    override var icon: Material? = model.icon
    override var category: WarpCategory? = model.category
    override var description: Component? =
        model.description?.let { MiniMessage.miniMessage().deserialize(model.description) }
    override var type: WarpType = model.type @Internal set
    override val createdAt: Instant = Instant.ofEpochMilli(model.createdAt)
    override var location: Location =
        requireNotNull(model.location.toLocation()) {
            "Failed to load Warp $id: failed to get location ${model.location}"
        }
    override val isSpawn: Boolean
        get() = type == WarpType.SPAWN || type == WarpType.SPAWN_DEFAULT
    override val isDefaultSpawn: Boolean
        get() = type == WarpType.SPAWN_DEFAULT

    override fun teleport(player: Player, prompt: Boolean) {
        moduleScope.launch {
            teleportSuspend(player, prompt)
        }
    }

    override suspend fun teleportSuspend(player: Player, prompt: Boolean) {
        withContext(Dispatchers.Default) {
            val options = teleportManager.getWorldTeleportOptions(location.world).copy(disableSafeCheck = true)
            // 必须异步触发
            val event = WarpTeleportEvent(player, player.location, this@WarpImpl).apply { callEvent() }
            if (event.isCancelled) return@withContext
            teleportManager.teleportSuspend(player, location, options, false)
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
                player.playSound(WARP_TELEPORT_SUCCEED_SOUND)
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
