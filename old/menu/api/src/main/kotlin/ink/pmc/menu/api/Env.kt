package ink.pmc.menu.api

import org.bukkit.Bukkit

inline val isMenuAvailable: Boolean
    get() = Bukkit.getPluginManager().getPlugin("plutoproject_menu") != null