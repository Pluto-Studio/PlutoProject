package plutoproject.framework.common.util.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

object PlutoCoroutineScope : CoroutineScope by CoroutineScope(SupervisorJob())
