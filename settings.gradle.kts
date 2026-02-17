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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
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
includeProject(":feature:whitelist-v2:api")
includeProject(":feature:whitelist-v2:core")
includeProject(":feature:whitelist-v2:adapter-common")
includeProject(":feature:whitelist-v2:infra-mongo")
includeProject(":feature:whitelist-v2:adapter-paper")
includeProject(":feature:whitelist-v2:adapter-velocity")

include("platform-paper")
include("platform-velocity")
