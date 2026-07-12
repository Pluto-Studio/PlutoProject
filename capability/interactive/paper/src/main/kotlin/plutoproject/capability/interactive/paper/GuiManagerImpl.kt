package plutoproject.capability.interactive.paper

import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.CoroutineScope
import org.bukkit.entity.Player
import plutoproject.capability.interactive.paper.inventory.InventoryScope
import plutoproject.capability.interactive.api.ComposableFunction
import plutoproject.capability.interactive.api.GuiInventoryScope
import plutoproject.capability.interactive.api.GuiManager
import plutoproject.capability.interactive.api.GuiScope
import plutoproject.capability.interactive.api.InteractiveScreen
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

class GuiManagerImpl(
    private val moduleScope: CoroutineScope,
    private val logger: Logger,
) : GuiManager {
    private val inventoryScopes = ConcurrentHashMap<Player, GuiInventoryScope>()

    override fun get(player: Player): GuiScope<*>? = inventoryScopes[player]

    override fun getInventory(player: Player): GuiInventoryScope? = inventoryScopes[player]

    override fun has(player: Player): Boolean = get(player) != null

    override fun hasInventory(player: Player): Boolean = getInventory(player) != null

    private fun disposeExistedScope(player: Player) {
        if (!has(player)) return
        logger.log(
            Level.WARNING,
            "Player ${player.name} has running Inventory/Form scope, disposing it before launch another",
        )
        dispose(player)
    }

    override fun startInventory(player: Player, contents: ComposableFunction): GuiInventoryScope {
        disposeExistedScope(player)
        return InventoryScope(player, contents, this, moduleScope, logger).also { inventoryScopes[player] = it }
    }

    override fun startScreen(player: Player, screen: InteractiveScreen) {
        startInventory(player) { Navigator(screen) }
    }

    override fun removeScope(scope: GuiScope<*>) {
        if (!scope.isDisposed) scope.dispose()
        inventoryScopes.values.remove(scope)
    }

    override fun dispose(player: Player) {
        get(player)?.dispose()
    }

    override fun disposeAll() {
        inventoryScopes.values.forEach { it.dispose() }
        inventoryScopes.clear()
    }
}
