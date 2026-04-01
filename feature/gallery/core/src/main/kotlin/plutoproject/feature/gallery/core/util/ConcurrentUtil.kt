package plutoproject.feature.gallery.core.util

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend fun checkpoint() {
    currentCoroutineContext().ensureActive()
}
