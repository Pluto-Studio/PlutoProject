package ink.pmc.menu.hook

import net.coreprotect.config.ConfigHandler
import org.bukkit.Bukkit
import org.bukkit.entity.Player

const val CO_NEAR_COMMAND = "coreprotect:co near"

val isCoreProtectAvailable: Boolean
    get() = Bukkit.getPluginManager().getPlugin("CoreProtect") != null

var Player.isInspecting: Boolean
    set(value) {
        if (!isCoreProtectAvailable) return
        if (value) {
            ConfigHandler.inspecting[name] = true
        } else {
            ConfigHandler.inspecting.remove(name)
        }
    }
    get() = if (!isCoreProtectAvailable) false else ConfigHandler.inspecting.getOrDefault(name, false)