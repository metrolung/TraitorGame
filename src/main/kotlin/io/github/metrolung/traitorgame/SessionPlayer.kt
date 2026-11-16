package io.github.metrolung.traitorgame

import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.trait.Trait
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.`object`.ObjectContents
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemType
import java.util.*

interface SessionPlayer {
    val uniqueId: UUID
    val name: String
    val role: Role
    val server: Server

    var returnLocation: Location?
    var canReturn: Boolean
    var isAlive: Boolean

    val data: TypedDataHolder
    val playerCommunicationManager: PlayerCommunicationManager// = PlayerCommunicationManager(" | ".colored(Colors.LIGHT_GRAY))

    val player: Player?
    val onlineSessionPlayer: OnlineSessionPlayer?

    fun traits(): Sequence<Pair<Trait, Trait.TraitCause>>
    fun giveTrait(trait: Trait, cause: Trait.TraitCause? = null)

    fun whenOnline(action: (OnlineSessionPlayer) -> Unit)
    fun notifyOnline()
}

fun SessionPlayer(
    uniqueId: UUID,
    name: String,
    role: Role,
    server: Server
) = SessionPlayerImpl(
    uniqueId = uniqueId,
    name = name,
    role = role,
    server = server
)

class SessionPlayerImpl(
    override val uniqueId: UUID,
    override val name: String,
    override val role: Role,
    override val server: Server
) : SessionPlayer {
    override var returnLocation: Location? = null
    override var canReturn: Boolean = false
    override var isAlive: Boolean = true

    override val data = TypedDataHolder()
    override val playerCommunicationManager = PlayerCommunicationManager(" | ".colored(Colors.LIGHT_GRAY))
    override val player: Player?
        get() = server.getPlayer(uniqueId)

    override val onlineSessionPlayer: OnlineSessionPlayer?
        get() = this.player?.let { player -> OnlineSessionPlayerImpl(this, player) }

    private val traits = sortedSetOf<Trait>(Comparator { a, b ->
        if (a.settings.priority == b.settings.priority) {
            return@Comparator a.settings.key.compareTo(b.settings.key)
        }
        b.settings.priority.compareTo(a.settings.priority)
    })
    private val traitCauses = mutableMapOf<Trait, Trait.TraitCause>()

    override fun traits(): Sequence<Pair<Trait, Trait.TraitCause>> {
        return traits.asSequence().map { trait -> trait to (traitCauses[trait] ?: Trait.TraitCause.Unspecified) }
    }

    override fun giveTrait(
        trait: Trait,
        cause: Trait.TraitCause?
    ) {
        traits.add(trait)
        if (cause != null) {
            traitCauses[trait] = cause
        }
    }

    val queuedActionsWhenOnline: MutableList<(OnlineSessionPlayer) -> Unit> = mutableListOf()

    override fun whenOnline(action: (OnlineSessionPlayer) -> Unit) {
        val onlineSessionPlayer = onlineSessionPlayer

        if (onlineSessionPlayer != null) {
            action(onlineSessionPlayer)
            return
        }

        queuedActionsWhenOnline.add(action)
    }

    override fun notifyOnline() {
        val onlineSessionPlayer = onlineSessionPlayer ?: return

        for (action in queuedActionsWhenOnline) {
            action(onlineSessionPlayer)
        }
    }

}

interface OnlineSessionPlayer : SessionPlayer {
    override val player: Player
}

class OnlineSessionPlayerImpl(val sessionPlayer: SessionPlayer, override val player: Player) : OnlineSessionPlayer, SessionPlayer by sessionPlayer {
    override fun whenOnline(action: (OnlineSessionPlayer) -> Unit) {
        return action(this)
    }
}

//sealed class SessionPlayer(
//    val uniqueId: UUID,
//    val name: String,
//    val role: Role,
//    val server: Server,
//) {
//    private val _traits = sortedSetOf<Trait>(Comparator { a, b ->
//        b.settings.priority.compareTo(a.settings.priority)
//    })
//    open val traits: Set<Trait>
//        get() = _traits
//
//    private val traitCauses = mutableMapOf<Trait, Trait.TraitCause>()
//
//    val data = TypedDataHolder()
//    val playerCommunicationManager = PlayerCommunicationManager(" | ".colored(Colors.LIGHT_GRAY))
//
//    var returnLocation: Location? = null
//    var canReturn: Boolean = false
//    var isAlive: Boolean = true
//    var activeEntity: Entity? = null
//
//    open val player: Player?
//        get() = server.getPlayer(uniqueId)
//
//    val onlineSessionPlayer: OnlineSessionPlayer?
//        get() = player?.let { player -> OnlineSessionPlayer(this, player) }
//
//    val queuedActionsWhenOnline: MutableList<(OnlineSessionPlayer) -> Unit> = mutableListOf()
//
//    fun giveTrait(trait: Trait, cause: Trait.TraitCause? = null) {
//        _traits.add(trait)
//        if (cause != null) {
//            traitCauses[trait] = cause
//        }
//    }
//
//    fun whenOnline(action: (OnlineSessionPlayer, Player) -> Unit) {
//        val onlineSessionPlayer = onlineSessionPlayer
//        if (onlineSessionPlayer == null) {
//            queuedActionsWhenOnline.add({ sessionPlayer ->
//                action(sessionPlayer, sessionPlayer.player)
//            })
//        } else {
//            action(onlineSessionPlayer, onlineSessionPlayer.player)
//        }
//    }
//
//    fun whenOnline(action: (OnlineSessionPlayer) -> Unit) {
//        val onlineSessionPlayer = onlineSessionPlayer
//        if (onlineSessionPlayer == null) {
//            queuedActionsWhenOnline.add(action)
//        } else {
//            action(onlineSessionPlayer)
//        }
//    }
//}
//
//class OnlineSessionPlayer(val sessionPlayer: SessionPlayer, override val player: Player) : SessionPlayer(
//    uniqueId = sessionPlayer.uniqueId,
//    name = sessionPlayer.name,
//    role = sessionPlayer.role,
//    server = sessionPlayer.server
//) {
//    override val traits: Set<Trait>
//        get() = sessionPlayer.traits
//}