package plutoproject.feature.paper.sit.player

import org.bukkit.Location
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.Player
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

    private fun createSeatEntity(location: Location): AreaEffectCloud? {
        val entity = location.world.spawn(location, AreaEffectCloud::class.java) {
            it.duration = Int.MAX_VALUE
            it.radius = 0f
            it.isInvisible = true
            it.isInvulnerable = true
            it.setGravity(false)
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

        println(" ")
        println("Begin addPlayer - index: $index, player: ${player.name}, options: $options, cause: $cause")

        if (contains(player)) {
            println("FAILED: Player ${player.name} already in this stack")
            if (callJoinEvent(player, options, cause, PlayerStackJoinAttemptResult.ALREADY_IN).isCancelled) {
                return PlayerStackJoinFinalResult.CANCELLED_BY_PLUGIN
            }
            return PlayerStackJoinFinalResult.ALREADY_IN
        }

        if (player.isInsideVehicle && !player.leaveVehicle()) {
            println("CANCELLED: Player ${player.name} is inside a vehicle, but leave vehicle operation was cancelled by other plugins")
            return PlayerStackJoinFinalResult.CANCELLED_BY_PLUGIN
        }

        val currentPlayer = getPlayer(index)
        val playerBelow = getPlayer(index - 1)

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
        }

        internalPlayers.add(index, player)
        println("Updated list: $internalPlayers")

        if (options.playSitSound) {
            player.playSitSound()
        }

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
        println(" ")
        println("Begin removePlayer - index: $index, cause: $cause")
        println("Player at index $index is ${getPlayer(index)}")

        val player = getPlayer(index) ?: return false
        val playerAbove = getPlayer(index + 1)
        val playerBelow = getPlayer(index - 1)

        if (callQuitEvent(player, cause).isCancelled && cause.isCancellable) {
            return false
        }

        if (playerBelow != null) {
            println("There is a player below: ${playerBelow.name}")
            val playerSitContext = internalSit.getContext(player) as PassengerSitContext
            val seatEntity = playerSitContext.seatEntity
            if (!seatEntity.leaveVehicle()) {
                println("CANCELLED: ${player.name}'s seat entity leave vehicle operation was cancelled by other plugins")
                return false
            }
            seatEntity.remove()
            println("Removed ${player.name}'s seat entity")
        }

        if (playerAbove != null) {
            println("There is a player above: $playerAbove")
            val playerAboveSitContext = internalSit.getContext(player) as PassengerSitContext
            val seatEntity = playerAboveSitContext.seatEntity
            if (!seatEntity.leaveVehicle()) {
                println("CANCELLED: ${playerAbove.name}'s seat entity leave vehicle operation was cancelled by other plugins")
                return false
            }
            if (internalSit.isCarrier(player)) {
                println("${player.name} is carrier, make ${playerAbove.name} to be new carrier")
                if (!playerAbove.leaveVehicle()) {
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

        internalPlayers.remove(player)
        internalSit.removeContext(player)

        if (internalPlayers.isEmpty()) {
            destroy(PlayerStackDestroyCause.NO_PLAYER_LEFT)
        }

        println("End remove player")
        println(" ")

        return true
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

        if (callDestroyEvent(cause).isCancelled && cause.isCancellable) {
            return false
        }

        isValid = false

        while (internalPlayers.size > 0) {
            removePlayerOnTop(cause = PlayerStackQuitCause.STACK_DESTROY)
        }

        internalSit.removeStack(this)
        internalPlayers.clear()

        return true
    }
}
