package plutoproject.framework.paper.util.inventory.anvil

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import plutoproject.framework.paper.util.entity.nextContainerId
import plutoproject.framework.paper.util.inventory.item.PacketItemStack

typealias RenameHandler = (player: Player, text: String) -> Unit
typealias ClickHandler = (player: Player, clickType: ClickType, text: String) -> Unit

private const val ANVIL_INVENTORY_TYPE_ID = 8

class AnvilMenu(
    private val viewer: Player,
    title: Component,
    private val onRename: RenameHandler,
    private val onClick: ClickHandler,
) {
    private val user = PacketEvents.getAPI().playerManager.getUser(viewer) ?: error("Unexpected")
    private val containerId = viewer.nextContainerId()
    private var containerState = 0
    private var isBeingRefresh: Boolean = false
    private var _title = title
    private var text: String = ""
    private var inputLeft: PacketItemStack = PacketItemStack.EMPTY
    private var inputRight: PacketItemStack = PacketItemStack.EMPTY
    private var output: PacketItemStack = PacketItemStack.EMPTY

    fun open() {
        val openWindowPacket = WrapperPlayServerOpenWindow(containerId, ANVIL_INVENTORY_TYPE_ID, _title)
        val itemsPacket = WrapperPlayServerWindowItems(
            containerId,
            ++containerState,
            listOf(inputLeft, inputRight, output),
            null
        )
        user.sendPacket(openWindowPacket)
        user.sendPacket(itemsPacket)
    }

    private fun refresh(block: () -> Unit) {
        isBeingRefresh = true
        block()
        isBeingRefresh = false
    }
}
