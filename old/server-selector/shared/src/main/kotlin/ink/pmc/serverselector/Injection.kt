package ink.pmc.serverselector

import ink.pmc.framework.provider.Provider
import ink.pmc.framework.provider.getCollection
import ink.pmc.serverselector.storage.UserRepository
import org.koin.dsl.module

val sharedModule = module {
    single<UserRepository> { UserRepository(Provider.getCollection("server_selector_users")) }
}