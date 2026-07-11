plugins {
    id("plutoproject.paper")
    id("plutoproject.kotlin-test")
}

dependencies {
    api(project(":kernel:common"))
    api(project(":kernel:api:paper"))
    implementation(libs.kotlinx.coroutine.core)
}
