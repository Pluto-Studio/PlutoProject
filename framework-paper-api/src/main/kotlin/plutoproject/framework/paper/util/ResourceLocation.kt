package plutoproject.framework.paper.util

import net.minecraft.resources.ResourceLocation

fun resourceLocationOf(namespace: String, path: String): ResourceLocation =
    ResourceLocation.fromNamespaceAndPath(namespace, path)

fun resourceLocationOf(namespacedPath: String): ResourceLocation {
    val namespace = namespacedPath.substringBefore(':')
    val key = namespacedPath.substringAfter(':')
    return resourceLocationOf(namespace, key)
}
