plugins {
    id("plutoproject.kotlin-library")
    id("plutoproject.kotlin-test")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":capability:database-persist:api"))
    api(project(":kernel:api"))
    implementation(project(":capability:mongo:api"))
    implementation(project(":capability:server-identifier:api"))
    implementation(libs.bundles.mongodb)
    implementation(libs.kotlinx.serialization)
    implementation(libs.koin.core)
}
