package plutoproject.framework.paper.api.databasepersist

import org.bukkit.OfflinePlayer
import plutoproject.framework.common.api.databasepersist.DatabasePersist
import plutoproject.framework.common.api.databasepersist.PersistContainer

/**
 * 该玩家的 [PersistContainer]。
 *
 * 这是 DatabasePersist API 提供的持久化容器，注意不要和 Bukkit 的搞混了。
 */
val OfflinePlayer.persistContainer: PersistContainer
    get() = DatabasePersist.getContainer(uniqueId)
