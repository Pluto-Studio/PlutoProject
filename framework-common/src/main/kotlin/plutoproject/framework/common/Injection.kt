package plutoproject.framework.common

import com.mongodb.kotlin.client.coroutine.MongoCollection
import org.koin.dsl.binds
import org.koin.dsl.module
import plutoproject.framework.common.api.databasepersist.DatabasePersist
import plutoproject.framework.common.api.feature.FeatureManager
import plutoproject.framework.common.api.options.OptionsManager
import plutoproject.framework.common.api.options.factory.OptionDescriptorFactory
import plutoproject.framework.common.api.profile.ProfileLookup
import plutoproject.framework.common.api.provider.Provider
import plutoproject.framework.common.api.provider.getCollection
import plutoproject.framework.common.api.rpc.RpcClient
import plutoproject.framework.common.api.rpc.RpcServer
import plutoproject.framework.common.builddata.BuildInfoImpl
import plutoproject.framework.common.config.ProviderConfig
import plutoproject.framework.common.config.RpcConfig
import plutoproject.framework.common.databasepersist.*
import plutoproject.framework.common.feature.FeatureManagerImpl
import plutoproject.framework.common.options.OptionDescriptorFactoryImpl
import plutoproject.framework.common.options.OptionsManagerImpl
import plutoproject.framework.common.options.repositories.OptionsContainerRepository
import plutoproject.framework.common.profile.ProfileLookupImpl
import plutoproject.framework.common.profile.ProfileRepository
import plutoproject.framework.common.provider.ProviderImpl
import plutoproject.framework.common.rpc.RpcClientImpl
import plutoproject.framework.common.rpc.RpcServerImpl
import plutoproject.framework.common.util.COMMON_FRAMEWORK_RESOURCE_PREFIX
import plutoproject.framework.common.util.buildinfo.BuildInfo
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.frameworkDataFolder
import plutoproject.framework.common.util.jvm.extractFileFromJar
import plutoproject.framework.common.util.pluginDataFolder

inline fun <reified T : Any> getModuleConfig(resourcePrefix: String, id: String): T {
    val file = frameworkDataFolder.resolve(id).resolve("config.conf")
    file.parentFile?.mkdirs()
    if (!file.exists()) {
        extractFileFromJar("$resourcePrefix/$id/config.conf", file.toPath())
    }
    return loadConfig(file)
}

private fun getPlutoConfig(): PlutoConfig {
    val file = pluginDataFolder.resolve("config.conf")
    file.parentFile?.mkdirs()
    if (!file.exists()) {
        extractFileFromJar("config.conf", file.toPath())
    }
    return loadConfig(file)
}

val FrameworkCommonModule = module {
    single<PlutoConfig> { getPlutoConfig() }
    single<FeatureManager> { FeatureManagerImpl() }
    single<ProviderConfig> { getModuleConfig(COMMON_FRAMEWORK_RESOURCE_PREFIX, "provider") }
    single<RpcConfig> { getModuleConfig(COMMON_FRAMEWORK_RESOURCE_PREFIX, "rpc") }
    single<Provider> { ProviderImpl() }
    single<RpcClient> { RpcClientImpl() }
    single<RpcServer> { RpcServerImpl() }
    single<OptionsContainerRepository> { OptionsContainerRepository(Provider.getCollection("options_data")) }
    single<OptionsManager> { OptionsManagerImpl() }
    single<OptionDescriptorFactory> { OptionDescriptorFactoryImpl() }
    single<ProfileLookup> { ProfileLookupImpl() }
    single<ProfileRepository> { ProfileRepository(Provider.getCollection("framework_profile_profiles")) }
    single<BuildInfo> { BuildInfoImpl() }
    single { DatabasePersistImpl() } binds arrayOf(DatabasePersist::class, InternalDatabasePersist::class)
    single<MongoCollection<ContainerModel>> { Provider.getCollection("database_persist_containers") }
    single<ContainerRepository> { ContainerRepository() }
    single { DataChangeStream() }
}
