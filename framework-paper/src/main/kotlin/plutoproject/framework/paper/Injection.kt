package plutoproject.framework.paper

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.koin.dsl.module
import plutoproject.framework.common.databasepersist.AutoUnloadCondition
import plutoproject.framework.common.getModuleConfig
import plutoproject.framework.common.options.OptionsUpdateNotifier
import plutoproject.framework.common.playerdb.DatabaseNotifier
import plutoproject.framework.common.util.PAPER_FRAMEWORK_RESOURCE_PREFIX
import plutoproject.framework.paper.api.interactive.GuiManager
import plutoproject.framework.paper.api.statistic.StatisticProvider
import plutoproject.framework.paper.api.worldalias.WorldAlias
import plutoproject.framework.paper.config.WorldAliasConfig
import plutoproject.framework.paper.databasepersist.AutoUnloadConditionImpl
import plutoproject.framework.paper.interactive.GuiManagerImpl
import plutoproject.framework.paper.options.BackendOptionsUpdateNotifier
import plutoproject.framework.paper.playerdb.BackendDatabaseNotifier
import plutoproject.framework.paper.statistic.providers.NativeStatisticProvider
import plutoproject.framework.paper.statistic.providers.SparkStatisticProvider
import plutoproject.framework.paper.util.command.PlatformAnnotationParser
import plutoproject.framework.paper.util.command.PlatformCommandManager
import plutoproject.framework.paper.util.hook.sparkHook
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.worldalias.WorldAliasImpl

val FrameworkPaperModule = module {
    single<PlatformCommandManager> {
        LegacyPaperCommandManager.createNative(
            plugin,
            ExecutionCoordinator.asyncCoordinator()
        ).apply {
            registerBrigadier()
        }
    }
    single<PlatformAnnotationParser> {
        AnnotationParser(get<PlatformCommandManager>(), CommandSender::class.java).installCoroutineSupport()
    }
    single<WorldAliasConfig> { getModuleConfig(PAPER_FRAMEWORK_RESOURCE_PREFIX, "world_alias") }
    single<GuiManager> { GuiManagerImpl() }
    single<OptionsUpdateNotifier> { BackendOptionsUpdateNotifier() }
    single<DatabaseNotifier> { BackendDatabaseNotifier() }
    single<WorldAlias> { WorldAliasImpl() }
    single<StatisticProvider> {
        if (sparkHook != null) {
            SparkStatisticProvider(sparkHook!!.instance)
        } else {
            NativeStatisticProvider()
        }
    }
    single<AutoUnloadCondition> { AutoUnloadConditionImpl() }
}
