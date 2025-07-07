package plutoproject.feature.paper.sit.player

import net.kyori.adventure.text.Component
import org.bukkit.Location
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
        val entity = location.world.spawn(location, AreaEffectCloud::class.java) {
            it.duration = Int.MAX_VALUE
            it.radius = 0f
            it.isInvisible = true
            it.isInvulnerable = true
            it.setGravity(false)
            it.persistentDataContainer.set(internalSit.seatEntityMarkerKey, PersistentDataType.BOOLEAN, true)
        }
        return if (entity.isValid) entity else null
    }

    private fun callJoinEvent(
        player: Player,
        options: SitOptions,
        cause: PlayerStackJoinCause,
        attemptResult: PlayerStackJoinAttemptResult
    ): PlayerStackJoinEvent {
        return PlayerStackJoinEvent(player, this, options, cause, attemptResult).apply { callEvent() }
    }

    override fun addPlayer(
        index: Int,
        player: Player,
        options: SitOptions,
        cause: PlayerStackJoinCause
    ): PlayerStackJoinFinalResult {
        check(isValid) { "PlayerStack instance already destroyed." }
        check(index >= 0) { "Index must be greater or equal to 0." }

        println(" ")
        println("Begin addPlayer - index: $index, player: ${player.name}, options: $options, cause: $cause")
        println("Current internalPlayers: ${internalPlayers.map { it.name }}")

        if (contains(player)) {
            println("FAILED: Player ${player.name} already in this stack")
            if (callJoinEvent(player, options, cause, PlayerStackJoinAttemptResult.ALREADY_IN).isCancelled) {
                return PlayerStackJoinFinalResult.CANCELLED_BY_PLUGIN
            }
            return PlayerStackJoinFinalResult.ALREADY_IN
        }

        if (index > 0 && player.isInsideVehicle && !player.leaveVehicle()) {
            println("CANCELLED: Player ${player.name} is inside a vehicle, but leave vehicle operation was cancelled by other plugins")
            return PlayerStackJoinFinalResult.CANCELLED_BY_PLUGIN
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

        if (currentPlayer != null) {
            println("Current player at index $index is ${currentPlayer.name}")
            val currentPlayerSeat = when (val currentPlayerContext = internalSit.getContext(currentPlayer)) {
                is CarrierSitContext -> {
                    println("Player ${currentPlayer.name} is carrier")
                    if (currentPlayer.isInsideVehicle && !currentPlayer.leaveVehicle()) {
                        println("CANCELLED: ${player.name} is inside a vehicle, but leave vehicle operation was cancelled by other plugins")
                        return PlayerStackJoinFinalResult.CANCELLED_BY_PLUGIN
                    }
                    createSeatEntity(currentPlayer.location)?.also {
                        println("Create a seat entity for ${currentPlayer.name}")
                        if (!it.addPassenger(currentPlayer)) {
                            println("CANCELLED: mount ${currentPlayer.name} to his new seat entity was cancelled by other plugins")
                            it.remove()
                            return PlayerStackJoinFinalResult.CANCELLED_BY_PLUGIN
                        }
                        println("Update context to passenger for ${currentPlayer.name}")
                        internalSit.setContext(currentPlayer, PassengerSitContext(this, it, SitOptions()))
                    } ?: run {
                        println("CANCELLED: Seat entity for ${currentPlayer.name} failed to create")
                        return PlayerStackJoinFinalResult.CANCELLED_BY_PLUGIN
                    }
                }

                is PassengerSitContext -> {
                    println("Player ${currentPlayer.name} is a passenger")
                    currentPlayerContext.seatEntity
                }

                else -> error("Unexpected")
            }
            if (currentPlayerSeat.isInsideVehicle && !currentPlayerSeat.leaveVehicle()) {
                println("CANCELLED: ${currentPlayer.name}'s seat entity is inside a vehicle, but leave vehicle operation was cancelled by other plugins")
                return PlayerStackJoinFinalResult.CANCELLED_BY_PLUGIN
            }
            if (!player.addPassenger(currentPlayerSeat)) {
                println("CANCELLED: mount ${currentPlayer.name}'s seat entity to ${player.name} was cancelled by other plugins")
                return PlayerStackJoinFinalResult.CANCELLED_BY_PLUGIN
            }
        }

        if (playerBelow != null) {
            println("There is a player ${playerBelow.name} below")
            val seatEntity = createSeatEntity(player.location) ?: run {
                println("CANCELLED: Seat entity for ${player.name} failed to create")
                return PlayerStackJoinFinalResult.CANCELLED_BY_PLUGIN
            }
            if (!seatEntity.addPassenger(player)) {
                println("CANCELLED: mount ${player.name} to his new seat entity was cancelled by other plugins")
                return PlayerStackJoinFinalResult.CANCELLED_BY_PLUGIN
            }
            if (!playerBelow.addPassenger(seatEntity)) {
                println("CANCELLED: mount ${player.name}'s new seat entity to ${playerBelow.name} was cancelled by other plugins")
                return PlayerStackJoinFinalResult.CANCELLED_BY_PLUGIN
            }
            internalSit.setContext(player, PassengerSitContext(this, seatEntity, options))
        } else {
            println("There isn't a player below, make ${player.name} as the new carrier")
            internalSit.setContext(player, CarrierSitContext(this))
        }

        if (options.playSitSound) {
            player.playSitSound()
        }

        InternalOperationMarker.apply {
            unmarkInOperation(player)
            currentPlayer?.let { unmarkInOperation(it) }
            playerBelow?.let { unmarkInOperation(it) }
        }

        internalPlayers.add(index, player)
        println("Current internalPlayers: ${internalPlayers.map { it.name }}")
        println("End addPlayer")
        println(" ")
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

    override fun removePlayer(index: Int, cause: PlayerStackQuitCause): Boolean {
        check(isValid) { "PlayerStack instance already destroyed." }
        check(index >= 0) { "Index must be greater or equal to 0." }

        println(" ")
        println("Begin removePlayer - index: $index, cause: $cause")
        println("Player at index $index is ${getPlayer(index)?.name}")
        println("Current internalPlayers: ${internalPlayers.map { it.name }}")

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

        if (playerBelow != null) {
            println("There is a player below: ${playerBelow.name}")
            val playerPassengerContext = playerSitContext as PassengerSitContext
            val seatEntity = playerPassengerContext.seatEntity
            if (seatEntity.isValid && seatEntity.isInsideVehicle && !seatEntity.leaveVehicle()) {
                println("CANCELLED: ${player.name}'s seat entity leave vehicle operation was cancelled by other plugins")
                return false
            }
            seatEntity.remove()
            println("Removed ${player.name}'s seat entity")
        }

        if (playerAbove != null) {
            println("There is a player above: ${playerAbove.name}")
            val playerAboveSitContext = internalSit.getContext(playerAbove) as PassengerSitContext
            val seatEntity = playerAboveSitContext.seatEntity
            if (seatEntity.isValid && seatEntity.isInsideVehicle && !seatEntity.leaveVehicle()) {
                println("CANCELLED: ${playerAbove.name}'s seat entity leave vehicle operation was cancelled by other plugins")
                return false
            }
            if (internalSit.isCarrier(player)) {
                println("${player.name} is carrier, make ${playerAbove.name} to be new carrier")
                if (playerAbove.isInsideVehicle && !playerAbove.leaveVehicle()) {
                    println("CANCELLED: ${playerAbove.name} leave vehicle operation was cancelled by other plugins")
                    return false
                }
                seatEntity.remove()
                internalSit.setContext(playerAbove, CarrierSitContext(this))
                println("Update ${playerAbove.name}'s sit context to carrier context")
            } else {
                println("${player.name} isn't carrier, mount ${playerAbove.name}'s seat entitiy to ${playerBelow?.name}")
                if (!playerBelow!!.addPassenger(seatEntity)) {
                    return false
                }
            }
        }

        if (playerSitContext is PassengerSitContext && playerSitContext.options.playSitSound) {
            player.playSitSound()
        }
        player.sendActionBar(Component.empty())

        InternalOperationMarker.apply {
            unmarkInOperation(player)
            playerAbove?.let { unmarkInOperation(it) }
            playerBelow?.let { unmarkInOperation(it) }
        }
        internalPlayers.remove(player)
        internalSit.removeContext(player)

        if (internalPlayers.isEmpty() && !isInDestroy) {
            destroy(PlayerStackDestroyCause.NO_PLAYER_LEFT)
        }

        println("Current internalPlayers: ${internalPlayers.map { it.name }}")
        println("End remove player")
        println(" ")

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

        println("Begin destroy - cause: $cause")
        isInDestroy = true

        if (callDestroyEvent(cause).isCancelled && cause.isCancellable) {
            println("CANCELLED: destroy cancelled by other plugins")
            return false
        }

        while (internalPlayers.size > 0) {
            println("There are ${internalPlayers.size} players left, remove them one by one")
            removePlayerOnTop(cause = PlayerStackQuitCause.STACK_DESTROY)
        }

        isValid = false
        isInDestroy = false
        println("Mark invalid")
        internalSit.removeStack(this)
        println("Remove stack")
        internalPlayers.clear()
        println("Cleared players, now: ${internalPlayers.map { it.name }}")

        return true
    }
}
