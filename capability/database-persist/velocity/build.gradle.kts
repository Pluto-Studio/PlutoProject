plugins {
    id("plutoproject.velocity")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(project(":capability:database-persist:common"))
    implementation(project(":kernel:api"))
    implementation(project(":kernel:api:velocity"))
}
