plugins {
    id("plutoproject.paper")
    id("plutoproject.runtime-module")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.kernel.api.paper)
    implementation(projects.foundation.common)
    implementation(projects.foundation.paper)
    implementation(projects.capability.mongo.api)
    implementation(projects.capability.serverIdentifier.api)
    implementation(projects.capability.legacyCloudCommands.api.paper)
    implementation(projects.feature.teleport.api.paper)
    implementation(projects.feature.home.api.paper)
    implementation(projects.feature.warp.api.paper)
    implementation(projects.feature.back.api.paper)
    implementation(projects.feature.randomTeleport.api.paper)
    implementation(libs.adventureKt)
    implementation(libs.bundles.hoplite)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.cloud)
    implementation(libs.bundles.mongodb)
    implementation(libs.bundles.mccoroutine.paper)
}
