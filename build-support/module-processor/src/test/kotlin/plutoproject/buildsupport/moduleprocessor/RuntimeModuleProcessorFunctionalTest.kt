package plutoproject.buildsupport.moduleprocessor

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class RuntimeModuleProcessorFunctionalTest {
    @TempDir
    lateinit var projectDir: Path

    @Test
    fun `valid feature generates unified paper descriptor`() {
        writeProject(
            """
            package fixture

            import plutoproject.kernel.api.Feature
            import plutoproject.kernel.api.Platform
            import plutoproject.kernel.api.RuntimeModule

            @Feature(
                id = "home",
                platform = Platform.PAPER,
                requiredFeatures = ["teleport"],
                optionalFeatures = ["menu"],
                requiredCapabilities = ["mongo"],
            )
            class HomeFeature : RuntimeModule
            """.trimIndent(),
        )

        val result = runner().withArguments("kspKotlin", "--stacktrace").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":kspKotlin")?.outcome)
        val descriptor = projectDir.resolve(
            "build/generated/ksp/main/resources/META-INF/plutoproject/modules/paper/home.json",
        )
        assertEquals(
            """{"schemaVersion":1,"id":"home","type":"FEATURE","platform":"PAPER","entrypoint":"fixture.HomeFeature","requiredFeatures":["teleport"],"optionalFeatures":["menu"],"requiredCapabilities":["mongo"]}""",
            descriptor.readText(),
        )
    }

    @Test
    fun `invalid entrypoint fails compilation with processor diagnostic`() {
        writeProject(
            """
            package fixture

            import plutoproject.kernel.api.Capability
            import plutoproject.kernel.api.Platform
            import plutoproject.kernel.api.RuntimeModule

            @Capability(id = "Invalid ID", platform = Platform.VELOCITY)
            private abstract class InvalidCapability

            @Capability(id = "object-capability", platform = Platform.VELOCITY)
            object ObjectCapability : RuntimeModule
            """.trimIndent(),
        )

        val result = runner().withArguments("kspKotlin", "--stacktrace").buildAndFail()

        assertTrue(result.output.contains("must match [a-z][a-z0-9_-]*"))
        assertTrue(result.output.contains("Entrypoint must be public"))
        assertTrue(result.output.contains("Entrypoint must implement RuntimeModule"))
        assertTrue(result.output.contains("Entrypoint must be a class, not an object or interface"))
    }

    private fun writeProject(source: String) {
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"processor-fixture\"")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            import java.io.File

            plugins {
                kotlin("jvm") version "2.3.21"
                id("com.google.devtools.ksp") version "2.3.8"
            }

            repositories { mavenCentral() }

            dependencies {
                implementation(files("${escapedProperty("kernelApi.jar")}"))
                ksp(files("${escapedProperty("moduleProcessor.classpath")}".split(File.pathSeparator)))
            }

            ksp { arg("runtimeModule.projectPath", project.path) }
            """.trimIndent(),
        )
        projectDir.resolve("src/main/kotlin/fixture/Entrypoint.kt").apply {
            parent.createDirectories()
            writeText(source)
        }
    }

    private fun escapedProperty(name: String) =
        System.getProperty(name).replace("\\", "\\\\")

    private fun runner() = GradleRunner.create()
        .withProjectDir(projectDir.toFile())
        .forwardOutput()
}
