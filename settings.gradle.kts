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

fun includeProject(path: String, projectDir: String? = null) {
    include(path)

    val inferredProjectDir = when {
        projectDir != null -> projectDir
        path.startsWith(":") -> path.removePrefix(":").replace(":", "/")
        else -> path
    }

    project(path).projectDir = file(inferredProjectDir)
}

fun includeModule(type: String, id: String, vararg projects: String) {
    projects.forEach { includeProject(":$type:$id:$it") }
}

fun includeCapability(id: String, vararg projects: String) {
    includeModule("capability", id, *projects)
}

fun includeFeature(id: String, vararg projects: String) {
    includeModule("feature", id, *projects)
}

includeProject(":platform:common")
includeProject(":platform:paper")
includeProject(":platform:velocity")

includeProject(":kernel:api")
includeProject(":kernel:api:paper")
includeProject(":kernel:api:velocity")
includeProject(":kernel:common")
includeProject(":kernel:paper")
includeProject(":kernel:velocity")
includeProject(":kernel:module-processor")

includeProject(":foundation:common")
includeProject(":foundation:paper")
includeProject(":foundation:velocity")

includeCapability("mongo", "api", "common", "paper", "velocity")
includeCapability("charonflow", "api", "common", "paper", "velocity")
includeCapability("geoip", "api", "common", "paper", "velocity")
includeCapability("server-identifier", "api", "common", "paper", "velocity")
includeCapability("database-persist", "api", "common", "paper", "velocity")
includeCapability("profile", "api", "common", "paper", "velocity")
includeCapability("interactive", "paper", "api")
includeCapability("server-statistics", "api", "paper")
includeCapability("world-alias", "api", "paper")
includeCapability("legacy-cloud-commands", "api:paper", "api:velocity", "paper", "velocity")

includeFeature("whitelist", "api", "core", "common", "mongo", "paper", "velocity")
includeFeature("gallery", "api", "core", "common", "mongo", "paper", "frontend")
includeFeature("join-quit-message", "velocity")
includeFeature("motd", "velocity")
includeFeature("player-cap", "velocity")
includeFeature("version-checker", "velocity")
includeFeature("afk", "api:paper", "paper")
includeFeature("align", "paper")
includeFeature("creeper-firework", "paper")
includeFeature("elevator", "api:paper", "paper")
includeFeature("farm-protection", "paper")
includeFeature("gm", "paper")
includeFeature("hat", "paper")
includeFeature("head", "paper")
includeFeature("itemframe-protection", "paper")
includeFeature("lectern-protection", "paper")
includeFeature("menu", "api:paper", "paper")
includeFeature("teleport", "api:paper", "paper")
includeFeature("home", "api:paper", "paper")
includeFeature("warp", "api:paper", "paper")
includeFeature("back", "api:paper", "paper")
includeFeature("random-teleport", "api:paper", "paper")
includeFeature("daily", "api:paper", "paper")
includeFeature("exchange-shop", "api:paper", "paper")
includeFeature("dynamic-scheduler", "api:paper", "paper")
includeFeature("sit", "api:paper", "paper")
includeFeature("pvp-toggle", "api:paper", "paper")
includeFeature("recipe", "paper")
includeFeature("no-player-cap", "paper")
includeFeature("no-join-quit-message", "paper")
includeFeature("overload-warning", "paper")
includeFeature("suicide", "paper")
includeFeature("status", "paper")
includeFeature("dev-watermark", "paper")
includeFeature("recipe-unlock", "paper")
includeFeature("no-creeper-block-breaks", "paper")
