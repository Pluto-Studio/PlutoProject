plugins {
    id("plutoproject.paper-conventions")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(project(":capability:server-statistics:api"))
    implementation(project(":kernel:api"))
}
