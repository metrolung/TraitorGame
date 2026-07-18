package io.github.metrolung.traitorgame.api

import org.geysermc.floodgate.api.FloodgateApi
import java.util.UUID


object Floodgate {
    val api: FloodgateApi? = try {
        FloodgateApi.getInstance()
    } catch (_: ClassNotFoundException) {
        null
    }

    fun getPlayer(uniqueId: UUID) = api?.getPlayer(uniqueId)
    fun isFloodgatePlayer(uniqueId: UUID) = api?.isFloodgatePlayer(uniqueId) ?: false
}