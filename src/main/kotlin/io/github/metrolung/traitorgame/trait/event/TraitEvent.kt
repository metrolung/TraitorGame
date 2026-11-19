package io.github.metrolung.traitorgame.trait.event

import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack

interface TraitEvent {
    val session: Session
    val player: SessionPlayer

    interface Handleable : TraitEvent {
        var isHandled: Boolean
    }
    interface Cancellable : TraitEvent {
        var isCancelled: Boolean
    }
    interface Animated : TraitEvent {
        var isAnimated: Boolean
    }

    data class RolePresentedEvent(
        override val session: Session,
        override val player: SessionPlayer
    ) : TraitEvent

    data class TickEvent(
        override val session: Session,
        override val player: SessionPlayer,
        val tick: Int,
    ) : TraitEvent

    data class InteractionEvent(
        override val session: Session,
        override val player: OnlineSessionPlayer,
        val itemStack: ItemStack?,
        val block: Block?,
        val entity: Entity?,
        val clickedPlayer: OnlineSessionPlayer?,
        override var isCancelled: Boolean = false,
        override var isAnimated: Boolean = true,
        override var isHandled: Boolean = false,
    ) : TraitEvent, Cancellable, Animated, Handleable

    interface DamageEvent : TraitEvent {
        val attackingPlayer: OnlineSessionPlayer?
        val victimPlayer: OnlineSessionPlayer?
        val attackingEntity: Entity?
        val victimEntity: Entity?
        val isKilling: Boolean

        data class Direct(
            override val session: Session,
            override val player: SessionPlayer,
            override val attackingPlayer: OnlineSessionPlayer?,
            override val victimPlayer: OnlineSessionPlayer?,
            override val attackingEntity: Entity?,
            override val victimEntity: Entity?,
            override val isKilling: Boolean,
            override var isHandled: Boolean = false,
            override var isCancelled: Boolean = false,
        ) : DamageEvent, Cancellable, Handleable

        data class Indirect(
            override val session: Session,
            override val player: SessionPlayer,
            override val attackingPlayer: OnlineSessionPlayer?,
            override val victimPlayer: OnlineSessionPlayer?,
            override val attackingEntity: Entity?,
            override val victimEntity: Entity?,
            override val isKilling: Boolean,
        ) : DamageEvent
    }
}