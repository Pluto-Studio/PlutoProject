package plutoproject.feature.paper.sitV2

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sitV2.SitOptions

data class SitContext(
    val block: Block?,
    val blockTopSurfaceY: Double?, // 部分方块有状态变化，变化后就无法正确获取顶面高度了，所以预先存储
    val targetPlayer: Player?,
    val armorStand: ArmorStand?,
    val options: SitOptions
)
