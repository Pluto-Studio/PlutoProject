package ink.pmc.hypervisor

import org.bukkit.entity.SpawnCategory

typealias Double2IntSample = List<Pair<Double, Int>>
typealias Double2IntPoint = Pair<Double, Int>
typealias SpawnStrategy = Map<SpawnCategory, Double2IntSample>
