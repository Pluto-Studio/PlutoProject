package plutoproject.feature.dynamicscheduler.paper

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction
import org.apache.commons.math3.fitting.PolynomialCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoints
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.entity.SpawnCategory
import plutoproject.kernel.api.koinInject
import plutoproject.feature.dynamicscheduler.api.paper.DynamicScheduler
import plutoproject.feature.dynamicscheduler.api.paper.DynamicViewDistanceState
import plutoproject.feature.dynamicscheduler.paper.config.DynamicSchedulerConfig
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.capability.databasepersist.api.adapters.BooleanTypeAdapter
import plutoproject.feature.dynamicscheduler.paper.moduleScope
import plutoproject.foundation.common.collection.listMultimapOf
import plutoproject.foundation.common.collection.set
import plutoproject.capability.serverstatistics.api.statistic.MeasuringTime
import plutoproject.capability.serverstatistics.api.statistic.StatisticProvider
import plutoproject.feature.dynamicscheduler.paper.server
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.system.measureTimeMillis

class DynamicSchedulerImpl : DynamicScheduler {
    private val config by koinInject<DynamicSchedulerConfig>()
    private val dynamicViewDistanceState = mutableMapOf<Player, DynamicViewDistanceState>()
    private var cycleJob: Job? = null

    private var isCurveCalculated = false
    private var defaultSimulateDistanceCurve: Double2IntCurve? = null
    private val simulateDistanceCurve = mutableMapOf<World, Double2IntCurve>()
    private val defaultSpawnLimitsCurve = mutableListOf<SpawnCurve>()
    private val spawnLimitsCurve = listMultimapOf<World, SpawnCurve>()
    private val defaultTicksPerSpawnCurve = mutableListOf<SpawnCurve>()
    private val ticksPerSpawnCurve = listMultimapOf<World, SpawnCurve>()

    override val viewDistanceEnabled: Boolean
        get() = config.viewDistance.enabled
    override val simulateDistanceEnabled: Boolean
        get() = config.simulateDistance.enabled
    override val spawnLimitsEnabled: Boolean
        get() = config.spawnLimits.enabled
    override val ticksPerSpawnEnabled: Boolean
        get() = config.ticksPerSpawn.enabled
    override var isRunning: Boolean = false

    override fun start() {
        check(!isRunning) { "Dynamic scheduler cycle job already running" }
        isRunning = true
        calculateCurves()
        // 视距不能小于模拟距离
        // hypervisor 会完全接管视距和模拟距离，起始视距需要在插件配置里调整
        server.worlds.forEach { world ->
            val curve = simulateDistanceCurve[world] ?: defaultSimulateDistanceCurve!!
            val max = curve.getMaxByY().second
            if (world.viewDistance < max) {
                world.viewDistance = max
            }
        }
        cycleJob = moduleScope.launch {
            while (isRunning) {
                server.onlinePlayers.forEach { player ->
                    val ping = player.ping
                    when {
                        ping > config.viewDistance.maximumPing
                                && !getViewDistance(player)
                                && !getViewDistanceLocally(player).isDisabledLocally ->
                            setViewDistanceLocally(player, DynamicViewDistanceState.DISABLED_DUE_PING)

                        ping > config.viewDistance.maximumPing
                                && getViewDistance(player)
                                && !getViewDistanceLocally(player).isDisabledLocally ->
                            setViewDistanceLocally(player, DynamicViewDistanceState.ENABLED_BUT_DISABLED_DUE_PING)

                        ping <= config.viewDistance.maximumPing
                                && getViewDistance(player)
                                && getViewDistanceLocally(player) == DynamicViewDistanceState.ENABLED_BUT_DISABLED_DUE_PING ->
                            setViewDistanceLocally(player, DynamicViewDistanceState.ENABLED)

                        ping <= config.viewDistance.maximumPing
                                && getViewDistanceLocally(player) == DynamicViewDistanceState.DISABLED_DUE_PING ->
                            setViewDistanceLocally(player, DynamicViewDistanceState.DISABLED)
                    }
                }
                if (isCurveCalculated) {
                    val millsPerTick = plutoproject.kernel.api.koinGet<StatisticProvider>().getMillsPerTick(MeasuringTime.SECONDS_10)
                    if (millsPerTick != null) {
                        server.worlds.forEach { world ->
                            val simulateDistance = getSimulateDistanceWhen(millsPerTick, world)
                            val currentSimulateDistance = world.simulationDistance
                            if (currentSimulateDistance != simulateDistance) {
                                world.simulationDistance = simulateDistance
                            }

                            SpawnCategory.entries.forEach categoryLoop@{ category ->
                                if (category == SpawnCategory.MISC) return@categoryLoop // Bukkit 不支持
                                val spawnLimit = getSpawnLimitWhen(millsPerTick, world, category)
                                val currentSpawnLimit = world.getSpawnLimit(category)
                                if (currentSpawnLimit != spawnLimit) {
                                    world.setSpawnLimit(category, spawnLimit)
                                }

                                val ticksPerSpawn = getTicksPerSpawnWhen(millsPerTick, world, category)
                                val currentTicksPerSpawn = world.getTicksPerSpawns(category).toInt()
                                if (currentTicksPerSpawn != ticksPerSpawn) {
                                    world.setTicksPerSpawns(category, ticksPerSpawn)
                                }
                            }
                        }
                    }
                }
                delay(config.cyclePeriod)
            }
        }
        featureLogger.info("Dynamic scheduler cycle job started")
    }

    override fun stop() {
        check(isRunning) { "Dynamic scheduler cycle job isn't running" }
        isRunning = false
        cycleJob?.cancel()
        cycleJob = null
        dynamicViewDistanceState.clear()
        clearCurvesData()
        featureLogger.info("Dynamic scheduler cycle job stopped")
    }

    override fun setViewDistance(player: Player, state: Boolean) {
        val container = plutoproject.kernel.api.koinGet<DatabasePersist>().getContainer(player.uniqueId)
        container.set(VIEW_BOOST_TOGGLE_PERSIST_KEY, BooleanTypeAdapter, state)
        when {
            !state && getViewDistanceLocally(player) == DynamicViewDistanceState.ENABLED ->
                setViewDistanceLocally(player, DynamicViewDistanceState.DISABLED)

            state && getViewDistanceLocally(player) == DynamicViewDistanceState.DISABLED
                -> setViewDistanceLocally(player, DynamicViewDistanceState.ENABLED)
        }
        moduleScope.launch {
            container.save()
        }
    }

    override fun setViewDistanceLocally(player: Player, state: DynamicViewDistanceState) {
        dynamicViewDistanceState[player] = state
    }

    override fun toggleViewDistance(player: Player) {
        if (getViewDistance(player)) {
            setViewDistance(player, false)
        } else {
            setViewDistance(player, true)
        }
    }

    override fun getViewDistance(player: Player): Boolean {
        val container = plutoproject.kernel.api.koinGet<DatabasePersist>().getContainer(player.uniqueId)
        return runBlocking { container.getOrDefault(VIEW_BOOST_TOGGLE_PERSIST_KEY, BooleanTypeAdapter, false) }
    }

    override fun getViewDistanceLocally(player: Player): DynamicViewDistanceState {
        return dynamicViewDistanceState.getOrPut(player) {
            if (getViewDistance(player)) {
                DynamicViewDistanceState.ENABLED
            } else {
                DynamicViewDistanceState.DISABLED
            }
        }
    }

    override fun removeLocalViewDistanceState(player: Player) {
        dynamicViewDistanceState.remove(player)
    }

    private fun checkSample(sample: Double2IntSample) {
        check(sample.isNotEmpty()) { "Curve sample cannot be empty" }
        check(sample.size >= 2) { "Curve sample requires at least 2 points" }
    }

    private fun fitDoubleToIntCurve(sample: Double2IntSample): Double2IntCurve {
        checkSample(sample)
        val points = WeightedObservedPoints()
        sample.forEach { (x, y) ->
            points.add(x, y.toDouble())
        }
        val fitter = PolynomialCurveFitter.create(2)
        val coefficients = fitter.fit(points.toList())
        return Double2IntCurve(sample, PolynomialFunction(coefficients))
    }

    private fun fitSpawnStrategyCurve(strategy: SpawnStrategy): List<SpawnCurve> {
        return strategy.map { (category, sample) ->
            SpawnCurve(category, fitDoubleToIntCurve(sample))
        }
    }

    private fun clearCurvesData() {
        isCurveCalculated = false
        defaultSimulateDistanceCurve = null
        defaultSpawnLimitsCurve.clear()
        defaultTicksPerSpawnCurve.clear()
        spawnLimitsCurve.clear()
        ticksPerSpawnCurve.clear()
    }

    override fun calculateCurves() {
        clearCurvesData()
        val time = measureTimeMillis {
            config.simulateDistance.default.also {
                defaultSimulateDistanceCurve = fitDoubleToIntCurve(it)
            }

            config.simulateDistance.world.forEach { (world, sample) ->
                val bukkitWorld = Bukkit.getWorld(world) ?: return@forEach
                simulateDistanceCurve[bukkitWorld] = fitDoubleToIntCurve(sample)
            }

            config.spawnLimits.default.also {
                defaultSpawnLimitsCurve.addAll(fitSpawnStrategyCurve(it))
            }

            config.spawnLimits.world.forEach { (world, strategy) ->
                val bukkitWorld = Bukkit.getWorld(world) ?: return@forEach
                spawnLimitsCurve[bukkitWorld] = fitSpawnStrategyCurve(strategy)
            }

            config.ticksPerSpawn.default.also {
                defaultTicksPerSpawnCurve.addAll(fitSpawnStrategyCurve(it))
            }

            config.ticksPerSpawn.world.forEach { (world, strategy) ->
                val bukkitWorld = Bukkit.getWorld(world) ?: return@forEach
                ticksPerSpawnCurve[bukkitWorld] = fitSpawnStrategyCurve(strategy)
            }
        }
        featureLogger.info("Finished curve calculations in ${time}ms")
        isCurveCalculated = true
    }

    override fun getSimulateDistanceWhen(millsPerSecond: Double, world: World?): Int {
        check(isCurveCalculated) { "Curve not calculated" }
        val curve = if (world == null) defaultSimulateDistanceCurve!! else simulateDistanceCurve[world]
            ?: defaultSimulateDistanceCurve!!
        val min = curve.getMinByY().second
        val max = curve.getMaxByY().second
        return curve.function.value(millsPerSecond).toInt().coerceIn(min, max)
    }

    override fun getSimulateDistanceCurve(world: World?): PolynomialFunction {
        check(isCurveCalculated) { "Curve not calculated" }
        return defaultSimulateDistanceCurve!!.function
    }

    override fun getSpawnLimitWhen(millsPerSecond: Double, world: World?, category: SpawnCategory): Int {
        check(isCurveCalculated) { "Curve not calculated" }
        val default = defaultSpawnLimitsCurve.firstOrNull { it.category == category }
        val curve = if (world == null) default
            ?: return Bukkit.getSpawnLimit(category) else spawnLimitsCurve[world].firstOrNull { it.category == category }
            ?: default ?: return world.getSpawnLimit(category)
        val min = curve.curve.getMinByY().second
        val max = curve.curve.getMaxByY().second
        return curve.curve.function.value(millsPerSecond).toInt().coerceIn(min, max)
    }

    override fun getSpawnLimitsCurve(world: World?, category: SpawnCategory): PolynomialFunction? {
        check(isCurveCalculated) { "Curve not calculated" }
        val default = defaultSpawnLimitsCurve.firstOrNull { it.category == category }?.curve?.function
        return if (world == null) {
            default
        } else {
            spawnLimitsCurve[world].firstOrNull { it.category == category }?.curve?.function ?: default
        }
    }

    override fun getTicksPerSpawnWhen(millsPerSecond: Double, world: World?, category: SpawnCategory): Int {
        check(isCurveCalculated) { "Curve not calculated" }
        val default = defaultTicksPerSpawnCurve.firstOrNull { it.category == category }
        val curve = if (world == null) default
            ?: return Bukkit.getTicksPerSpawns(category) else ticksPerSpawnCurve[world].firstOrNull { it.category == category }
            ?: default ?: return world.getTicksPerSpawns(category).toInt()
        val min = curve.curve.getMinByY().second
        val max = curve.curve.getMaxByY().second
        return curve.curve.function.value(millsPerSecond).toInt().coerceIn(min, max)
    }


    override fun getTicksPerSpawnCurve(world: World?, category: SpawnCategory): PolynomialFunction? {
        check(isCurveCalculated) { "Curve not calculated" }
        val default = defaultTicksPerSpawnCurve.firstOrNull { it.category == category }?.curve?.function
        return if (world == null) {
            default
        } else {
            ticksPerSpawnCurve[world].firstOrNull { it.category == category }?.curve?.function ?: default
        }
    }
}
