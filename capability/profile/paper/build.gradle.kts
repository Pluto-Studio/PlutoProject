plugins {
    id("plutoproject.paper")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(project(":capability:profile:common"))
    implementation(project(":kernel:api"))
}
