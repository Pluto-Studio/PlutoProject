package plutoproject.framework.common.util.coroutine

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import java.util.logging.Logger

private val logger = Logger.getLogger("framework/common/util/coroutine")

@OptIn(DelicateCoroutinesApi::class)
fun shutdownCoroutineEnvironment() {
    if (PlutoCoroutineScope.isActive) {
        PlutoCoroutineScope.cancel()
    }
    Dispatchers.shutdown()
    waitFinalize()
}

private fun waitFinalize() {
    logger.info("Wait 1s for finalizing...")
    Thread.sleep(1000)
}
