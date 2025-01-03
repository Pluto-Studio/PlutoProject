package plutoproject.framework.paper

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.koin.core.qualifier.named
import org.koin.dsl.module
import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.getModuleConfig
import plutoproject.framework.common.options.OptionsUpdateNotifier
import plutoproject.framework.common.playerdb.DatabaseNotifier
import plutoproject.framework.common.util.PAPER_FRAMEWORK_RESOURCE_PREFIX
import plutoproject.framework.paper.api.interactive.GuiManager
import plutoproject.framework.paper.api.toast.ToastFactory
import plutoproject.framework.paper.api.toast.ToastRenderer
import plutoproject.framework.paper.api.worldalias.WorldAlias
import plutoproject.framework.paper.bridge.BackendBridge
import plutoproject.framework.paper.config.WorldAliasConfig
import plutoproject.framework.paper.interactive.GuiManagerImpl
import plutoproject.framework.paper.options.BackendOptionsUpdateNotifier
import plutoproject.framework.paper.playerdb.BackendDatabaseNotifier
import plutoproject.framework.paper.toast.ToastFactoryImpl
import plutoproject.framework.paper.toast.renderers.NmsToastRenderer
import plutoproject.framework.paper.util.command.PlatformAnnotationParser
import plutoproject.framework.paper.util.command.PlatformCommandManager
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
    single<ToastFactory> { ToastFactoryImpl() }
    single<ToastRenderer<Player>>(named("default")) { NmsToastRenderer() }
    single<Bridge> { BackendBridge() }
    single<GuiManager> { GuiManagerImpl() }
    single<OptionsUpdateNotifier> { BackendOptionsUpdateNotifier() }
    single<DatabaseNotifier> { BackendDatabaseNotifier() }
    single<WorldAlias> { WorldAliasImpl() }
}
