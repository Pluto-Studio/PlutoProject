package plutoproject.framework.velocity.bridge

import com.velocitypowered.api.command.CommandSource
import net.kyori.adventure.text.Component
import org.incendo.cloud.annotation.specifier.Quoted
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.framework.common.api.bridge.player.BridgePlayer
import plutoproject.framework.common.bridge.AbstractBridgeCommand
import java.time.Duration
import kotlin.time.toKotlinDuration

@Suppress("UNUSED")
object BridgeCommand : AbstractBridgeCommand<CommandSource>() {
    override fun CommandSource.message(component: Component) = sendMessage(component)

    @Command("bridgeproxy list_servers")
    @Permission("bridge.command")
    fun CommandSource.commandListServers() = listServers()

    @Command("bridgeproxy list_players")
    @Permission("bridge.command")
    suspend fun CommandSource.commandListPlayers() = listPlayers()

    @Command("bridgeproxy player <player> teleport <other>")
    @Permission("bridge.command")
    suspend fun CommandSource.commandTeleport(
        player: BridgePlayer,
        other: BridgePlayer
    ) = teleport(player, other)

    @Command("bridgeproxy player <player> send_message <message>")
    @Permission("bridge.command")
    suspend fun CommandSource.commandSendMessage(
        player: BridgePlayer,
        @Argument("message", parserName = "bridge-component") message: Component
    ) = sendMessage(player, message)

    @Command("bridgeproxy player <player> show_title <mainTitle> [subTitle] [fadeIn] [stay] [fadeOut]")
    @Permission("bridge.command")
    suspend fun commandShowTitle(
        sender: CommandSource,
        player: BridgePlayer,
        @Argument("mainTitle", parserName = "bridge-component") mainTitle: Component,
        @Argument("subTitle", parserName = "bridge-component") subTitle: Component = Component.empty(),
        fadeIn: Duration = Duration.ofMillis(500),
        stay: Duration = Duration.ofMillis(3500),
        fadeOut: Duration = Duration.ofMillis(1000),
    ) = sender.showTitle(
        player,
        mainTitle,
        subTitle,
        fadeIn.toKotlinDuration(),
        stay.toKotlinDuration(),
        fadeOut.toKotlinDuration()
    )

    @Command("bridgeproxy player <player> play_sound <key> [volume] [pitch]")
    @Permission("bridge.command")
    suspend fun commandPlaySound(
        sender: CommandSource,
        player: BridgePlayer,
        @Quoted key: String,
        volume: Float = 1F,
        pitch: Float = 1F
    ) = sender.playSound(player, key, volume, pitch)

    @Command("bridgeproxy player <player> perform_command <command>")
    @Permission("bridge.command")
    suspend fun commandPerformCommand(
        sender: CommandSource,
        player: BridgePlayer,
        @Quoted command: String
    ) = sender.performCommand(player, command)

    @Command("bridgeproxy list_worlds")
    @Permission("bridge.command")
    fun CommandSource.commandListWorlds() = listWorlds()
}
