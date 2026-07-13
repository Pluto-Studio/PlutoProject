package plutoproject.foundation.velocity.command

import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.annotations.AnnotationParser

class CloudCommandRegistration private constructor(
    private val parser: AnnotationParser<CommandSource>,
    private val roots: Set<String>,
) : AutoCloseable {
    override fun close() {
        roots.forEach(parser.manager()::deleteRootCommand)
    }

    companion object {
        fun register(
            parser: AnnotationParser<CommandSource>,
            vararg commandContainers: Any,
        ): CloudCommandRegistration {
            val rootsBefore = parser.manager().rootCommands().toSet()
            try {
                parser.parse(*commandContainers)
                return CloudCommandRegistration(
                    parser,
                    parser.manager().rootCommands().toSet() - rootsBefore,
                )
            } catch (cause: Throwable) {
                (parser.manager().rootCommands().toSet() - rootsBefore)
                    .forEach(parser.manager()::deleteRootCommand)
                throw cause
            }
        }
    }
}
