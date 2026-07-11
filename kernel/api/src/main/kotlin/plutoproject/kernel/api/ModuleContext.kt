package plutoproject.kernel.api

import java.lang.System.Logger
import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope

interface ModuleContext {
    val id: String
    val logger: Logger
    val dataFolder: Path
    val coroutineScope: CoroutineScope
}
