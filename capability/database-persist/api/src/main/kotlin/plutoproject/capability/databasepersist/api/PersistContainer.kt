package plutoproject.capability.databasepersist.api

import java.util.*

interface PersistContainer {
    val playerId: UUID

    fun <T : Any> set(key: String, adapter: DataTypeAdapter<T>, value: T)

    fun remove(key: String)

    suspend fun <T : Any> get(key: String, adapter: DataTypeAdapter<T>): T?

    suspend fun <T : Any> getOrDefault(key: String, adapter: DataTypeAdapter<T>, default: T): T

    suspend fun <T : Any> getOrElse(key: String, adapter: DataTypeAdapter<T>, defaultValue: () -> T): T

    suspend fun <T : Any> getOrSet(key: String, adapter: DataTypeAdapter<T>, defaultValue: () -> T): T

    suspend fun contains(key: String): Boolean

    suspend fun save()
}
