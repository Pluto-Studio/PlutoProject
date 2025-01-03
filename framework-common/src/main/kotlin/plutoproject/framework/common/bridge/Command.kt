package plutoproject.framework.common.bridge

import net.kyori.adventure.text.minimessage.MiniMessage
import org.incendo.cloud.CommandManager
import org.incendo.cloud.minecraft.extras.parser.ComponentParser
import org.incendo.cloud.parser.standard.StringParser
import plutoproject.framework.common.api.bridge.command.parsers.bridgePlayerParser
import plutoproject.framework.common.api.bridge.command.parsers.bridgeServerParser

fun CommandManager<*>.registerBridgeArgumentParsers() {
    parserRegistry().apply {
        registerParser(bridgePlayerParser())
        registerParser(bridgeServerParser())
        registerNamedParser(
            "bridge-component",
            ComponentParser.componentParser(MiniMessage.miniMessage(), StringParser.StringMode.QUOTED)
        )
    }
}
