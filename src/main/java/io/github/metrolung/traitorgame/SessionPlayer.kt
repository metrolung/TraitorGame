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
    var lastChanceUsed: Boolean = false
    var returnLocation: Location? = null
    var canReturn: Boolean = false

    val player: Player?
        get() = server.getPlayer(playerUuid)

    val onlineSessionPlayer: OnlineSessionPlayer?
        get() = player?.let { player -> OnlineSessionPlayer(player, this) }

    val queuedActionsWhenOnline: MutableList<(OnlineSessionPlayer) -> Unit> = mutableListOf()

    fun whenOnline(action: (OnlineSessionPlayer, Player) -> Unit) {
        val onlineSessionPlayer = onlineSessionPlayer
        if (onlineSessionPlayer == null) {
            queuedActionsWhenOnline.add({ sessionPlayer ->
                action(sessionPlayer, sessionPlayer.player)
            })
        } else {
            action(onlineSessionPlayer, onlineSessionPlayer.player)
        }
    }

    fun whenOnline(action: (OnlineSessionPlayer) -> Unit) {
        val onlineSessionPlayer = onlineSessionPlayer
        if (onlineSessionPlayer == null) {
            queuedActionsWhenOnline.add(action)
        } else {
            action(onlineSessionPlayer)
        }
    }
}

data class OnlineSessionPlayer(val player: Player, val sessionPlayer: SessionPlayer) {
    val playerUuid: UUID
        get() = sessionPlayer.playerUuid
    val name: String
        get() = sessionPlayer.name
    val role: Role
        get() = sessionPlayer.role
    val server: Server
        get() = sessionPlayer.server

    var lastChanceUsed: Boolean
        get() = sessionPlayer.lastChanceUsed
        set(value) { sessionPlayer.lastChanceUsed = value }
    var returnLocation: Location?
        get() = sessionPlayer.returnLocation
        set(value) { sessionPlayer.returnLocation = value }

    var canReturn: Boolean
        get() = sessionPlayer.canReturn
        set(value) { sessionPlayer.canReturn = value }

    fun doOnlineActions() {
        println("do online actions")
        sessionPlayer.queuedActionsWhenOnline.forEach { it(this) }
        sessionPlayer.queuedActionsWhenOnline.clear()
    }
}
