package plutoproject.feature.whitelist.common

import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import plutoproject.feature.whitelist.common.impl.KnownVisitors
import plutoproject.feature.whitelist.common.impl.WhitelistServiceImpl
import plutoproject.feature.whitelist.api.WhitelistService
import plutoproject.feature.whitelist.core.VisitorRecordRepository
import plutoproject.feature.whitelist.core.WhitelistRecordRepository
import plutoproject.feature.whitelist.core.usecase.*
import plutoproject.capability.mongo.api.MongoConnection
import plutoproject.capability.mongo.api.getCollection
import plutoproject.feature.whitelist.infra.mongo.MongoVisitorRecordRepository
import plutoproject.feature.whitelist.infra.mongo.MongoWhitelistRecordRepository
import kotlinx.coroutines.CoroutineScope
import java.time.Clock

private const val WHITELIST_PREFIX = "whitelist_v2_"
private const val WHITELIST_RECORD_COLLECTION = "whitelist_records"
private const val VISITOR_RECORD_COLLECTION = "visitor_records"

private inline fun <reified T : Any> MongoConnection.whitelistCollection(name: String): MongoCollection<T> {
    return getCollection("$WHITELIST_PREFIX$name")
}

val commonModule = module {
    single { Clock.systemUTC() }
    single { KnownVisitors() }

    single<WhitelistRecordRepository> {
        MongoWhitelistRecordRepository(get<MongoConnection>().whitelistCollection(WHITELIST_RECORD_COLLECTION))
    }
    single<VisitorRecordRepository> {
        val repo = MongoVisitorRecordRepository(get<MongoConnection>().whitelistCollection(VISITOR_RECORD_COLLECTION))
        get<CoroutineScope>().launch(Dispatchers.IO) {
            repo.ensureIndexes()
        }
        repo
    }

    singleOf(::IsWhitelistedUseCase)
    singleOf(::LookupWhitelistRecordUseCase)
    singleOf(::GrantWhitelistUseCase)
    singleOf(::RevokeWhitelistUseCase)
    singleOf(::LookupVisitorRecordUseCase)
    singleOf(::CreateVisitorRecordUseCase)
    singleOf(::LookupVisitorRecordsByCidrUseCase)
    singleOf(::LookupVisitorRecordsByIpUseCase)

    singleOf(::WhitelistServiceImpl) bind WhitelistService::class
}
