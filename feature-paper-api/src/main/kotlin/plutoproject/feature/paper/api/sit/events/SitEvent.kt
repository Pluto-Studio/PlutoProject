package plutoproject.feature.paper.api.sit.events

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.player.PlayerEvent
import plutoproject.feature.paper.api.sit.SitAttemptResult
import plutoproject.feature.paper.api.sit.SitOptions

abstract class SitEvent(
    player: Player,
    val options: SitOptions,
    val expectedResult: SitAttemptResult,
) : PlayerEvent(player), Cancellable
