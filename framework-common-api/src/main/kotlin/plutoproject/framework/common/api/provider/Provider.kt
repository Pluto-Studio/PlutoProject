package plutoproject.framework.common.api.provider

import com.maxmind.geoip2.DatabaseReader
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import plutoproject.framework.common.util.inject.inlinedGet
import java.io.Closeable

interface Provider : Closeable {
    companion object : Provider by inlinedGet()

    val mongoClient: MongoClient
    val defaultMongoDatabase: MongoDatabase
    val geoIpDatabase: DatabaseReader
}

inline fun <reified T : Any> Provider.getCollection(name: String): MongoCollection<T> {
    return defaultMongoDatabase.getCollection(name)
}
