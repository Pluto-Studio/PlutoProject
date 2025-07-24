package plutoproject.framework.velocity.api.databasepersist

import com.velocitypowered.api.proxy.Player
import plutoproject.framework.common.api.databasepersist.DatabasePersist
import plutoproject.framework.common.api.databasepersist.PersistContainer

/**
 * 获取指定玩家的 [PersistContainer] 实例
 *
 * 此函数在执行时仅会创建本地的数据容器，不会进行 IO 操作。
 * 在调用 [PersistContainer.get] 或 [PersistContainer.save] 时才会读写数据库。
 *
 * @param player 需要获取的玩家
 * @return 该玩家的 [PersistContainer] 实例
 * @see PersistContainer
 */
fun DatabasePersist.getContainer(player: Player): PersistContainer {
    return getContainer(player.uniqueId)
}
