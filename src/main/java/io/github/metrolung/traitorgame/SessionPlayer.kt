package io.github.metrolung.traitorgame

import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.entity.Player
import java.util.*

data class SessionPlayer(
    val playerUuid: UUID,
    val name: String,
    val role: Role,
    val server: Server
) {
    var lastChanceUsed = false
    var returnLocation: Location? = null
    var canReturn: Boolean = false

    val player: Player?
        get() = server.getPlayer(playerUuid)
}
