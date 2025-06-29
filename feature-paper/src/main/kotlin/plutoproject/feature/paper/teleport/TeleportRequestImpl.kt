package plutoproject.feature.paper.teleport

import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import plutoproject.feature.paper.api.teleport.*
import plutoproject.feature.paper.api.teleport.events.RequestStateChangeEvent
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.serverThread
import java.time.Instant
import java.util.*

class TeleportRequestImpl(
    override val options: RequestOptions,
    override val source: Player,
    override val destination: Player,
    override val direction: TeleportDirection
) : TeleportRequest, KoinComponent {
    override val id: UUID = UUID.randomUUID()
    override val createdAt: Instant = Instant.now()
    override var state: RequestState = RequestState.WAITING
    override val isFinished: Boolean
        get() = state != RequestState.WAITING

    override fun accept(prompt: Boolean) {
        check(Thread.currentThread() != serverThread) { "Request operations can be only performed asynchronously" }
        if (isFinished) {
            return
        }

        if (!RequestStateChangeEvent(this, state, RequestState.ACCEPTED).callEvent()) return
        state = RequestState.ACCEPTED

        when (direction) {
            TeleportDirection.GO -> TeleportManager.teleport(source, destination, prompt = prompt)
            TeleportDirection.COME -> TeleportManager.teleport(destination, source, prompt = prompt)
        }

        if (!prompt) {
            return
        }

        source.sendMessage(TELEPORT_REQUEST_ACCEPTED_SOURCE.replace("<player>", destination.name))
    }

    override fun deny(prompt: Boolean) {
        check(Thread.currentThread() != serverThread) { "Request operations can be only performed asynchronously" }
        if (isFinished) {
            return
        }

        if (!RequestStateChangeEvent(this, state, RequestState.DENIED).callEvent()) return
        state = RequestState.DENIED

        if (!prompt) {
            return
        }

        source.sendMessage(TELEPORT_REQUEST_DENIED_SOURCE.replace("<player>", destination.name))
        source.playSound(TELEPORT_REQUEST_DENIED_SOUND)
    }

    override fun expire(prompt: Boolean) {
        check(Thread.currentThread() != serverThread) { "Request operations can be only performed asynchronously" }
        if (isFinished) {
            return
        }

        if (!RequestStateChangeEvent(this, state, RequestState.EXPIRED).callEvent()) return
        state = RequestState.EXPIRED

        if (!prompt) {
            return
        }

        source.sendMessage(TELEPORT_REQUEST_EXPIRED_SOURCE.replace("<player>", destination.name))
        source.playSound(TELEPORT_REQUEST_CANCELLED_SOUND)
    }

    override fun cancel(prompt: Boolean) {
        check(Thread.currentThread() != serverThread) { "Request operations can be only performed asynchronously" }
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
