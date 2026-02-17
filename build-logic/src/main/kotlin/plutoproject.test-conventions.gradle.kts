plugins {
    id("plutoproject.core-conventions")
}

dependencies {
    testImplementation(libs.bundles.language)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)

    testImplementation(libs.kotlinx.coroutine.core)
    testImplementation(libs.kotlinx.coroutine.test)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
