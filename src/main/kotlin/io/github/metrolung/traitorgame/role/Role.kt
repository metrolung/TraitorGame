package io.github.metrolung.traitorgame.role

import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.GuiHolder
import io.github.metrolung.traitorgame.Meeting
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.Vote
import io.github.metrolung.traitorgame.textColor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.ItemStack

interface Role {
    val roleColor: Int
    val name: String
    val stylized: TextComponent
        get() = Component.text(name).color(roleColor.textColor)

    val type: RoleType

    fun isWinner(session: Session, sessionPlayer: SessionPlayer, endGameReason: EndGameReason): Boolean = false

    fun getGoal(roleSettings: RoleSettings): String

    fun handleGuiClick(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, guiHolder: GuiHolder, action: InventoryAction): Boolean = false
    fun handleInventoryClick(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, guiHolder: GuiHolder, action: InventoryAction): Boolean = false
    fun handleEquipmentUse(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, equipmentType: String, block: Location?): Boolean = false
    fun handleEquipmentInventoryClick(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, equipmentType: String, action: InventoryAction): Boolean = false
    fun handleEquipmentDropped(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, equipmentType: String, itemEntity: Item): Boolean = false
    fun handleEquipmentUseOnEntity(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, equipmentType: String, entity: Entity): Boolean = false
    fun handleMaterialUse(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, materialType: String, block: Location?): Boolean = false
    fun handleMaterialUseOnEntity(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, materialType: String, entity: Entity): Boolean = false
    fun onMeetingStart(session: Session, meeting: Meeting, sessionPlayer: SessionPlayer) {}
    fun onMeetingEnd(session: Session, meeting: Meeting, sessionPlayer: SessionPlayer, turnout: Map<Vote, Int>) {}
    fun onRolePresented(session: Session, sessionPlayer: OnlineSessionPlayer) {}
    fun onPlayerDied(session: Session, sessionPlayer: SessionPlayer, deadPlayer: SessionPlayer) {}
    fun onTickOnline(session: Session, sessionPlayer: OnlineSessionPlayer) {}

    fun interface Generator {
        fun generate(): Role
    }
}