package plutoproject.framework.paper.api.interactive.canvas

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.wesjd.anvilgui.AnvilGUI
import org.bukkit.inventory.ItemStack
import plutoproject.framework.paper.api.interactive.LocalGuiScope
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.util.coroutine.runSync
import plutoproject.framework.paper.util.plugin

typealias CloseListener = (AnvilGUI.StateSnapshot) -> Unit
typealias ClickListener = (Int, AnvilGUI.StateSnapshot) -> List<AnvilGUI.ResponseAction>

@Composable
@Suppress("FunctionName")
fun Anvil(
    title: Component,
    left: ItemStack? = null,
    right: ItemStack? = null,
    output: ItemStack? = null,
    text: String = "",
    onClose: CloseListener = {},
    onClick: ClickListener = { _, _ -> emptyList() },
) {
    val player = LocalPlayer.current
    val scope = LocalGuiScope.current
    var textState by rememberSaveable { mutableStateOf(text) }
    remember(title, left, right, output) {
        AnvilGUI.Builder()
            .plugin(plugin)
            .jsonTitle(GsonComponentSerializer.gson().serialize(title))
            .text(textState)
            .apply {
                if (left != null) itemLeft(left)
                if (right != null) itemRight(right)
                if (output != null) itemOutput(output)
            }
            .onClose {
                textState = it.text
                onClose(it)
            }
            .onClick { i, s ->
                textState = s.text
                onClick(i, s)
            }
            .also {
                scope.setBeingRefresh(true)
                runSync { it.open(player) }
            }
    }
}
