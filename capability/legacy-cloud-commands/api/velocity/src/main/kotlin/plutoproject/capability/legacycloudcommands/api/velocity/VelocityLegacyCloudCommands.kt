package plutoproject.capability.legacycloudcommands.api.velocity

import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.annotations.AnnotationParser

interface VelocityLegacyCloudCommands {
    val parser: AnnotationParser<CommandSource>
}
