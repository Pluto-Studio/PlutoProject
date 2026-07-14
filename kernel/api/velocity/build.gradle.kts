plugins {
    id("plutoproject.velocity")
}

dependencies {
    api(project(":kernel:api"))
    api(libs.kotlin.stdlib)
}
