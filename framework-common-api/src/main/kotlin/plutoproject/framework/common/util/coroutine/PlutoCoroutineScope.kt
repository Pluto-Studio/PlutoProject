package plutoproject.framework.common.util.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object PlutoCoroutineScope : CoroutineScope by CoroutineScope(Dispatchers.Default)
