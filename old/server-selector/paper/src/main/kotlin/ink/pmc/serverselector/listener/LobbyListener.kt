package ink.pmc.serverselector.listener

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent
import ink.pmc.advkt.component.text
import ink.pmc.advkt.showTitle
import ink.pmc.advkt.title.*
import ink.pmc.framework.startScreen
import ink.pmc.framework.concurrent.submitAsync
import ink.pmc.framework.player.addItemOrDrop
import ink.pmc.framework.chat.mochaPink
import ink.pmc.framework.chat.mochaText
import ink.pmc.serverselector.*
import ink.pmc.serverselector.screen.ServerSelectorScreen
import ink.pmc.serverselector.storage.UserRepository
import net.kyori.adventure.util.Ticks
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.*
import org.bukkit.event.player.*
import org.bukkit.event.weather.WeatherChangeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

val KAOMOJIS = arrayOf(
    "ヽ( ° ▽°)ノ",
    "(｢･ω･)｢",
    "╰(*°▽°*)╯",
    "ヾ(´︶`*)ﾉ♬",
    "( ～'ω')～",
    "(*´∀`)~♥",
    "(￣▽￣)/",
    "( ^ω^)",
    "(๑¯∀¯๑)",
    "(〃´∀｀)"
)

@Suppress("UNUSED")
object LobbyListener : Listener, KoinComponent {
    private val userRepo by inject<UserRepository>()
    private val config by lazy { get<ServerSelectorConfig>().lobby }

    @EventHandler
    fun PlayerJoinEvent.e() {
        /*
        if (!player.hasPermission(PROTECTION_BYPASS)) {
            player.inventory.clear()
        }
        */
        if (!player.inventory.contents
                .filterNotNull()
                .any { it.isServerSelector }
        ) {
            // 玩家在此处更多使用的是选择服务器物品，将其放置在物品栏第一位
            player.inventory.addToFirstHotbarSlot(ServerSelectorItem)
        }
        player.teleportAsync(lobbyWorldSpawn)
        player.health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: error("Unexpected")
        player.foodLevel = 20
        player.saturation = 20f
        player.clearActivePotionEffects()
        player.setRespawnLocation(lobbyWorldSpawn, true)
        submitAsync {
            player.showPromptTitle()
        }
        // if (player.hasPermission(PROTECTION_BYPASS)) return
        player.gameMode = GameMode.ADVENTURE
    }

    private fun PlayerInventory.addToFirstHotbarSlot(itemStack: ItemStack) {
        if (isEmpty) {
            addItem(itemStack)
            return
        }
        val air = ItemStack(Material.AIR)
        val keepHotbar = mutableListOf<ItemStack>()
        for (i in 0..8) {
            keepHotbar.add(getItem(i) ?: continue)
            setItem(i, air)
        }
        setItem(0, itemStack)
        addItemOrDrop(*keepHotbar.toTypedArray())
    }

    private suspend fun Player.showPromptTitle() {
        val userModel = userRepo.findOrCreate(uniqueId)
        showTitle {
            times {
                fadeIn(Ticks.duration(5))
                stay(Ticks.duration(35))
                fadeOut(Ticks.duration(20))
            }
            mainTitle {
                if (userModel.hasJoinedBefore) {
                    text("欢迎回来") with mochaPink
                } else {
                    text("很高兴见到你！") with mochaPink
                }
            }
            subTitle {
                text("使用指南针来传送服务器 ${KAOMOJIS.random()}") with mochaText
            }
        }
        if (userModel.hasJoinedBefore) return
        userRepo.saveOrUpdate(userModel.copy(hasJoinedBefore = true))
    }

    @EventHandler
    fun PlayerInteractEvent.e() {
        if (action.isRightClick && item?.isServerSelector == true) {
            isCancelled = true
            hand?.let { player.swingHand(it) }
            player.startScreen(ServerSelectorScreen())
            return
        }
        if (action == Action.PHYSICAL && clickedBlock?.type == Material.FARMLAND) {
            isCancelled = true
            return
        }
        if (player.hasPermission(PROTECTION_BYPASS)) return
        isCancelled = true
    }

    @EventHandler
    fun PlayerDropItemEvent.e() {
        if (player.hasPermission(PROTECTION_BYPASS)) return
        isCancelled = true
    }

    @EventHandler
    fun EntityPickupItemEvent.e() {
        if (entity !is Player) return
        if (entity.hasPermission(PROTECTION_BYPASS)) return
        isCancelled = true
    }

    @EventHandler
    fun PlayerPickupArrowEvent.e() {
        if (player.hasPermission(PROTECTION_BYPASS)) return
        isCancelled = true
    }

    @EventHandler
    fun PlayerPickupExperienceEvent.e() {
        if (player.hasPermission(PROTECTION_BYPASS)) return
        isCancelled = true
    }

    @EventHandler
    fun EntityDamageEvent.e() {
        if (entity !is Player) return
        isCancelled = true
    }

    @EventHandler
    fun EntityDamageByEntityEvent.e() {
        if (damager !is Player) return
        if (damager.hasPermission(PROTECTION_BYPASS)) return
        isCancelled = true
    }

    @EventHandler
    fun FoodLevelChangeEvent.e() {
        if (entity !is Player) return
        isCancelled = true
    }

    @EventHandler
    fun PlayerAdvancementCriterionGrantEvent.e() {
        isCancelled = true
    }

    @EventHandler
    fun PlayerRecipeDiscoverEvent.e() {
        isCancelled = true
    }

    @EventHandler
    fun PlayerMoveEvent.e() {
        val world = player.world
        if (world != lobbyWorld) return
        if (player.location.blockY in world.minHeight..world.maxHeight) return
        player.teleportAsync(lobbyWorldSpawn)
    }

    @EventHandler
    fun PlayerChangedWorldEvent.e() {
        if (player.world == lobbyWorld) return
        player.resetPlayerTime()
    }

    @EventHandler
    fun EntitySpawnEvent.e() {
        if (entity.world != lobbyWorld) return
        if (config.entitySpawning.whitelist.contains(entity.type)) return
        isCancelled = true
    }

    @EventHandler
    fun WeatherChangeEvent.e() {
        if (world != lobbyWorld) return
        isCancelled = true
    }
}