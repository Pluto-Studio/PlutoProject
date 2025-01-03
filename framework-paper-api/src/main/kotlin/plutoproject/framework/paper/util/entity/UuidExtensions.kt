package plutoproject.framework.paper.util.entity

import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import java.util.*

fun UUID.getEntityWithThis(): Entity? = Bukkit.getEntity(this)
