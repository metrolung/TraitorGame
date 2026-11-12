package io.github.metrolung.traitorgame

import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.trait.Trait
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.*

class SessionPlayer(
    val uniqueId: UUID,
    val name: String,
    val role: Role,
    val server: Server,
    val playerCommunicationManager: PlayerCommunicationManager,
    vararg defaultTraits: Trait
) {
    val traits = sortedSetOf<Trait>(Comparator { a, b ->
        b.settings.priority.compareTo(a.settings.priority)
    })
    val data = TypedDataHolder()

    init {
        for (trait in defaultTraits) {
            traits.add(trait)
        }
    }

    var returnLocation: Location? = null
    var canReturn: Boolean = false
    var isAlive: Boolean = true
    var activeEntity: Entity? = null

    val actualPlayer: Player?
        get() = server.getPlayer(uniqueId)

    val onlineSessionPlayer: OnlineSessionPlayer?
        get() = actualPlayer?.let { player -> OnlineSessionPlayer(player, this) }

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
    val uniqueId: UUID
        get() = sessionPlayer.uniqueId
    val name: String
        get() = sessionPlayer.name
    val role: Role
        get() = sessionPlayer.role
    val server: Server
        get() = sessionPlayer.server
    val traits: SortedSet<Trait>
        get() = sessionPlayer.traits
    val playerCommunicationManager: PlayerCommunicationManager
        get() = sessionPlayer.playerCommunicationManager

//    var lastChanceUsed: Boolean
//        get() = sessionPlayer.lastChanceUsed
//        set(value) { sessionPlayer.lastChanceUsed = value }
    var returnLocation: Location?
        get() = sessionPlayer.returnLocation
        set(value) { sessionPlayer.returnLocation = value }

    var canReturn: Boolean
        get() = sessionPlayer.canReturn
        set(value) { sessionPlayer.canReturn = value }

    var isDead: Boolean
        get() = sessionPlayer.isAlive
        set(value) { sessionPlayer.isAlive = value }

    var activeEntity: Entity?
        get() = sessionPlayer.activeEntity
        set(value) { sessionPlayer.activeEntity = value }


    fun doOnlineActions() {
        println("do online actions")
        sessionPlayer.queuedActionsWhenOnline.forEach { it(this) }
        sessionPlayer.queuedActionsWhenOnline.clear()
    }
}
