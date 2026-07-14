package plutoproject.feature.afk.paper

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.afk.api.paper.AfkManager
import plutoproject.foundation.paper.command.ensurePlayer
import plutoproject.kernel.api.koinInject

@Suppress("UNUSED")
object AfkCommand {
    private val manager by koinInject<AfkManager>()

    @Command("afk")
    @Permission("plutoproject.afk.command.afk")
    fun CommandSender.afk() = ensurePlayer {
        manager.toggle(this, true)
    }
}
