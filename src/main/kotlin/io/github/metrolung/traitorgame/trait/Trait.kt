package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.Meeting
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.Vote
import io.github.metrolung.traitorgame.colored
import net.kyori.adventure.text.TextComponent
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.ItemProvider

interface Trait {
    fun isWinner(event: Event, endGameReason: EndGameReason) = Unit
    fun isLoser(event: Event, endGameReason: EndGameReason) = Unit
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
//    fun onRolePresented(session: Session, sessionPlayer: OnlineSessionPlayer) = Unit
//
//    fun onAnyPlayerTakeDamage(event: Event, damageInfo: DamageInfo) = Unit

    fun onTakeDamage(event: Event, damage: DamageInfo) = Unit
    fun onDealDamage(event: Event, damage: DamageInfo) = Unit

//    fun onKilledByPlayer(session: Session, sessionPlayer: OnlineSessionPlayer, killer: OnlineSessionPlayer): Boolean = false
//    fun onAttackingPlayer(session: Session, sessionPlayer: OnlineSessionPlayer, victim: OnlineSessionPlayer): Boolean = false
//    fun onKillingPlayer(session: Session, sessionPlayer: OnlineSessionPlayer, victim: OnlineSessionPlayer): Boolean = false
//    fun onAttackedByPlayer(session: Session, sessionPlayer: OnlineSessionPlayer, attacker: OnlineSessionPlayer): Boolean = false

    fun onTickOnline(session: Session, sessionPlayer: OnlineSessionPlayer, tick: Int) {}

    val settings: Settings

    data class Event(
        val session: Session,
        val player: SessionPlayer,
        var handled: Boolean = false,
        var cancelMinecraft: Boolean = false,
        var doAnimation: Boolean = false,
    )

    data class DamageInfo(
        val victimPlayer: OnlineSessionPlayer?,
        val victimEntity: Entity,
        val attackingPlayer: OnlineSessionPlayer?,
        val attackingEntity: Entity?,
        val isKilling: Boolean,
    )

    fun interface Builder {
        fun build(): Trait
    }

    data class Settings(
        val key: NamespacedKey,
        val priority: Double = 0.0,
        val name: String,
        val activeWhileDead: Boolean,
        val traitColor: Int,
        val stylized: TextComponent = name.colored(traitColor),
        val itemProvider: ItemProvider,
        val builder: Trait.Builder,
    )
}