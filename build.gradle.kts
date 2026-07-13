import java.time.Instant
import org.gradle.jvm.tasks.Jar

plugins {
    id("plutoproject.build-logic")
    id("plutoproject.distribution")
    id("plutoproject.dokka-conventions")
}

allprojects {
    repositories {
        mavenCentral()
        google()
        maven("https://jitpack.io")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.nostal.ink/repository/maven-public")
        maven("https://repo.lucko.me/")
        maven("https://maven.playpro.com/")
        maven("https://repo.opencollab.dev/main/")
        maven("https://repo.codemc.org/repository/maven-public")
    }
}

dependencies {
    implementation(projects.platform.common)
    implementation(projects.platform.paper)
    implementation(projects.platform.velocity)
}

tasks.shadowJar {
    archiveClassifier.set(null as String?)
    mergeServiceFiles()
    dependsOn("generateModulePackageIndex")
    from(tasks.named("generateModulePackageIndex")) {
        into("META-INF/plutoproject")
    }
}

val jarIdentityFile = layout.buildDirectory.file("generated-resources/jar-identity/plutoproject_jar_identity")
val generateJarIdentity = tasks.register("generateJarIdentity") {
    outputs.file(jarIdentityFile)
    doLast {
        jarIdentityFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText("")
        }
    }
}

tasks.withType<Jar>().configureEach {
    dependsOn(generateJarIdentity)
    from(jarIdentityFile) {
        into("/")
    }
}


val gitCommitProvider = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}
val gitBranchProvider = providers.exec {
    commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
}
val buildTimestampProvider = providers.provider {
    Instant.now().toEpochMilli()
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "PlutoProject-Version" to project.version,
            "PlutoProject-Release-Name" to "${project.version}",
            "PlutoProject-Release-Channel" to "stable",
            "PlutoProject-Git-Commit" to gitCommitProvider.standardOutput.asText.map { it.trim() },
            "PlutoProject-Git-Branch" to gitBranchProvider.standardOutput.asText.map { it.trim() },
            "PlutoProject-Build-Time" to buildTimestampProvider
        )
    }
}
