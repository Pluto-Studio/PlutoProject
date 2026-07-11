plugins {
    id("plutoproject.kotlin-test")
}

dependencies {
    api(project(":kernel:api"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutine.core)
    implementation(libs.kotlinx.serialization)
}
