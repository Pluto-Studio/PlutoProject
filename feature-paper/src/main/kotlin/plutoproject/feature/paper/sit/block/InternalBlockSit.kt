package plutoproject.feature.paper.sit.block

import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sit.block.BlockSit

interface InternalBlockSit : BlockSit {
    val sitContexts: MutableMap<Player, BlockSitContext>

    fun isSeatEntityInUse(entity: ArmorStand): Boolean
}
