plugins {
    id("plutoproject.kotlin-library")
    id("plutoproject.kotlin-test")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":capability:profile:api"))
    api(project(":kernel:api"))
    implementation(project(":capability:mongo:api"))
    implementation(project(":foundation:common"))
    implementation(libs.bundles.mongodb)
    implementation(libs.kotlinx.serialization)
    implementation(libs.koin.core)
    implementation(libs.okhttp)
}
