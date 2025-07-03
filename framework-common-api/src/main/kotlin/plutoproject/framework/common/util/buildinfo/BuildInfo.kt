package plutoproject.framework.common.util.buildinfo

import plutoproject.framework.common.util.inject.Koin
import java.time.Instant

interface BuildInfo {
    companion object : BuildInfo by Koin.get()

    val version: String
    val releaseName: String
    val releaseChannel: ReleaseChannel
    val isStable: Boolean
        get() = releaseChannel == ReleaseChannel.STABLE
    val gitCommit: String
    val gitBranch: String
    val buildTime: Instant
}
