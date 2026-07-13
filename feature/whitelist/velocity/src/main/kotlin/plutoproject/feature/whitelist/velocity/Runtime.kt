package plutoproject.feature.whitelist.velocity

import plutoproject.kernel.api.currentModuleContext

internal val moduleScope
    get() = currentModuleContext().coroutineScope
