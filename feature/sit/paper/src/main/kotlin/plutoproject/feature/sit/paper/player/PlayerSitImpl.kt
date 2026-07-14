package plutoproject.feature.sit.paper.player

import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import plutoproject.feature.sit.api.paper.SitOptions
import plutoproject.feature.sit.api.paper.player.PlayerStack
import plutoproject.feature.sit.api.paper.player.PlayerStackDestroyCause
import plutoproject.feature.sit.api.paper.player.StackOptions
import plutoproject.feature.sit.api.paper.player.events.PlayerStackCreateEvent
import plutoproject.feature.sit.paper.PLAYER_SIT_TOGGLE_PERSIST_KEY
import plutoproject.feature.sit.paper.player.contexts.CarrierSitContext
import plutoproject.feature.sit.paper.player.contexts.PassengerSitContext
import plutoproject.feature.sit.paper.player.contexts.PlayerSitContext
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.capability.databasepersist.api.adapters.BooleanTypeAdapter
import plutoproject.foundation.common.collection.toImmutable
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.koinInject
import plutoproject.kernel.api.paper.PaperModuleContext

class PlayerSitImpl : InternalPlayerSit {
    private val databasePersist by koinInject<DatabasePersist>()
    private val internalStacks = mutableSetOf<PlayerStack>()
    private val sitContext = mutableMapOf<Player, PlayerSitContext>()

    override val stacks: Collection<PlayerStack> = internalStacks.toImmutable()
    override val seatEntityMarkerKey = NamespacedKey(
        (currentModuleContext() as PaperModuleContext).plugin,
        "sit.player_sit_marker",
    )

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
        val nmsEntity = (entity as CraftEntity).handle
        return nmsEntity is SeatEntity || entity.persistentDataContainer.has(seatEntityMarkerKey)
    }

    override suspend fun isFeatureEnabled(player: Player): Boolean {
        val container = databasePersist.getContainer(player.uniqueId)
        return container.getOrElse(PLAYER_SIT_TOGGLE_PERSIST_KEY, BooleanTypeAdapter) { true }
    }

    override suspend fun toggleFeature(player: Player, state: Boolean) {
        val container = databasePersist.getContainer(player.uniqueId)
        container.set(PLAYER_SIT_TOGGLE_PERSIST_KEY, BooleanTypeAdapter, state)
        container.save()
    }
}
