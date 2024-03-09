package ink.pmc.common.misc

import ink.pmc.common.misc.api.MiscAPI
import ink.pmc.common.misc.api.sit.SitManager
import ink.pmc.common.misc.impl.MiscAPIImpl
import ink.pmc.common.misc.impl.SitManagerImpl
import ink.pmc.common.misc.listeners.PlayerListener
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.PaperCommandManager

lateinit var commandManager: PaperCommandManager<CommandSender>
lateinit var plugin: JavaPlugin
lateinit var sitManager: SitManager
var disabled = true

class MiscPlugin : JavaPlugin() {

    override fun onEnable() {
        plugin = this
        disabled = false

        commandManager = PaperCommandManager.createNative(
            this,
            ExecutionCoordinator.asyncCoordinator()
        )

        sitManager = SitManagerImpl()

        MiscAPI.instance = MiscAPIImpl
        MiscAPIImpl.internalSitManager = sitManager

        commandManager.registerBrigadier()
        commandManager.command(suicideCommand)
        commandManager.command(sitCommand)

        server.pluginManager.registerEvents(PlayerListener, this)
    }

    override fun onDisable() {
        sitManager.standAll()
        disabled = true
    }

}