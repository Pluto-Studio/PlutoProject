plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin)
    implementation(libs.kotlin.serialization)
    implementation(libs.gremlin.gradle)
    implementation(libs.paperweight.userdev)
    // implementation(libs.resourceFactory)
    implementation(libs.compose)
    implementation(libs.compose.compiler)
    implementation(libs.protobuf)
    implementation(libs.ksp)
    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
