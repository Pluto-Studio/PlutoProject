package plutoproject.feature.teleport.api.paper

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.player.PlayerEvent

@Suppress("UNUSED")
abstract class AbstractTeleportEvent(
    player: Player,
    val from: Location,
    val to: Location
) : PlayerEvent(player, true), Cancellable
