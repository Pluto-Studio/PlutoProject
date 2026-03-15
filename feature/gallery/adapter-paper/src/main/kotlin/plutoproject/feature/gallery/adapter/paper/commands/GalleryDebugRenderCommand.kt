package plutoproject.feature.gallery.adapter.paper.commands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapRenderer
import org.bukkit.map.MapView
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotation.specifier.Quoted
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.gallery.core.AnimatedImageData
import plutoproject.feature.gallery.core.StaticImageData
import plutoproject.feature.gallery.core.TilePool
import plutoproject.feature.gallery.core.decode.DecodeImageRequest
import plutoproject.feature.gallery.core.decode.DecodeResult
import plutoproject.feature.gallery.core.decode.DecodedImage
import plutoproject.feature.gallery.core.render.DitherAlgorithm
import plutoproject.feature.gallery.core.render.RepositionMode
import plutoproject.feature.gallery.core.render.RenderAnimatedImageRequest
import plutoproject.feature.gallery.core.render.RenderProfile
import plutoproject.feature.gallery.core.render.RenderResult
import plutoproject.feature.gallery.core.render.ScaleAlgorithm
import plutoproject.feature.gallery.core.render.RenderStaticImageRequest
import plutoproject.feature.gallery.core.render.tile.decodeTile
import plutoproject.feature.gallery.core.usecase.DecodeImageUseCase
import plutoproject.feature.gallery.core.usecase.RenderAnimatedImageUseCase
import plutoproject.feature.gallery.core.usecase.RenderStaticImageUseCase
import plutoproject.framework.paper.util.command.ensurePlayer
import plutoproject.framework.paper.util.coroutine.coroutineContext
import plutoproject.framework.paper.util.server
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI

@Suppress("UNUSED")
object GalleryDebugRenderCommand : KoinComponent {
    private const val PERMISSION = "plutoproject.gallery.command.debug.render"
    private const val MAX_DOWNLOAD_BYTES = 25 * 1024 * 1024
    private val decodeImageUseCase by inject<DecodeImageUseCase>()
    private val renderStaticImageUseCase by inject<RenderStaticImageUseCase>()
    private val renderAnimatedImageUseCase by inject<RenderAnimatedImageUseCase>()

    @Command("gallery debug render <url> <blocksX> <blocksY> [dither] [bgRgbHex] [scale] [reposition]")
    @Permission(PERMISSION)
    suspend fun CommandSender.render(
        @Argument("url") @Quoted url: String,
        @Argument("blocksX") blocksX: Int,
        @Argument("blocksY") blocksY: Int,
        @Argument("dither") dither: String?,
        @Argument("bgRgbHex") bgRgbHex: String?,
        @Argument("scale") scale: String?,
        @Argument("reposition") reposition: String?,
    ) = ensurePlayer {
        val profile = parseRenderProfile(
            dither = dither,
            bgRgbHex = bgRgbHex,
            scale = scale,
            reposition = reposition,
        ).getOrElse {
            sendMessage("[Gallery] Invalid render options: ${it.message}")
            sendMessage("[Gallery] dither=none|bayer|fs, bgRgbHex=RRGGBB, scale=bilinear|lanczos, reposition=cover|contain|stretch")
            return@ensurePlayer
        }

        val totalStart = System.nanoTime()
        var downloadNanos = 0L
        var decodeNanos = 0L
        var renderNanos = 0L
        var giveNanos = 0L

        val bytes = try {
            val started = System.nanoTime()
            val downloaded = downloadImageBytes(url)
            downloadNanos = System.nanoTime() - started
            downloaded
        } catch (e: Exception) {
            sendMessage(
                "[Gallery] Download failed: ${e.message}, " +
                    "total=${formatNanosMillis(System.nanoTime() - totalStart)}, " +
                    "download=${formatNanosMillis(downloadNanos)}"
            )
            return@ensurePlayer
        }

        val decodeResult = run {
            val started = System.nanoTime()
            val result = decodeImageUseCase.execute(
                DecodeImageRequest(
                    bytes = bytes,
                    fileNameHint = extractFileNameHint(url),
                )
            )
            decodeNanos = System.nanoTime() - started
            result
        }
        if (decodeResult is DecodeResult.Failure) {
            sendMessage(
                "[Gallery] Decode failed: ${decodeResult.status}, " +
                    "total=${formatNanosMillis(System.nanoTime() - totalStart)}, " +
                    "download=${formatNanosMillis(downloadNanos)}, " +
                    "decode=${formatNanosMillis(decodeNanos)}"
            )
            return@ensurePlayer
        }

        val decodedImage = (decodeResult as? DecodeResult.Success)?.data
            ?: run {
                sendMessage(
                    "[Gallery] Decode failed: empty data, " +
                        "total=${formatNanosMillis(System.nanoTime() - totalStart)}, " +
                        "download=${formatNanosMillis(downloadNanos)}, " +
                        "decode=${formatNanosMillis(decodeNanos)}"
                )
                return@ensurePlayer
            }

        val rendered = run {
            val started = System.nanoTime()
            val result = when (decodedImage) {
                is DecodedImage.Static -> renderStatic(decodedImage, blocksX, blocksY, profile)
                is DecodedImage.Animated -> renderAnimated(decodedImage, blocksX, blocksY, profile)
            }
            renderNanos = System.nanoTime() - started
            result
        }

        if (rendered is RenderResult.Failure) {
            sendMessage(
                "[Gallery] Render failed: ${rendered.status}, " +
                    "total=${formatNanosMillis(System.nanoTime() - totalStart)}, " +
                    "download=${formatNanosMillis(downloadNanos)}, " +
                    "decode=${formatNanosMillis(decodeNanos)}, " +
                    "render=${formatNanosMillis(renderNanos)}"
            )
            return@ensurePlayer
        }

        val tilePack = (rendered as? RenderResult.Success)?.imageData
            ?: run {
                sendMessage(
                    "[Gallery] Render failed: empty image data, " +
                        "total=${formatNanosMillis(System.nanoTime() - totalStart)}, " +
                        "download=${formatNanosMillis(downloadNanos)}, " +
                        "decode=${formatNanosMillis(decodeNanos)}, " +
                        "render=${formatNanosMillis(renderNanos)}"
                )
                return@ensurePlayer
            }

        val giveResult = run {
            val started = System.nanoTime()
            val result = withContext(server.coroutineContext) {
                giveTileMaps(
                    player = this@ensurePlayer,
                    mapXBlocks = blocksX,
                    mapYBlocks = blocksY,
                    tilePack = tilePack,
                    sourceUrl = url,
                )
            }
            giveNanos = System.nanoTime() - started
            result
        }

        val totalNanos = System.nanoTime() - totalStart

        sendMessage(
            "[Gallery] Done. decode=${decodeResult.status}, render=${rendered.status}, " +
                "tiles=${blocksX}x${blocksY}, dither=${profile.ditherAlgorithm}, bg=#${profile.alphaBackgroundColorRgb.toString(16).padStart(6, '0')}, " +
                "scale=${profile.scaleAlgorithm}, reposition=${profile.repositionMode}, " +
                "added=${giveResult.addedItems}, dropped=${giveResult.droppedItems}, " +
                "total=${formatNanosMillis(totalNanos)}, download=${formatNanosMillis(downloadNanos)}, " +
                "decode=${formatNanosMillis(decodeNanos)}, render=${formatNanosMillis(renderNanos)}, give=${formatNanosMillis(giveNanos)}"
        )
    }

    private suspend fun downloadImageBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        val uri = URI(url)
        val connection = (uri.toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            instanceFollowRedirects = true
        }

        connection.inputStream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) {
                    break
                }
                total += read
                if (total > MAX_DOWNLOAD_BYTES) {
                    throw IllegalArgumentException("image too large (>${MAX_DOWNLOAD_BYTES} bytes)")
                }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
    }

    private suspend fun renderStatic(
        decoded: DecodedImage.Static,
        blocksX: Int,
        blocksY: Int,
        profile: RenderProfile,
    ): RenderResult<TilePack> {
        val renderResult = renderStaticImageUseCase.execute(
            RenderStaticImageRequest(
                sourceImage = decoded.image,
                mapXBlocks = blocksX,
                mapYBlocks = blocksY,
                profile = profile,
            )
        )
        return when (renderResult) {
            is RenderResult.Failure -> RenderResult.Failure(renderResult.status)
            is RenderResult.Success -> RenderResult.Success(
                imageData = renderResult.imageData?.toTilePackForStatic(),
            )
        }
    }

    private suspend fun renderAnimated(
        decoded: DecodedImage.Animated,
        blocksX: Int,
        blocksY: Int,
        profile: RenderProfile,
    ): RenderResult<TilePack> {
        val renderResult = renderAnimatedImageUseCase.execute(
            RenderAnimatedImageRequest(
                source = decoded.source,
                mapXBlocks = blocksX,
                mapYBlocks = blocksY,
                profile = profile,
            )
        )
        return when (renderResult) {
            is RenderResult.Failure -> RenderResult.Failure(renderResult.status)
            is RenderResult.Success -> RenderResult.Success(
                imageData = renderResult.imageData?.toTilePackForFirstFrame(blocksX * blocksY),
            )
        }
    }

    private fun giveTileMaps(
        player: Player,
        mapXBlocks: Int,
        mapYBlocks: Int,
        tilePack: TilePack,
        sourceUrl: String,
    ): GiveMapsResult {
        var added = 0
        var dropped = 0

        for (y in 0 until mapYBlocks) {
            for (x in 0 until mapXBlocks) {
                val tileIndex = y * mapXBlocks + x
                val tilePoolIndex = tilePack.tileIndexes[tileIndex].toInt() and 0xFFFF
                val tilePixels = decodeTile(extractTileData(tilePack.tilePool, tilePoolIndex))
                val item = createMapItem(player, x, y, tilePixels, sourceUrl)
                val remaining = player.inventory.addItem(item)
                if (remaining.isNotEmpty()) {
                    dropped += remaining.size
                    remaining.values.forEach { stack ->
                        player.world.dropItemNaturally(player.location, stack)
                    }
                } else {
                    added += 1
                }
            }
        }

        return GiveMapsResult(addedItems = added, droppedItems = dropped)
    }

    private fun createMapItem(
        player: Player,
        tileX: Int,
        tileY: Int,
        tilePixels: ByteArray,
        sourceUrl: String,
    ): ItemStack {
        val mapView = server.createMap(player.world)
        mapView.renderers.toList().forEach(mapView::removeRenderer)
        mapView.addRenderer(FixedTileMapRenderer(tilePixels))

        val mapItem = ItemStack(Material.FILLED_MAP)
        val mapMeta = mapItem.itemMeta as MapMeta
        mapMeta.mapView = mapView
        mapMeta.setDisplayName("Gallery tile($tileX,$tileY)")
        mapMeta.lore = listOf("url=$sourceUrl", "tile=($tileX,$tileY)")
        mapItem.itemMeta = mapMeta
        return mapItem
    }

    private fun extractTileData(tilePool: TilePool, tilePoolIndex: Int): ByteArray {
        require(tilePoolIndex in 0 until tilePool.offsets.size - 1) {
            "tilePoolIndex out of bounds: $tilePoolIndex"
        }
        val start = tilePool.offsets[tilePoolIndex]
        val end = tilePool.offsets[tilePoolIndex + 1]
        return tilePool.blob.copyOfRange(start, end)
    }

    private fun extractFileNameHint(url: String): String? {
        val path = runCatching { URI(url).path }.getOrNull() ?: return null
        return path.substringAfterLast('/', missingDelimiterValue = "").ifBlank { null }
    }

    private fun parseRenderProfile(
        dither: String?,
        bgRgbHex: String?,
        scale: String?,
        reposition: String?,
    ): Result<RenderProfile> = runCatching {
        RenderProfile(
            ditherAlgorithm = parseDither(dither),
            alphaBackgroundColorRgb = parseRgb24(bgRgbHex),
            scaleAlgorithm = parseScale(scale),
            repositionMode = parseReposition(reposition),
        )
    }

    private fun parseDither(raw: String?): DitherAlgorithm {
        return when (raw?.trim()?.lowercase()) {
            null, "", "bayer", "ordered", "ordered_bayer" -> DitherAlgorithm.ORDERED_BAYER
            "none", "off" -> DitherAlgorithm.NONE
            "fs", "floyd", "floyd_steinberg" -> DitherAlgorithm.FLOYD_STEINBERG
            else -> error("unsupported dither '$raw'")
        }
    }

    private fun parseScale(raw: String?): ScaleAlgorithm {
        return when (raw?.trim()?.lowercase()) {
            null, "", "bilinear", "linear" -> ScaleAlgorithm.BILINEAR
            "lanczos" -> ScaleAlgorithm.LANCZOS
            else -> error("unsupported scale '$raw'")
        }
    }

    private fun parseReposition(raw: String?): RepositionMode {
        return when (raw?.trim()?.lowercase()) {
            null, "", "cover" -> RepositionMode.COVER
            "contain" -> RepositionMode.CONTAIN
            "stretch" -> RepositionMode.STRETCH
            else -> error("unsupported reposition '$raw'")
        }
    }

    private fun parseRgb24(raw: String?): Int {
        if (raw == null || raw.isBlank()) {
            return 0xFFFFFF
        }

        val normalized = raw.trim().removePrefix("#")
        if (normalized.length != 6 || normalized.any { !it.isDigit() && it.lowercaseChar() !in 'a'..'f' }) {
            error("bgRgbHex must be 6-digit hex, got '$raw'")
        }
        return normalized.toInt(16)
    }
}

private fun formatNanosMillis(nanos: Long): String {
    val millis = nanos / 1_000_000.0
    return "${"%.1f".format(millis)}ms"
}

private class FixedTileMapRenderer(
    private val tilePixels: ByteArray,
) : MapRenderer() {
    private var rendered = false

    @Suppress("DEPRECATION")
    override fun render(map: MapView, canvas: MapCanvas, player: Player) {
        if (rendered) {
            return
        }
        for (y in 0 until 128) {
            val base = y * 128
            for (x in 0 until 128) {
                canvas.setPixel(x, y, tilePixels[base + x])
            }
        }
        rendered = true
    }
}

private data class TilePack(
    val tilePool: TilePool,
    val tileIndexes: ShortArray,
)

private data class GiveMapsResult(
    val addedItems: Int,
    val droppedItems: Int,
)

private fun StaticImageData.toTilePackForStatic(): TilePack = TilePack(
    tilePool = tilePool,
    tileIndexes = tileIndexes,
)

private fun AnimatedImageData.toTilePackForFirstFrame(singleFrameTileCount: Int): TilePack {
    require(tileIndexes.size >= singleFrameTileCount) {
        "animated tileIndexes too short for first frame: size=${tileIndexes.size}, singleFrameTileCount=$singleFrameTileCount"
    }
    return TilePack(
        tilePool = tilePool,
        tileIndexes = tileIndexes.copyOfRange(0, singleFrameTileCount),
    )
}
