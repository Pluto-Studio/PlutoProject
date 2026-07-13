package plutoproject.capability.legacycloudcommands.api.velocity

import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.annotations.AnnotationParser

interface LegacyVelocityCloudCommands {
    val parser: AnnotationParser<CommandSource>
}
