package ink.pmc.daily.commands

import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import ink.pmc.daily.PERMISSION_CMD_DAILY
import ink.pmc.daily.plugin
import ink.pmc.framework.chat.mochaPink
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

@Suppress("UNUSED")
object DailyCommand {
    @Command("daily reload")
    @Permission(PERMISSION_CMD_DAILY)
    fun CommandSender.reload() {
        plugin.reload()
        send {
            text("已重载配置文件") with mochaPink
        }
    }
}