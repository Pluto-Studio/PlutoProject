package plutoproject.framework.paper.api.worldalias

import org.bukkit.World
import plutoproject.framework.common.util.inject.globalKoin

interface WorldAlias {
    companion object : WorldAlias by globalKoin.get()

    fun getAlias(world: World): String?

    fun getAliasOrName(world: World): String
}
