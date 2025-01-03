package ink.pmc.runtime

import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer
import com.google.inject.Inject

@Suppress("UNUSED", "UNUSED")
class VelocityPlugin @Inject constructor(spc: SuspendingPluginContainer) {
    init {
        spc.initialize(this)
    }
}