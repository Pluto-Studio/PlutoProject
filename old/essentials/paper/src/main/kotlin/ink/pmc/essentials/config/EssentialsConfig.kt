package ink.pmc.essentials.config

import org.bukkit.Material
import org.bukkit.block.Biome
import kotlin.time.Duration

data class EssentialsConfig(
    val serverName: String,
    val teleport: Teleport,
    val randomTeleport: RandomTeleport,
    val back: Back,
    val home: Home,
    val warp: Warp,
    val afk: Afk,
    val containerProtection: ContainerProtection,
    val action: Action,
    val item: Item,
    val recipe: Recipe,
    val join: Join,
    val disableJoinQuitMessage: DisableJoinQuitMessage = DisableJoinQuitMessage(),
    val demoWorld: DemoWorld,
    val head: Head = Head(),
)

data class Teleport(
    val enabled: Boolean,
    val maxRequestsStored: Int = 50,
    val request: Request,
    val queueProcessPerTick: Int = 1,
    val chunkPrepareMethod: ChunkPrepareMethod = ChunkPrepareMethod.ASYNC,
    val default: TeleportOptions,
    val worlds: Map<String, TeleportOptions>,
    val blacklistedWorlds: List<String>
)

data class TeleportOptions(
    val avoidVoid: Boolean = true,
    val safeLocationSearchRadius: Int = 20,
    val chunkPrepareRadius: Int = 0,
    val blacklistedBlocks: List<Material> = listOf(Material.WATER, Material.LAVA)
)

data class Request(
    val expireAfter: Duration = Duration.parse("1m"),
    val removeAfter: Duration = Duration.parse("10m")
)

data class RandomTeleport(
    val enabled: Boolean,
    val cacheInterval: Int,
    val cooldown: Duration = Duration.parse("60s"),
    val default: RandomTeleportOptions,
    val worlds: Map<String, RandomTeleportOptions>,
    val enabledWorlds: List<String> = listOf("world")
)

data class RandomTeleportOptions(
    val spawnpointAsCenter: Boolean = true,
    val center: Center = Center(),
    val cacheAmount: Int = 5,
    val chunkPreserveRadius: Int = -1,
    val startRadius: Int = 0,
    val endRadius: Int = 10000,
    val maxHeight: Int = -1,
    val minHeight: Int = -1,
    val noCover: Boolean = true,
    val maxAttempts: Int = 5,
    val cost: Double = 0.0,
    val blacklistedBiomes: List<Biome> = emptyList(),
)

data class Center(
    val x: Double = 0.0,
    val z: Double = 0.0
)

data class Back(
    val enabled: Boolean,
    val blacklistedWorlds: List<String>
)

data class Home(
    val enabled: Boolean,
    val maxHomes: Int,
    val nameLengthLimit: Int,
    val blacklistedWorlds: List<String>
)

data class Warp(
    val enabled: Boolean,
    val nameLengthLimit: Int,
    val blacklistedWorlds: List<String>
)

data class Afk(
    val enabled: Boolean,
    val idleDuration: Duration
)

data class ContainerProtection(
    val enabled: Boolean,
    val itemframe: Boolean,
    val lectern: Boolean
)

data class Action(
    val enabled: Boolean,
)

data class Item(
    val enabled: Boolean,
)

data class Recipe(
    val enabled: Boolean,
    val autoUnlock: Boolean,
    val vanillaExtend: Boolean
)

data class Join(
    val enabled: Boolean,
)

data class DisableJoinQuitMessage(
    val enabled: Boolean = true,
)

data class DemoWorld(
    val enabled: Boolean,
    val worlds: Map<String, DemoWorldOptions>
)

data class DemoWorldOptions(
    val spawnpoint: Spawnpoint,
    val teleportHeight: Int
)

data class Spawnpoint(
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float = 0.0F,
    val pitch: Float = 0.0F,
)

data class Head(
    val enabled: Boolean = true,
    val cost: Double = 3.0
)