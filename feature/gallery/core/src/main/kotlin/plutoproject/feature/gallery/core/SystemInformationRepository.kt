package plutoproject.feature.gallery.core

interface SystemInformationRepository {
    /**
     * 原子地分配一段连续 map id。
     *
     * @return 分配后最新的 lastAllocatedId；若溢出 [mapIdRange] 则返回 null。
     */
    suspend fun allocateMapIds(
        count: Int,
        mapIdRange: MapIdRange,
    ): Int?
}
