package plutoproject.feature.paper.sit.player

import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sit.player.PlayerStack
import plutoproject.feature.paper.api.sit.player.PlayerStackDestroyCause
import plutoproject.feature.paper.sit.player.contexts.CarrierSitContext
import plutoproject.feature.paper.sit.player.contexts.PassengerSitContext
import plutoproject.feature.paper.sit.player.contexts.PlayerSitContext
import plutoproject.framework.common.api.options.OptionsManager
import plutoproject.framework.common.util.data.collection.toImmutable

class PlayerSitImpl : InternalPlayerSit {
    private val internalStacks = mutableSetOf<PlayerStack>()
    private val sitContext = mutableMapOf<Player, PlayerSitContext>()

    override val stacks: Collection<PlayerStack> = internalStacks.toImmutable()

    override fun getContext(player: Player): PlayerSitContext? {
        return sitContext[player]
    }

    override fun removeContext(player: Player) {
        sitContext.remove(player)
    }

    override fun setContext(player: Player, newContext: PlayerSitContext) {
        sitContext[player] = newContext
    }

    override fun removeStack(stack: PlayerStack) {
        internalStacks.remove(stack)
    }

    override fun createStack(carrier: Player, initialPassenger: Player): PlayerStack? {
        TODO("Not yet implemented")
    }

    override fun destroyStack(stack: PlayerStack, cause: PlayerStackDestroyCause): Boolean {
        return stack.destroy(cause)
    }

    override fun getStack(player: Player): PlayerStack? {
        return getContext(player)?.stack
    }

    override fun isInStack(player: Player): Boolean {
        return getStack(player) != null
    }

    override fun isCarrier(player: Player): Boolean {
        val context = getContext(player) ?: return false
        return context is CarrierSitContext
    }

    override fun isPassenger(player: Player): Boolean {
        val context = getContext(player) ?: return false
        return context is PassengerSitContext
    }

    override suspend fun isFeatureEnabled(player: Player): Boolean {
        val default = PlayerSitOptionDescriptor.defaultValue!!
        val options = OptionsManager.getOptions(player.uniqueId) ?: return default
        return options.getEntry(PlayerSitOptionDescriptor)?.value ?: default
    }
}
