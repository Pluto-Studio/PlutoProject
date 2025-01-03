package ink.pmc.misc

import net.kyori.adventure.text.TextReplacementConfig
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

fun handleJoinMessage(event: PlayerJoinEvent) {
    val player = event.player
    val name = player.displayName()

    val nameReplacement = TextReplacementConfig.builder()
        .match("<player>")
        .replacement(name)
        .build()

    event.joinMessage(JOIN_FORMAT.replaceText(nameReplacement))
}

fun handleQuitMessage(event: PlayerQuitEvent) {
    val player = event.player
    val name = player.displayName()

    val nameReplacement = TextReplacementConfig.builder()
        .match("<player>")
        .replacement(name)
        .build()

    event.quitMessage(QUIT_FORMAT.replaceText(nameReplacement))
}