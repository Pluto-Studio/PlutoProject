# Gallery Display Runtime Plan

## 范围

- 实现地图画展示 runtime：
  - `DisplayJob` 具体实现（静态图 / 动图）
  - `DisplayManager` 的 runtime 编排与索引
  - `SendJob` 发送队列与限流循环
  - 区块加载 / 卸载时的 adapter 生命周期编排
  - `DisplayInstance` / `Image` / `ImageDataEntry` 的批量加载 UseCase
- 不包含：
  - CRUD UseCase 内自动联动 display runtime 生命周期
  - 正式缓存驱逐策略（当前阶段仍按区块卸载时主动 unload）
  - 跨 chunk 的复杂引用计数生命周期
  - 渲染与解码能力本身（已有单独 Plan）

## 既定决策（写死的约束）

- `DisplayJob` 是运行时对象；`DisplayInstance` / `Image` / `ImageDataEntry` 是共享数据对象。
- `DisplayJob` 不做 snapshot / clone / wrapper；直接持有已加载对象引用。
- `DisplayJob.wake()` 每次直接读取 `imageDataEntry.data`，因此 `ReplaceImageDataEntryUseCase` 修改后，后续 wake 可以自然看到新内容。
- 不在 CRUD UseCase 内自动调用 display runtime 的 start / stop / refresh；如需编排，由 adapter 显式调用对应 runtime UseCase。
- 业务层面的对象共享更新是允许的：如果 `Image` 或 `ImageDataEntry` 在共享对象上被修改，已运行的 `DisplayJob` 后续 wake 可以看到变化；这不视为“自动热重载 Job”。
- `DisplayJob` 持有完整三件套：`DisplayInstance`、`Image`、`ImageDataEntry`；`Image` 保留完整对象，不拆 `tileMapIds`。
- 对于同一个地图画（`belongsTo` 相同），内存中只应存在一份共享的 `Image` / `ImageDataEntry` 业务对象；后续 cache 驱逐机制需要保证“被运行中 `DisplayJob` 持有的对象不可被驱逐”。
- `DisplayJob` 自己管理 `managedDisplayInstances`；`DisplayManager` 只管理 job 注册表、实例缓存、send job 注册表与索引，不自动在 empty 时 stop `DisplayJob`。
- `DisplayJob.stop()` 是终态：
  - `stop()` 幂等
  - `attach(...)` 在 stopped 后必须抛异常
  - `detach(...)` 在 stopped 后 no-op
  - `wake()` 在 stopped 后直接返回
- `DisplayJob` 不对外暴露 `scheduleNextAwake()`；后续调度由 job 内部通过注入的 `DisplayScheduler` 完成。首次启动调度由 `StartDisplayJobUseCase` 调用 `DisplayScheduler.scheduleAwakeAt(...)`。
- 区块加载时最多 3 次数据库 IO：
  - 一次批量加载 `DisplayInstance`
  - 一次批量加载 `Image`
  - 一次批量加载 `ImageDataEntry`
- 批量查询接口返回稀疏 `Map`，缺失项直接不出现在结果中。
- 区块生命周期以 origin item frame 所在 chunk 为准；不为横跨 chunk 做额外生命周期补偿。
- 当前阶段 chunk unload 时，除了 detach / stop job，也一并 unload 相关 `DisplayInstance` / `Image` / `ImageDataEntry` 缓存；后续接入正式 cache 驱逐机制再调整。
- `SendJob` 是每玩家唯一的发送队列运行时对象；一个玩家有且只有一个 `SendJob`。
- `SendJob` 由 core 自己管理发送循环；adapter 只负责玩家进入/退出时显式 start / stop。
- `SendJob` 的 loop coroutine 是按需启停的，不常驻：
  - 队列空且无 loop 时为 `IDLING`
  - 有任务正在 drain 时为 `RUNNING`
  - `stop()` 后为 `STOPPED`
- `SendJob` 不需要类似 `DisplayScheduler` 的 reschedule signal；只要队列非空就保持 loop 运行，发空后自动退出并转 `IDLING`。
- `SendJob` 队列策略固定为“新鲜度优先”：如果入队时已达到最大队列长度，则直接清空旧队列，再把当前 update 作为新队列起点。
- `MapUpdatePort` 只代表“发送单条更新”这个最小平台 I/O 动作，不代表队列。
- `DisplayJob` 不直接依赖 `MapUpdatePort`，而是通过 `DisplayManager` 获取目标玩家的 `SendJob` 并调用 `enqueue(...)`。

## Spec

### 核心运行时接口

```kotlin
interface DisplayJob {
    val belongsTo: UUID
    val isStopped: Boolean
    val managedDisplayInstances: Map<UUID, DisplayInstance>

    fun attach(
        displayInstance: DisplayInstance,
        image: Image,
        imageDataEntry: ImageDataEntry<*>,
    )

    fun detach(displayInstanceId: UUID): DisplayInstance?

    fun isEmpty(): Boolean

    fun wake()

    fun stop()
}
```

```kotlin
interface DisplayJobFactory {
    fun create(
        image: Image,
        imageDataEntry: ImageDataEntry<*>,
    ): DisplayJob
}
```

约定：

- `attach(...)` 负责：
  - 接管一个 `DisplayInstance`
  - 首次接管时保存 `image` / `imageDataEntry` 共享引用
  - 非首次接管时做 `belongsTo` / `type` 一致性校验，不替换已有引用
- `detach(...)` 只移除实例与相关几何缓存
- `stop()` 只终止 job 自己的运行时状态，不卸载 manager 缓存

### 发送运行时接口

```kotlin
data class MapUpdate(
    val mapId: Int,
    val mapColors: ByteArray,
)
```

```kotlin
interface MapUpdatePort {
    fun send(playerId: UUID, update: MapUpdate)
}
```

```kotlin
interface SendJob {
    val playerId: UUID
    val state: SendJobState

    fun enqueue(update: MapUpdate)

    fun stop()
}
```

```kotlin
enum class SendJobState {
    RUNNING,
    IDLING,
    STOPPED,
}
```

```kotlin
interface SendJobFactory {
    fun create(playerId: UUID): SendJob
}
```

约定：

- `MapUpdate.mapColors.size == 128 * 128`
- `SendJob` 是 per-player FIFO 队列
- `SendJob.enqueue(...)` 只负责入队，不直接发送
- `SendJob` 内部 loop 每轮最多发送 `maxUpdatesInSpan` 条，然后等待 `updateLimitSpanMs`
- 若本轮发送结束后队列为空，则 loop 自动退出并转 `IDLING`
- `SendJob.stop()` 必须：
  - 转 `STOPPED`
  - 清空队列
  - 取消 loop
- stopped 后再次 `enqueue(...)` 应抛 `IllegalStateException`

### Repository 批量读取接口

```kotlin
interface DisplayInstanceRepository {
    suspend fun findById(id: UUID): DisplayInstance?
    suspend fun findByIds(ids: Collection<UUID>): Map<UUID, DisplayInstance>
    suspend fun findByBelongsTo(belongsTo: UUID): List<DisplayInstance>
    suspend fun findByChunk(chunkX: Int, chunkZ: Int): List<DisplayInstance>
    suspend fun save(displayInstance: DisplayInstance)
    suspend fun deleteById(id: UUID)
}
```

```kotlin
interface ImageRepository {
    suspend fun findById(id: UUID): Image?
    suspend fun findByIds(ids: Collection<UUID>): Map<UUID, Image>
    suspend fun findByOwner(owner: UUID): List<Image>
    suspend fun save(image: Image)
    suspend fun deleteById(id: UUID)
}
```

```kotlin
interface ImageDataEntryRepository {
    suspend fun findByBelongsTo(belongsTo: UUID): ImageDataEntry<*>?
    suspend fun findByBelongsToIn(belongsToList: Collection<UUID>): Map<UUID, ImageDataEntry<*>>
    suspend fun save(entry: ImageDataEntry<*>)
    suspend fun deleteByBelongsTo(belongsTo: UUID)
}
```

### Manager 批量缓存与运行时索引

#### `ImageManager` 新增方法

```kotlin
class ImageManager {
    fun getLoadedImages(ids: Collection<UUID>): Map<UUID, Image>
    fun loadImages(images: Collection<Image>)
    fun unloadImages(ids: Collection<UUID>): List<Image>

    fun getLoadedImageDataEntries(belongsToList: Collection<UUID>): Map<UUID, ImageDataEntry<*>>
    fun loadImageDataEntries(entries: Collection<ImageDataEntry<*>>)
    fun unloadImageDataEntries(belongsToList: Collection<UUID>): List<ImageDataEntry<*>>
}
```

#### `DisplayManager` 新增方法

```kotlin
class DisplayManager {
    fun getLoadedDisplayInstances(ids: Collection<UUID>): Map<UUID, DisplayInstance>
    fun loadDisplayInstances(displayInstances: Collection<DisplayInstance>)
    fun unloadDisplayInstances(ids: Collection<UUID>): List<DisplayInstance>

    fun getLoadedDisplayJob(belongsTo: UUID): DisplayJob?
    fun getLoadedDisplayJobs(): List<DisplayJob>

    fun registerDisplayJob(job: DisplayJob): DisplayJob
    fun removeDisplayJob(belongsTo: UUID): DisplayJob?

    fun bindDisplayInstanceToJob(displayInstanceId: UUID, belongsTo: UUID)
    fun unbindDisplayInstanceFromJob(displayInstanceId: UUID): UUID?
    fun getJobBelongsToByDisplayInstanceId(displayInstanceId: UUID): UUID?

    fun getLoadedSendJob(playerId: UUID): SendJob?
    fun getLoadedSendJobs(): List<SendJob>

    fun registerSendJob(job: SendJob): SendJob
    fun removeSendJob(playerId: UUID): SendJob?
}
```

约定：

- `DisplayManager` 不自动 stop empty `DisplayJob`
- `bind/unbind` 只维护索引，不调用 `attach/detach`
- `DisplayManager` 同时承担 display runtime 与 send runtime 的注册表职责，不额外拆 `SendJobManager`

### 批量加载 UseCase

```kotlin
class GetDisplayInstancesByIdsUseCase(
    private val displayInstances: DisplayInstanceRepository,
    private val displayManager: DisplayManager,
) {
    sealed class Result {
        data class Ok(val displayInstances: Map<UUID, DisplayInstance>) : Result()
    }

    suspend fun execute(ids: Collection<UUID>): Result
}
```

```kotlin
class GetImagesByIdsUseCase(
    private val images: ImageRepository,
    private val imageManager: ImageManager,
) {
    sealed class Result {
        data class Ok(val images: Map<UUID, Image>) : Result()
    }

    suspend fun execute(ids: Collection<UUID>): Result
}
```

```kotlin
class GetImageDataEntriesByBelongsToUseCase(
    private val entries: ImageDataEntryRepository,
    private val imageManager: ImageManager,
) {
    sealed class Result {
        data class Ok(val entries: Map<UUID, ImageDataEntry<*>>) : Result()
    }

    suspend fun execute(belongsToList: Collection<UUID>): Result
}
```

约定：

- 先查 loaded cache，再只对 missing 子集查 repo
- 查到的 missing 对象要回填 manager cache
- 结果始终返回完整合并后的 `Map`

### Display Runtime 生命周期 UseCase

```kotlin
class StartDisplayJobUseCase(
    private val clock: Clock,
    private val displayScheduler: DisplayScheduler,
    private val displayManager: DisplayManager,
    private val displayJobFactory: DisplayJobFactory,
) {
    sealed class Result {
        data class Ok(val job: DisplayJob) : Result()
        data class AlreadyStarted(val job: DisplayJob) : Result()
    }

    fun execute(
        displayInstance: DisplayInstance,
        image: Image,
        imageDataEntry: ImageDataEntry<*>,
    ): Result
}
```

```kotlin
class StopDisplayJobUseCase(
    private val displayScheduler: DisplayScheduler,
    private val displayManager: DisplayManager,
) {
    sealed class Result {
        data class Ok(val job: DisplayJob) : Result()
        data object NotStarted : Result()
    }

    fun execute(belongsTo: UUID): Result
}
```

```kotlin
class AttachDisplayInstanceToJobUseCase(
    private val displayManager: DisplayManager,
) {
    sealed class Result {
        data class Ok(val job: DisplayJob) : Result()
        data object JobNotStarted : Result()
    }

    fun execute(
        displayInstance: DisplayInstance,
        image: Image,
        imageDataEntry: ImageDataEntry<*>,
    ): Result
}
```

```kotlin
class DetachDisplayInstanceFromJobUseCase(
    private val displayManager: DisplayManager,
) {
    sealed class Result {
        data class Ok(
            val job: DisplayJob,
            val detachedDisplayInstance: DisplayInstance?,
        ) : Result()

        data object JobNotStarted : Result()
    }

    fun execute(displayInstanceId: UUID): Result
}
```

约定：

- 这 4 个 UseCase 都不做数据库 IO
- `StartDisplayJobUseCase`
  - 校验三者 `belongsTo` / `type` 一致
  - factory 创建 job
  - register job
  - attach first instance
  - bind instance -> job
  - `displayScheduler.scheduleAwakeAt(job, clock.instant())`
- `StopDisplayJobUseCase`
  - `displayScheduler.unschedule(job)`
  - 解绑该 job 管理的全部 `displayInstanceId`
  - `job.stop()`
  - 从 manager 移除 job
- `AttachDisplayInstanceToJobUseCase`
  - 根据 `displayInstance.belongsTo` 查已启动 job
  - `job.attach(...)`
  - `bindDisplayInstanceToJob(...)`
- `DetachDisplayInstanceFromJobUseCase`
  - 通过 `displayInstanceId -> belongsTo -> job`
  - `job.detach(...)`
  - `unbindDisplayInstanceFromJob(...)`

### Send Runtime 生命周期 UseCase

```kotlin
class StartSendJobUseCase(
    private val displayManager: DisplayManager,
    private val sendJobFactory: SendJobFactory,
) {
    sealed class Result {
        data class Ok(val job: SendJob) : Result()
        data class AlreadyStarted(val job: SendJob) : Result()
    }

    fun execute(playerId: UUID): Result
}
```

```kotlin
class StopSendJobUseCase(
    private val displayManager: DisplayManager,
) {
    sealed class Result {
        data class Ok(val job: SendJob) : Result()
        data object NotStarted : Result()
    }

    fun execute(playerId: UUID): Result
}
```

约定：

- `StartSendJobUseCase`
  - 若已存在则返回 `AlreadyStarted`
  - 否则 factory 创建并 register
- `StopSendJobUseCase`
  - `job.stop()`
  - 从 manager 移除

### 具体 Job 类型

```kotlin
class StaticDisplayJob(...) : DisplayJob
class AnimatedDisplayJob(...) : DisplayJob
class DefaultSendJob(...) : SendJob
```

#### `StaticDisplayJob` 运行时状态

- `managedDisplayInstances`
- `displayGeometryByInstanceId`
- `receivedMapIdsByPlayer`
- `image`
- `imageDataEntry`

#### `AnimatedDisplayJob` 运行时状态

- `managedDisplayInstances`
- `displayGeometryByInstanceId`
- `animationStartedAt`
- `lastSentPoolIndexes`
- `image`
- `imageDataEntry`

#### `DefaultSendJob` 运行时状态

- `playerId`
- `state`
- `queue: ArrayDeque<MapUpdate>`
- `loopJob`
- `maxQueueSize`
- `maxUpdatesInSpan`
- `updateLimitSpanMs`

约定：

- 两种 `DisplayJob` 都直接依赖注入：
  - `DisplayScheduler`
  - `ViewPort`
  - `Clock`
  - `DisplayManager`
- decode 在 core 内通过现有 `decodeTile(...)` 完成
- 可视判断继续使用 `DisplayGeometry.computeVisibleTiles(...)`
- `DisplayJob` 在 `wake()` 中对每个玩家调用：
  - `displayManager.getLoadedSendJob(playerId)?.enqueue(update)`
  - 若该玩家没有 `SendJob`，则直接跳过

## Milestones 与分点计划

### TODO（当前跟踪）

- [x] Milestone 0：运行时契约与共享对象语义
- [x] Milestone 1：批量读取仓储与批量加载 UseCase
- [x] Milestone 2：DisplayManager runtime 注册表与索引
- [x] Milestone 3：SendJob 与单条发送 Port
- [x] Milestone 4：StaticDisplayJob 贯通
- [x] Milestone 5：AnimatedDisplayJob 贯通
- [x] Milestone 6：Display / Send 生命周期 UseCase
- [ ] Milestone 7：Adapter-Paper 生命周期接线
- [ ] Milestone 8：测试、构建与手动验收

### Milestone 0：运行时契约与共享对象语义

- [x] 扩展 `DisplayJob` 目标契约：`belongsTo`、`isStopped`、`attach`、`detach`、`isEmpty`、`stop`
- [x] 定义 `DisplayJobFactory`
- [x] 定义 `MapUpdate`、`MapUpdatePort`、`SendJob`、`SendJobFactory`
- [x] 在 KDoc 明确：
  - `DisplayJob` 持有共享对象引用，不做 clone / snapshot
  - `wake()` 每次直接读取 `imageDataEntry.data`
  - CRUD 不自动编排 display lifecycle
  - empty job 不自动 stop
  - stopped `DisplayJob` 不可复活
- [x] 单元测试：`attach/detach/stop` 的一致性校验、重复 stop 幂等、stopped 后 attach 抛异常

### Milestone 1：批量读取仓储与批量加载 UseCase

- [x] 为 `DisplayInstanceRepository` 增加 `findByIds`
- [x] 为 `ImageRepository` 增加 `findByIds`
- [x] 为 `ImageDataEntryRepository` 增加 `findByBelongsToIn`
- [x] 补 Mongo 实现与对应测试
- [x] 为 `ImageManager` / `DisplayManager` 增加批量 cache helper
- [x] 实现：
  - `GetDisplayInstancesByIdsUseCase`
  - `GetImagesByIdsUseCase`
  - `GetImageDataEntriesByBelongsToUseCase`
- [x] 测试：
  - loaded + missing 混合场景
  - 结果回填 cache
  - 稀疏 map 返回正确
  - `ChunkLoad` 最坏情况下只需 3 次 DB IO

### Milestone 2：DisplayManager runtime 注册表与索引

- [x] 为 `DisplayManager` 增加：
  - `loadedJobsByBelongsTo`
  - `displayInstanceId -> belongsTo` job 索引
  - `loadedSendJobsByPlayerId`
- [x] 实现 `register/remove/bind/unbind/query` 方法
- [x] 保留现有 `DisplayInstance` 缓存索引逻辑不变
- [x] 测试：
  - register / remove `DisplayJob`
  - register / remove `SendJob`
  - bind / unbind displayInstance 与 job
  - runtime 索引与数据缓存索引互不污染

### Milestone 3：SendJob 与单条发送 Port

- [x] 在 core 实现 `DefaultSendJob`
- [x] 在 core 实现 `DefaultSendJobFactory`
- [x] 在 `adapter-paper` 实现 `MapUpdatePort.send(playerId, update)`
- [x] `DefaultSendJob` 接入：
  - `maxQueueSize`
  - `maxUpdatesInSpan`
  - `updateLimitSpanMs`
  - 队列溢出清空策略
  - 按需启停 loop
- [x] 测试：
  - enqueue 正常入队
  - 超过 `maxQueueSize` 时旧 backlog 被清空
  - 队列发空后自动转 `IDLING`
  - stop 后不再接受 enqueue

### Milestone 4：StaticDisplayJob 贯通

- [x] 实现 `StaticDisplayJob`
- [x] 流程：
  - 读取 world 玩家视图
  - 对每个 managed instance 算可视 `TileRect`
  - 汇总每玩家可见 tile
  - 用 `receivedMapIdsByPlayer` 过滤已发送内容
  - `decodeTile(...)`
  - 生成 `MapUpdate`
  - 获取玩家 `SendJob` 并 `enqueue`
  - 若仍非 empty，schedule 下一次 awake
- [x] 测试：
  - 不可视不发
  - 首次可视会发
  - 已收到 mapId 不重复发
- [ ] 后续补充：多实例同图汇总正确

### Milestone 5：AnimatedDisplayJob 贯通

- [x] 实现 `AnimatedDisplayJob`
- [x] 流程：
  - 计算动画当前帧
  - 与 `lastSentPoolIndexes` diff
  - 对每玩家按可视 rect 过滤
  - `decodeTile(...)`
  - 生成 `MapUpdate`
  - 获取玩家 `SendJob` 并 `enqueue`
  - 更新动画状态
  - 按 `maxFramePerSecond` 预算 schedule 下一次 awake
- [x] 约束：
  - `maxFramePerSecond == -1` 表示不限制
  - `0` 或其他非法负数在构造时拒绝
- [x] 测试：
  - 帧推进正确
  - diff 增量发送正确
  - budget / 超时 / `-1` 语义正确

### Milestone 6：Display / Send 生命周期 UseCase

- [x] 实现：
  - `StartDisplayJobUseCase`
  - `StopDisplayJobUseCase`
  - `AttachDisplayInstanceToJobUseCase`
  - `DetachDisplayInstanceFromJobUseCase`
  - `StartSendJobUseCase`
  - `StopSendJobUseCase`
- [x] 明确 UseCase 分工：
  - display runtime UseCase 只做 runtime 操作
  - send runtime UseCase 只做 send job 生命周期
  - 两者都不做 DB IO
- [x] 测试：
  - start display 时创建 job、attach 首实例、首次 schedule
  - attach 已启动 job 成功
  - detach 后 job 仍保留
  - stop display 时 unschedule + stop + remove
  - start / stop send job 正常

### Milestone 7：Adapter-Paper 生命周期接线

- [ ] 实现 chunk PDC 索引读取/写入工具
- [ ] `ChunkLoad` 编排：
  - 从 PDC 读 `displayInstanceIds`
  - 批量加载 `DisplayInstance`
  - 批量加载 `Image`
  - 批量加载 `ImageDataEntry`
  - 按 `belongsTo` 分组
  - job 不存在则 start；存在则 attach
- [ ] `ChunkUnload` 编排：
  - 从 PDC 读 `displayInstanceIds`
  - 找受影响 job
  - 预判 detach 后是否 empty
  - empty 则 stop；否则 detach
  - 当前阶段同步 unload 对应 `DisplayInstance` / `Image` / `ImageDataEntry`
- [ ] `PlayerJoin`：start `SendJob`
- [ ] `PlayerQuit`：stop `SendJob`
- [ ] `PluginDisable`：
  - stop 所有 `SendJob`
  - stop 所有 `DisplayJob`
  - stop `DisplayScheduler`
- [ ] 验证：一个区块多个 instance 但同图时，只启动 1 个 `DisplayJob`

### Milestone 8：测试、构建与手动验收

- [ ] 补齐 core 单元测试：
  - batch repo / usecase
  - manager runtime registry
  - send job
  - static / animated display job
  - lifecycle usecases
- [ ] 补 adapter 集成测试或最小可测替身
- [ ] 执行 `./gradlew shadowJar`
- [ ] 执行 `./gradlew test`
- [ ] 手动验收：
  - origin chunk load 时 display 正常开始更新
  - origin chunk unload 时 display 停止更新
  - 同图多实例只跑一个 `DisplayJob`
  - 玩家进入后存在唯一 `SendJob`
  - 玩家退出后 `SendJob` 正常 stop
  - `SendJob` backlog 溢出时旧任务被清空
  - 替换 `ImageDataEntry.data` 后，已运行 display 后续 wake 可看到变化
  - 静态图不会重复给同玩家发送已收过的 map
  - 动图按预期 FPS / budget 播放

## 文件/包建议（实现时可调整）

- core：
  - `plutoproject.feature.gallery.core`（DisplayJob / SendJob / Manager / runtime models）
  - `plutoproject.feature.gallery.core.usecase`（batch loading / runtime lifecycle usecases）
- adapter-paper：
  - chunk PDC 索引读写
  - chunk / player / plugin lifecycle listener
  - `MapUpdatePort` 的 Paper 实现

---

## 附录：原始流程说明（保留）

我现在正在设计我地图画系统的展示模块，请你评估并回答我的疑问，然后给出建议。

首先了解四种数据结构：
- Image -> 代表一副地图画，存储存储元数据（如创建者、名称等等），其自身不存储图像数据，意味着可以时刻从数据库里读取使用而不用担心 IO 时间 / 内存占用。
- AnimatedImageData & StaticImageData -> 图像数据纯数据类，自身只持有 tilePool、frameCount、tileIndexes 等字段（取决于是 Animated 还是 Static），其不与任何地图画关联，只代表数据本身。两者是独立的类，没有共同基类。
- ImageDataEntry -> 代表一个和地图画关联了的图像数据。其有一个 belongsTo（持有这份图像数据的地图 ID）和 imageData（实际图像数据对象）。ImageData 在存入数据库时会包装成 ImageDataEntry。
- DisplayInstance -> 代表一个地图画展出，一副地图画可以有多个展出。其有一个独立的 id: UUID 字段和一个 belongsTo 字段。

此外，还有运行时任务管理：
- DisplayScheduler -> 负责唤醒服务器上加载的所有 DisplayJob（调用 `onAwake`），其自身占用一个 Coroutine Job 并在 `loadedDisplayJobs` 不为空时保持循环。
- DisplayJob -> 代表一个显示任务基类，主要生命周期函数为 `onAwake`，会在到达或超过计划的时间戳后由 DisplayScheduler 执行。
- AnimatedDisplayJob & StaticDisplayJob -> 动图和静态图更新任务的各自实现，继承 DisplayJob。

对于每个加载了至少一个 DisplayInstance 的地图画，都会有一个对应的 DisplayJob 实例，该实例负责管理该地图画所有的 DisplayInstance。

当放出地图画时，会创建一个对应的 DisplayInstance 并写入数据库。以及 DisplayInstance 会存储一个索引在 Chunk 的 Persistant Data Container 内（Bukkit 提供的功能），在加载区块的时候先获取这个区块所有的 DisplayInstance 索引，再决定要不要从数据库读取实际 Instance 数据。全部存在区块内/全部存在数据库内都有各自的问题，这算是最好的折中做法。
DisplayInstance 所属的区块取决于地图画的第一个 Tile（也就是 0,0）所在的区块，因此对于横跨多个区块的 Display 也可以正确处理。

当加载区块时，会先读取该区块的 Display 列表，若列表不为空，就进行一次数据库查询批量获取该区块内的所有 Instance 数据。

接下来寻找该 Instance 对应的 Image 是否已有创建的 DisplayJob。若有，则将该 Instance 加入该 DisplayJob 的接管列表，若无则先创建一个新的再加入。

以下是完整的 Display 更新步骤，以动图为例。

1. DisplayScheduler 获取预期 Awake 时间戳 <= 当前时间戳的 AnimatedDisplayJob。
2. 为每个 AnimatedDisplayJob 创建一个 Coroutine，并调用 `onAwake`。
3. 进入 `onAwake` 流程。
4. 先通过 VoxelPort 获取距离 DisplayInstance 整个地图画中心半径 64（可配置，MC 默认 Misc 实体追踪是 64）内的玩家并缓存备用，在 Bukkit 上的实现通常是 `World.getNearbyPlayers(...)`。
5. 用当前时间戳，减去动画开始时间戳，获得动画进度时间，再用动画进度时间反取对应的动画帧内容。若动画开始时间为 null 则意味着需要开始新一轮动画，此时将开始时间设为当前时间戳。
6. 用上一步取到的动画帧内容的 TilePool 索引数组和 `lastSentPoolIndexes` 数组对比，获得这一帧变化了的、要发送的 Tile ID（从上到下、从左到右的索引，约定第一块为 0）。
7. 用这些 Tile ID 从 Frame 的 TilePool 索引数组里获取每个 Tile ID 对应的 TilePool 内数据索引，用索引从 TilePool 依次取出并解码成 128*128 的 Map Color 字节数组。
8. 遍历前面备用的玩家列表，为每个玩家计算可视 Tile 列表，再获取表内所有变化了的 Tile 的 Map ID，构造 MapUpdate 并加入该玩家的发送队列。MapUpdate 会包含 Map ID 和 Map Color 数据。
9. 这一步是 DisplayJob 以外的异步任务：每个玩家自己的 MapUpdate 队列开始工作，根据指定的 `maxUpdatesInSpan` 和 `updateLimitSpanMs`，取出 <本周期应发数量> 个 MapUpdate 任务，挨个发送给该玩家。然后 delay 到下一个周期开始的时间，循环往复。
10. 判断这一轮动画是否已经播放结束，如果动画播放结束就将动画开始时间设为 null，下次唤醒时开始新一轮动画。
11. 根据 DisplayJob 构造时传入的 `maxFramePerSecond`，向 DisplayScheduler 计划下一次应该被唤醒的时间。

额外说明：
1. 静态图像的更新流程和动图基本一致。不一样的点在于没有获取当前帧内容并计算要更新的 Tile 的步骤，多了一个 `receivedMapIdsByPlayer` 字段，记录每个玩家已经收到过的 Map ID。因为 Minecraft 客户端会缓存 Map ID 对应的内容，静态图不需要更新内容，所以如果一个玩家发过了一个 Map ID 的内容，就不会再发。其他帧速控制和可视检查的逻辑都一致，静态图的场景下更新循环的意义更多在于实现「不可视不发，可视了就发」，而不是帧更新。
2. 动图更新的第 10 步实际的算法是：用 1000ms / `maxFramePerSecond`，获取一帧的 budget ms（比如 60 FPS，budget 是 ~16.67ms）。然后用本次更新结束时间减去开始时间，获取更新用时，再用 budget ms 减去更新用时算出要等待的时间（如果为负数意味着这一帧超时了，用 `maxFramePerSeconds` 为 -1 时候相同的处理方式），然后用当前时间 + 等待时间算出最终的预期唤醒时间。此外，`maxFramePerSecond` 为 -1 时意味着不限制 FPS，此时计划唤醒时间时会直接传入当前时间。因为 DisplayScheduler 实际的检查方式是 Awake 时间 <= 当前时间，所以意味着每个 DisplayScheduler 的运行周期这个 DisplayJob 都会被唤醒。`maxFramePerSecond` 不可以为 0 或者除了 -1 的负数，会在 DisplayJob 构造时检查并抛出异常
3. 由于 AnimatedDisplayJob 的设计（全局动图进度而不是玩家动图进度），动图对于所有玩家的播放进度都是一样的。且只要 DisplayJob 加载，即使没人在看动图的时间也在走。暂时觉得这两个点不算问题，且不希望引入额外的复杂度，所以保持这种行为。

我的疑问：
1. 计算可视 Tile 列表的算法具体要怎么设计？我暂时没有好想法，如果对每个展示框实体都做可视检查那太慢了。是否应该获取整个地图画墙在玩家视野里可以看到的部分，然后获取这部分里的展示框再取 Tile ID / Map ID？
2. DisplayInstance 地图画中心（也就是整个地图画墙的中心）具体要怎么计算？考虑到地图画是可以贴在墙上也可以贴在地板 / 天花板上的，所以这个「墙」其实有竖直和水平两种状态。
3. `World.getNearbyPlayer(...)` 和可视 Tile 计算都是需要耗时的 CPU 操作，且它们也是更新流程的一部分。是否可能出现极端情况下这两个计算任务导致整个帧更新时间超过 budget？是否需要前置计算任务？比如单独加一个 `onPreCalculation` 的生命周期函数，然后 `onAwake` 里 schedule 一个提早的预计算工作？但是这样可能会带来复杂度而且即使提早时间也不一定够用，如果到了 `onAwake` 的时间预计算还没跑完怎么办？或者说换一种思路，让 DisplayJob 被动地接受要管理的玩家 + 每个玩家要更新的 Tile，通过 PlayerMoveEvent 在玩家移动的时候就计算然后写入 DisplayJob？我想到的两种其他方案都要更复杂且更难实现。
