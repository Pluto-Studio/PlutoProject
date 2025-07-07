package plutoproject.feature.paper.sit.player

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.sit.SitOptions
import plutoproject.feature.paper.api.sit.player.*
import plutoproject.feature.paper.api.sit.player.events.PlayerStackDestroyEvent
import plutoproject.feature.paper.api.sit.player.events.PlayerStackJoinEvent
import plutoproject.feature.paper.api.sit.player.events.PlayerStackQuitEvent
import plutoproject.feature.paper.sit.playSitSound
import plutoproject.feature.paper.sit.player.contexts.CarrierSitContext
import plutoproject.feature.paper.sit.player.contexts.PassengerSitContext
import plutoproject.feature.paper.sit.player.contexts.PlayerSitContext
import plutoproject.framework.common.util.data.collection.toImmutable

class PlayerStackImpl(carrier: Player, override val options: StackOptions) : PlayerStack, KoinComponent {
    private val internalSit by inject<InternalPlayerSit>()
    private val internalPlayers = mutableListOf(carrier)
    private var isInDestroy = false

    override val carrier: Player
        get() = internalPlayers.first()
    override val players: Collection<Player> = internalPlayers.toImmutable()
    override var isValid: Boolean = true

    override fun getPlayer(index: Int): Player? {
        return internalPlayers.getOrNull(index)
    }

    override fun getPlayerOnTop(): Player {
        return internalPlayers.last()
    }

    override fun getPlayerAtBottom(): Player {
        return internalPlayers.first()
    }

    override fun contains(player: Player): Boolean {
        return internalPlayers.contains(player)
    }

    override fun indexOf(player: Player): Int? {
        val index = internalPlayers.indexOf(player)
        return if (index >= 0) index else null
    }

    private fun createSeatEntity(location: Location): AreaEffectCloud? {
        val level = (location.world as CraftWorld).handle
        val seatEntity = SeatEntity(location)
        if (!level.addFreshEntity(seatEntity)) {
            return null
        }
        val bukkitEntity = location.world.getEntity(seatEntity.uuid)!!.apply {
            persistentDataContainer.set(internalSit.seatEntityMarkerKey, PersistentDataType.BOOLEAN, true)
        }
        return if (bukkitEntity.isValid) bukkitEntity as AreaEffectCloud else null
    }

    private fun callJoinEvent(
        player: Player,
        options: SitOptions,
        cause: PlayerStackJoinCause,
        attemptResult: PlayerStackJoinAttemptResult
    ): PlayerStackJoinEvent {
        return PlayerStackJoinEvent(player, this, options, cause, attemptResult).apply { callEvent() }
    }

    private fun moveCurrentPlayerUp(currentPlayer: Player?, player: Player): Boolean {
        if (currentPlayer == null) return true

        val currentPlayerSeat = when (val currentPlayerContext = internalSit.getContext(currentPlayer)) {
            is CarrierSitContext -> {
                if (currentPlayer.isInsideVehicle && !currentPlayer.leaveVehicle()) return false

                val seat = createSeatEntity(currentPlayer.location) ?: return false
                if (!seat.addPassenger(currentPlayer)) {
                    seat.remove()
                    return false
                }

                internalSit.setContext(currentPlayer, PassengerSitContext(this, seat, SitOptions()))
                seat
            }

            is PassengerSitContext -> currentPlayerContext.seatEntity

            else -> error("Unexpected context type for currentPlayer.")
        }

        if (currentPlayerSeat.isInsideVehicle && !currentPlayerSeat.leaveVehicle()) return false
        if (!player.addPassenger(currentPlayerSeat)) return false

        return true
    }

    private fun attachPlayerToBelowPlayer(
        player: Player,
        playerBelow: Player?,
        options: SitOptions
    ): Boolean {
        if (playerBelow == null) {
            internalSit.setContext(player, CarrierSitContext(this))
            return true
        }

        val seat = createSeatEntity(player.location) ?: return false
        if (!seat.addPassenger(player)) return false
        if (!playerBelow.addPassenger(seat)) return false

        internalSit.setContext(player, PassengerSitContext(this, seat, options))
        return true
    }

    override fun addPlayer(
        index: Int,
        player: Player,
        options: SitOptions,
        cause: PlayerStackJoinCause
    ): PlayerStackJoinFinalResult {
        check(isValid) { "PlayerStack instance already destroyed." }
        check(index >= 0) { "Index must be greater or equal to 0." }

        if (contains(player)) {
            if (callJoinEvent(player, options, cause, PlayerStackJoinAttemptResult.ALREADY_IN).isCancelled) {
                return PlayerStackJoinFinalResult.CANCELLED_BY_PLUGIN
            }
            return PlayerStackJoinFinalResult.ALREADY_IN
        }

        val currentPlayer = getPlayer(index)
        val playerBelow = getPlayer(index - 1)

        InternalOperationMarker.apply {
            markInOperation(player)
            currentPlayer?.let { markInOperation(it) }
            playerBelow?.let { markInOperation(it) }
        }

        if (callJoinEvent(player, options, cause, PlayerStackJoinAttemptResult.SUCCESS).isCancelled) {
            return PlayerStackJoinFinalResult.CANCELLED_BY_PLUGIN
        }

        try {
            if (!moveCurrentPlayerUp(currentPlayer, player)) {
                return PlayerStackJoinFinalResult.CANCELLED_BY_PLUGIN
            }
            if (!attachPlayerToBelowPlayer(player, playerBelow, options)) {
                return PlayerStackJoinFinalResult.CANCELLED_BY_PLUGIN
            }
        } finally {
            InternalOperationMarker.apply {
                unmarkInOperation(player)
                currentPlayer?.let(::unmarkInOperation)
                playerBelow?.let(::unmarkInOperation)
            }
        }

        if (options.playSitSound) {
            player.playSitSound()
        }

        internalPlayers.add(index, player)
        return PlayerStackJoinFinalResult.SUCCESS
    }

    override fun addPlayerOnTop(
        player: Player,
        options: SitOptions,
        cause: PlayerStackJoinCause
    ): PlayerStackJoinFinalResult {
        return addPlayer(internalPlayers.lastIndex + 1, player, options, cause)
    }

    override fun addPlayerAtBottom(
        player: Player,
        options: SitOptions,
        cause: PlayerStackJoinCause
    ): PlayerStackJoinFinalResult {
        return addPlayer(0, player, options, cause)
    }

    private fun callQuitEvent(player: Player, cause: PlayerStackQuitCause): PlayerStackQuitEvent {
        return PlayerStackQuitEvent(player, this, cause).apply { callEvent() }
    }

    private fun detachFromBelowPlayer(
        playerBelow: Player?,
        context: PlayerSitContext?
    ): Boolean {
        if (playerBelow == null) return true
        val passengerContext = context as? PassengerSitContext ?: return false
        val seatEntity = passengerContext.seatEntity

        if (seatEntity.isValid && seatEntity.isInsideVehicle && !seatEntity.leaveVehicle()) return false
        seatEntity.remove()
        return true
    }

    private fun relinkAbovePlayer(
        playerAbove: Player?,
        playerBelow: Player?,
        player: Player
    ): Boolean {
        if (playerAbove == null) return true
        val aboveContext = internalSit.getContext(playerAbove) as? PassengerSitContext ?: return false
        val seatEntity = aboveContext.seatEntity

        if (seatEntity.isValid && seatEntity.isInsideVehicle && !seatEntity.leaveVehicle()) return false

        return if (internalSit.isCarrier(player)) {
            if (playerAbove.isInsideVehicle && !playerAbove.leaveVehicle()) return false
            seatEntity.remove()
            internalSit.setContext(playerAbove, CarrierSitContext(this))
            true
        } else {
            playerBelow?.addPassenger(seatEntity) ?: false
        }
    }

    private fun handleRemovePlayerSounds(
        player: Player,
        playerAbove: Player?,
        context: PlayerSitContext?
    ) {
        when (context) {
            is CarrierSitContext -> if (options.playCastOffSound) playerAbove?.playSitSound()
            is PassengerSitContext -> if (context.options.playSitSound) player.playSitSound()
        }
    }

    override fun removePlayer(index: Int, cause: PlayerStackQuitCause): Boolean {
        check(isValid) { "PlayerStack instance already destroyed." }
        check(index >= 0) { "Index must be greater or equal to 0." }

        val player = getPlayer(index) ?: return false
        val playerAbove = getPlayer(index + 1)
        val playerBelow = getPlayer(index - 1)
        val playerSitContext = internalSit.getContext(player)

        InternalOperationMarker.apply {
            markInOperation(player)
            playerAbove?.let { markInOperation(it) }
            playerBelow?.let { markInOperation(it) }
        }

        if (callQuitEvent(player, cause).isCancelled && cause.isCancellable) {
            return false
        }

        try {
            if (!detachFromBelowPlayer(playerBelow, playerSitContext)) return false
            if (!relinkAbovePlayer(playerAbove, playerBelow, player)) return false
            handleRemovePlayerSounds(player, playerAbove, playerSitContext)
            player.sendActionBar(Component.empty())
        } finally {
            InternalOperationMarker.apply {
                unmarkInOperation(player)
                playerAbove?.let(::unmarkInOperation)
                playerBelow?.let(::unmarkInOperation)
            }
        }

        internalPlayers.remove(player)
        internalSit.removeContext(player)

        if (internalPlayers.isEmpty() && !isInDestroy) {
            destroy(PlayerStackDestroyCause.NO_PLAYER_LEFT)
        }

        return true
    }

    override fun removePlayer(player: Player, cause: PlayerStackQuitCause): Boolean {
        val index = indexOf(player) ?: return false
        return removePlayer(index, cause)
    }

    override fun removePlayerOnTop(cause: PlayerStackQuitCause): Boolean {
        return removePlayer(internalPlayers.lastIndex, cause)
    }

    override fun removePlayerAtBottom(cause: PlayerStackQuitCause): Boolean {
        return removePlayer(0, cause)
    }

    private fun callDestroyEvent(cause: PlayerStackDestroyCause): PlayerStackDestroyEvent {
        return PlayerStackDestroyEvent(this, cause).apply { callEvent() }
    }

    override fun destroy(cause: PlayerStackDestroyCause): Boolean {
        check(isValid) { "PlayerStack instance already destroyed." }

        isInDestroy = true
        if (callDestroyEvent(cause).isCancelled && cause.isCancellable) return false

        while (internalPlayers.size > 0) {
            removePlayerOnTop(cause = PlayerStackQuitCause.STACK_DESTROY)
        }

        isValid = false
        isInDestroy = false
        internalSit.removeStack(this)
        internalPlayers.clear()

        return true
    }
}
