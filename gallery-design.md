# Minecraft 动态地图系统架构总结

## 一、整体目标
实现支持 **静态图 / 动图** 的 Minecraft 地图展示系统：

- 支持任意尺寸地图（N×M maps）
- 支持 GIF 等动图
- 高效内存占用
- 最小网络更新
- 可扩展渲染参数（插值、抖动）
- 不依赖 Bukkit Scheduler（使用自定义调度 / 协程）

核心抽象：
Tile = 最小单位  
Frame = tile diff  
TilePool = tile 去重  
FrameBuffer = 当前帧展开  
UpdateJob = 时间驱动更新  

---

## 二、导入流程（Import Pipeline）

image  
→ resize 到 (mapWidth×128, mapHeight×128)  
→ RGB → MapColor 映射  
→ 切割为 128×128 tiles  
→ Tile 压缩  
→ TilePool 去重  
→ 构建 Frame（keyframe/diff/reuse）  
→ 持久化 RenderCache  

缩放策略：

- 大图使用 mipmap 思路逐级缩小
- 最终使用高质量插值（Lanczos / Bicubic）

---

## 三、MapColor 数据

Minecraft MapColor 为 **8 bit（0–255）**

结构：

bbbbbb ll

- 高 6 bit：baseColor
- 低 2 bit：brightness

解析：

brightness = color & 3  
baseColor = color >> 2  

---

## 四、Tile（最小渲染单位）

Tile = **128×128 像素**

原始大小：

16384 bytes

压缩结构：

palette + RLE

Tile 结构：

Tile  
- id  
- hash  
- palette[]  
- segments[]  

segments：

(runLength, paletteIndex)

示例：

(40,3) → 颜色3连续40像素

扫描顺序：

- 默认：row-major
- v2：Hilbert curve

---

## 五、TilePool（Unique Tile Pool）

用于 Tile 去重。

TilePool  
- Map<Hash, TileId>  
- Tile[]  

流程：

tile → hash → 查 pool → 存在复用 / 否则插入

效果：

60–90% tile 去重

---

## 六、Frame（动画帧）

Frame 类型：

KEYFRAME / DIFF / REUSE

结构：

Frame  
- type  
- baseFrame  
- tileChanges[]  
- durationMillis  

tileChanges：

(tileIndex, tileId)

REUSE：

完全引用另一帧。

---

## 七、RenderCache（渲染资源）

RenderCache  
- mapWidth  
- mapHeight  
- tileCount  
- TilePool  
- frames[]  
- totalDurationMillis  

tileCount = mapWidth × mapHeight

---

## 八、DisplayInstance（世界展示）

表示世界中的实际展示实例：

DisplayInstance  
- imageId  
- world  
- itemFrames[]  
- mapIds[]  
- chunkPosition  

多个 DisplayInstance 可共享同一 RenderCache。

---

## 九、FrameBuffer（关键内存策略）

每个展示实例仅维护 **当前帧**。

FrameBuffer  
- tiles[]  
- tileHashes[]  

tiles：

byte[16384]

示例：

8×8 maps  
64 tiles × 16KB ≈ 1MB

避免：

预展开全部帧（100 帧 ≈ 100MB）。

---

## 十、动画时间系统

时间驱动：

frameIndex = resolve(time)

支持：

- source FPS
- 可变 frame duration（GIF）

流程：

currentTime → frameIndex → apply frame diff

---

## 十一、UpdateJob（更新逻辑）

每个 DisplayInstance 运行一个 UpdateJob。

UpdateJobState  
- startedAtMillis  
- lastFrameIndex  
- lastSentTileHash[]  

流程：

1. 计算当前 frame  
2. frame 变化 → apply diff  
3. 对比 tile hash  
4. 仅发送变化 tile

---

## 十二、网络更新策略

使用：

ClientboundMapItemDataPacket

更新方式：

partial update

粒度：

tile = 128×128

若 tileHash 未变化：

不发送更新。

---

## 十三、内存模型

假设：

8×8 maps  
64 tiles  
100 frames

优化后：

TilePool ≈ 300 tiles（≈300KB）  
Frame diff ≈ 500KB  
FrameBuffer ≈ 1MB  

总计：

≈ 2MB  
（而非 100MB）

---

## 十四、调度系统

不使用 Bukkit Scheduler。

使用：

CoroutineDispatcher + UpdateJob

特性：

- 支持高 FPS
- 不绑定 20 TPS
- 控制线程池并发

---

## 十五、核心优化

1. TilePool（tile 去重）
2. Frame diff（帧差）
3. palette + RLE 压缩
4. FrameBuffer 仅当前帧
5. tile hash 跳过重复发送

v2 扩展：

- bit packing
- Hilbert scan
- tile decode cache

---

## 十六、架构原则

Tile = 最小数据单位  
Frame = tile diff  
TilePool = 去重  
FrameBuffer = 当前帧  
UpdateJob = 时间驱动  

目标：

CPU 低  
内存低  
网络低  
可扩展