import java.io.File

plugins {
    id("plutoproject.test")
}

dependencies {
    implementation(project(":kernel:api"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ksp.api)
    testImplementation(gradleTestKit())
}

tasks.test {
    val processorJar = tasks.jar
    val kernelApiJar = project(":kernel:api").tasks.named("jar")
    val processorRuntimeClasspath = configurations.runtimeClasspath
    dependsOn(processorJar, kernelApiJar)
    doFirst {
        val processorClasspath = listOf(processorJar.get().archiveFile.get().asFile) +
            processorRuntimeClasspath.get().files
        systemProperty("moduleProcessor.classpath", processorClasspath.joinToString(File.pathSeparator))
        systemProperty(
            "kernelApi.jar",
            kernelApiJar.get().outputs.files.singleFile.absolutePath,
        )
    }
}
