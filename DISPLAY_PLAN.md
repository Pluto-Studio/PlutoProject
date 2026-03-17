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
1. 静态图像的更新流程和动图基本一致。不一样的点在于没有获取当前帧内容并计算要更新的 Tile 的步骤，多了一个 `playerToReceivedMapIds` 字段，记录每个玩家已经收到过的 Map ID。因为 Minecraft 客户端会缓存 Map ID 对应的内容，静态图不需要更新内容，所以如果一个玩家发过了一个 Map ID 的内容，就不会再发。其他帧速控制和可视检查的逻辑都一致，静态图的场景下更新循环的意义更多在于实现「不可视不发，可视了就发」，而不是帧更新。
2. 动图更新的第 10 步实际的算法是：用 1000ms / `maxFramePerSecond`，获取一帧的 budget ms（比如 60 FPS，budget 是 ~16.67ms）。然后用本次更新结束时间减去开始时间，获取更新用时，再用 budget ms 减去更新用时算出要等待的时间（如果为负数意味着这一帧超时了，用 `maxFramePerSeconds` 为 -1 时候相同的处理方式），然后用当前时间 + 等待时间算出最终的预期唤醒时间。此外，`maxFramePerSecond` 为 -1 时意味着不限制 FPS，此时计划唤醒时间时会直接传入当前时间。因为 DisplayScheduler 实际的检查方式是 Awake 时间 <= 当前时间，所以意味着每个 DisplayScheduler 的运行周期这个 DisplayJob 都会被唤醒。`maxFramePerSecond` 不可以为 0 或者除了 -1 的负数，会在 DisplayJob 构造时检查并抛出异常
3. 由于 AnimatedDisplayJob 的设计（全局动图进度而不是玩家动图进度），动图对于所有玩家的播放进度都是一样的。且只要 DisplayJob 加载，即使没人在看动图的时间也在走。暂时觉得这两个点不算问题，且不希望引入额外的复杂度，所以保持这种行为。

我的疑问：
1. 计算可视 Tile 列表的算法具体要怎么设计？我暂时没有好想法，如果对每个展示框实体都做可视检查那太慢了。是否应该获取整个地图画墙在玩家视野里可以看到的部分，然后获取这部分里的展示框再取 Tile ID / Map ID？
2. DisplayInstance 地图画中心（也就是整个地图画墙的中心）具体要怎么计算？考虑到地图画是可以贴在墙上也可以贴在地板 / 天花板上的，所以这个「墙」其实有竖直和水平两种状态。
3. `World.getNearbyPlayer(...)` 和可视 Tile 计算都是需要耗时的 CPU 操作，且它们也是更新流程的一部分。是否可能出现极端情况下这两个计算任务导致整个帧更新时间超过 budget？是否需要前置计算任务？比如单独加一个 `onPreCalculation` 的生命周期函数，然后 `onAwake` 里 schedule 一个提早的预计算工作？但是这样可能会带来复杂度而且即使提早时间也不一定够用，如果到了 `onAwake` 的时间预计算还没跑完怎么办？或者说换一种思路，让 DisplayJob 被动地接受要管理的玩家 + 每个玩家要更新的 Tile，通过 PlayerMoveEvent 在玩家移动的时候就计算然后写入 DisplayJob？我想到的两种其他方案都要更复杂且更难实现。