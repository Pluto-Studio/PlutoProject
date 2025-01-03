package plutoproject.framework.velocity

import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.incendo.cloud.velocity.VelocityCommandManager
import org.koin.dsl.module
import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.options.OptionsUpdateNotifier
import plutoproject.framework.common.playerdb.DatabaseNotifier
import plutoproject.framework.velocity.bridge.ProxyBridge
import plutoproject.framework.velocity.util.command.PlatformAnnotationParser
import plutoproject.framework.velocity.util.command.PlatformCommandManager
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server
import plutoproject.framework.velocity.options.OptionsUpdateNotifier as VelocityOptionsUpdateNotifier
import plutoproject.framework.velocity.playerdb.DatabaseNotifier as VelocityDatabaseNotifier

val FrameworkVelocityModule = module {
    single<OptionsUpdateNotifier> { VelocityOptionsUpdateNotifier() }
    single<DatabaseNotifier> { VelocityDatabaseNotifier() }
    single<Bridge> { ProxyBridge() }
    single<PlatformCommandManager> {
        VelocityCommandManager(
            plugin,
            server,
            ExecutionCoordinator.asyncCoordinator(),
            SenderMapper.identity()
        )
    }
    single<PlatformAnnotationParser> {
        AnnotationParser(get<PlatformCommandManager>(), CommandSource::class.java).installCoroutineSupport()
    }
}
