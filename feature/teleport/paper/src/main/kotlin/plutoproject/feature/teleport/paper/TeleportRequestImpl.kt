package plutoproject.feature.teleport.paper

import org.bukkit.entity.Player
import plutoproject.kernel.api.koinInject
import plutoproject.feature.teleport.api.paper.*
import plutoproject.feature.teleport.api.paper.events.RequestStateChangeEvent
import plutoproject.foundation.common.text.replace
import org.bukkit.Bukkit
import java.time.Instant
import java.util.*

class TeleportRequestImpl(
    override val options: RequestOptions,
    override val source: Player,
    override val destination: Player,
    override val direction: TeleportDirection
) : TeleportRequest {
    override val id: UUID = UUID.randomUUID()
    override val createdAt: Instant = Instant.now()
    override var state: RequestState = RequestState.WAITING
    override val isFinished: Boolean
        get() = state != RequestState.WAITING

    override fun accept(prompt: Boolean) {
        check(!Bukkit.isPrimaryThread()) { "Request operations can be only performed asynchronously" }
        if (isFinished) {
            return
        }

        if (!RequestStateChangeEvent(this, state, RequestState.ACCEPTED).callEvent()) return
        state = RequestState.ACCEPTED

        when (direction) {
            TeleportDirection.GO -> teleportManager.teleport(source, destination, prompt = prompt)
            TeleportDirection.COME -> teleportManager.teleport(destination, source, prompt = prompt)
        }

        if (!prompt) {
            return
        }

        source.sendMessage(TELEPORT_REQUEST_ACCEPTED_SOURCE.replace("<player>", destination.name))
    }

    override fun deny(prompt: Boolean) {
        check(!Bukkit.isPrimaryThread()) { "Request operations can be only performed asynchronously" }
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
        check(!Bukkit.isPrimaryThread()) { "Request operations can be only performed asynchronously" }
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
        check(!Bukkit.isPrimaryThread()) { "Request operations can be only performed asynchronously" }
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
