package plutoproject.framework.common.playerdb

import org.bson.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.playerdb.Database
import plutoproject.framework.common.util.data.convertToUuid
import java.util.*
import java.util.logging.Level

/*
* 基于 Bson 的玩家数据库。
* 从老的 Member 系统代码分离而得。
* 那时的代码写的有点垃圾，将就用吧（
* */
@Suppress("UNCHECKED_CAST")
class DatabaseImpl(model: DatabaseModel) : Database, KoinComponent {
    private val repo by inject<DatabaseRepository>()
    private val databaseNotifier by inject<DatabaseNotifier>()

    override val id: UUID = model.id.convertToUuid()
    override val contents: BsonDocument = model.contents.clone()

    private fun toBson(obj: Any?): BsonValue {
        if (obj == null) return BsonNull()
        return when (obj) {
            is Byte -> BsonInt32(obj.toInt())
            is Short -> BsonInt32(obj.toInt())
            is Int -> BsonInt32(obj)
            is Long -> BsonInt64(obj)
            is Float -> BsonDouble(obj.toDouble())
            is Double -> BsonDouble(obj)
            is Char -> BsonString(obj.toString())
            is Boolean -> BsonBoolean(obj)
            is String -> BsonString(obj)
            is Collection<*> -> BsonArray(obj.filterNotNull().map { toBson(it) })
            is Map<*, *> -> BsonDocument(obj.entries.filter { it.key is String && it.value != null }
                .map { BsonElement(it.key as String, toBson(it.value!!)) })

            else -> throw IllegalStateException("Type not supported: ${obj::class.java.name}")
        }
    }

    private fun fromBson(value: BsonValue?): Any? {
        if (value == null) return null
        return when (value) {
            is BsonInt32 -> value.value
            is BsonInt64 -> value.value
            is BsonDouble -> value.value
            is BsonString -> value.value
            is BsonBoolean -> value.value
            is BsonNull -> return null
            is BsonArray -> return value.values.map { fromBson(it) }
            is BsonDocument -> return value.entries.associate { it.key to fromBson(it.value) }
            else -> throw IllegalStateException("Type not supported: ${value::class.java.name}")
        }
    }

    private fun isNestedKey(key: String): Boolean {
        return key.contains('.')
    }

    private fun isLegalNestedKey(key: String): Boolean {
        return !key.startsWith('.') && !key.endsWith('.')
    }

    private fun throwIfIllegalNestedKey(key: String) {
        check(isLegalNestedKey(key)) { "Illegal nested key: $key" }
    }

    private fun setNested(key: String, value: BsonValue) {
        throwIfIllegalNestedKey(key)
        val keys = key.split('.')
        val range = keys.indices
        val last = range.last
        var curr = contents

        for (i in range) {
            if (i == last) {
                curr[keys[i]] = value
                break
            }
            val next = curr.computeIfAbsent(keys[i]) { BsonDocument() }
            check(next is BsonDocument) { "Key ${keys.subList(0, i + 1).joinToString(".")} isn't a BsonDocument" }
            curr = next
        }
    }

    private fun getNested(key: String): BsonValue? {
        throwIfIllegalNestedKey(key)
        val keys = key.split('.')
        val range = keys.indices
        val last = range.last
        var curr = contents

        for (i in range) {
            if (i == last) return curr[keys[i]]
            val next = curr[keys[i]] ?: return null
            check(next is BsonDocument) { "Key ${keys.subList(0, i + 1).joinToString(".")} isn't a BsonDocument" }
            curr = next
        }

        return null
    }


    private fun removeNested(key: String) {
        throwIfIllegalNestedKey(key)
        val keys = key.split('.')
        val range = keys.indices
        val last = range.last
        var curr = contents

        for (i in range) {
            if (i == last) {
                curr.remove(keys[i])
                break
            }
            val next = curr[keys[i]] ?: return
            check(next is BsonDocument) { "Key ${keys.subList(0, i + 1).joinToString(".")} isn't a BsonDocument" }
            curr = next
        }
    }

    private fun containsNested(key: String): Boolean {
        return getNested(key) != null
    }

    override fun set(key: String, value: Any) {
        try {
            setBson(key, toBson(value))
        } catch (e: Exception) {
            logger.log(
                Level.SEVERE,
                "Failed to set a value in $id's database (key=$key, value=$value)",
                e
            )
        }
    }

    override fun get(key: String): Any? {
        return try {
            fromBson(getBson(key))
        } catch (e: Exception) {
            logger.log(
                Level.SEVERE,
                "Failed to get a value in $id's database (key=$key)",
                e
            )
            null
        }
    }

    override fun <T : Any> computeIfAbsent(key: String, compute: (String) -> T): T {
        if (contains(key)) return get(key) as T
        val computed = compute(key)
        set(key, computed)
        return computed
    }

    override fun setBson(key: String, value: BsonValue) {
        try {
            if (isNestedKey(key)) {
                setNested(key, value)
                return
            }
            contents[key] = value
        } catch (e: Exception) {
            logger.log(
                Level.SEVERE,
                "Failed to set a value in $id's database (key=$key, value=$value)",
                e
            )
        }
    }

    override fun getBson(key: String): BsonValue? {
        return try {
            if (isNestedKey(key)) {
                return getNested(key)
            }
            contents[key]
        } catch (e: Exception) {
            logger.log(
                Level.SEVERE,
                "Failed to get a value in $id's database (key=$key)",
                e
            )
            null
        }
    }

    override fun remove(key: String) {
        try {
            if (isNestedKey(key)) {
                removeNested(key)
                return
            }
            contents.remove(key)
        } catch (e: Exception) {
            logger.log(
                Level.SEVERE,
                "Failed to remove a value in $id's database (key=$key)",
                e
            )
        }
    }

    override fun getString(key: String): String? {
        return get(key) as String?
    }

    override fun getByte(key: String): Byte? {
        return getString(key)?.toByte()
    }

    override fun getShort(key: String): Short? {
        return getInt(key)?.toShort()
    }

    override fun getInt(key: String): Int? {
        return get(key) as Int?
    }

    override fun getLong(key: String): Long? {
        if (get(key) is Int) {
            return getInt(key)?.toLong()
        }
        return get(key) as Long?
    }

    override fun getFloat(key: String): Float? {
        return getDouble(key)?.toFloat()
    }

    override fun getDouble(key: String): Double? {
        return get(key) as Double?
    }

    override fun getChar(key: String): Char? {
        return getString(key)?.get(0)
    }

    override fun getBoolean(key: String): Boolean {
        return get(key) as Boolean? ?: false
    }

    override fun <T> getList(key: String): List<T>? {
        return try {
            get(key) as List<T>?
        } catch (e: Exception) {
            logger.log(
                Level.SEVERE,
                "Failed to get a list in $id's database (key=$key)",
                e
            )
            null
        }
    }

    override fun <T> getMap(key: String): Map<String, T>? {
        return try {
            get(key) as Map<String, T>?
        } catch (e: Exception) {
            logger.log(
                Level.SEVERE,
                "Failed to get a map in $id's database (key=$key)",
                e
            )
            null
        }
    }

    override fun contains(key: String): Boolean {
        if (isNestedKey(key)) {
            return containsNested(key)
        }
        return contents.containsKey(key)
    }

    override fun clear() {
        contents.clear()
    }

    override suspend fun update() {
        repo.saveOrUpdate(toModel())
        databaseNotifier.notify(id)
    }
}
