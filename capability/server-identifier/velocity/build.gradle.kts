plugins {
    id("plutoproject.kotlin-library")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(project(":capability:server-identifier:common"))
    implementation(project(":kernel:api"))
}
