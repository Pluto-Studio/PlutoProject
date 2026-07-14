package plutoproject.buildlogic

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateModulePackageIndexTask : DefaultTask() {
    @get:Input
    abstract val classDirectoriesByFamily: MapProperty<String, List<String>>

    @get:Input
    abstract val descriptorDirectoriesByFamily: MapProperty<String, List<String>>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classDirectories: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val descriptorDirectories: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val moduleIds = descriptorDirectoriesByFamily.get().mapValues { (family, directories) ->
            directories.asSequence()
                .flatMap(::descriptorFiles)
                .sortedBy(Path::toString)
                .map(::readModuleId)
                .firstOrNull()
                ?: throw GradleException("No runtime module descriptor found for $family")
        }

        val owners = linkedMapOf<String, String>()
        classDirectoriesByFamily.get().toSortedMap().forEach { (family, directories) ->
            val moduleId = moduleIds[family]
                ?: throw GradleException("No module ID found for $family")
            directories.asSequence()
                .flatMap(::classFiles)
                .mapNotNull(::packageName)
                .distinct()
                .sorted()
                .forEach { packageName ->
                    val previous = owners.putIfAbsent(packageName, moduleId)
                    if (previous != null && previous != moduleId) {
                        throw GradleException(
                            "Package '$packageName' belongs to both module '$previous' and '$moduleId'",
                        )
                    }
                }
        }

        val output = outputFile.get().asFile.toPath()
        Files.createDirectories(output.parent)
        Files.write(
            output,
            owners.toSortedMap().map { (packageName, moduleId) -> "$packageName\t$moduleId" },
            Charsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE,
        )
    }

    private fun descriptorFiles(directory: String): Sequence<Path> = sequence {
        val root = Path.of(directory)
        if (!Files.isDirectory(root)) return@sequence
        Files.walk(root).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".json") }
                .toList()
                .forEach { yield(it) }
        }
    }

    private fun classFiles(directory: String): Sequence<Path> = sequence {
        val root = Path.of(directory)
        if (!Files.isDirectory(root)) return@sequence
        Files.walk(root).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".class") }
                .toList()
                .forEach { yield(it) }
        }
    }

    private fun readModuleId(file: Path): String {
        val content = Files.readString(file)
        return moduleIdPattern.find(content)?.groupValues?.get(1)
            ?: throw GradleException("Runtime module descriptor '$file' has no module ID")
    }

    private fun packageName(file: Path): String? {
        if (file.fileName.toString() == "module-info.class") return null
        val relative = classDirectories.files.asSequence()
            .mapNotNull { directory ->
                val root = directory.toPath()
                file.normalize().takeIf { it.startsWith(root) }?.let { root.relativize(it) }
            }
            .minByOrNull(Path::getNameCount)
            ?: return null
        return relative.parent
            ?.toString()
            ?.replace(file.fileSystem.separator, ".")
            ?.takeIf(String::isNotBlank)
    }

    private companion object {
        val moduleIdPattern = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"")
    }
}
