plugins {
    id("plutoproject.kotlin-library")
}

dependencies {
    implementation(projects.foundation.common)
    implementation(projects.capability.mongo.api)
    api(projects.feature.whitelist.api)
    implementation(projects.feature.whitelist.core)
    implementation(projects.feature.whitelist.mongo)
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutine.core)
    implementation(libs.bundles.mongodb)
}
