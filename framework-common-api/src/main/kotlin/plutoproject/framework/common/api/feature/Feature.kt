package plutoproject.framework.common.api.feature

import kotlinx.coroutines.CoroutineScope
import org.koin.core.Koin
import org.koin.dsl.KoinAppDeclaration
import java.io.File
import java.nio.file.Path
import java.util.logging.Logger

interface Feature {
    val id: String
    val state: State
    val platform: Platform
    val logger: Logger
    val dataFolder: File
    val resourcePrefixInJar: String
    val coroutineScope: CoroutineScope
    val featureKoin: Koin

    fun onLoad() {}

    fun onEnable() {}

    fun onReload() {}

    fun onDisable() {}

    fun saveConfig(resourcePrefix: String? = null): File

    fun saveResource(
        path: String,
        output: Path? = null,
        resourcePrefix: String? = null,
    ): File

    fun featureKoin(declaration: KoinAppDeclaration)
}
