package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.ConfigurableItemRegistry
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.TraitRegistry
import io.github.metrolung.traitorgame.colored
import io.github.metrolung.traitorgame.component
import io.github.metrolung.traitorgame.role.Role
import net.kyori.adventure.text.TextComponent
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
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

    fun onRightClick(event: Event, itemStack: ItemStack?, block: Block?, entity: Entity?) = Unit

    fun onRolePresented(event: Event) = Unit

    fun onAnyDamageDealt(event: Event, damage: DamageInfo) = Unit

    fun onTakeDamage(event: Event, damage: DamageInfo) = Unit
    fun onDealDamage(event: Event, damage: DamageInfo) = Unit

    fun onTick(event: Event, tick: Int) {}

    val settings: Settings

    interface TraitCause {
        fun getSimpleCauseMessage(): TextComponent?

        object Unspecified : TraitCause {
            override fun getSimpleCauseMessage() = null
        }
        object AssignedToAll : TraitCause {
            override fun getSimpleCauseMessage() = "Common".component
        }
        object RandomlyAssigned : TraitCause {
            override fun getSimpleCauseMessage() = null
        }
        data class CausedByRole(val role: Role) : TraitCause {
            override fun getSimpleCauseMessage() = role.settings.stylized
        }
    }

    class Event(
        val session: Session,
        val player: SessionPlayer,
        var isHandled: Boolean = false,
        var isCancelled: Boolean = false,
        var willAnimate: Boolean = false,
    ) {
        fun handleAndCancel(doAnimation: Boolean = false) {
            this.isHandled = true
            this.isCancelled = true
            this.willAnimate = doAnimation
        }

        fun handle(doAnimation: Boolean = false) {
            this.isHandled = true
            this.willAnimate = doAnimation
        }
    }

    data class DamageInfo(
        val victimPlayer: OnlineSessionPlayer?,
        val victimEntity: Entity,
        val attackingPlayer: OnlineSessionPlayer?,
        val attackingEntity: Entity?,
        val isKilling: Boolean,
    )

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