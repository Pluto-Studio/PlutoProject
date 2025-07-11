package plutoproject.framework.velocity

import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.incendo.cloud.velocity.VelocityCommandManager
import org.koin.dsl.module
import plutoproject.framework.common.databasepersist.AutoUnloadCondition
import plutoproject.framework.velocity.databasepersist.AutoUnloadConditionImpl
import plutoproject.framework.velocity.util.command.PlatformAnnotationParser
import plutoproject.framework.velocity.util.command.PlatformCommandManager
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server

val FrameworkVelocityModule = module {
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
    single<AutoUnloadCondition> { AutoUnloadConditionImpl() }
}
