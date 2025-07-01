package plutoproject.feature.paper.api.sitV2

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import plutoproject.framework.common.util.inject.Koin

interface Sit {
    companion object : Sit by Koin.get()

    fun getState(player: Player): SitState

    fun getBlockSatOn(player: Player): Block?

    fun getPlayerSatOn(player: Player): Player?

    fun getSitOptions(player: Player): SitOptions?

    fun sitOnBlock(sitter: Player, target: Block, sitOptions: SitOptions = SitOptions()): SitResult

    fun sitOnBlock(sitter: Player, target: Location, sitOptions: SitOptions = SitOptions()): SitResult
}
