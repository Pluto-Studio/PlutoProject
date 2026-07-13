plugins {
    id("plutoproject.kotlin-library")
    id("plutoproject.kotlin-test")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.foundation.common)
    implementation(projects.capability.databasePersist.api)
    implementation(projects.capability.mongo.api)
    implementation(projects.capability.serverIdentifier.api)
    implementation(libs.bundles.mongodb)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koin.core)
}
