package plutoproject.feature.paper.api.exchangeshop

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import plutoproject.framework.common.util.inject.Koin
import java.util.*

/**
 * 兑换商店主接口。
 */
interface ExchangeShop {
    companion object : ExchangeShop by Koin.get()

    /**
     * 商店中已添加的类别。
     */
    val categories: Collection<ShopCategory>

    /**
     * 商店中所有类别里已添加的物品。
     */
    val items: Collection<ShopItem>

    /**
     * 获取指定玩家的用户。
     *
     * @param player 需要获取的玩家
     * @return 获取到的用户对象，若不存在则为空
     */
    suspend fun getUser(player: Player): ShopUser?

    /**
     * 获取指定 UUID 的用户。
     *
     * @param uniqueId 需要获取的 UUID
     * @return 获取到的用户对象，若不存在则为空
     */
    suspend fun getUser(uniqueId: UUID): ShopUser?

    /**
     * 判断是否存在指定玩家的用户。
     *
     * @param player 需要判断的玩家
     * @return 若存在则为 true，不存在为 false
     */
    suspend fun hasUser(player: Player): Boolean

    /**
     * 判断是否存在指定 UUID 的用户。
     *
     * @param uniqueId 需要判断的玩家
     * @return 若存在则为 true，不存在为 false
     */
    suspend fun hasUser(uniqueId: UUID): Boolean

    /**
     * 创建指定玩家的用户。
     *
     * @param player 需要创建的玩家
     * @return 新创建的用户对象
     * @throws IllegalArgumentException 该玩家的用户已存在
     */
    suspend fun createUser(player: Player): ShopUser

    /**
     * 创建指定 UUID 的用户。
     *
     * @param uniqueId 需要创建的 UUID
     * @return 新创建的用户对象
     * @throws IllegalArgumentException 该 UUID 的用户已存在
     */
    suspend fun createUser(uniqueId: UUID): ShopUser

    /**
     * 获取指定玩家的用户，若不存在则创建。
     *
     * @param player 需要获取的玩家
     * @return 获取到或新创建的用户对象
     */
    suspend fun getUserOrCreate(player: Player): ShopUser

    /**
     * 获取指定 UUID 的用户，若不存在则创建。
     *
     * @param uniqueId 需要获取的 UUID
     * @return 获取到或新创建的用户对象
     */
    suspend fun getUserOrCreate(uniqueId: UUID): ShopUser

    /**
     * 创建一个新的类别。
     *
     * @param id 类别的 ID，需唯一
     * @param icon 类别在菜单中显示的图标
     * @param name 类别在菜单中显示的名称
     * @param description 类别在菜单中显示的介绍
     * @return 创建的 [ShopCategory]
     * @throws IllegalArgumentException 同 ID 的类别已存在或 ID 不合法
     */
    fun createCategory(
        id: String,
        icon: Material,
        name: Component,
        description: List<Component>
    ): ShopCategory

    /**
     * 获取指定 ID 的类别。
     *
     * @param id 要获取的 ID
     * @return 获取到的类别，若不存在则为空
     */
    fun getCategory(id: String): ShopCategory?

    /**
     * 检查是否有指定 ID 的类别。
     *
     * @param id 要判断的 ID
     * @return 是否有指定 ID 的类别
     */
    fun hasCategory(id: String): Boolean

    /**
     * 移除一个指定 ID 的类别。
     *
     * @param id 要移除的 ID
     * @return 被移除的类别，若不存在则为空
     */
    fun removeCategory(id: String): ShopCategory?
}
