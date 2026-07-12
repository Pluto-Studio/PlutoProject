package plutoproject.capability.worldalias.paper.worldalias

import org.bukkit.World
import plutoproject.capability.worldalias.paper.config.WorldAliasConfig
import plutoproject.capability.worldalias.api.worldalias.WorldAlias

class WorldAliasImpl(private val config: WorldAliasConfig) : WorldAlias {
    override fun getAlias(world: World): String? {
        return config.aliases[world.name]
    }

    override fun getAliasOrName(world: World): String {
        return getAlias(world) ?: world.name
    }
}
