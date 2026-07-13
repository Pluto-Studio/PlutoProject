package plutoproject.capability.legacycloudcommands.api.paper

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.AnnotationParser

interface LegacyPaperCloudCommands {
    val parser: AnnotationParser<CommandSender>
}
