package ink.pmc.serverselector

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.EntityType

data class ServerSelectorConfig(
    val lobby: Lobby,
    val menu: Menu = Menu(),
    val servers: List<Server> = listOf()
)

data class Lobby(
    val world: String,
    val spawnpoint: Spawnpoint,
    val entitySpawning: EntitySpawning = EntitySpawning()
)

data class Spawnpoint(
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float
)

data class EntitySpawning(
    val whitelist: List<EntityType> = listOf()
)

data class Menu(
    val rows: Int = 3,
    val pattern: List<String> = listOf(),
    val ingredients: Map<String, Ingredient> = mapOf(),
)

data class Ingredient(
    val material: Material = Material.PAPER,
)

data class Server(
    val bridgeId: String,
    val menuIcon: String,
    val name: String,
    val displayName: Component = Component.text(name),
    val description: List<Component> = listOf()
)