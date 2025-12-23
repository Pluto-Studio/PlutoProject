package plutoproject.feature.paper.hello

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.koin.core.component.KoinComponent
import net.kyori.adventure.text.Component
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.common.util.chat.palettes.mochaPink
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "hello_world",
    platform = Platform.PAPER
)
class HelloWorldFeature : PaperFeature() {
    override fun onEnable() {
        // Register a simple listener that sends a colored message on join
        server.pluginManager.registerSuspendingEvents(HelloWorldListener, plugin)
    }
}

object HelloWorldListener : Listener, KoinComponent {
    @EventHandler
    suspend fun PlayerJoinEvent.e() {
        player.sendMessage(Component.text("Hello World!").color(mochaPink))
    }
}
