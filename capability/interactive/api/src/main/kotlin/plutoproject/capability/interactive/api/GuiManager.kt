package plutoproject.capability.interactive.api

import org.bukkit.entity.Player
import plutoproject.capability.interactive.api.node.InventoryNode

typealias GuiInventoryScope = GuiScope<InventoryNode>

interface GuiManager {
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
