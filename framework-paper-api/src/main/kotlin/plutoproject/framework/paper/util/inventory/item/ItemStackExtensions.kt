package plutoproject.framework.paper.util.inventory.item

import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.inventory.ItemStack

typealias PacketItemStack = com.github.retrooper.packetevents.protocol.item.ItemStack

fun ItemStack.toPacket(): PacketItemStack = SpigotConversionUtil.fromBukkitItemStack(this)
