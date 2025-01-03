package plutoproject.framework.common

import com.sksamuel.hoplite.PropertySource
import org.koin.dsl.module
import plutoproject.framework.common.api.options.OptionsManager
import plutoproject.framework.common.api.options.factory.OptionDescriptorFactory
import plutoproject.framework.common.api.playerdb.PlayerDB
import plutoproject.framework.common.api.profile.ProfileCache
import plutoproject.framework.common.api.provider.Provider
import plutoproject.framework.common.api.provider.getCollection
import plutoproject.framework.common.api.rpc.RpcClient
import plutoproject.framework.common.api.rpc.RpcServer
import plutoproject.framework.common.config.BridgeConfig
import plutoproject.framework.common.config.ProviderConfig
import plutoproject.framework.common.config.RpcConfig
import plutoproject.framework.common.options.OptionDescriptorFactoryImpl
import plutoproject.framework.common.options.OptionsManagerImpl
import plutoproject.framework.common.options.repositories.OptionsContainerRepository
import plutoproject.framework.common.playerdb.DatabaseRepository
import plutoproject.framework.common.playerdb.PlayerDBImpl
import plutoproject.framework.common.profile.ProfileCacheImpl
import plutoproject.framework.common.profile.ProfileCacheRepository
import plutoproject.framework.common.provider.ProviderImpl
import plutoproject.framework.common.rpc.RpcClientImpl
import plutoproject.framework.common.rpc.RpcServerImpl
import plutoproject.framework.common.util.COMMON_FRAMEWORK_RESOURCE_PREFIX
import plutoproject.framework.common.util.config.ConfigLoaderBuilder
import plutoproject.framework.common.util.frameworkDataFolder
import plutoproject.framework.common.util.jvm.extractFileFromJar

inline fun <reified T : Any> getModuleConfig(resourcePrefix: String, id: String): T {
    val file = frameworkDataFolder.resolve(id).resolve("config.conf")
    file.parentFile?.mkdirs()
    if (!(file.exists())) {
        extractFileFromJar("$resourcePrefix/$id/config.conf", file.toPath())
    }
    return ConfigLoaderBuilder()
        .addPropertySource(PropertySource.file(file))
        .build()
        .loadConfigOrThrow<T>()
}

val FrameworkCommonModule = module {
    single<BridgeConfig> { getModuleConfig(COMMON_FRAMEWORK_RESOURCE_PREFIX, "bridge") }
    single<ProviderConfig> { getModuleConfig(COMMON_FRAMEWORK_RESOURCE_PREFIX, "provider") }
    single<RpcConfig> { getModuleConfig(COMMON_FRAMEWORK_RESOURCE_PREFIX, "rpc") }
    single<Provider> { ProviderImpl() }
    single<RpcClient> { RpcClientImpl() }
    single<RpcServer> { RpcServerImpl() }
    single<DatabaseRepository> { DatabaseRepository(Provider.getCollection("player_database_data")) }
    single<PlayerDB> { PlayerDBImpl() }
    single<OptionsContainerRepository> { OptionsContainerRepository(Provider.getCollection("options_data")) }
    single<OptionsManager> { OptionsManagerImpl() }
    single<OptionDescriptorFactory> { OptionDescriptorFactoryImpl() }
    single<ProfileCache> { ProfileCacheImpl() }
    single<ProfileCacheRepository> { ProfileCacheRepository(Provider.getCollection("framework_utils_profile_cache")) }
}
