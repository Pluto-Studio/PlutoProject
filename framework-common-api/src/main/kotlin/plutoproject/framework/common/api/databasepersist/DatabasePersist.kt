package plutoproject.framework.common.api.databasepersist

import plutoproject.framework.common.util.inject.Koin
import java.util.*

/**
 * 玩家数据库容器 API
 */
interface DatabasePersist {
    companion object : DatabasePersist by Koin.get()

    /**
     * 获取指定 [UUID] 玩家的 [PersistContainer] 实例
     *
     * 此函数在执行时仅会创建本地的数据容器，不会进行 IO 操作。
     * 在调用 [PersistContainer.get] 或 [PersistContainer.save] 时才会读写数据库。
     *
     * @param playerId 需要获取的玩家 UUID
     * @return 该玩家的 [PersistContainer] 实例
     * @see PersistContainer
     */
    fun getContainer(playerId: UUID): PersistContainer

    /**
     * 卸载一个被加载到内存中的容器实例
     *
     * 所有未保存的值都会丢失。
     *
     * @param playerId 指定的容器玩家 [UUID]
     * @return 若指定的容器在内存中且成功卸载则为 true，反之为 false
     */
    fun unloadContainer(playerId: UUID): Boolean
}
