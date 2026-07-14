package plutoproject.capability.interactive.paper

import net.kyori.adventure.audience.Audience
import java.util.logging.Level
import java.util.logging.Logger

inline fun <T> T.catchInteractiveException(
    logger: Logger,
    audience: Audience? = null,
    action: T.() -> Unit,
) {
    runCatching(action).onFailure {
        audience?.sendMessage(UI_INTERACTION_FAILED)
        logger.log(Level.SEVERE, "Exception caught while handling interaction", it)
    }
}
