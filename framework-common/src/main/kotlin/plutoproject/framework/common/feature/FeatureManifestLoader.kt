package plutoproject.framework.common.feature

import kotlinx.serialization.json.Json
import okio.buffer
import okio.source
import plutoproject.framework.common.api.feature.FeatureMetadata
import plutoproject.framework.common.util.PlatformType
import java.net.JarURLConnection
import java.util.jar.JarFile

internal object FeatureManifestLoader {
    private const val IDENTITY_FILE = "plutoproject_jar_identity"

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun load(platformType: PlatformType): Map<String, FeatureMetadata> {
        val platformId = platformType.identifier
        val modularPrefix = "META-INF/plutoproject/features/$platformId/"
        val legacyPath = "$platformId-features.json"

        val merged = LinkedHashMap<String, LoadedFeatureMetadata>()

        val jar = locatePlutoJarFile()
        if (jar == null) {
            logger.warning(
                "无法定位到 PlutoProject 插件自身的 JAR（缺少 $IDENTITY_FILE 资源，或类加载器不支持通过 JarURLConnection 反查 JAR 路径）。" +
                    "将尝试仅加载旧版清单：$legacyPath"
            )

            val legacyContent = readClasspathResourceOrNull(legacyPath) ?: return emptyMap()
            val legacy = FeatureManifestSource(legacyPath, legacyContent)
            mergeInto(merged, legacy)
            return merged.mapValues { it.value.metadata }
        }

        jar.use { jf ->
            val modularEntries = jf.entries().asSequence()
                .map { it.name }
                .filter { it.startsWith(modularPrefix) && it.endsWith(".json") }
                .filter {
                    val relative = it.removePrefix(modularPrefix)
                    relative.isNotBlank() && !relative.contains('/')
                }
                .sorted()
                .toList()

            modularEntries.forEach { entryName ->
                val content = jf.getInputStream(jf.getEntry(entryName)).source().buffer().readUtf8()
                mergeInto(merged, FeatureManifestSource(entryName, content))
            }

            jf.getEntry(legacyPath)?.let { legacyEntry ->
                val content = jf.getInputStream(legacyEntry).source().buffer().readUtf8()
                mergeInto(merged, FeatureManifestSource(legacyPath, content))
            }
        }

        return merged.mapValues { it.value.metadata }
    }

    private fun mergeInto(
        target: LinkedHashMap<String, LoadedFeatureMetadata>,
        source: FeatureManifestSource,
    ) {
        val list = try {
            json.decodeFromString<List<FeatureMetadata>>(source.content)
        } catch (e: Exception) {
            throw IllegalStateException(
                "解析 Feature 清单失败：${source.path}。请检查 JSON 格式是否正确，以及是否符合 FeatureMetadata 的 schema。",
                e,
            )
        }
        list.forEach { meta ->
            val existing = target[meta.id]
            if (existing == null) {
                target[meta.id] = LoadedFeatureMetadata(meta, source.path)
                return@forEach
            }

            if (existing.metadata.entrypoint != meta.entrypoint) {
                error(
                    "检测到重复的 Feature ID：'${meta.id}'，且来自多个清单文件。" +
                        "但入口类不一致：'${existing.metadata.entrypoint}'（${existing.source}）" +
                        " vs '${meta.entrypoint}'（${source.path}）"
                )
            }

            if (existing.metadata != meta) {
                error(
                    "检测到重复的 Feature ID：'${meta.id}'，且来自多个清单文件，但元数据内容不一致。" +
                        "来源：${existing.source}、${source.path}"
                )
            }
        }
    }

    private fun readClasspathResourceOrNull(path: String): String? {
        val stream = FeatureManifestLoader::class.java.classLoader.getResourceAsStream(path) ?: return null
        return stream.source().buffer().use { it.readUtf8() }
    }

    private fun locatePlutoJarFile(): JarFile? {
        val loader = FeatureManifestLoader::class.java.classLoader
        val urls = loader.getResources(IDENTITY_FILE)
        urls.asSequence().forEach { url ->
            val connection = url.openConnection()
            val jarConnection = connection as? JarURLConnection ?: return@forEach
            val jarFileUrl = jarConnection.jarFileURL
            val jarPath = runCatching { java.nio.file.Paths.get(jarFileUrl.toURI()).toString() }.getOrNull()
                ?: jarFileUrl.toURI().path

            val jar = runCatching { JarFile(jarPath) }.getOrNull() ?: return@forEach
            if (jar.getEntry(IDENTITY_FILE) != null) {
                return jar
            }
            jar.close()
        }
        return null
    }

    private data class FeatureManifestSource(
        val path: String,
        val content: String,
    )

    private data class LoadedFeatureMetadata(
        val metadata: FeatureMetadata,
        val source: String,
    )
}
