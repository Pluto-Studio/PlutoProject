plugins {
    id("plutoproject.test-conventions")
}

dependencies {
    api(project(":feature:whitelist-v2:core"))

    compileOnly(project(":framework-common-api"))
    implementation(libs.bundles.mongodb)

    testImplementation(project(":framework-common-api"))
    testImplementation(libs.bundles.mongodb)
    testImplementation(libs.testcontainers.mongodb)
}
