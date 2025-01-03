package plutoproject.framework.velocity.util.command

import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.velocity.VelocityCommandManager

typealias PlatformCommandManager = VelocityCommandManager<CommandSource>
typealias PlatformAnnotationParser = AnnotationParser<CommandSource>
typealias PlatformCommandContext = CommandContext<CommandSource>
