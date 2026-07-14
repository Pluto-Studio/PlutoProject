plugins {
    id("plutoproject.test")
}

dependencies {
    api(project(":kernel:api"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutine.core)
    implementation(libs.kotlinx.serialization.json)
}
