package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.Meeting
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.Vote
import io.github.metrolung.traitorgame.colored
import io.github.metrolung.traitorgame.role.Role.Builder
import io.github.metrolung.traitorgame.role.RoleAlignment
import io.github.metrolung.traitorgame.role.RoleSettings
import net.kyori.adventure.text.TextComponent
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.ItemProvider

interface Trait {
    fun isWinner(session: Session, sessionPlayer: SessionPlayer, endGameReason: EndGameReason) = Unit
    fun isLoser(session: Session, sessionPlayer: SessionPlayer, endGameReason: EndGameReason) = Unit

    fun itemPickup(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, entity: Item) = Unit
    fun itemDropped(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, entity: Item) = Unit

    fun handleEquipmentUse(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, equipmentType: String, block: Location?) = Unit

    fun handleEquipmentDropped(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, equipmentType: String, itemEntity: Item) = Unit
    fun handleEquipmentUseOnEntity(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, equipmentType: String, entity: Entity) = Unit
    fun handleMaterialUse(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, materialType: String, block: Location?) = Unit
    fun handleMaterialUseOnEntity(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, materialType: String, entity: Entity) = Unit

    fun onMeetingStart(session: Session, meeting: Meeting, sessionPlayer: SessionPlayer) = Unit
    fun onMeetingEnd(session: Session, meeting: Meeting, sessionPlayer: SessionPlayer, turnout: Map<Vote, Int>) = Unit
    fun onRolePresented(session: Session, sessionPlayer: OnlineSessionPlayer) = Unit
    fun onPlayerDied(session: Session, sessionPlayer: SessionPlayer, deadPlayer: SessionPlayer) = Unit

    fun onTakeDamage(event: Event, damage: DamageInfo) = Unit
    fun onDealDamage(event: Event, damage: DamageInfo) = Unit

//    fun onKilledByPlayer(session: Session, sessionPlayer: OnlineSessionPlayer, killer: OnlineSessionPlayer): Boolean = false
//    fun onAttackingPlayer(session: Session, sessionPlayer: OnlineSessionPlayer, victim: OnlineSessionPlayer): Boolean = false
//    fun onKillingPlayer(session: Session, sessionPlayer: OnlineSessionPlayer, victim: OnlineSessionPlayer): Boolean = false
//    fun onAttackedByPlayer(session: Session, sessionPlayer: OnlineSessionPlayer, attacker: OnlineSessionPlayer): Boolean = false

    fun onTickOnline(session: Session, sessionPlayer: OnlineSessionPlayer, tick: Int) {}

    data class Event(
        val session: Session,
        val player: SessionPlayer,
        var handled: Boolean = false,
        var cancelMinecraft: Boolean = false,
        var doNotAnimate: Boolean = false,
    )

    data class DamageInfo(
        val victimPlayer: OnlineSessionPlayer?,
        val victimEntity: Entity,
        val attackingPlayer: OnlineSessionPlayer?,
        val attackingEntity: Entity,
        val isKilling: Boolean,
    )

    data class Settings(
        val key: NamespacedKey,
        val name: String,
        val traitColor: Int,
        val stylized: TextComponent = name.colored(traitColor),
        val alignment: RoleAlignment,
        val itemProvider: ItemProvider,
        val goalProvider: (RoleSettings) -> String,
        val builder: Builder,
    )
}