plugins {
    id("plutoproject.core-conventions")
    kotlin("kapt")
    id("com.google.devtools.ksp")
}

tasks.findByName("kspKotlin")?.apply {
    outputs.cacheIf { false }
}

dependencies {
    implementation(libs.bundles.language)
    implementation(libs.bundles.mongodb)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.hoplite)
    implementation(libs.bundles.commons)
    implementation(libs.bundles.ktor.server)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.caffeine)
    implementation(libs.catppuccin)
    implementation(libs.classgraph)
    implementation(libs.geoip2)
    implementation(libs.aedile)
    implementation(libs.guava)
    implementation(libs.okio)
    implementation(libs.hash4j)
    implementation(libs.imageioWebp)
    implementation(libs.zstdJni)
    implementation(libs.charonflow)
}
