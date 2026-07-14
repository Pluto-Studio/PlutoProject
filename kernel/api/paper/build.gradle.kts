plugins {
    id("plutoproject.paper")
}

dependencies {
    api(project(":kernel:api"))
    api(libs.kotlin.stdlib)
}
