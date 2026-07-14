package plutoproject.kernel.api.paper

import org.bukkit.plugin.Plugin
import plutoproject.kernel.api.ModuleContext

interface PaperModuleContext : ModuleContext {
    val plugin: Plugin
}
