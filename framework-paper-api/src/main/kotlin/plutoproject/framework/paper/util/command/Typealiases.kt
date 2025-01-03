package plutoproject.framework.paper.util.command

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.paper.LegacyPaperCommandManager

typealias PlatformCommandManager = LegacyPaperCommandManager<CommandSender>
typealias PlatformAnnotationParser = AnnotationParser<CommandSender>
typealias PlatformCommandContext = CommandContext<CommandSender>
