package plutoproject.feature.velocity.whitelist_v2

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class KnownVisitors {
    private val set = ConcurrentHashMap.newKeySet<UUID>()

    fun add(uniqueId: UUID) {
        set.add(uniqueId)
    }

    fun remove(uniqueId: UUID) {
        set.remove(uniqueId)
    }

    fun contains(uniqueId: UUID): Boolean {
        return set.contains(uniqueId)
    }
}
