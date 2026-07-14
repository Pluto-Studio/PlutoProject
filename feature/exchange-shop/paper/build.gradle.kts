plugins {
    id("plutoproject.paper")
    id("plutoproject.runtime-module")
    id("plutoproject.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.kernel.api.paper)
    implementation(projects.foundation.common)
    implementation(projects.foundation.paper)
    implementation(projects.capability.mongo.api)
    implementation(projects.capability.serverIdentifier.api)
    implementation(projects.capability.databasePersist.api)
    implementation(projects.capability.interactive.api)
    implementation(projects.capability.legacyCloudCommands.api.paper)
    implementation(projects.feature.menu.api.paper)
    implementation(projects.feature.daily.paper)
    implementation(projects.feature.exchangeShop.api.paper)
    implementation(libs.adventureKt)
    implementation(libs.bundles.hoplite)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.mccoroutine.paper)
    implementation(libs.bundles.mongodb)
    implementation(libs.bundles.voyager)
}
