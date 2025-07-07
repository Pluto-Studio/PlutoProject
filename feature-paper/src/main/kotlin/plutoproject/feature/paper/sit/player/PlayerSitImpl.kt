package plutoproject.feature.paper.sit.player

import org.bukkit.NamespacedKey
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sit.SitOptions
import plutoproject.feature.paper.api.sit.player.PlayerStack
import plutoproject.feature.paper.api.sit.player.PlayerStackDestroyCause
import plutoproject.feature.paper.api.sit.player.StackOptions
import plutoproject.feature.paper.api.sit.player.events.PlayerStackCreateEvent
import plutoproject.feature.paper.sit.player.contexts.CarrierSitContext
import plutoproject.feature.paper.sit.player.contexts.PassengerSitContext
import plutoproject.feature.paper.sit.player.contexts.PlayerSitContext
import plutoproject.framework.common.api.options.OptionsManager
import plutoproject.framework.common.util.data.collection.toImmutable
import plutoproject.framework.paper.util.plugin

class PlayerSitImpl : InternalPlayerSit {
    private val internalStacks = mutableSetOf<PlayerStack>()
    private val sitContext = mutableMapOf<Player, PlayerSitContext>()

    override val stacks: Collection<PlayerStack> = internalStacks.toImmutable()
    override val seatEntityMarkerKey = NamespacedKey(plugin, "sit.player_sit_marker")

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

    override fun isSeatEntityInUse(entity: AreaEffectCloud): Boolean {
        if (!isTemporarySeatEntity(entity)) return false
        return getSeatEntityOwner(entity) != null
    }

    override fun getSeatEntityOwner(entity: AreaEffectCloud): Player? {
        if (!isTemporarySeatEntity(entity)) return null
        return sitContext.entries.firstOrNull {
            val passengerContext = it.value as? PassengerSitContext
            passengerContext != null && passengerContext.seatEntity == entity
        }?.key
    }

    private fun callStackCreateEvent(carrier: Player, options: StackOptions): PlayerStackCreateEvent {
        return PlayerStackCreateEvent(carrier, options).apply { callEvent() }
    }

    override fun createStack(carrier: Player, options: StackOptions): PlayerStack? {
        if (callStackCreateEvent(carrier, options).isCancelled) {
            return null
        }

        val stack = PlayerStackImpl(carrier, options)
        setContext(carrier, CarrierSitContext(stack = stack))
        internalStacks.add(stack)

        return stack
    }

    override fun destroyStack(stack: PlayerStack, cause: PlayerStackDestroyCause): Boolean {
        return stack.destroy(cause)
    }

    override fun getStack(player: Player): PlayerStack? {
        return getContext(player)?.stack
    }

    override fun getOptions(player: Player): SitOptions? {
        if (getContext(player) == null || getContext(player) !is PassengerSitContext) {
            return null
        }
        val context = getContext(player) as PassengerSitContext
        return context.options
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

    override fun isTemporarySeatEntity(entity: Entity): Boolean {
        return entity.persistentDataContainer.has(seatEntityMarkerKey)
    }

    override suspend fun isFeatureEnabled(player: Player): Boolean {
        val default = PlayerSitOptionDescriptor.defaultValue!!
        val options = OptionsManager.getOptions(player.uniqueId) ?: return default
        return options.getEntry(PlayerSitOptionDescriptor)?.value ?: default
    }

    override suspend fun toggleFeature(player: Player, state: Boolean) {
        val options = OptionsManager.getOptionsOrCreate(player.uniqueId)
        options.setEntry(PlayerSitOptionDescriptor, state)
        options.save()
    }
}
