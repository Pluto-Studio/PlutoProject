package plutoproject.framework.velocity.feature

import plutoproject.framework.common.api.feature.FeatureMetadata
import plutoproject.framework.common.api.feature.metadata.AbstractFeature
import plutoproject.framework.common.feature.CommonFeatureManager
import plutoproject.framework.common.util.getFeatureDataFolder
import plutoproject.framework.common.util.jvm.findClass
import plutoproject.framework.velocity.api.feature.VelocityFeature
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server
import java.util.logging.Logger
import kotlin.reflect.full.createInstance

class VelocityFeatureManager : CommonFeatureManager() {
    override fun createAndInitFeature(metadata: FeatureMetadata): AbstractFeature<*, *> {
        val clazz = findClass(metadata.mainClass)?.kotlin ?: error("Feature class not found: ${metadata.id}")
        val feature = clazz.createInstance() as VelocityFeature
        feature.init(
            id = metadata.id,
            server = server,
            plugin = plugin,
            logger = Logger.getLogger("feature/velocity/${metadata.id}"),
            dataFolder = getFeatureDataFolder(metadata.id)
        )
        return feature
    }
}
