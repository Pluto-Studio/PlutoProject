package plutoproject.feature.whip.paper

private val VANILLA_SOUND_ID = Regex("minecraft:[a-z0-9._/-]+")

data class WhipConfig(
    val lengths: List<Double> = listOf(4.0, 5.5, 7.0, 8.5, 10.0),
    val simulation: WhipSimulationConfig = WhipSimulationConfig(),
    val combat: WhipCombatConfig = WhipCombatConfig(),
    val knockback: WhipKnockbackConfig = WhipKnockbackConfig(),
    val sounds: WhipSoundsConfig = WhipSoundsConfig(),
) {
    init {
        require(lengths.size == WhipLevel.entries.size) {
            "lengths must contain exactly ${WhipLevel.entries.size} values"
        }
        require(lengths.all { it.isFinite() && it > 0.0 }) {
            "lengths must contain only finite positive values"
        }
        require(lengths.zipWithNext().all { (left, right) -> left < right }) {
            "lengths must be strictly increasing"
        }
    }

    fun length(level: WhipLevel): Double = lengths[level.ordinal]
}

data class WhipSimulationConfig(
    val gravity: Double = 0.08,
    val damping: Double = 0.92,
    val constraintIterations: Int = 4,
    val sweepThickness: Double = 0.15,
) {
    init {
        require(gravity.isFinite() && gravity >= 0.0) {
            "simulation.gravity must be finite and non-negative"
        }
        require(damping.isFinite() && damping in 0.0..1.0) {
            "simulation.damping must be finite and between 0 and 1"
        }
        require(constraintIterations > 0) {
            "simulation.constraintIterations must be positive"
        }
        require(sweepThickness.isFinite() && sweepThickness > 0.0) {
            "simulation.sweepThickness must be finite and positive"
        }
    }
}

data class WhipCombatConfig(
    val hitIntervalTicks: Int = 10,
    val minAcceleration: Double = 0.08,
    val damageScale: Double = 8.0,
) {
    init {
        require(hitIntervalTicks > 0) {
            "combat.hitIntervalTicks must be positive"
        }
        require(minAcceleration.isFinite() && minAcceleration >= 0.0) {
            "combat.minAcceleration must be finite and non-negative"
        }
        require(damageScale.isFinite() && damageScale >= 0.0) {
            "combat.damageScale must be finite and non-negative"
        }
    }
}

data class WhipKnockbackConfig(
    val scale: Double = 0.25,
    val maxVelocityIncrement: Double = 1.2,
) {
    init {
        require(scale.isFinite() && scale >= 0.0) {
            "knockback.scale must be finite and non-negative"
        }
        require(maxVelocityIncrement.isFinite() && maxVelocityIncrement >= 0.0) {
            "knockback.maxVelocityIncrement must be finite and non-negative"
        }
    }
}

data class WhipSoundsConfig(
    val crackThreshold: Double = 1.2,
    val crack: WhipSoundConfig = WhipSoundConfig(
        sound = "minecraft:item.trident.throw",
        volume = 1.0,
        pitch = 1.0,
    ),
    val hit: WhipSoundConfig = WhipSoundConfig(
        sound = "minecraft:entity.player.attack.strong",
        volume = 0.8,
        pitch = 1.0,
    ),
) {
    init {
        require(crackThreshold.isFinite() && crackThreshold >= 0.0) {
            "sounds.crackThreshold must be finite and non-negative"
        }
    }
}

data class WhipSoundConfig(
    val sound: String,
    val volume: Double = 1.0,
    val pitch: Double = 1.0,
) {
    init {
        require(VANILLA_SOUND_ID.matches(sound)) {
            "sounds sound must be a vanilla namespaced sound id, got '$sound'"
        }
        require(volume.isFinite() && volume >= 0.0) {
            "sound volume must be finite and non-negative"
        }
        require(pitch.isFinite() && pitch in 0.0..2.0) {
            "sound pitch must be finite and between 0 and 2"
        }
    }
}
