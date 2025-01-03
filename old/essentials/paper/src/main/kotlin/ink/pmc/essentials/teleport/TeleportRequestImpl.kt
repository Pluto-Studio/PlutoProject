package ink.pmc.essentials.teleport

import ink.pmc.essentials.*
import ink.pmc.essentials.api.teleport.*
import ink.pmc.essentials.api.teleport.TeleportDirection.COME
import ink.pmc.essentials.api.teleport.TeleportDirection.GO
import ink.pmc.framework.chat.replace
import ink.pmc.framework.platform.paperThread
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.*

class TeleportRequestImpl(
    override val options: RequestOptions,
    override val source: Player,
    override val destination: Player,
    override val direction: TeleportDirection
) : TeleportRequest, KoinComponent {
    private val manager by inject<TeleportManager>()

    override val id: UUID = UUID.randomUUID()
    override val createdAt: Instant = Instant.now()
    override var state: RequestState = RequestState.WAITING
    override val isFinished: Boolean
        get() = state != RequestState.WAITING

    override fun accept(prompt: Boolean) {
        check(Thread.currentThread() != paperThread) { "Request operations can be only performed asynchronously" }
        if (isFinished) {
            return
        }

        if (!RequestStateChangeEvent(this, state, RequestState.ACCEPTED).callEvent()) return
        state = RequestState.ACCEPTED

        when (direction) {
            GO -> manager.teleport(source, destination, prompt = prompt)
            COME -> manager.teleport(destination, source, prompt = prompt)
        }

        if (!prompt) {
            return
        }

        source.sendMessage(
            TELEPORT_REQUEST_ACCEPTED_SOURCE
                .replace("<player>", destination.name)
        )
    }

    override fun deny(prompt: Boolean) {
        check(Thread.currentThread() != paperThread) { "Request operations can be only performed asynchronously" }
        if (isFinished) {
            return
        }

        if (!RequestStateChangeEvent(this, state, RequestState.DENYED).callEvent()) return
        state = RequestState.DENYED

        if (!prompt) {
            return
        }

        source.sendMessage(
            TELEPORT_REQUEST_DENIED_SOURCE
                .replace("<player>", destination.name)
        )
        source.playSound(TELEPORT_REQUEST_DENIED_SOUND)
    }

    override fun expire(prompt: Boolean) {
        check(Thread.currentThread() != paperThread) { "Request operations can be only performed asynchronously" }
        if (isFinished) {
            return
        }

        if (!RequestStateChangeEvent(this, state, RequestState.EXPIRED).callEvent()) return
        state = RequestState.EXPIRED

        if (!prompt) {
            return
        }

        source.sendMessage(
            TELEPORT_REQUEST_EXPIRED_SOURCE
                .replace("<player>", destination.name)
        )
        source.playSound(TELEPORT_REQUEST_CANCELLED_SOUND)
    }

    override fun cancel(prompt: Boolean) {
        check(Thread.currentThread() != paperThread) { "Request operations can be only performed asynchronously" }
        if (isFinished) {
            return
        }

        if (!RequestStateChangeEvent(this, state, RequestState.CANCELED).callEvent()) return
        state = RequestState.CANCELED

        if (!prompt) {
            return
        }

        destination.sendMessage(
            TELEPORT_REQUEST_CANCELED
                .replace("<player>", source.name)
        )
        destination.playSound(TELEPORT_REQUEST_CANCELLED_SOUND)
    }
}