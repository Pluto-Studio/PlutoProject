package plutoproject.framework.paper.bridge

import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotation.specifier.Quoted
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.framework.common.api.bridge.player.BridgePlayer
import plutoproject.framework.common.bridge.AbstractBridgeCommand
import java.time.Duration
import kotlin.time.toKotlinDuration

@Suppress("UNUSED")
object BridgeCommand : AbstractBridgeCommand<CommandSender>() {
    override fun CommandSender.message(component: Component) {
        sendMessage(component)
    }

    @Command("bridgebackend list_servers")
    @Permission("bridge.command")
    fun CommandSender.commandListServers() {
        listServers()
    }

    @Command("bridgebackend list_players")
    @Permission("bridge.command")
    suspend fun CommandSender.commandListPlayers() {
        listPlayers()
    }

    @Command("bridgebackend player <player> teleport <other>")
    @Permission("bridge.command")
    suspend fun CommandSender.commandTeleport(
        player: BridgePlayer,
        other: BridgePlayer
    ) {
        teleport(player, other)
    }

    @Command("bridgebackend player <player> send_message <message>")
    @Permission("bridge.command")
    suspend fun CommandSender.commandSendMessage(
        player: BridgePlayer,
        @Argument("message", parserName = "bridge-component") message: Component
    ) {
        sendMessage(player, message)
    }

    @Command("bridgebackend player <player> show_title <mainTitle> [subTitle] [fadeIn] [stay] [fadeOut]")
    @Permission("bridge.command")
    suspend fun commandShowTitle(
        sender: CommandSender,
        player: BridgePlayer,
        @Argument("mainTitle", parserName = "bridge-component") mainTitle: Component,
        @Argument("subTitle", parserName = "bridge-component") subTitle: Component = Component.empty(),
        fadeIn: Duration = Duration.ofMillis(500),
        stay: Duration = Duration.ofMillis(3500),
        fadeOut: Duration = Duration.ofMillis(1000),
    ) {
        sender.showTitle(
            player,
            mainTitle,
            subTitle,
            fadeIn.toKotlinDuration(),
            stay.toKotlinDuration(),
            fadeOut.toKotlinDuration()
        )
    }

    @Command("bridgebackend player <player> play_sound <key> [volume] [pitch]")
    @Permission("bridge.command")
    suspend fun commandPlaySound(
        sender: CommandSender,
        player: BridgePlayer,
        @Quoted key: String,
        volume: Float = 1F,
        pitch: Float = 1F
    ) {
        sender.playSound(player, key, volume, pitch)
    }

    @Command("bridgebackend player <player> perform_command <command>")
    @Permission("bridge.command")
    suspend fun commandPerformCommand(
        sender: CommandSender,
        player: BridgePlayer,
        @Quoted command: String
    ) {
        sender.performCommand(player, command)
    }

    @Command("bridgebackend list_worlds")
    @Permission("bridge.command")
    fun CommandSender.commandListWorlds() {
        listWorlds()
    }
}
