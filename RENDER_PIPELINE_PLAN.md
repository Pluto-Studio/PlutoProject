# Gallery Render Pipeline Plan

## 范围

- 实现“预渲染”：静态图/动图 -> `StaticImageData` / `AnimatedImageData`（`TilePool + tileIndexes`）
- 不包含：上传与输入校验、Image 创建/CRUD、播放 loop

## 既定决策（写死的约束）

- RenderPipeline：固定骨架 + `RenderProfile` 选择各步实现；不允许自定义步骤顺序
- UseCase：core 只提供 `suspend`；不暴露 `Deferred`；并发由 adapter 层 `async` + `Dispatchers.Default.limitedParallelism(n)` 控制
- Animated：GIF delay 单位为 `1/100s`；`effectiveDelayMillis = max(delay*10, minDelayMillis)`，默认 `minDelayMillis = 20`
- AnimatedImageData：仅存 `frameCount + durationMillis`（无 per-frame duration）；render 端需把源帧重采样成“等间隔输出帧序列”（repeat 表达长 delay）
- TilePool：unique tiles 上限 `65536`；超出返回错误 Status（例如 `UNIQUE_TILE_OVERFLOW`）
- TilePool paletteSize：1 byte，约定 `paletteSize == 0` 表示 `256 colors`
- MapColor：透明只输出 byte `0`；byte `1..3` 视为透明别名，永不作为非透明映射结果；非透明候选默认用 `4..247`
- TileDedup hash：使用外部库 hash4j 的 `XXH3_64bits`（Java API：`com.dynatrace.hash4j.hashing.Hashing.xxh3_64()`）

## 里程碑与分点计划

### TODO（当前跟踪）

- [x] Milestone 0：核心类型与契约
- [x] Milestone 1：MapColor 调色板与 RGB565 查表
- [x] Milestone 2：TilePool 编码/解码工具
- [x] Milestone 3：TilePoolBuilder + Dedup 阶段
- [x] Milestone 4：几何阶段（Reposition + Scale）
- [x] Milestone 5：Alpha 处理 + MapColorQuantize（含抖动策略）
- [x] Milestone 6：TileSplit（128x128）与静态渲染贯通
- [x] Milestone 7：FrameSampler + 动图渲染贯通
- [ ] Milestone 8：性能与内存收敛

### Milestone 0：核心类型与契约

- [x] 在 `feature/gallery/core` 定义输入/输出/配置类型：`RgbaImage8888`、`RenderProfile`、`RenderResult/Status`
- [x] 定义 UseCases：`RenderStaticImageUseCase`、`RenderAnimatedImageUseCase`（`suspend fun execute(...)`）
- [x] 明确并写入 KDoc：像素 row-major；tile 顺序左->右/上->下；Animated 帧拼接顺序
- [x] 单元测试：数组长度与溢出检查（tileCount/frameCount/tileIndexes 长度），Status 分支覆盖

### Milestone 1：MapColor 调色板与 RGB565 查表

- [x] 在 core 实现 `MapColorPalette`（`candidates: ByteArray`、`rgbOfMapColor: IntArray(256)`）
- [x] 实现调色板来源：只内置每个 baseColor 的 HIGH RGB（62 个），用固定亮度系数推导 4 个 modifier（与 `MINECRAFT_MAP_COLORS.md` 一致）
- [x] 实现 `calcRgb565ToMapColor(): ByteArray(65536)`；tie-break 规则固定（距离相等取 byte 更小）
- [x] 测试：查表输出永不为 `1..3`；构建稳定可复现；抽样点映射结果稳定

### Milestone 2：TilePool 编码/解码工具

- [x] 实现 `encodeTile`：`ByteArray(16384)` mapColor -> TilePool tile bytes（palette + segmentBytes + segments）
- [x] 实现 `decodeTile`：TilePool tile bytes -> `ByteArray(16384)`（用于测试/必要时 dedup 碰撞验证）
- [x] 边界处理：`paletteSize==0 => 256`；`paletteSize==1 => bpp==0 => segments 可为空`；`segmentBytes` 为 U16 little-endian
- [x] 测试：encode->decode 字节级一致；覆盖 paletteSize `1/2/16/255/256` 与 run/literal 组合

### Milestone 3：TilePoolBuilder + Dedup 阶段

- [x] 引入依赖 `com.dynatrace.hash4j:hash4j`（通过 `gradle/libs.versions.toml` 版本目录管理）
- [x] 实现 `TilePoolBuilder`：追加 tile bytes，维护 `offsets/blob`，`build(): TilePool`
- [x] 实现 `TileDeduper`：
  - 输入：tile 原始 mapColor（来自 splitter 的 work buffer/view）
  - 输出：tilePoolIndex（U16，写入 `ShortArray tileIndexes`）
  - 约束：unique tiles > 65536 => `Status.UNIQUE_TILE_OVERFLOW`
- [x] Hash 策略：使用 hash4j `XXH3_64bits` 对 `ByteArray(16384)` tile mapColor 计算 `Long` hash；碰撞时用比较/解码校验保证正确性
- [x] 测试：重复 tile 复用 index；可控碰撞分支仍正确；溢出错误

### Milestone 4：几何阶段（Reposition + Scale）

- [x] 定义 `Repositioner` 接口与 3 种策略：cover/contain/stretch（输出 dest->src transform 参数）
- [x] 定义 `Scaler` 接口：Bilinear；缩小采用逐半接近再收敛（mipmap）
- [x] 输出统一为目标分辨率：`width = mapXBlocks*128`、`height = mapYBlocks*128`，仍为 RGBA8888（`IntArray`）
- [x] 测试：cover/contain 的边界采样点、缩放后尺寸、极端宽高比不崩

### Milestone 5：Alpha 处理 + MapColorQuantize（含抖动策略）

- [x] 定义 `AlphaCompositor`：`alpha==0 -> transparent`；`alpha>0` 与背景色合成到 RGB（背景色在 `RenderProfile`）
- [x] 定义 `MapColorQuantizer`：输出 `ByteArray finalPixels`；内部支持策略：
  - None：直接查 `rgb565ToMapColor`
  - Ordered：Bayer 阈值矩阵扰动后查表
  - Floyd：误差扩散；每像素 quantize 得到 mapColor，再用 `rgbOfMapColor` 还原量化色计算误差；固定点实现，避免 float/GC
- [x] 约束：非透明像素永不输出 `0..3`；透明像素只输出 `0`
- [x] 测试：透明边缘合成；ordered/floyd 输出范围与确定性；floyd 按整图处理（不按 tile 并行）

### Milestone 6：TileSplit（128x128）与静态渲染贯通

- [x] 实现 `TileSplitter`：遍历 tile 顺序；用可复用 work buffer（避免 per-tile allocation）把 128x128 bytes 喂给 Deduper
- [x] 实现 `RenderStaticImageUseCase`：串起 Milestone 4/5/6 + dedup/builder，产出 `StaticImageData(tilePool, tileIndexes)`
- [x] 测试：tileIndexes 顺序/长度；同色大图应产出很小 tilePool；`mapXBlocks/mapYBlocks` 边界

### Milestone 7：FrameSampler + 动图渲染贯通

- [x] 定义 `FrameSampler`（挂在 `RenderProfile`）：输入 GIF delays（1/100s）并应用 `minDelayMillis=20`，输出等间隔输出帧序列 `outToSourceFrameIndex`
- [x] 默认采样：`repeat = ceil(effectiveDelayMillis / frameSampleIntervalMillis)`，`frameSampleIntervalMillis` 默认 20；可加 `maxOutFrameCount` 保护
- [x] `durationMillis = sum(effectiveDelayMillis)`
- [x] 实现 `RenderAnimatedImageUseCase`：
  - 对每个“用到的 source frame”只渲染一次（memoize），repeat/outFrame 直接复制 tileIndexes 段
  - 复用同一个 `TilePoolBuilder` 做跨帧 dedup
  - 产出 `AnimatedImageData(frameCount, durationMillis, tilePool, tileIndexes)`
- [x] 测试：repeat 生效（长 delay 帧产生多个 out frame 且 tileIndexes 相同）；跨帧 tile 复用；溢出报错

### Milestone 8：后续优化计划（暂不实施）

- [ ] `OPT-01` Tile 编码（GC/CPU 热点）
  - 位置：`feature/gallery/core/src/main/kotlin/plutoproject/feature/gallery/core/render/tile/TileEncoder.kt`
  - 现状：`encodeTile(...)` 每个 tile 都会分配 `IntArray(TILE_PIXEL_COUNT)`（16384 int ≈ 64KB），以及 `IntArray(256)`、`ByteArray(256)`、`BitWriter` 内部 buffer、`segments ByteArray`、最终 `tileData ByteArray`
  - 影响：切块后每张图是 `mapXBlocks*mapYBlocks` 个 tile；动图每个“被采样到的源帧”也会跑一遍，GC 压力明显
  - 优化方向：引入 per-render workspace，复用 palette 映射、literal/run 临时缓存、bit writer buffer；编码走“写入复用 buffer + 返回长度”模式；尽量避免整 tile 级 `pixelPaletteIndexes` 大数组

- [ ] `OPT-02` MapColor 查表构建缓存（启动/首次渲染成本）
  - 位置：`feature/gallery/core/src/main/kotlin/plutoproject/feature/gallery/core/render/mapcolor/MapColorQuantizer.kt`
  - 现状：`newDefaultMapColorQuantizer()` 每次会 `MapColorPalette.vanilla()` + `calcRgb565ToMapColor(...)`（65536 * candidates 距离比较）
  - 影响：如果 renderer 不是单例复用，会重复做重活
  - 优化方向：palette + table 做全局 lazy 单例/缓存（线程安全、仅构建一次）

- [ ] `OPT-03` AlphaCompositor / Quantizer 中间大数组（峰值内存 + 分配）
  - 位置：
    - `feature/gallery/core/src/main/kotlin/plutoproject/feature/gallery/core/render/mapcolor/AlphaCompositor.kt`
    - `feature/gallery/core/src/main/kotlin/plutoproject/feature/gallery/core/render/mapcolor/MapColorQuantizer.kt`
  - 现状：每帧分配 `rgb24Pixels IntArray` + `transparentMask BooleanArray` + `mapColorPixels ByteArray`
  - 优化方向：
    - 轻量：workspace 复用这些数组
    - 激进：将“alpha 合成 + 量化”融合为单 pass，直接从 `RgbaImage8888.pixels` 写 `ByteArray mapColorPixels`

- [ ] `OPT-04` Floyd–Steinberg 误差缓冲复用
  - 位置：`feature/gallery/core/src/main/kotlin/plutoproject/feature/gallery/core/render/mapcolor/MapColorQuantizer.kt`
  - 现状：每次 `quantizeFloydSteinberg` 分配 6 个 `IntArray(width+2)`
  - 优化方向：放入 workspace，按最大宽度扩容复用（逐行 `fill(0)` 保持）

- [ ] `OPT-05` FrameSampler 装箱/集合优化（中等）
  - 位置：`feature/gallery/core/src/main/kotlin/plutoproject/feature/gallery/core/render/FrameSampler.kt`
  - 现状：使用 `ArrayList<Int>` 累积输出帧映射（发生装箱）
  - 优化方向：两遍扫描先算 `outFrameCount` 再一次性分配 `IntArray`，或引入无装箱 `IntArrayBuilder`

- [ ] `OPT-06` 几何缩放的 double/floor 计算优化（中等，偏 CPU）
  - 位置：`feature/gallery/core/src/main/kotlin/plutoproject/feature/gallery/core/render/geometry/BilinearScaler.kt`
  - 现状：每像素执行 double 计算、`floor`、4 点采样与插值
  - 优化方向：预计算每列 sourceX/每行 sourceY，增量步进替代重复除法，必要时自定义 `fastFloor`

- [ ] `OPT-07` TileDeduper 容器优化（视 unique tile 数量）
  - 位置：`feature/gallery/core/src/main/kotlin/plutoproject/feature/gallery/core/render/tile/TileDeduper.kt`
  - 现状：`HashMap<Long, IntIndexBucket>` 会有 Long 装箱与节点对象开销
  - 优化方向：必要时替换为 primitive long->bucket 的 open addressing map，或至少按预估 tile 数预设容量

- [ ] `OPT-08` Profile/安全阈值（防极端输入打爆）
  - 位置：`RenderProfile` / `FrameSampler` / Animated 渲染入口
  - 现状：缺少 `maxOutFrameCount`、`maxDurationMillis` 等硬阈值
  - 优化方向：增加可配置或固定阈值，并返回明确状态码，限制极端 GIF 带来的计算/内存消耗

- [ ] `OPT-09` 基准与画像（验收基线）
  - 位置：`feature/gallery/core` 基准与记录文档
  - 现状：尚无系统化基准数据
  - 优化方向：补充典型场景（1x1、4x4、8x8；静态/动图）的耗时、分配、峰值内存与结果记录

## 文件/包建议（实现时可调整）

- core：
  - `plutoproject.feature.gallery.core.render`（pipeline/profile/stages/workspace）
  - `plutoproject.feature.gallery.core.render.mapcolor`（palette/rgb565 table/quantize）
  - `plutoproject.feature.gallery.core.render.tile`（split/dedup/builder/encoder/decoder）
  - `plutoproject.feature.gallery.core.usecase`（render use cases）
- adapter（后续集成 decode；不作为本计划的必做项）：
  - `adapter-paper`：把上传字节流解码成 `RgbaImage8888` / frames+delays，再调用 use case
