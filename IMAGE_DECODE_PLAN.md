# Gallery Image Decode Plan

## 目标

把“图片文件内容”解码成渲染阶段的统一输入：

- 静态图：`RgbaImage8888`（RGBA8888，`0xAARRGGBB`，row-major）
- 动图：`List<AnimatedSourceFrame>`（每帧 `RgbaImage8888` + `delayCentiseconds`）

支持格式：PNG、JPG/JPEG、WEBP、GIF。

## 设计思路（最终确认版）

- decode 作为独立 core UseCase：`DecodeImageUseCase`。
- core 不做文件/网络 I/O，只接收 `ByteArray`：
  - core 保持纯转换、可测试、无资源生命周期（不负责 stream 关闭/临时文件/异常泄漏）。
  - adapter 负责把 `Path/InputStream/上传数据` 读成 `ByteArray`，并可在 adapter 做更严格的 `maxBytes` 限制。
- UseCase 固定分发骨架：先识别格式（magic bytes 优先），再调用对应 decoder。
- decoder 注入写死（4 个参数）：`pngDecoder/jpgDecoder/webpDecoder/gifDecoder`。
  - 当前格式数量有限且未来大概率不扩展，写死分支可读性最好。
  - 若未来扩展格式，再考虑 list/registry。
- 动图与静态图输出不同，但不需要两套 decoder 接口：
  - 统一输出 `DecodedImage`（sealed）：`Static` / `Animated`。
  - PNG/JPEG/WEBP decoder 返回 `DecodedImage.Static`；GIF decoder 返回 `DecodedImage.Animated`。
- WEBP 采用 TwelveMonkeys（ImageIO SPI）：运行时 classpath 具备依赖即可自动支持 `ImageIO.read`。
- 先做对、做稳：尤其 GIF 不能把 `reader.read(i)` 当作最终帧，必须按 metadata 合成“时间线完整帧”。

## 范围

- 包含：解码与基础约束校验；GIF 帧合成；格式 sniff。
- 不包含：上传/权限/内容审核；图片 CRUD；播放 loop；平台 I/O；mimetype/扩展名可信校验（只作为 hint）。

## 既定决策（写死的约束）

- 输入：`ByteArray bytes` + 可选 `fileNameHint`（仅用于扩展名提示/日志）。
- 格式判断优先 magic bytes；扩展名仅作为 fallback。
- UseCase：core 只提供 `suspend`；不暴露 `Deferred`；并发由 adapter 控制。
- 依赖策略：第三方解码库按项目约定走 platform `runtimeDownload`；core 用 `compileOnly + testImplementation`。
- 硬约定 1：Decode 输出像素语义统一为 `sRGB` + `8-bit/channel` + `non-premultiplied` 的 `RGBA8888 (0xAARRGGBB)`。
  - 第一版实现优先保证“输出一致性”而不是极致性能：不做基于 `BufferedImage.type` 的分叉快路径，统一走一次像素收敛（例如 `getRGB(...)`）来避免隐藏色彩空间/像素格式差异导致的量化偏移。
- 硬约定 2：GIF 解码对 `disposalMethod=restoreToPrevious` 做容错，行为尽量贴近主流播放器。
  - 若 `restoreToPrevious` 需要回滚但当前没有可用 snapshot（未保存/不可用），则 fallback 为 `doNotDispose/none`（不回滚，不清除）。
  - GIF patch 的绘制与清理都必须做边界 clip；即使 metadata 越界也不能越界写数组或抛出异常。
- GIF delay：`delayTime` 单位为 `centiseconds`（`1/100s`），允许为 `0`；最小帧延迟的 clamp 在渲染侧 `RenderProfile.minFrameDelayMillis` 生效。

## 产物（核心类型）

- `DecodedImage`（sealed）
  - `Static(image: RgbaImage8888)`
  - `Animated(frames: List<AnimatedSourceFrame>)`
- `DecodeImageRequest`
  - `bytes: ByteArray`
  - `fileNameHint: String? = null`
  - `constraints: DecodeConstraints = DecodeConstraints()`
- `DecodeConstraints`（默认值先给“安全但不苛刻”的基线，后续可微调）
  - `maxBytes: Int = 25 * 1024 * 1024` (25 MiB)
  - `maxPixels: Int = 16_777_216` (4096 * 4096)
  - `maxFrames: Int = 500` (GIF)
- `DecodeStatus`（不复用 `RenderStatus`）
  - `SUCCEED`
  - `UNSUPPORTED_FORMAT`
  - `INVALID_IMAGE`
  - `IMAGE_TOO_LARGE`
  - `TOO_MANY_FRAMES`
  - `DECODE_FAILED`
- `DecodeResult<T>(status, data?)`（约定同 `RenderResult`）

## Milestones 与分点计划

### Milestone 0：契约与格式识别

- [x] 定义 `DecodeImageRequest/Result/Status/Constraints` 与 `DecodedImage`
- [x] 实现 `sniffFormat(bytes, fileNameHint)`（magic 优先）
  - PNG: `89 50 4E 47 0D 0A 1A 0A`
  - JPEG: `FF D8 FF`
  - GIF: `GIF87a` / `GIF89a`
  - WEBP: `RIFF....WEBP`（至少校验 RIFF + WEBP 标记）
- [x] `DecodeImageUseCase`
  - 入口约束校验：`maxBytes/maxPixels/maxFrames` + 溢出检查
  - `when(format)` 分发到对应 decoder
  - cancellation：阶段边界 `ensureActive()`；`CancellationException` 直接 rethrow
  - 兜底异常：记录并返回 `DECODE_FAILED`（logger 注入，默认 `java.util.logging.Logger`）

### Milestone 1：静态图 decoder（PNG/JPEG/WEBP）

- [ ] `PngDecoder/JpegDecoder/WebpDecoder`（内部实现可复用 ImageIO 读取逻辑）
  - `ImageIO.createImageInputStream(ByteArrayInputStream(bytes))`
  - 读 `BufferedImage`，转换为 `RgbaImage8888`
    - 快路径：`TYPE_INT_ARGB` 且 `DataBufferInt` -> 直接获取像素数组（必要时 copy）
    - fallback：`getRGB(0, 0, w, h, dst, 0, w)`
  - 校验：`w/h > 0`、`w*h` 溢出、`maxBytes/maxPixels` 超限

### Milestone 2：GIF decoder（正确合成帧）

- [ ] `GifDecoder` 输出 `DecodedImage.Animated(frames)`
- [ ] 必须输出“时间线完整帧”：优化 GIF（只存 patch）也要合成
  - 读取逻辑屏幕尺寸（全图宽高）
  - 每帧 metadata：left/top/width/height、`delayTime`（centiseconds）、`disposalMethod`
  - 维护全尺寸 ARGB canvas（`IntArray`）
  - 绘制 patch 到 canvas 指定 offset
  - disposal 处理：
    - `none/doNotDispose`: 不处理
    - `restoreToBackgroundColor`: 将当前 patch 区域清为透明
    - `restoreToPrevious`: 回滚到绘制该帧前的快照（仅在需要时保存，避免每帧复制）
  - 每帧边界 checkpoint：`ensureActive()`
- [ ] 约束：`maxFrames`、`maxPixels`（基于逻辑屏幕 `width*height`）

### Milestone 3：依赖接入（TwelveMonkeys WebP）

- [ ] 依赖坐标：`com.twelvemonkeys.imageio:imageio-webp:3.13.0`（通过 ImageIO SPI 提供 WebP reader）
- [ ] `gradle/libs.versions.toml` 增加 TwelveMonkeys WebP 坐标
- [ ] `feature/gallery/core/build.gradle.kts`：`compileOnly` + `testImplementation`
- [ ] `build-logic/...legacy-base-conventions...`：`downloadIfRequired(...)` 让 platform 运行时下载
- [ ] 目标：平台运行时 classpath 含 SPI 后，`ImageIO.read` 自动可解 WebP

### Milestone 4：测试（正确性优先）

- [ ] sniffFormat：覆盖 4 种头 + 非法输入
- [ ] PNG/JPEG：测试中动态生成 `BufferedImage` -> `ImageIO.write` 得到 bytes -> decode -> 断言宽高 + 抽样像素
- [ ] WEBP：内嵌 base64 的最小 webp 样本（避免依赖 writer）
- [ ] GIF：内嵌 base64 样本，覆盖：
  - 优化帧（非全尺寸 patch）合成正确
  - `restoreToBackgroundColor` 清除区域正确
  - `restoreToPrevious` 回滚正确
  - `delayCentiseconds` 读取正确

## 文件/包建议

- `feature/gallery/core/src/main/kotlin/plutoproject/feature/gallery/core/decode/`
  - `DecodeImageUseCase.kt`
  - `DecodeModels.kt`（request/result/status/constraints/DecodedImage）
  - `ImageFormatSniffer.kt`
  - `decoder/PngDecoder.kt`
  - `decoder/JpegDecoder.kt`
  - `decoder/WebpDecoder.kt`
  - `decoder/GifDecoder.kt`
- `feature/gallery/core/src/test/kotlin/plutoproject/feature/gallery/core/decode/`
  - `ImageFormatSnifferTest.kt`
  - `PngJpegDecoderTest.kt`
  - `WebpDecoderTest.kt`
  - `GifDecoderTest.kt`

## Adapter 集成（后续，不在本计划必做）

- adapter-paper：负责 I/O（上传/文件读取）-> `ByteArray`，做 `maxBytes`/mimetype/hint 处理，然后调用 `DecodeImageUseCase`
- 将 `DecodedImage` 映射到 `RenderStaticImageRequest` / `RenderAnimatedImageRequest`，打通全链路
