package plutoproject.foundation.paper.command

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.AnnotationParser

class CloudCommandRegistration private constructor(
    private val parser: AnnotationParser<CommandSender>,
    private val roots: Set<String>,
) : AutoCloseable {
    override fun close() {
        roots.forEach(parser.manager()::deleteRootCommand)
    }

    companion object {
        fun register(
            parser: AnnotationParser<CommandSender>,
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
