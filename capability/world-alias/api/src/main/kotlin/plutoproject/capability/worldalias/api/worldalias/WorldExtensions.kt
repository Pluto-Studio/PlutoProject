package plutoproject.capability.worldalias.api.worldalias

import org.bukkit.World

fun World.alias(worldAlias: WorldAlias): String? = worldAlias.getAlias(this)

fun World.aliasOrName(worldAlias: WorldAlias): String = worldAlias.getAliasOrName(this)
