enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.jpenilla.xyz/snapshots")
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
}

rootProject.name = "plutoproject"

includeBuild("build-logic")
include("catalog")

include("framework-common-api")
include("framework-paper-api")
include("framework-velocity-api")
include("framework-common")
include("framework-paper")
include("framework-velocity")

include("feature-common-api")
include("feature-paper-api")
include("feature-velocity-api")
include("feature-common")
include("feature-paper")
include("feature-velocity")

fun includeProject(path: String, projectDir: String? = null) {
    include(path)

    val inferredProjectDir = when {
        projectDir != null -> projectDir
        path.startsWith(":") -> path.removePrefix(":").replace(":", "/")
        else -> path
    }

    project(path).projectDir = file(inferredProjectDir)
}

// 新版 Feature 结构部分
includeProject(":foundation:common")

includeProject(":feature:whitelist-v2:api")
includeProject(":feature:whitelist-v2:core")
includeProject(":feature:whitelist-v2:adapter-common")
includeProject(":feature:whitelist-v2:infra-mongo")
includeProject(":feature:whitelist-v2:adapter-paper")
includeProject(":feature:whitelist-v2:adapter-velocity")

includeProject(":feature:gallery:api")
includeProject(":feature:gallery:core")
includeProject(":feature:gallery:adapter-common")
includeProject(":feature:gallery:infra-mongo")
includeProject(":feature:gallery:adapter-paper")
includeProject(":feature:gallery:frontend")

includeProject(":kernel:api")
includeProject(":kernel:api:paper")
includeProject(":kernel:api:velocity")
includeProject(":kernel:common")
includeProject(":kernel:paper")
includeProject(":kernel:velocity")
includeProject(":kernel:module-processor")

includeProject(":capability:mongo:api")
includeProject(":capability:mongo:common")
includeProject(":capability:mongo:paper")
includeProject(":capability:mongo:velocity")

includeProject(":capability:charonflow:api")
includeProject(":capability:charonflow:common")
includeProject(":capability:charonflow:paper")
includeProject(":capability:charonflow:velocity")

includeProject(":capability:geoip:api")
includeProject(":capability:geoip:common")
includeProject(":capability:geoip:paper")
includeProject(":capability:geoip:velocity")

includeProject(":capability:server-identifier:api")
includeProject(":capability:server-identifier:common")
includeProject(":capability:server-identifier:paper")
includeProject(":capability:server-identifier:velocity")

includeProject(":capability:database-persist:api")
includeProject(":capability:database-persist:common")
includeProject(":capability:database-persist:paper")
includeProject(":capability:database-persist:velocity")

include("platform-paper")
include("platform-velocity")
