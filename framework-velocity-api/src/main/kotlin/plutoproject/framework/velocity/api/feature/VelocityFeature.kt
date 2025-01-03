package plutoproject.framework.velocity.api.feature

import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import plutoproject.framework.common.api.feature.metadata.AbstractFeature
import plutoproject.framework.common.api.feature.Platform

abstract class VelocityFeature : AbstractFeature<ProxyServer, PluginContainer>() {
    final override val platform: Platform = Platform.VELOCITY
    final override val resourcePrefixInJar: String
        get() = "feature/velocity/$id"
}
