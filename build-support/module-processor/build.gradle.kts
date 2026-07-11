plugins {
    id("plutoproject.kotlin-test")
}

dependencies {
    implementation(project(":kernel:api"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.ksp.api)
    testImplementation(gradleTestKit())
}

tasks.test {
    val processorJar = tasks.jar
    val kernelApiJar = project(":kernel:api").tasks.named("jar")
    dependsOn(processorJar, kernelApiJar)
    doFirst {
        systemProperty("moduleProcessor.jar", processorJar.get().archiveFile.get().asFile.absolutePath)
        systemProperty(
            "kernelApi.jar",
            kernelApiJar.get().outputs.files.singleFile.absolutePath,
        )
    }
}
