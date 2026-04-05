package plutoproject.feature.gallery.core.display.job

import java.util.UUID

class SendJobRegistry(
    private val sendJobFactory: SendJobFactory,
) {
    private val lock = Any()
    private val jobsByPlayerId = HashMap<UUID, SendJob>()

    fun get(playerId: UUID): SendJob? {
        return synchronized(lock) {
            jobsByPlayerId[playerId]
        }
    }

    fun start(playerId: UUID): SendJob {
        return synchronized(lock) {
            jobsByPlayerId.getOrPut(playerId) {
                sendJobFactory.create(playerId)
            }
        }
    }

    fun stop(playerId: UUID): SendJob? {
        return synchronized(lock) {
            jobsByPlayerId.remove(playerId)
        }?.also(SendJob::stop)
    }

    fun close() {
        val jobs = synchronized(lock) {
            jobsByPlayerId.values.toList().also { jobsByPlayerId.clear() }
        }
        jobs.forEach(SendJob::stop)
    }
}
