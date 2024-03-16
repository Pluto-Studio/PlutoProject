plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java-library")
    kotlin("plugin.serialization") version "1.9.21"
    kotlin("jvm") version "1.9.21"
    id("xyz.jpenilla.run-paper") version "2.2.2"
    id("xyz.jpenilla.run-velocity") version "2.2.2"
}

fun kotlinDep(s: String): String {
    return "org.jetbrains.kotlin.$s"
}

allprojects {

    apply {
        plugin("java")
        plugin("com.github.johnrengelman.shadow")
        plugin("java-library")
        plugin(kotlinDep("jvm"))
        plugin(kotlinDep("plugin.serialization"))
    }

    val bukkitAPIVersion by extra("1.20")

    this.group = "ink.pmc.common"
    this.version = "1.0.0"

    repositories {
        mavenCentral()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        maven {
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
    }

    dependencies {
        // api("org.jetbrains.kotlinx:kotlinx-serialization-json")
        // api("org.jetbrains.kotlinx:kotlinx-serialization-toml")
        // api("org.jetbrains.kotlinx:kotlinx-serialization-hocon")
        compileOnlyApi("com.squareup.okhttp3:okhttp:5.0.0-alpha.12")
        compileOnlyApi("com.google.code.gson:gson:2.10.1")
        compileOnlyApi("org.mongojack:mongojack:4.8.2")
        compileOnlyApi("org.mongodb:mongodb-driver-sync:4.11.1")
        compileOnlyApi("com.github.ben-manes.caffeine:caffeine:3.1.8")
        compileOnlyApi("com.catppuccin:catppuccin-palette:1.0.0")
        compileOnlyApi("org.incendo:cloud-paper:2.0.0-beta.2")
        compileOnlyApi("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

        compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    }

    tasks.processResources {
        filesMatching("plugin.yml") {
            expand("version" to rootProject.version, "api" to bukkitAPIVersion)
        }
    }

    tasks.shadowJar {
        clearOutputsDir()
        archiveClassifier = ""
        onlyIf { project != rootProject && !project.name.startsWith("common-library-") }
        destinationDirectory.set(file("$rootDir/build-outputs"))
    }
}

fun copyJars() {
    val outputsDir = file("$rootDir/build-outputs")

    outputsDir.listFiles()!!.forEach {
        if (it.name.startsWith("common-library-")) {
            return@forEach
        }

        val folder = file("$rootDir/run/plugins/")

        if (!folder.exists()) {
            folder.mkdirs()
        }

        val target = File(folder, it.name)

        if (target.exists()) {
            target.delete()
        }

        it.copyTo(target)
    }
}

fun clearOutputsDir() {
    val dir = file("$rootDir/build-outputs")

    if (!dir.exists()) {
        dir.mkdirs()
    }

    file(file("$rootDir/build-outputs")).listFiles()!!.forEach {
        it.delete()
    }
}

fun Task.runTest(task: Task) {
    group = "pluto develop testing"
    dependsOn(allprojects.map { it.tasks.named("shadowJar") })

    doLast {
        copyJars()
        task.actions.forEach { it.execute(task) }
    }
}

tasks.register("Paper") {
    runTest(tasks.runServer.get())
}

tasks.register("Folia") {
    runTest(tasks.named("runFolia").get())
}

tasks.register("Velocity") {
    runTest(tasks.runVelocity.get())
}

runPaper.folia.registerTask()
runPaper.disablePluginJarDetection()

tasks.runServer.configure {
    minecraftVersion("1.20.4")
}

tasks.runVelocity {
    velocityVersion("3.1.2-SNAPSHOT")
}