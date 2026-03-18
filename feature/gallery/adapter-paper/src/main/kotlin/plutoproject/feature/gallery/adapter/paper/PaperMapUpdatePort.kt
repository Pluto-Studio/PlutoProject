package plutoproject.feature.gallery.adapter.paper

import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.world.level.saveddata.maps.MapId
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import org.bukkit.Bukkit
import plutoproject.feature.gallery.core.MapUpdate
import plutoproject.feature.gallery.core.MapUpdatePort
import plutoproject.framework.paper.util.entity.sendPacket
import java.util.UUID

class PaperMapUpdatePort : MapUpdatePort {
    override fun send(playerId: UUID, update: MapUpdate) {
        val player = Bukkit.getPlayer(playerId) ?: return
        val packet = ClientboundMapItemDataPacket(
            MapId(update.mapId),
            0,
            false,
            emptyList(),
            MapItemSavedData.MapPatch(0, 0, 128, 128, update.mapColors),
        )
        player.sendPacket(packet)
    }
}
