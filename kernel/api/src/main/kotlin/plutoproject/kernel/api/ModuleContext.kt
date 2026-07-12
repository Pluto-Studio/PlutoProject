package plutoproject.kernel.api

import java.lang.System.Logger
import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope

interface ModuleContext {
    val id: String
    val logger: Logger
    val dataFolder: Path
    val coroutineScope: CoroutineScope

    fun saveResource(
        path: String,
        output: Path = Path.of(path),
        resourcePrefix: String? = null,
        replace: Boolean = false,
    ): Path
}
