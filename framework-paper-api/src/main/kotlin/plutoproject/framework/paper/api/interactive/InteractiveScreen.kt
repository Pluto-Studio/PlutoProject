package plutoproject.framework.paper.api.interactive

import cafe.adriel.voyager.core.platform.multiplatformName
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import java.util.*

abstract class InteractiveScreen : Screen {
    // Voyager 的 ScreenModel 存储是按照 ScreenKey 来的
    // 也就是说，在不同玩家打开的菜单中，同一个 Screen 类（例如 NotebookScreen）的 ScreenModel 是同一个，导致状态冲突
    // 为每个 InteractiveScreen 的实例都生成一个唯一的 key 来解决这个问题
    override val key: ScreenKey = "${this::class.multiplatformName}\$${UUID.randomUUID()}"
}
