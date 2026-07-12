package plutoproject.capability.interactive.api.animations

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.bukkit.Material
import plutoproject.capability.interactive.api.internal.LoadingIconAnimation
import kotlin.time.Duration.Companion.seconds

@Composable
fun loadingIconAnimation(): Material {
    val animation = remember { LoadingIconAnimation() }
    var frame by remember { mutableStateOf(animation.currentFrame) }
    LaunchedEffect(Unit) {
        while (this.isActive) {
            delay(1.seconds)
            frame = animation.nextFrame()
        }
    }
    return frame
}
