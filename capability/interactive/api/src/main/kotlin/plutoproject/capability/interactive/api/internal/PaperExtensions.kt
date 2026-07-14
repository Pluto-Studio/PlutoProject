package plutoproject.capability.interactive.api.internal

import net.minecraft.network.protocol.common.ClientboundClearDialogPacket
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

fun Player.clearDialog() {
    (this as CraftPlayer).handle.connection.send(ClientboundClearDialogPacket.INSTANCE)
}
