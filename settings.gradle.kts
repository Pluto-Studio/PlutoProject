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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "plutoproject"

/*
include("misc")
include("misc:api")
include("misc:paper")

include("hypervisor")
include("hypervisor:api")
include("hypervisor:paper")

include("protocol-checker")
include("protocol-checker:velocity")

include("essentials")
include("essentials:paper")
include("essentials:velocity")
include("essentials:api")

include("menu")
include("menu:api")
include("menu:paper")

include("daily")
include("daily:api")
include("daily:paper")

include("whitelist")
include("whitelist:velocity")

include("runtime")
include("runtime:paper")
include("runtime:velocity")

include("server-selector")
include("server-selector:paper")
include("server-selector:shared")
include("server-selector:velocity")
*/
includeBuild("build-logic")
include("catalog")

include("framework-common-api")
include("framework-paper-api")
include("framework-velocity-api")
include("framework-common")
include("framework-paper")
include("framework-velocity")
include("framework-proto")

include("feature-common-api")
include("feature-paper-api")
include("feature-velocity-api")
include("feature-common")
include("feature-paper")
include("feature-velocity")
include("feature-proto")

include("platform-paper")
include("platform-velocity")
