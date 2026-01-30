package plutoproject.framework.common

import org.koin.dsl.binds
import org.koin.dsl.module
import plutoproject.framework.common.api.connection.GeoIpConnection
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.api.connection.getCollection
import plutoproject.framework.common.api.databasepersist.DatabasePersist
import plutoproject.framework.common.api.feature.FeatureManager
import plutoproject.framework.common.api.profile.ProfileLookup
import plutoproject.framework.common.builddata.BuildInfoImpl
import plutoproject.framework.common.connection.ExternalConnectionConfig
import plutoproject.framework.common.connection.GeoIpConnectionImpl
import plutoproject.framework.common.connection.MongoConnectionImpl
import plutoproject.framework.common.databasepersist.ContainerRepository
import plutoproject.framework.common.databasepersist.DataChangeStream
import plutoproject.framework.common.databasepersist.DatabasePersistImpl
import plutoproject.framework.common.databasepersist.InternalDatabasePersist
import plutoproject.framework.common.feature.FeatureManagerImplV2
import plutoproject.framework.common.profile.ProfileLookupImpl
import plutoproject.framework.common.profile.ProfileRepository
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
    single<FeatureManager> { FeatureManagerImplV2() }
    single<ExternalConnectionConfig> { getModuleConfig(COMMON_FRAMEWORK_RESOURCE_PREFIX, "connection") }
    single<MongoConnection> { MongoConnectionImpl() }
    single<GeoIpConnection> { GeoIpConnectionImpl() }
    single<ProfileLookup> { ProfileLookupImpl() }
    single<ProfileRepository> { ProfileRepository(MongoConnection.getCollection("framework_profile_profiles")) }
    single<BuildInfo> { BuildInfoImpl() }
    single { DatabasePersistImpl() } binds arrayOf(DatabasePersist::class, InternalDatabasePersist::class)
    single<ContainerRepository> { ContainerRepository(MongoConnection.getCollection("database_persist_containers")) }
    single { DataChangeStream() }
}
