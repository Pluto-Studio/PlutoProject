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
include("feature-common")
include("feature-paper")

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
includeProject(":platform:common")
includeProject(":platform:paper")
includeProject(":platform:velocity")

includeProject(":foundation:common")
includeProject(":foundation:paper")
includeProject(":foundation:velocity")

includeProject(":feature:whitelist:api")
includeProject(":feature:whitelist:core")
includeProject(":feature:whitelist:common")
includeProject(":feature:whitelist:mongo")
includeProject(":feature:whitelist:paper")
includeProject(":feature:whitelist:velocity")

includeProject(":feature:gallery:api")
includeProject(":feature:gallery:core")
includeProject(":feature:gallery:common")
includeProject(":feature:gallery:mongo")
includeProject(":feature:gallery:paper")
includeProject(":feature:gallery:frontend")

includeProject(":feature:join-quit-message:velocity")
includeProject(":feature:motd:velocity")
includeProject(":feature:player-cap:velocity")
includeProject(":feature:version-checker:velocity")

includeProject(":feature:afk:api:paper")
includeProject(":feature:afk:paper")
includeProject(":feature:align:paper")
includeProject(":feature:creeper-firework:paper")
includeProject(":feature:elevator:api:paper")
includeProject(":feature:elevator:paper")
includeProject(":feature:farm-protection:paper")
includeProject(":feature:gm:paper")
includeProject(":feature:hat:paper")
includeProject(":feature:head:paper")
includeProject(":feature:itemframe-protection:paper")
includeProject(":feature:lectern-protection:paper")

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

includeProject(":capability:profile:api")
includeProject(":capability:profile:common")
includeProject(":capability:profile:paper")
includeProject(":capability:profile:velocity")

includeProject(":capability:interactive:paper")
includeProject(":capability:interactive:api")

includeProject(":capability:server-statistics:api")
includeProject(":capability:server-statistics:paper")

includeProject(":capability:world-alias:api")
includeProject(":capability:world-alias:paper")

includeProject(":capability:legacy-cloud-commands:api:paper")
includeProject(":capability:legacy-cloud-commands:api:velocity")
includeProject(":capability:legacy-cloud-commands:paper")
includeProject(":capability:legacy-cloud-commands:velocity")
