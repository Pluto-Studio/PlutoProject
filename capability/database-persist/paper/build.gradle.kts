plugins {
    id("plutoproject.paper")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(project(":capability:database-persist:common"))
    implementation(project(":kernel:api"))
    implementation(project(":kernel:api:paper"))
}
