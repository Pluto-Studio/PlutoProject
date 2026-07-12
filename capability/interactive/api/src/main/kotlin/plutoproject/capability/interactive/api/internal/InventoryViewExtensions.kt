package plutoproject.capability.interactive.api.internal

import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftContainer
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryView

fun InventoryView.updateTitle(component: Component) {
    val player = player as Player
    val serverPlayer = (player as CraftPlayer).handle
    val packet = ClientboundOpenScreenPacket(
        serverPlayer.containerMenu.containerId,
        CraftContainer.getNotchInventoryType(topInventory),
        PaperAdventure.asVanilla(component),
    )
    serverPlayer.connection.send(packet)
    serverPlayer.containerMenu.sendAllDataToRemote()
}
