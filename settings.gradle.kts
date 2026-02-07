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

// New hierarchical feature layout (DDD template)
include(":feature:whitelist-v2:api")
include(":feature:whitelist-v2:core")
include(":feature:whitelist-v2:infra-mongo")
include(":feature:whitelist-v2:infra-messaging")
include(":feature:whitelist-v2:paper")
include(":feature:whitelist-v2:velocity")

project(":feature:whitelist-v2:api").projectDir = file("feature/whitelist-v2/api")
project(":feature:whitelist-v2:core").projectDir = file("feature/whitelist-v2/core")
project(":feature:whitelist-v2:infra-mongo").projectDir = file("feature/whitelist-v2/infra-mongo")
project(":feature:whitelist-v2:infra-messaging").projectDir = file("feature/whitelist-v2/infra-messaging")
project(":feature:whitelist-v2:paper").projectDir = file("feature/whitelist-v2/paper")
project(":feature:whitelist-v2:velocity").projectDir = file("feature/whitelist-v2/velocity")

include("platform-paper")
include("platform-velocity")
