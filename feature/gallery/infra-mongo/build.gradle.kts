plugins {
    id("plutoproject.test-conventions")
}

dependencies {
    api(project(":feature:gallery:core"))

    compileOnly(project(":framework-common-api"))
    compileOnly(libs.bundles.mongodb)
    compileOnly(libs.zstdJni)

    testImplementation(project(":framework-common-api"))
    testImplementation(libs.bundles.mongodb)
    testImplementation(libs.testcontainers.mongodb)
    testImplementation(libs.zstdJni)
}
