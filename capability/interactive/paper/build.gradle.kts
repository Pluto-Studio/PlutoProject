plugins {
    id("plutoproject.paper-conventions")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(project(":capability:interactive:api"))
    implementation(project(":kernel:api"))
    implementation(project(":kernel:api:paper"))
    implementation(project(":framework-common-api"))
}
