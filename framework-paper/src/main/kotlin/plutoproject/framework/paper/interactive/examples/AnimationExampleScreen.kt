package plutoproject.framework.paper.interactive.examples

import androidx.compose.runtime.Composable
import net.kyori.adventure.text.Component
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.paper.api.interactive.InteractiveScreen
import plutoproject.framework.paper.api.interactive.animations.loadingIconAnimation
import plutoproject.framework.paper.api.interactive.animations.spinnerAnimation
import plutoproject.framework.paper.api.interactive.canvas.Menu
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.jetpack.Arrangement
import plutoproject.framework.paper.api.interactive.layout.Column
import plutoproject.framework.paper.api.interactive.layout.Row
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.modifiers.fillMaxSize
import plutoproject.framework.paper.api.interactive.modifiers.fillMaxWidth
import plutoproject.framework.paper.api.interactive.modifiers.height

class AnimationExampleScreen : InteractiveScreen() {
    @Composable
    override fun Content() {
        Menu(
            title = Component.text("动画示例"),
            rows = 6
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Row(modifier = Modifier.fillMaxWidth().height(2), horizontalArrangement = Arrangement.Center) {
                    Item(
                        material = loadingIconAnimation(),
                        name = Component.text("${spinnerAnimation()} 正在加载...").color(mochaSubtext0)
                    )
                }
            }
        }
    }
}
