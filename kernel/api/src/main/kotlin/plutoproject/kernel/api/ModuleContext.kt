package plutoproject.kernel.api

import java.nio.file.Path
import java.util.logging.Logger
import kotlinx.coroutines.CoroutineScope
import org.koin.core.Koin

interface ModuleContext {
    val id: String
    val logger: Logger
    val dataFolder: Path
    val coroutineScope: CoroutineScope
    val koin: Koin
    val services: ModuleServices

    fun saveResource(
        path: String,
        output: Path = Path.of(path),
        resourcePrefix: String? = null,
        replace: Boolean = false,
    ): Path
}
