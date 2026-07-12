package plutoproject.capability.databasepersist.common

import org.koin.dsl.module
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.capability.mongo.api.MongoConnection
import plutoproject.capability.mongo.api.getCollection
import plutoproject.capability.server_identifier.api.ServerIdentifier
import plutoproject.kernel.api.*

class DatabasePersistCapability(
    private val autoUnloadConditionFactory: (ModuleContext) -> AutoUnloadCondition,
) : RuntimeModule {
    override suspend fun onLoad(context: ModuleContext) {
        context.importServiceToKoin<MongoConnection>()
        context.importServiceToKoin<ServerIdentifier>()
        val serverIdentifier = context.koinGet<ServerIdentifier>().identifierOrThrow()

        context.loadKoinModuleDefinitions(module {
            single<AutoUnloadCondition> { autoUnloadConditionFactory(context) }
            single {
                ContainerRepository(get<MongoConnection>().getCollection<ContainerModel>(CONTAINER_COLLECTION_NAME))
            }
            single {
                DataChangeStream(
                    scope = context.coroutineScope,
                    repository = get(),
                    mongoConnection = get(),
                    logger = context.logger,
                    serverIdentifier = serverIdentifier,
                )
            }
            single {
                DatabasePersistImpl(
                    scope = context.coroutineScope,
                    changeStream = get(),
                    repository = get(),
                    autoUnloadCondition = get(),
                    mongoConnection = get(),
                    serverIdentifier = serverIdentifier,
                    logger = context.logger,
                )
            }
            single<InternalDatabasePersist> { get<DatabasePersistImpl>() }
            single<DatabasePersist> { get<DatabasePersistImpl>() }
        })
        context.services.exportServiceFromKoin<DatabasePersist>()
    }

    override suspend fun onDisable(context: ModuleContext) {
        context.koinGet<DatabasePersistImpl>().close()
    }
}
