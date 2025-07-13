package plutoproject.framework.common.util.coroutine

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import java.util.logging.Logger

private val logger = Logger.getLogger("PlutoProject/Coroutine")

@OptIn(DelicateCoroutinesApi::class)
fun shutdownCoroutineEnvironment() {
    if (PlutoCoroutineScope.isActive) {
        PlutoCoroutineScope.cancel()
    }
    waitForCompletion()
    Dispatchers.shutdown()
}

private fun waitForCompletion() {
    logger.info("Waiting 1s for coroutine completion...")
    Thread.sleep(1000)
}
