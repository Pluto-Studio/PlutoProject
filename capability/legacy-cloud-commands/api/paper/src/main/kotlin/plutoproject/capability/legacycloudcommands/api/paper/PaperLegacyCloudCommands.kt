package plutoproject.capability.legacycloudcommands.api.paper

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.AnnotationParser

interface PaperLegacyCloudCommands {
    val parser: AnnotationParser<CommandSender>
}
