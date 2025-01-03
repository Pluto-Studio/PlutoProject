package ink.pmc.misc

import com.electronwill.nightconfig.core.file.FileConfig
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import ink.pmc.framework.command.annotationParser
import ink.pmc.framework.command.commandManager
import ink.pmc.framework.inject.modifyExistedKoinOrCreate
import ink.pmc.misc.api.elevator.ElevatorManager
import ink.pmc.misc.api.sit.SitManager
import ink.pmc.misc.commands.SitCommand
import ink.pmc.misc.commands.SuicideCommand
import ink.pmc.misc.impl.elevator.ElevatorManagerImpl
import ink.pmc.misc.impl.elevator.builders.IronElevatorBuilder
import ink.pmc.misc.impl.sit.SitManagerImpl
import ink.pmc.misc.listeners.CreeperAntiExplodeListener
import ink.pmc.misc.listeners.ElevatorListener
import ink.pmc.misc.listeners.SitListener
import org.bukkit.plugin.java.JavaPlugin
import org.koin.dsl.module
import java.io.File

lateinit var plugin: JavaPlugin
lateinit var fileConfig: FileConfig
var disabled = true

@Suppress("UNUSED")
class PaperPlugin : JavaPlugin() {

    private val bukkitModule = module {
        single<SitManager> {
            check(MiscConfig.sit) { "Sit not enabled" }
            SitManagerImpl()
        }
        single<ElevatorManager> {
            check(MiscConfig.elevator) { "Elevator not enabled" }
            ElevatorManagerImpl()
        }
    }

    override fun onEnable() {
        plugin = this
        disabled = false

        val config = File(dataFolder, "config.conf")

        if (!config.exists()) {
            saveResource("config.conf", false)
        }

        fileConfig = config.loadConfig()
        loadConfigValues()

        modifyExistedKoinOrCreate {
            modules(bukkitModule)
        }

        commandManager().annotationParser().apply {
            parse(SuicideCommand)
            if (MiscConfig.sit) parse(SitCommand)
        }

        if (MiscConfig.sit) {
            server.pluginManager.registerSuspendingEvents(SitListener, this)
            runSitCheckTask()
            runActionBarOverrideTask()
        }

        if (MiscConfig.elevator) {
            if (MiscConfig.elevatorRegisterDefaultBuilder) {
                ElevatorManager.registerBuilder(IronElevatorBuilder)
            }
            server.pluginManager.registerSuspendingEvents(ElevatorListener, this)
        }

        if (MiscConfig.creeperAntiExplode) {
            server.pluginManager.registerSuspendingEvents(CreeperAntiExplodeListener, this)
        }
    }

    override fun onDisable() {
        SitManager.standAll()
        disabled = true
    }

    private fun loadConfigValues() {
        MiscConfig.sit = fileConfig.get("sit.enabled")
        MiscConfig.elevator = fileConfig.get("elevator.enabled")
        MiscConfig.elevatorRegisterDefaultBuilder = fileConfig.get("elevator.register-default-builder")
        MiscConfig.creeperAntiExplode = fileConfig.get("creeper-anti-explode.enabled")
        MiscConfig.creeperAntiExplodeFirework = fileConfig.get("creeper-anti-explode.firework")
    }

    private fun File.loadConfig(): FileConfig {
        return FileConfig.builder(this)
            .async()
            .autoreload()
            .onAutoReload { loadConfigValues() }
            .build()
            .apply { load() }
    }

}