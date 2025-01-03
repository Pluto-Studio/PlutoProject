package ink.pmc.hypervisor

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction

data class Double2IntCurve(
    val sample: Double2IntSample,
    val function: PolynomialFunction
) {
    fun getMinByY(): Double2IntPoint {
        return sample.minByOrNull { it.second } ?: error("No point")
    }

    fun getMaxByY(): Double2IntPoint {
        return sample.maxByOrNull { it.second } ?: error("No point")
    }
}