package plutoproject.framework.paper.api.feature

import org.bukkit.Server
import org.bukkit.plugin.java.JavaPlugin
import plutoproject.framework.common.api.feature.metadata.AbstractFeature
import plutoproject.framework.common.api.feature.Platform

abstract class PaperFeature : AbstractFeature<Server, JavaPlugin>() {
    final override val platform: Platform = Platform.PAPER
    final override val resourcePrefixInJar: String
        get() = "feature/paper/$id"
}
