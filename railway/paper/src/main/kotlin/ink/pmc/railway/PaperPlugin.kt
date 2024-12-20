package ink.pmc.railway

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import ink.pmc.framework.inject.modifyExistedKoinOrCreate
import org.bukkit.plugin.Plugin
import org.koin.dsl.module

internal lateinit var plugin: Plugin

class PaperPlugin : SuspendingJavaPlugin() {
    private val bukkitModule = module {

    }

    override fun onEnable() {
        plugin = this
        modifyExistedKoinOrCreate {
            modules(bukkitModule)
        }
    }

    override fun onDisable() {

    }
}