package plutoproject.framework.velocity.api.databasepersist

import com.velocitypowered.api.proxy.Player
import plutoproject.framework.common.api.databasepersist.DatabasePersist
import plutoproject.framework.common.api.databasepersist.PersistContainer

/**
 * 该玩家的 [PersistContainer]。
 */
val Player.persistContainer: PersistContainer
    get() = DatabasePersist.getContainer(uniqueId)
