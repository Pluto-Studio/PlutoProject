package plutoproject.feature.menu.paper

import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.kernel.api.koinInject
import plutoproject.feature.menu.paper.screens.MenuScreen
import plutoproject.foundation.common.text.mochaLavender
import plutoproject.foundation.common.text.mochaText
import plutoproject.feature.menu.paper.startScreen
import plutoproject.foundation.paper.command.ensurePlayer

@Suppress("UNUSED")
object MenuCommand : Any() {
    private val migrator by koinInject<PersistMigrator>()

    @Command("menu")
    @Permission("plutoproject.menu.command.menu")
    fun CommandSender.menu() = ensurePlayer {
        startScreen(MenuScreen())
    }

    @Command("menu migrate")
    @Permission("plutoproject.menu.command.menu.migrate")
    suspend fun migrate(sender: CommandSender) {
        sender.send {
            text("正在迁移到 DatabasePersist 存储...") with mochaText
        }
        val size = migrator.migrate().size
        sender.send {
            text("已成功迁移 ") with mochaText
            text("$size ") with mochaLavender
            text("个条目") with mochaText
        }
    }
}
