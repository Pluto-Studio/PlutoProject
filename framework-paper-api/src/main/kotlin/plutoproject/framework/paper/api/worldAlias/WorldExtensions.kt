package plutoproject.framework.paper.api.worldAlias

import org.bukkit.World

val World.alias: String?
    get() = WorldAlias.getAlias(this)

val World.aliasOrName: String
    get() = WorldAlias.getAliasOrName(this)
