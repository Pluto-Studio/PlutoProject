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

include("framework")
include("framework:api")
include("framework:velocity")
include("framework:paper")
include("framework:shared")
include("framework:proto")

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

include("railway")
include("railway:api")
include("railway:paper")
