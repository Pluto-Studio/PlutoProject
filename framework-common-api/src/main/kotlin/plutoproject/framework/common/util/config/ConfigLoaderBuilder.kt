package plutoproject.framework.common.util.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import plutoproject.framework.common.util.config.decoders.CharDecoder
import plutoproject.framework.common.util.config.decoders.ComponentDecoder
import java.io.File

@OptIn(ExperimentalHoplite::class)
fun ConfigLoaderBuilder(): ConfigLoaderBuilder = ConfigLoaderBuilder.empty()
    .withClassLoader(object {}::class.java.classLoader)
    .withExplicitSealedTypes()
    .addDefaults()
    .addDecoder(ComponentDecoder)
    .addDecoder(CharDecoder)

typealias BuilderModifier = ConfigLoaderBuilder.() -> Unit

inline fun <reified T> loadConfig(
    file: File,
    noinline builderModifier: BuilderModifier = {}
): T = ConfigLoaderBuilder()
    .addPropertySource(PropertySource.file(file))
    .apply(builderModifier)
    .build()
    .loadConfigOrThrow()
