package plutoproject.feature.gallery.core.decode

import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeBytes

const val WEBP_1X1_TRANSPARENT_BASE64: String =
    // 1x1 transparent WebP sample used for decode smoke test.
    // Source: public minimal sample string (RIFF/WEBP, VP8L) from Dirask snippet.
    // Expected decoded image: width=1, height=1, alpha=0.
    "UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA=="

const val GIF_PATCH_TIMELINE_BASE64: String =
    // Generated via jshell + ImageIO GIF writer.
    // logicalScreen=2x1; frames:
    //   1) patch 2x1 @ (0,0), red, delay=1cs, disposal=none
    //   2) patch 1x1 @ (1,0), green, delay=7cs, disposal=none
    "R0lGODlhAgABAPAAAP8AAP8AACH5BAABAAAALAAAAAACAAEAQAgFAAEACAgAIfkEAAcAAAAsAQAAAAEAAQDAAP8AAP8ACAQAAQQEADs="

const val GIF_RESTORE_BACKGROUND_CLIPPED_BASE64: String =
    // Generated via jshell + ImageIO GIF writer.
    // logicalScreen=2x1; frames:
    //   1) patch 2x1 @ (0,0), red, delay=1cs, disposal=none
    //   2) patch 2x1 @ (1,0), green, delay=0cs, disposal=restoreToBackgroundColor
    //      (right half is out-of-bounds to exercise clipping)
    //   3) patch 1x1 @ (0,0), blue, delay=1cs, disposal=none
    "R0lGODlhAgABAPAAAP8AAP8AACH5BAABAAAALAAAAAACAAEAQAgFAAEACAgAIfkECAAAAAAsAQAAAAIAAQDAAP8AAP8ACAUAAQAICAAh+QQAAQAAACwAAAAAAQABAMAAAP8AAP8IBAABBAQAOw=="

const val GIF_RESTORE_PREVIOUS_BASE64: String =
    // Generated via jshell + ImageIO GIF writer.
    // logicalScreen=2x1; frames:
    //   1) patch 2x1 @ (0,0), red, delay=1cs, disposal=none
    //   2) patch 1x1 @ (1,0), green, delay=1cs, disposal=restoreToPrevious
    //   3) patch 1x1 @ (0,0), blue, delay=1cs, disposal=none
    "R0lGODlhAgABAPAAAP8AAP8AACH5BAABAAAALAAAAAACAAEAQAgFAAEACAgAIfkEDAEAAAAsAQAAAAEAAQDAAP8AAP8ACAQAAQQEACH5BAABAAAALAAAAAABAAEAwAAA/wAA/wgEAAEEBAA7"

const val GIF_TRANSPARENT_PATCH_OVERLAY_BASE64: String =
    // Generated via jshell + ImageIO GIF writer.
    // logicalScreen=2x1; frames:
    //   1) patch 2x1 @ (0,0), red, delay=1cs, disposal=none
    //   2) patch 1x1 @ (1,0), transparent, delay=1cs, disposal=none
    // Expected: frame2 keeps previous red pixel at x=1 instead of clearing to transparent.
    "R0lGODlhAgABAPAAAP8AAP8AACH5BAABAAAALAAAAAACAAEAQAgFAAEACAgAIfkEAQEAAAAsAQAAAAEAAQDAAAAAAAAACAQAAQQEADs="

fun decodeBase64(value: String): ByteArray = Base64.getDecoder().decode(value)

suspend inline fun <T> withTempImageFile(bytes: ByteArray, crossinline block: suspend (Path) -> T): T {
    val path = Files.createTempFile("gallery-decode-", ".img")
    path.writeBytes(bytes)
    return try {
        block(path)
    } finally {
        path.deleteIfExists()
    }
}
