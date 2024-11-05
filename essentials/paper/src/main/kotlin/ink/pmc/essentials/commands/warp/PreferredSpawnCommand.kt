package ink.pmc.essentials.commands.warp

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import ink.pmc.essentials.COMMAND_PREFERRED_SPAWN_FAILED_ALREADY
import ink.pmc.essentials.COMMAND_PREFERRED_SPAWN_SUCCEED
import ink.pmc.essentials.COMMAND_PREFERRED_SPAWN_WARP_IS_NOT_SPAWN
import ink.pmc.essentials.api.warp.Warp
import ink.pmc.essentials.api.warp.WarpManager
import ink.pmc.framework.utils.chat.replace
import ink.pmc.framework.utils.command.ensurePlayer
import ink.pmc.framework.utils.concurrent.submitAsync
import ink.pmc.framework.utils.visual.mochaSubtext0
import ink.pmc.framework.utils.visual.mochaText
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.exception.ExceptionHandler
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser.FutureArgumentParser
import org.incendo.cloud.suggestion.SuggestionProvider
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull

@Suppress("UNUSED", "UNUSED_PARAMETER", "UnusedReceiverParameter")
object PreferredSpawnCommand {
    @Command("preferredspawn <spawn>")
    @Permission("essentials.defaultspawn")
    suspend fun CommandSender.preferredSpawn(@Argument("spawn", parserName = "spawn") spawn: Warp) = ensurePlayer {
        val current = WarpManager.getPreferredSpawn(this)
        if (current?.name == spawn.name) {
            sendMessage(
                COMMAND_PREFERRED_SPAWN_FAILED_ALREADY.replace("<name>", if (spawn.alias != null) component {
                    text("${spawn.alias} ") with mochaText
                    text("(${spawn.name})") with mochaSubtext0
                } else Component.text(spawn.name)))
            return
        }
        WarpManager.setPreferredSpawn(this, spawn)
        sendMessage(
            COMMAND_PREFERRED_SPAWN_SUCCEED.replace("<name>", if (spawn.alias != null) component {
                text("${spawn.alias} ") with mochaText
                text("(${spawn.name})") with mochaSubtext0
            } else Component.text(spawn.name)))
    }

    @ExceptionHandler(WarpIsNotSpawnException::class)
    fun CommandSender.warpIsNotSpawn(exception: WarpIsNotSpawnException) {
        sendMessage(COMMAND_PREFERRED_SPAWN_WARP_IS_NOT_SPAWN.replace("<name>", exception.name))
    }
}

class SpawnParser : FutureArgumentParser<CommandSender, Warp> {
    private val warpParser = WarpParser(false)

    override fun parseFuture(
        commandContext: CommandContext<CommandSender>,
        commandInput: CommandInput
    ): CompletableFuture<ArgumentParseResult<Warp>> = submitAsync<ArgumentParseResult<Warp>> {
        val warp = warpParser.parseFuture(commandContext, commandInput).await()
            .also {
                it.failure().getOrNull()?.also { e -> throw e }
            }.parsedValue().getOrNull() ?: error("Error while parsing spawn")
        if (warp.isSpawn) {
            ArgumentParseResult.success(warp)
        } else {
            throw WarpIsNotSpawnException(warp.name)
        }
    }.asCompletableFuture()

    override fun suggestionProvider(): SuggestionProvider<CommandSender> {
        return warpParser
    }
}

class WarpIsNotSpawnException(val name: String) : Exception()