package plutoproject.kernel.common.callback

import plutoproject.kernel.api.currentModuleContextOrNull

internal object ClosedContextCallback {
    fun lookup() = currentModuleContextOrNull()
}
