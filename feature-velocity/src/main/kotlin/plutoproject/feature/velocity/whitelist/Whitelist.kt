package plutoproject.feature.velocity.whitelist

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import org.koin.dsl.module
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.api.connection.getCollection
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.velocity.api.feature.VelocityFeature
import plutoproject.framework.velocity.util.command.AnnotationParser
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server

@Feature(
    id = "whitelist",
    platform = Platform.VELOCITY,
)
@Suppress("UNUSED")
class Whitelist : VelocityFeature() {
    private val featureModule = module {
        single<WhitelistRepository> { WhitelistRepository(MongoConnection.getCollection<WhitelistModel>("whitelist_data")) }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        AnnotationParser.parse(WhitelistCommand)
        server.eventManager.registerSuspend(plugin, WhitelistListener)
    }
}
