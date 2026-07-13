package plutoproject.feature.sit.paper.block

import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import plutoproject.feature.sit.api.paper.block.BlockSit

interface InternalBlockSit : BlockSit {
    val sitContexts: MutableMap<Player, BlockSitContext>

    fun isSeatEntityInUse(entity: ArmorStand): Boolean

    fun getSeatEntityOwner(entity: ArmorStand): Player?
}
