package plutoproject.feature.paper.api.sit.player

import org.bukkit.entity.Player
import plutoproject.framework.common.util.inject.Koin

interface PlayerSit {
    companion object : PlayerSit by Koin.get()

    val stacks: Collection<PlayerStack>

    fun createStack(carrier: Player, options: StackOptions = StackOptions()): PlayerStack?

    fun destroyStack(stack: PlayerStack, cause: PlayerStackDestroyCause = PlayerStackDestroyCause.PLUGIN): Boolean

    fun getStack(player: Player): PlayerStack?

    fun isInStack(player: Player): Boolean

    fun isCarrier(player: Player): Boolean

    fun isPassenger(player: Player): Boolean

    suspend fun isFeatureEnabled(player: Player): Boolean
}
