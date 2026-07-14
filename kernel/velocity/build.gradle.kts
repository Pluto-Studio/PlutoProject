plugins {
    id("plutoproject.velocity")
    id("plutoproject.test")
}

dependencies {
    api(project(":kernel:common"))
    api(project(":kernel:api:velocity"))
    implementation(libs.kotlinx.coroutine.core)
    testImplementation(libs.velocity.api)
}
