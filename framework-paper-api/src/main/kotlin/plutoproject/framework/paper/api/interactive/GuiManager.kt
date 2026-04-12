package plutoproject.framework.paper.api.interactive

import org.bukkit.entity.Player
import plutoproject.framework.common.util.inject.globalKoin
import plutoproject.framework.paper.api.interactive.node.InventoryNode

typealias GuiInventoryScope = GuiScope<InventoryNode>

interface GuiManager {
    companion object : GuiManager by globalKoin.get()

    fun get(player: Player): GuiScope<*>?

    fun getInventory(player: Player): GuiInventoryScope?

    fun has(player: Player): Boolean

    fun hasInventory(player: Player): Boolean

    fun startInventory(player: Player, contents: ComposableFunction): GuiInventoryScope

    fun startScreen(player: Player, screen: InteractiveScreen)

    fun removeScope(scope: GuiScope<*>)

    fun dispose(player: Player)

    fun disposeAll()
}
