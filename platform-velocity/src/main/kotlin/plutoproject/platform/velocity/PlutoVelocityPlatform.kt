package plutoproject.platform.velocity

import plutoproject.framework.common.FrameworkCommonModule
import plutoproject.framework.common.api.feature.FeatureManager
import plutoproject.framework.common.util.coroutine.shutdownCoroutineEnvironment
import plutoproject.framework.common.util.inject.globalKoin
import plutoproject.framework.velocity.FrameworkVelocityModule
import plutoproject.framework.velocity.disableFrameworkModules
import plutoproject.framework.velocity.enableFrameworkModules
import plutoproject.framework.velocity.loadFrameworkModules

class PlutoVelocityPlatform() {
    fun load() {
        globalKoin {
            modules(FrameworkCommonModule, FrameworkVelocityModule)
        }
        loadFrameworkModules()
        FeatureManager.loadAll()
    }

    fun enable() {
        enableFrameworkModules()
        FeatureManager.enableAll()
    }

    fun disable() {
        FeatureManager.disableAll()
        disableFrameworkModules()
        shutdownCoroutineEnvironment()
    }
}
