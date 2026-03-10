package plutoproject.feature.gallery.core

/**
 * 一个用于存储若干个 Tile 的数据结构，每个 [StaticImageData] 或 [AnimatedImageData] 持有一个。
 *
 * @see StaticImageData
 * @see AnimatedImageData
 */
class TilePool(
    /**
     * 每个 Tile Data 在 [blob] 中距离开头的字节偏移量。
     *
     * 数组大小始终为 `tileCount + 1`，最后一个元素作为 sentinel，
     * 用于表示最后一个 Tile Data 的结束位置。
     *
     * 第 `n` 个 Tile Data 在 [blob] 中的字节范围为：
     *
     * ```kotlin
     * [offsets[n], offsets[n + 1])
     * ```
     *
     * 其长度为：
     *
     * ```kotlin
     * offsets[n + 1] - offsets[n]
     * ```
     */
    val offsets: IntArray,

    /**
     * 连续存放所有 Tile Data 的字节数组。
     *
     * [offsets] 的第 n 个 Tile 的数据范围为：
     *
     * ```kotlin
     * [offsets[n], offsets[n + 1])
     * ```
     *
     * 每个 Tile Data 的布局（按字段顺序）：
     *
     * ```text
     * [paletteSize: 1 byte] // 0 表示 256
     * [paletteBytes: effectivePaletteSize bytes] // 每个 byte 是一个原版 Map Color
     * [segmentBytes: 2 bytes, unsigned, little-endian]
     * [segments: segmentBytes bytes]
     * ```
     *
     * `effectivePaletteSize` 决定每个像素的 palette index 的位宽（bpp, bits per pixel index）：
     *
     * ```text
     * effectivePaletteSize = (paletteSize == 0 ? 256 : paletteSize)
     * bpp = ceil(log2(effectivePaletteSize))
     * effectivePaletteSize == 1 -> bpp = 0 (整块 tile 都是 palette[0])
     * ```
     *
     * `segments` 用来表示 128*128=16384 个像素的 palette index，采用「control + payload」的分段压缩流：
     *
     * ```text
     * segments: 由多个 segment 顺序拼接而成
     * segment : [control: 1 byte][payload: N bits/bytes]
     * ```
     *
     * `control` 的格式：
     *
     * ```text
     * control = 1 bit mode + 7 bits (len - 1)
     * mode: 1 = run, 0 = literal
     * len: 本段代表的像素数量，范围 [1..128]
     *      由于控制字节只有 7 位来存长度，所以存的是 (len - 1)。
     *      例如：len=1 -> 写 0；len=128 -> 写 127。
     * ```
     *
     * - run：payload 为 1 个 index（占 bpp bits），输出该 index 重复 len 次。
     * - literal：payload 为 len 个 index（共 len*bpp bits），按顺序输出。
     *
     * index bits 采用 bit packing（高位在前）：
     * - payload 紧跟在 control 之后，不做额外 padding。
     * - 整个 `segments` 在结尾用 0 补齐到字节边界。
     */
    val blob: ByteArray,
)
