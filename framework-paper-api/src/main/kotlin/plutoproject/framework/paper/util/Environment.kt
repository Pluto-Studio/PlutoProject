package plutoproject.framework.paper.util

import net.minecraft.server.MinecraftServer
import org.bukkit.Server
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.plugin.java.JavaPlugin
import plutoproject.framework.common.util.jvm.findClass
import plutoproject.framework.common.util.serverThread

lateinit var plugin: JavaPlugin
lateinit var server: Server

val isFolia = findClass("io.papermc.paper.threadedregions.RegionizedServer") != null
val isAsync get() = Thread.currentThread() != serverThread
val isFoliaOrAsync get() = isFolia || isAsync

val Thread.isServerThread: Boolean
    get() = this == serverThread

fun Server.toNms(): MinecraftServer = (this as CraftServer).server
