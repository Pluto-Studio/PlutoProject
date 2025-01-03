package plutoproject.framework.common.util.network

import java.net.InetSocketAddress

fun InetSocketAddress.toHostPortString(): String =
    "${hostString}:${port}"
