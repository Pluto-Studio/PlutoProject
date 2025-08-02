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

include("platform-paper")
include("platform-velocity")
