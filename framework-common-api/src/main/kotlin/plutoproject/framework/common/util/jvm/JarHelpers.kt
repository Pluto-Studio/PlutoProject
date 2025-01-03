package plutoproject.framework.common.util.jvm

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

fun extractFileFromJar(filePathInJar: String, outputPath: Path): File {
    val inputStream: InputStream? = object {}.javaClass.classLoader.getResourceAsStream(filePathInJar)
    if (inputStream == null) {
        error("File not found in jar: $filePathInJar")
    }
    val outputFile = outputPath.toFile()
    outputFile.parentFile?.mkdirs()
    inputStream.use { input ->
        val copyOption = if (outputFile.exists()) {
            StandardCopyOption.ATOMIC_MOVE
        } else {
            StandardCopyOption.REPLACE_EXISTING
        }
        Files.copy(input, outputFile.toPath(), copyOption)
    }
    if (!(outputFile.exists())) {
        error("Unable to extract file from jar (source: $filePathInJar, to: $outputPath)")
    }
    return outputFile
}
