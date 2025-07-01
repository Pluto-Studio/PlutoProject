package plutoproject.feature.paper.api.sitV2.events

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.player.PlayerEvent
import plutoproject.feature.paper.api.sitV2.SitOptions

abstract class StandUpEvent(
    player: Player,
    val options: SitOptions
) : PlayerEvent(player), Cancellable
