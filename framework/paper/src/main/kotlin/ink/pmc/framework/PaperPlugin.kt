package ink.pmc.framework

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import ink.pmc.framework.bridge.Bridge
import ink.pmc.framework.bridge.backend.BackendBridge
import ink.pmc.framework.bridge.backend.BridgeCommand
import ink.pmc.framework.bridge.backend.listeners.BridgePlayerListener
import ink.pmc.framework.bridge.backend.listeners.BridgeWorldListener
import ink.pmc.framework.bridge.backend.startBridgeBackgroundTask
import ink.pmc.framework.bridge.backend.stopBridgeBackgroundTask
import ink.pmc.framework.interactive.GuiListener
import ink.pmc.framework.interactive.GuiManager
import ink.pmc.framework.interactive.GuiManagerImpl
import ink.pmc.framework.interactive.commands.InteractiveCommand
import ink.pmc.framework.interactive.inventory.InventoryListener
import ink.pmc.framework.options.BackendOptionsUpdateNotifier
import plutoproject.framework.common.options.OptionsUpdateNotifier
import ink.pmc.framework.options.listeners.BukkitOptionsListener
import ink.pmc.framework.options.startOptionsMonitor
import ink.pmc.framework.options.stopOptionsMonitor
import ink.pmc.framework.playerdb.BackendDatabaseNotifier
import plutoproject.framework.common.playerdb.DatabaseNotifier
import ink.pmc.framework.playerdb.startPlayerDbMonitor
import ink.pmc.framework.playerdb.stopPlayerDbMonitor
import ink.pmc.framework.provider.Provider
import ink.pmc.framework.rpc.RpcClient
import ink.pmc.framework.command.annotationParser
import ink.pmc.framework.command.commandManager
import ink.pmc.framework.concurrent.cancelFrameworkScopes
import ink.pmc.framework.hook.initPaperHooks
import ink.pmc.framework.inject.modifyExistedKoinOrCreate
import ink.pmc.framework.jvm.loadClassesInPackages
import ink.pmc.framework.platform.paper
import ink.pmc.framework.platform.paperThread
import ink.pmc.framework.storage.saveResourceIfNotExisted
import ink.pmc.framework.visual.display.text.*
import ink.pmc.framework.visual.display.text.renderers.NmsTextDisplayRenderer
import ink.pmc.framework.visual.toast.ToastRenderer
import ink.pmc.framework.visual.toast.renderers.NmsToastRenderer
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.incendo.cloud.minecraft.extras.parser.ComponentParser
import org.incendo.cloud.parser.standard.StringParser
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

@Suppress("UNUSED")
class PaperPlugin : SuspendingJavaPlugin(), KoinComponent {
    private val config by inject<FrameworkConfig>()
    private val bukkitModule = module {
        single<File>(FRAMEWORK_CONFIG) { saveResourceIfNotExisted("config.conf") }
        single<GuiManager> { GuiManagerImpl() }
        single<OptionsUpdateNotifier> { BackendOptionsUpdateNotifier() }
        single<DatabaseNotifier> { BackendDatabaseNotifier() }
    }

    override fun onLoad() {
        frameworkLogger = logger
        frameworkDataFolder = dataFolder.apply { if (!exists()) mkdirs() }
        paperThread = Thread.currentThread()
        this.server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
        modifyExistedKoinOrCreate {
            modules(commonModule, bukkitModule)
        }
        if (config.preload) preload()
        RpcClient.start()
        Provider
    }

    override suspend fun onEnableAsync() {
        paper.pluginManager.registerSuspendingEvents(GuiListener, frameworkPaper)
        paper.pluginManager.registerSuspendingEvents(InventoryListener, frameworkPaper)
        paper.pluginManager.registerSuspendingEvents(BukkitOptionsListener, frameworkPaper)
        paper.pluginManager.registerSuspendingEvents(TextDisplayListener, frameworkPaper)
        paper.pluginManager.registerSuspendingEvents(BridgePlayerListener, frameworkPaper)
        paper.pluginManager.registerSuspendingEvents(BridgeWorldListener, frameworkPaper)
        commandManager().apply {
            parserRegistry().apply {
                registerNamedParser(
                    "bridge-component",
                    ComponentParser.componentParser(MiniMessage.miniMessage(), StringParser.StringMode.QUOTED)
                )
            }
        }.annotationParser().apply {
            parse(InteractiveCommand)
            parse(BridgeCommand)
        }
        Bridge
        startPlayerDbMonitor()
        startOptionsMonitor()
        startBridgeBackgroundTask()
        initPaperHooks()
    }

    override fun onDisable() {
        GuiManager.disposeAll()
        stopPlayerDbMonitor()
        stopOptionsMonitor()
        stopBridgeBackgroundTask()
        Provider.close()
        RpcClient.stop()
        // gRPC 和数据库相关 IO 连接不会立马关闭
        // 可能导致在插件卸载之后，后台还有正在运行的 IO 操作
        // 若对应操作中加载了没有加载的类，而 framework 已经卸载，就会找不到类
        logger.info("Waiting 1s for finalizing...")
        Thread.sleep(1000)
        cancelFrameworkScopes()
    }

    private fun preload() {
        val start = currentUnixTimestamp
        frameworkLogger.info("Preloading framework to improve performance...")
        loadClassesInPackages(
            "androidx",
            "cafe.adriel.voyager",
            classLoader = runtimeClassLoader
        )
        loadClassesInPackages(
            "ink.pmc.framework",
            classLoader = frameworkClassLoader
        )
        val end = currentUnixTimestamp
        frameworkLogger.info("Preloading finished, took ${end - start}ms")
    }
}