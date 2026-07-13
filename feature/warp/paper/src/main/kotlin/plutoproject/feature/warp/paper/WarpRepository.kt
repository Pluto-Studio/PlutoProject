package plutoproject.feature.warp.paper

import com.github.benmanes.caffeine.cache.Caffeine
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.Indexes.descending
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toCollection
import plutoproject.feature.warp.api.paper.WarpCategory
import plutoproject.feature.warp.api.paper.WarpType
import java.time.Duration
import java.util.*
import kotlin.math.ceil

class WarpRepository(private val collection: MongoCollection<WarpModel>) {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .build<UUID, WarpModel>()

    suspend fun find(): Collection<WarpModel> {
        return collection.find().sort(descending("createdAt")).toCollection(mutableListOf())
    }

    suspend fun findById(id: UUID): WarpModel? {
        val cached = cache.getIfPresent(id) ?: run {
            val lookup = collection.find(eq("id", id.toString())).firstOrNull() ?: return null
            cache.put(id, lookup)
            lookup
        }
        return cached
    }

    suspend fun findByName(name: String): WarpModel? {
        val cached = cache.asMap().values.firstOrNull { it.name == name } ?: run {
            val lookup = collection.find(eq("name", name)).firstOrNull() ?: return null
            cache.put(lookup.id, lookup)
            lookup
        }
        return cached
    }

    suspend fun findSpawns(): Collection<WarpModel> {
        return collection.find(
            or(
                eq("type", WarpType.SPAWN),
                eq("type", WarpType.SPAWN_DEFAULT)
            )
        ).sort(descending("createdAt")).toCollection(mutableListOf())
    }

    suspend fun findByCategory(category: WarpCategory): Collection<WarpModel> {
        return collection.find(eq("category", category)).sort(descending("createdAt")).toCollection(mutableListOf())
    }

    suspend fun getPageCount(pageSize: Int, category: WarpCategory? = null): Int {
        val total = collection.let {
            if (category == null) it.countDocuments() else it.countDocuments(eq("category", category))
        }
        return ceil(total.toDouble() / pageSize).toInt()
    }

    suspend fun findByPage(pageSize: Int, page: Int, category: WarpCategory? = null): Collection<WarpModel> {
        val skip = page * pageSize
        return collection.let {
            if (category != null) it.find(eq("category", category)) else it.find()
        }.sort(descending("createdAt")).skip(skip).limit(pageSize).toCollection(mutableListOf())
    }

    suspend fun hasById(id: UUID): Boolean {
        return findById(id) != null
    }

    suspend fun hasByName(name: String): Boolean {
        return findByName(name) != null
    }

    suspend fun deleteById(id: UUID) {
        cache.invalidate(id)
        collection.deleteOne(eq("id", id.toString()))
    }

    suspend fun deleteByName(name: String) {
        cache.asMap().entries.removeIf { it.value.name == name }
        collection.deleteOne(eq("name", name))
    }

    suspend fun save(model: WarpModel) {
        require(!hasById(model.id)) { "WarpModel with id ${model.id} already existed" }
        collection.insertOne(model)
        cache.put(model.id, model)
    }

    suspend fun update(model: WarpModel) {
        require(hasById(model.id)) { "WarpModel with id ${model.id} not exist" }
        collection.replaceOne(eq("id", model.id.toString()), model)
    }
}
