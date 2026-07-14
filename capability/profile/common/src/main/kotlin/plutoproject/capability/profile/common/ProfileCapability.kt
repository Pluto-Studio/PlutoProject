package plutoproject.capability.profile.common

import org.koin.dsl.module
import plutoproject.capability.mongo.api.MongoConnection
import plutoproject.capability.mongo.api.getCollection
import plutoproject.capability.profile.api.ProfileLookup
import plutoproject.kernel.api.*

private const val PROFILE_COLLECTION_NAME = "plutoproject_capability_profile"

class ProfileCapability : RuntimeModule {
    override suspend fun onLoad(context: ModuleContext) {
        context.importServiceToKoin<MongoConnection>()
        context.loadKoinModuleDefinitions(module {
            single {
                ProfileRepository(get<MongoConnection>().getCollection<ProfileDocument>(PROFILE_COLLECTION_NAME))
            }
            single { MojangProfileFetcher() }
            single<ProfileLookup> {
                ProfileLookupImpl(
                    scope = context.coroutineScope,
                    repository = get(),
                    fetcher = get(),
                )
            }
        })
        context.services.exportServiceFromKoin<ProfileLookup>()
    }
}
