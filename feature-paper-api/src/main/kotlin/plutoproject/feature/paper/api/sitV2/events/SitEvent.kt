package plutoproject.feature.paper.api.sitV2.events

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.player.PlayerEvent
import plutoproject.feature.paper.api.sitV2.SitOptions
import plutoproject.feature.paper.api.sitV2.SitResult

abstract class SitEvent(
    player: Player,
    val options: SitOptions,
    val result: SitResult,
) : PlayerEvent(player), Cancellable
