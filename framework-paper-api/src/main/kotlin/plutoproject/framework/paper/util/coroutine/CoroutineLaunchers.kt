package plutoproject.framework.paper.util.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.entity.Entity
import plutoproject.framework.common.util.coroutine.PlutoCoroutineScope
import plutoproject.framework.paper.util.server

@Suppress("UNUSED")
fun <T> runSync(
    coroutineScope: CoroutineScope = PlutoCoroutineScope,
    block: suspend CoroutineScope.() -> T,
) = coroutineScope.async(server.coroutineContext) { block() }

@Suppress("UNUSED")
fun <T> Entity.runSync(
    coroutineScope: CoroutineScope = PlutoCoroutineScope,
    block: suspend CoroutineScope.() -> T,
) = coroutineScope.async(coroutineContext) { block() }

@Suppress("UNUSED")
fun <T> Chunk.runSync(
    coroutineScope: CoroutineScope = PlutoCoroutineScope,
    block: suspend CoroutineScope.() -> T,
) = coroutineScope.async(coroutineContext) { block() }

@Suppress("UNUSED")
fun <T> Location.runSync(
    coroutineScope: CoroutineScope = PlutoCoroutineScope,
    block: suspend CoroutineScope.() -> T,
) = coroutineScope.async(coroutineContext) { block() }

@Suppress("UNUSED")
suspend fun <T> withSync(block: suspend CoroutineScope.() -> T) =
    withContext(server.coroutineContext) { block() }

@Suppress("UNUSED")
suspend fun <T> Entity.withSync(block: suspend CoroutineScope.() -> T) =
    withContext(coroutineContext) { block() }

@Suppress("UNUSED")
suspend fun <T> Chunk.withSync(block: suspend CoroutineScope.() -> T) =
    withContext(coroutineContext) { block() }

@Suppress("UNUSED")
suspend fun <T> Location.withSync(block: suspend CoroutineScope.() -> T) =
    withContext(coroutineContext) { block() }
