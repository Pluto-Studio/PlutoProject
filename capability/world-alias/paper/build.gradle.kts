plugins {
    id("plutoproject.paper-conventions")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(project(":capability:world-alias:api"))
    implementation(project(":kernel:api"))
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.hocon)
}
