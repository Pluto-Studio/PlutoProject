import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.register
import plutoproject.buildlogic.GenerateModulePackageIndexTask

plugins {
    id("base")
}

val generateModulePackageIndex = tasks.register<GenerateModulePackageIndexTask>("generateModulePackageIndex") {
    outputFile.set(layout.buildDirectory.file("generated-resources/module-packages/META-INF/plutoproject/module-packages.idx"))
}

gradle.projectsEvaluated {
    val runtimeProjects = rootProject.subprojects.filter {
        it.plugins.hasPlugin("plutoproject.runtime-module")
    }
    val families = runtimeProjects.map { it.path.substringBeforeLast(':') }.distinct().sorted()

    families.forEach { family ->
        val familyProjects = rootProject.subprojects.filter { it.path.startsWith("$family:") }
        val familyClassDirectories = familyProjects.flatMap { project ->
            project.extensions.findByType<SourceSetContainer>()
                ?.findByName("main")
                ?.output
                ?.classesDirs
                ?.files
                .orEmpty()
        }.distinct()
        val descriptorDirectories = runtimeProjects
            .filter { it.path.startsWith("$family:") }
            .map { it.layout.buildDirectory.dir("generated/ksp/main/resources").get().asFile }

        generateModulePackageIndex.configure {
            classDirectoriesByFamily.put(family, familyClassDirectories.map { it.absolutePath })
            descriptorDirectoriesByFamily.put(family, descriptorDirectories.map { it.absolutePath })
            this.classDirectories.from(familyClassDirectories)
            this.descriptorDirectories.from(descriptorDirectories)
            dependsOn(familyProjects.flatMap { project ->
                project.tasks.matching { task -> task.name == "classes" }
            })
        }
    }
}
