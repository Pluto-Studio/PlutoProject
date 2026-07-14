package plutoproject.capability.worldalias.api.worldalias

import org.bukkit.World

interface WorldAlias {
    fun getAlias(world: World): String?

    fun getAliasOrName(world: World): String
}
