package plutoproject.feature.motd.velocity

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import java.nio.file.Path

data class MotdConfig(
    val startDate: String = "2020-01-01",
    val line1: String = "<gold>PlutoProject</gold>",
    val line2: String = "<gray>已开服 <yellow>\$days</yellow> 天</gray>",
)

@OptIn(ExperimentalHoplite::class)
internal fun loadMotdConfig(configFile: Path): MotdConfig = ConfigLoaderBuilder.empty()
    .withClassLoader(MotdConfig::class.java.classLoader)
    .withExplicitSealedTypes()
    .addDefaults()
    .addParser("conf", HoconParser())
    .addPropertySource(PropertySource.file(configFile.toFile()))
    .build()
    .loadConfigOrThrow()
