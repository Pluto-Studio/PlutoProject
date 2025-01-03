package ink.pmc.essentials

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.mojang.brigadier.arguments.StringArgumentType
import com.sksamuel.hoplite.PropertySource
import ink.pmc.essentials.afk.AfkManagerImpl
import ink.pmc.essentials.api.afk.AfkManager
import ink.pmc.essentials.api.back.BackManager
import ink.pmc.essentials.api.home.Home
import ink.pmc.essentials.api.home.HomeManager
import ink.pmc.essentials.api.teleport.TeleportManager
import ink.pmc.essentials.api.teleport.random.RandomTeleportManager
import ink.pmc.essentials.api.warp.Warp
import ink.pmc.essentials.api.warp.WarpManager
import ink.pmc.essentials.back.BackManagerImpl
import ink.pmc.essentials.button.*
import ink.pmc.essentials.commands.*
import ink.pmc.essentials.commands.afk.AfkCommand
import ink.pmc.essentials.commands.back.BackCommand
import ink.pmc.essentials.commands.home.*
import ink.pmc.essentials.commands.teleport.TeleportCommons
import ink.pmc.essentials.commands.teleport.TpaCommand
import ink.pmc.essentials.commands.teleport.TpacceptCommand
import ink.pmc.essentials.commands.teleport.TpcancelCommand
import ink.pmc.essentials.commands.teleport.random.RtpCommand
import ink.pmc.essentials.commands.warp.*
import ink.pmc.essentials.config.EssentialsConfig
import ink.pmc.essentials.home.HomeManagerImpl
import ink.pmc.essentials.hooks.EconomyHook
import ink.pmc.essentials.listeners.*
import ink.pmc.essentials.recipes.registerVanillaExtend
import ink.pmc.essentials.repositories.BackRepository
import ink.pmc.essentials.repositories.HomeRepository
import ink.pmc.essentials.repositories.WarpRepository
import ink.pmc.essentials.teleport.TeleportManagerImpl
import ink.pmc.essentials.teleport.random.RandomTeleportManagerImpl
import ink.pmc.essentials.warp.WarpManagerImpl
import ink.pmc.framework.command.annotationParser
import ink.pmc.framework.command.commandManager
import ink.pmc.framework.command.getKotlinMethodArgumentParser
import ink.pmc.framework.command.suggestion.PaperPrivilegedSuggestion
import ink.pmc.framework.config.preconfiguredConfigLoaderBuilder
import ink.pmc.framework.inject.modifyExistedKoinOrCreate
import ink.pmc.framework.storage.saveResourceIfNotExisted
import ink.pmc.menu.api.MenuManager
import ink.pmc.menu.api.isMenuAvailable
import io.leangen.geantyref.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.bukkit.parser.OfflinePlayerParser
import org.incendo.cloud.bukkit.parser.WorldParser
import org.incendo.cloud.minecraft.extras.parser.ComponentParser
import org.incendo.cloud.parser.ParserDescriptor
import org.incendo.cloud.parser.standard.StringParser
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.dsl.module

var disabled = true
var economyHook: EconomyHook? = null
lateinit var plugin: Plugin

val essentialsScope = CoroutineScope(Dispatchers.Default)

@Suppress("UNUSED")
class PaperPlugin : SuspendingJavaPlugin(), KoinComponent {
    private val config by inject<EssentialsConfig>()
    private lateinit var annotationParser: AnnotationParser<CommandSender>
    private val bukkitModule = module {
        single<EssentialsConfig> {
            preconfiguredConfigLoaderBuilder()
                .addPropertySource(PropertySource.file(saveResourceIfNotExisted("config.conf")))
                .build()
                .loadConfigOrThrow()
        }
        single<HomeRepository> { HomeRepository() }
        single<WarpRepository> { WarpRepository() }
        single<BackRepository> { BackRepository() }
        single<TeleportManager> {
            check(config.teleport.enabled)
            TeleportManagerImpl()
        }
        single<RandomTeleportManager> {
            check(config.randomTeleport.enabled)
            RandomTeleportManagerImpl()
        }
        single<HomeManager> {
            check(config.home.enabled)
            HomeManagerImpl()
        }
        single<WarpManager> {
            check(config.warp.enabled)
            WarpManagerImpl()
        }
        single<BackManager> {
            check(config.back.enabled)
            BackManagerImpl()
        }
        single<AfkManager> {
            check(config.afk.enabled)
            AfkManagerImpl()
        }
    }

    override suspend fun onEnableAsync() {
        plugin = this

        modifyExistedKoinOrCreate {
            modules(bukkitModule)
        }

        val commandManager = commandManager().apply {
            parserRegistry().apply {
                registerSuggestionProvider(
                    "rtp-world",
                    PaperPrivilegedSuggestion.of(WorldParser(), RANDOM_TELEPORT_SPECIFIC)
                )
                registerSuggestionProvider(
                    "homes-offlineplayer",
                    PaperPrivilegedSuggestion.of(OfflinePlayerParser(), HOMES_OTHER)
                )
                registerSuggestionProvider(
                    "warps",
                    WarpParser(false)
                )
                registerSuggestionProvider(
                    "warps-without-alias",
                    WarpParser(true)
                )
                registerNamedParser(
                    "warp",
                    ParserDescriptor.of(WarpParser(false), Warp::class.java)
                )
                registerNamedParser(
                    "warp-without-alias",
                    ParserDescriptor.of(WarpParser(true), Warp::class.java)
                )
                registerNamedParser(
                    "spawn",
                    ParserDescriptor.of(SpawnParser(), Warp::class.java)
                )
                registerNamedParser(
                    "editwarp-component",
                    ComponentParser.componentParser(MiniMessage.miniMessage(), StringParser.StringMode.QUOTED)
                )
            }
            brigadierManager().apply {
                registerMapping(TypeToken.get(WarpParser::class.java)) {
                    it.cloudSuggestions().to { parser ->
                        if (!parser.withoutAlias) StringArgumentType.greedyString() else StringArgumentType.string()
                    }
                }
                registerMapping(TypeToken.get(SpawnParser::class.java)) {
                    it.cloudSuggestions().to { StringArgumentType.greedyString() }
                }
                registerMapping(TypeToken.get(getKotlinMethodArgumentParser<CommandSender, Home>())) {
                    it.cloudSuggestions().to { StringArgumentType.greedyString() }
                }
            }
        }
        annotationParser = commandManager.annotationParser()

        register()
        initialize()
        disabled = false
    }

    override suspend fun onDisableAsync() {
        disabled = true
        essentialsScope.cancel()
        if (config.teleport.enabled) {
            TeleportManager.clearRequest()
        }
    }

    private fun register() {
        annotationParser.parse(
            EssentialsCommand,
            AlignCommand,
            GmCommand,
            HatCommand,
        )
        if (config.teleport.enabled) {
            server.pluginManager.registerSuspendingEvents(TeleportListener, this)
            annotationParser.parse(
                TeleportCommons,
                TpacceptCommand,
                TpaCommand,
                TpcancelCommand
            )
            if (isMenuAvailable) {
                MenuManager.registerButton(TELEPORT_BUTTON_DESCRIPTOR) { Teleport() }
            }
        }
        if (config.randomTeleport.enabled) {
            server.pluginManager.registerSuspendingEvents(RandomTeleportListener, this)
            annotationParser.parse(RtpCommand)
            if (isMenuAvailable) {
                MenuManager.registerButton(RANDOM_TELEPORT_BUTTON_DESCRIPTOR) { RandomTeleport() }
            }
        }
        if (config.home.enabled) {
            server.pluginManager.registerSuspendingEvents(HomeListener, this)
            annotationParser.parse(
                HomeCommons,
                DelHomeCommand,
                HomeCommand,
                HomesCommand,
                SetHomeCommand
            )
            if (isMenuAvailable) {
                MenuManager.registerButton(HOME_BUTTON_DESCRIPTOR) { Home() }
            }
        }
        if (config.warp.enabled) {
            annotationParser.parse(
                WarpCommons,
                DelWarpCommand,
                EditWarpCommand,
                PreferredSpawnCommand,
                SetWarpCommand,
                SpawnCommand,
                WarpCommand,
                WarpsCommand
            )
            if (isMenuAvailable) {
                MenuManager.registerButton(WARP_BUTTON_DESCRIPTOR) { Warp() }
                MenuManager.registerButton(SPAWN_BUTTON_DESCRIPTOR) { Spawn() }
            }
        }
        if (config.back.enabled) {
            server.pluginManager.registerSuspendingEvents(BackListener, this)
            annotationParser.parse(BackCommand)
        }
        if (config.afk.enabled) {
            server.pluginManager.registerSuspendingEvents(AfkListener, this)
            annotationParser.parse(AfkCommand)
        }
        if (config.containerProtection.enabled) {
            if (config.containerProtection.itemframe) {
                server.pluginManager.registerSuspendingEvents(ItemFrameListener, this)
                annotationParser.parse(ItemFrameCommand)
            }
            if (config.containerProtection.lectern) {
                server.pluginManager.registerSuspendingEvents(LecternListener, this)
                annotationParser.parse(LecternCommand)
            }
        }
        if (config.action.enabled) {
            server.pluginManager.registerSuspendingEvents(ActionListener, this)
        }
        if (config.item.enabled) {
            server.pluginManager.registerSuspendingEvents(ItemListener, this)
        }
        if (config.recipe.enabled) {
            if (config.recipe.autoUnlock) {
                server.pluginManager.registerSuspendingEvents(RecipeListener, this)
            }
            if (config.recipe.vanillaExtend) {
                server.registerVanillaExtend()
            }
        }
        if (config.join.enabled) {
            server.pluginManager.registerSuspendingEvents(JoinListener, this)
        }
        if (config.head.enabled) {
            annotationParser.parse(HeadCommand)
        }
        if (config.disableJoinQuitMessage.enabled) {
            server.pluginManager.registerSuspendingEvents(DisableJoinQuitMessageListener, this)
        }
        if (config.demoWorld.enabled) {
            server.pluginManager.registerSuspendingEvents(DemoWorldListener, this)
        }
    }

    private fun initialize() {
        // 初始化 AfkManager，开始后台任务
        if (config.afk.enabled) get<AfkManager>()
        if (server.pluginManager.getPlugin("Vault") != null) {
            economyHook = EconomyHook()
        }
    }

}