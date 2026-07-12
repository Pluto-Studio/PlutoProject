plugins {
    id("plutoproject.kotlin-library")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(project(":capability:charonflow:common"))
    implementation(project(":kernel:api"))
}
