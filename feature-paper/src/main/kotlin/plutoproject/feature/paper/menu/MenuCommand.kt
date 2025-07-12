package plutoproject.feature.paper.menu

import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.menu.screens.MenuScreen
import plutoproject.framework.common.util.chat.palettes.mochaLavender
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.paper.api.interactive.startScreen
import plutoproject.framework.paper.util.command.ensurePlayer

@Suppress("UNUSED")
object MenuCommand : KoinComponent {
    private val migrator by inject<PersistMigrator>()

    @Command("menu")
    fun CommandSender.menu() = ensurePlayer {
        startScreen(MenuScreen())
    }

    @Command("menu migrate")
    @Permission("menu.migrate")
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
