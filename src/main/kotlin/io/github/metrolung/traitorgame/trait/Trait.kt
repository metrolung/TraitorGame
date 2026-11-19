package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.ConfigurableItemRegistry
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.TraitRegistry
import io.github.metrolung.traitorgame.colored
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleAlignment
import io.github.metrolung.traitorgame.trait.event.TraitEvent
import net.kyori.adventure.text.TextComponent
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import xyz.xenondevs.invui.item.ItemProvider

interface Trait {
    fun getWinCondition(session: Session, sessionPlayer: SessionPlayer) = WinCondition.Default

    enum class WinCondition {
        Passive,
        Default,
        Unable;

        val isPassiveOrDefault: Boolean
            get() = this == Passive || this == Default

        val isUnableOrDefault: Boolean
            get() = this == Unable || this == Default

        val isPassive: Boolean
            get() = this == Passive

        val isDefault: Boolean
            get() = this == Default

        val isUnable: Boolean
            get() = this == Unable
    }

//    fun getCurrentWinCondition(event: Event) = CurrentWinCondition.Default
//
//    enum class CurrentWinCondition {
//        Passively,
//        Selfish,
//        Default,
//        Unable
//    }

//    fun isWinner(event: Event, endGameReason: EndGameReason) = Unit

//    fun willWin(session: Session, sessionPlayer: SessionPlayer, endGameReason: EndGameReason) = false
//    fun canWin(session: Session, sessionPlayer: SessionPlayer) = true
//    fun willNotWin(session: Session, sessionPlayer: SessionPlayer): Boolean = false
    //
//    fun itemPickup(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, entity: Item) = Unit
//    fun itemDropped(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, entity: Item) = Unit
//
//    fun handleEquipmentUse(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, equipmentType: String, block: Location?) = Unit
//
//    fun handleEquipmentDropped(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, equipmentType: String, itemEntity: Item) = Unit
//    fun handleEquipmentUseOnEntity(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, equipmentType: String, entity: Entity) = Unit
//    fun handleMaterialUse(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, materialType: String, block: Location?) = Unit
//    fun handleMaterialUseOnEntity(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, materialType: String, entity: Entity) = Unit
//
//    fun onMeetingStart(session: Session, meeting: Meeting, sessionPlayer: SessionPlayer) = Unit
//    fun onMeetingEnd(session: Session, meeting: Meeting, sessionPlayer: SessionPlayer, turnout: Map<Vote, Int>) = Unit
//

    fun onInteract(event: TraitEvent.InteractionEvent) = Unit

    fun onRolePresented(event: TraitEvent.RolePresentedEvent) = Unit

    fun onAnyDamageDealt(event: TraitEvent.DamageEvent.Indirect) = Unit
    fun onTakeDamage(event: TraitEvent.DamageEvent.Direct) = Unit
    fun onDealDamage(event: TraitEvent.DamageEvent.Direct) = Unit

    fun onTick(event: TraitEvent.TickEvent) {}

    val settings: Settings

    interface TraitCause {
        object Unspecified : TraitCause
        object AssignedToAll : TraitCause
        object RandomlyAssigned : TraitCause
        data class CausedByRole(val role: Role) : TraitCause
        data class CausedByAlignment(val alignment: RoleAlignment) : TraitCause
    }

//    class Event(
//        val session: Session,
//        val player: SessionPlayer,
//        var isHandled: Boolean = false,
//        var isCancelled: Boolean = false,
//        var willAnimate: Boolean = false,
//    ) {
//        fun handleAndCancel(doAnimation: Boolean = false) {
//            this.isHandled = true
//            this.isCancelled = true
//            this.willAnimate = doAnimation
//        }
//
//        fun handle(doAnimation: Boolean = false) {
//            this.isHandled = true
//            this.willAnimate = doAnimation
//        }
//    }

    data class Settings(
        val key: NamespacedKey,
        val priority: Double = 0.0,
        val name: String,
        val activeWhileDead: Boolean = false,
        val canWinWhileDead: Boolean = false,
        val traitColor: Int,
        val stylized: TextComponent = name.colored(traitColor),
        val itemProvider: ItemProvider,
        val descriptionProvider: () -> String,
        val provider: () -> Trait,
    ) {
        fun register(configurableItemRegistry: ConfigurableItemRegistry<Trait.Settings> = TraitRegistry) {
            configurableItemRegistry.register(this.key, this)
        }

        fun getConfig(configurableItemRegistry: ConfigurableItemRegistry<Trait.Settings> = TraitRegistry): ConfigurationSection? {
            return configurableItemRegistry.getConfig(this.key)
        }
    }
}