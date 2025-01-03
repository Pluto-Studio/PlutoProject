package plutoproject.framework.paper.worldalias

import org.bukkit.World
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.paper.api.worldalias.WorldAlias
import plutoproject.framework.paper.config.WorldAliasConfig

class WorldAliasImpl : WorldAlias, KoinComponent {
    private val config by inject<WorldAliasConfig>()

    override fun getAlias(world: World): String? {
        return config.aliases[world.name]
    }

    override fun getAliasOrName(world: World): String {
        return getAlias(world) ?: world.name
    }
}
