package plutoproject.framework.common.util.jvm

import io.github.classgraph.ClassGraph

fun findClass(fqn: String): Class<*>? {
    return runCatching {
        Class.forName(fqn)
    }.getOrNull()
}

fun loadClassesInPackages(
    vararg packageName: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
) = ClassGraph()
    .acceptPackages(*packageName)
    .scan().use { result ->
        result.allClasses.forEach {
            runCatching {
                classLoader.loadClass(it.name)
            }
        }
    }
