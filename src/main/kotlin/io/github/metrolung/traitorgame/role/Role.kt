package io.github.metrolung.traitorgame.role

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

interface Role {
//    val alignment: RoleAlignment
//    val isEvil: Boolean

    val settings: Settings

    fun isWinner(session: Session, sessionPlayer: SessionPlayer, endGameReason: EndGameReason): Boolean = false

    fun itemPickup(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, entity: Item): Boolean = false
    fun itemDropped(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, entity: Item): Boolean = false

    fun handleEquipmentUse(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, equipmentType: String, block: Location?): Boolean = false

    fun handleEquipmentDropped(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, equipmentType: String, itemEntity: Item): Boolean = false
    fun handleEquipmentUseOnEntity(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, equipmentType: String, entity: Entity): Boolean = false
    fun handleMaterialUse(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, materialType: String, block: Location?): Boolean = false
    fun handleMaterialUseOnEntity(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, materialType: String, entity: Entity): Boolean = false

    fun onMeetingStart(session: Session, meeting: Meeting, sessionPlayer: SessionPlayer) {}
    fun onMeetingEnd(session: Session, meeting: Meeting, sessionPlayer: SessionPlayer, turnout: Map<Vote, Int>) {}
    fun onRolePresented(session: Session, sessionPlayer: OnlineSessionPlayer) {}
    fun onPlayerDied(session: Session, sessionPlayer: SessionPlayer, deadPlayer: SessionPlayer) {}

    fun onKilled(session: Session, sessionPlayer: OnlineSessionPlayer, killer: OnlineSessionPlayer): Boolean = false
    fun onAttacking(session: Session, sessionPlayer: OnlineSessionPlayer, victim: OnlineSessionPlayer): Boolean = false
    fun onKilling(session: Session, sessionPlayer: OnlineSessionPlayer, victim: OnlineSessionPlayer): Boolean = false
    fun onAttacked(session: Session, sessionPlayer: OnlineSessionPlayer, attacker: OnlineSessionPlayer): Boolean = false

    fun onTickOnline(session: Session, sessionPlayer: OnlineSessionPlayer, tick: Int) {}

    fun interface Builder {
        fun build(): Role
    }

    data class Settings(
        val key: NamespacedKey,
        val name: String,
        val roleColor: Int,
        val stylized: TextComponent = name.colored(roleColor),
        val alignment: RoleAlignment,
        val itemProvider: ItemProvider,
        val goalProvider: (RoleSettings) -> String,
        val builder: Builder,
    )

//    data class Setting(
//        val amount: Int,
//        val chance: Double,
//        val alignment: RoleAlignment,
//        val info: Info,
//    )
}