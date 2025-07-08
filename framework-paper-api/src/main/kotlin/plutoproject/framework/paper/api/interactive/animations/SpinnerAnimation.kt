package plutoproject.framework.paper.api.interactive.animations

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import plutoproject.framework.common.util.animation.SpinnerAnimation
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun spinnerAnimation(): Char {
    val animation = remember { SpinnerAnimation() }
    var frame by remember { mutableStateOf(animation.currentFrame) }
    LaunchedEffect(Unit) {
        while (this.isActive) {
            delay(100.milliseconds)
            frame = animation.nextFrame()
        }
    }
    return frame
}
