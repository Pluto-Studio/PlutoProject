package plutoproject.platform.velocity

import plutoproject.framework.common.FrameworkCommonModule
import plutoproject.framework.common.util.coroutine.shutdownCoroutineEnvironment
import plutoproject.framework.common.util.inject.modifyExistedKoinOrCreate
import plutoproject.framework.velocity.FrameworkVelocityModule
import plutoproject.framework.velocity.disableFrameworkModules
import plutoproject.framework.velocity.enableFrameworkModules
import plutoproject.framework.velocity.loadFrameworkModules

class PlutoVelocityPlatform() {
    fun load() {
        modifyExistedKoinOrCreate {
            modules(FrameworkCommonModule, FrameworkVelocityModule)
        }
        loadFrameworkModules()
    }

    fun enable() {
        enableFrameworkModules()
    }

    fun disable() {
        disableFrameworkModules()
        shutdownCoroutineEnvironment()
    }
}
