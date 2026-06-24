package io.github.metrolung.traitorgame.role.roles

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.GuiHolder
import io.github.metrolung.traitorgame.ItemStacks
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.colored
import io.github.metrolung.traitorgame.component
import io.github.metrolung.traitorgame.role.Neutral
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleSettings
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.min
import kotlin.random.Random

class Mogul : Neutral {
    override val roleColor: Int
        get() = Colors.MOGUL_ORANGE

    override val name: String
        get() = "Mogul"

    override fun isWinner(session: Session, sessionPlayer: SessionPlayer, endGameReason: EndGameReason): Boolean {
        if (totalCash > session.settings.roleSettings.blackMarketeerMoneyGoal) {
            return true
        }
        return super.isWinner(session, sessionPlayer, endGameReason)
    }

    override fun getGoal(roleSettings: RoleSettings): String {
        return "You have no allegiances. Collect $${roleSettings.blackMarketeerMoneyGoal / 100.0} to win."
    }

    var totalCash = 0L
    var recentCash = 0L
    var meansToDoThis: Int = -1
    val boxMenu = MerchandiseBoxMenu()
    val merchandiseList: MutableList<Merchandise?> = MutableList(14) { i ->
        possibleMerchandise.random().random()
    }

    override fun handleEquipmentInventoryClick(
        session: Session,
        sessionPlayer: OnlineSessionPlayer,
        itemStack: ItemStack,
        equipmentType: String,
        action: InventoryAction
    ): Boolean {
        when (equipmentType) {
            "money_bag" -> {
                val cursor = sessionPlayer.player.itemOnCursor
                val value = when (cursor.type) {
                    Material.AMETHYST_SHARD -> 1
                    Material.COPPER_NUGGET -> 1

                    Material.LAPIS_LAZULI -> 5

                    Material.COPPER_INGOT -> 10

                    Material.GOLD_NUGGET -> 50
                    Material.COAL -> 50

                    Material.EMERALD -> 1_00
                    Material.IRON_NUGGET -> 1_00

                    Material.GOLD_INGOT -> 5_00
                    Material.IRON_INGOT -> 10_00
                    Material.DIAMOND -> 40_00
                    Material.NETHERITE_INGOT -> 250_00

                    else -> return true
                }

                sessionPlayer.player.setItemOnCursor(null)
                for (i in 0..<cursor.amount) {
                    session.server.scheduler.runTaskLater(session.plugin, { _ ->
                        totalCash += value
                        recentCash += value

                        sessionPlayer.player.playSound(Sound.sound {
                            it.source(Sound.Source.PLAYER)
                            it.type(NamespacedKey.minecraft("block.note_block.bell"))
                        })
                    }, i.toLong())
                }

                return true
            }
        }
        return super.handleEquipmentInventoryClick(session, sessionPlayer, itemStack, equipmentType, action)
    }

    override fun handleGuiClick(
        session: Session,
        sessionPlayer: OnlineSessionPlayer,
        itemStack: ItemStack,
        guiHolder: GuiHolder,
        action: InventoryAction
    ): Boolean {
        if (guiHolder === boxMenu) {
            val index = itemStack.persistentDataContainer.get(ItemStacks.MERCHANDISE_INDEX_KEY, PersistentDataType.INTEGER) ?: return true
            if (meansToDoThis != index) {
                sessionPlayer.player.sendMessage("Do you really mean to do this? Drop the item again to confirm.".colored(Colors.VERY_RED))
                meansToDoThis = index
                return true
            }
            meansToDoThis = -1

            val merchandise = merchandiseList[index] ?: return true
            merchandiseList[index] = null

            sessionPlayer.player.dropItem(merchandise.itemProvider())

            return true
        }

        return super.handleGuiClick(session, sessionPlayer, itemStack, guiHolder, action)
    }

    override fun onRolePresented(session: Session, sessionPlayer: OnlineSessionPlayer) {
//        for (slot in 9..16) {
//            val group = Random.nextInt(0, possibleMerchandise.size)
//            val index = Random.nextInt(0, possibleMerchandise[group].size)
//
//            val merchandise = possibleMerchandise[group][index]
//            val material = merchandise.itemProvider().type
//            val name = merchandise.name
//
//            sessionPlayer.player.inventory.setItem(slot, ItemStacks.merchandise(name, material, group, index, slot))
//        }
//        sessionPlayer.player.inventory.setItem(17, ItemStacks.moneyBag)
        sessionPlayer.player.give(ItemStacks.merchandise_box)
    }

    override fun handleEquipmentUse(
        session: Session,
        sessionPlayer: OnlineSessionPlayer,
        itemStack: ItemStack,
        equipmentType: String,
        block: Location?
    ): Boolean {
        when (equipmentType) {
            "merchandise_box" -> {
                sessionPlayer.player.openInventory(boxMenu.inventory)
            }
        }

        return super.handleEquipmentUse(session, sessionPlayer, itemStack, equipmentType, block)
    }

    override fun onTickOnline(session: Session, sessionPlayer: OnlineSessionPlayer) {
        if (!session.roleShown) {
            return
        }

        sessionPlayer.player.sendActionBar(
            Component.text {
                it.append("$${totalCash/100.0}".colored(Colors.VERY_YELLOW))
                it.append(" (recent: $${recentCash/100.0})".colored(Colors.MID_GRAY))
            }
        )
    }

    companion object {
        data class Merchandise(val name: Component, val itemProvider: () -> ItemStack)

        private val arrows = listOf(
            ItemStack.of(Material.ARROW),
            ItemStack.of(Material.SPECTRAL_ARROW),
            tippedArrow(PotionEffect(PotionEffectType.WEAKNESS, 20*11, 0)),
            tippedArrow(PotionEffect(PotionEffectType.WEAKNESS, 20*30, 0)),
            tippedArrow(PotionEffect(PotionEffectType.SLOWNESS, 20*11, 0)),
            tippedArrow(PotionEffect(PotionEffectType.SLOWNESS, 20*30, 0)),
            tippedArrow(PotionEffect(PotionEffectType.SLOWNESS, 20*2, 3)),
            tippedArrow(PotionEffect(PotionEffectType.LEVITATION, 20*10, 0)),
            tippedArrow(PotionEffect(PotionEffectType.LEVITATION, 20*5, 2)),
            tippedArrow(PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 0)),
            tippedArrow(PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1)),
            tippedArrow(PotionEffect(PotionEffectType.POISON, 20*5, 0)),
            tippedArrow(PotionEffect(PotionEffectType.POISON, 20*11, 0)),
            tippedArrow(PotionEffect(PotionEffectType.POISON, 20*2, 2)),
            tippedArrow(PotionEffect(PotionEffectType.WIND_CHARGED, 20*22, 0)),
            tippedArrow(PotionEffect(PotionEffectType.WEAVING, 20*22, 0)),
            tippedArrow(PotionEffect(PotionEffectType.OOZING, 20*22, 0)),
            tippedArrow(PotionEffect(PotionEffectType.INFESTED, 20*22, 0)),
        )
        private fun tippedArrow(effect: PotionEffect) =
            ItemStack.of(Material.TIPPED_ARROW).apply { editMeta { (it as PotionMeta).addCustomEffect(effect, false) } }

        private fun mixedQuiver(size: Int): ItemStack {
            var remaining = size
            val items = mutableListOf<ItemStack>()
            while (remaining > 0) {
                val arrow = arrows.random().clone()
                arrow.amount = Random.nextInt(0, min(8, remaining))
                remaining -= arrow.amount
            }
            return ItemStacks.bundle(items)
        }

        val possibleMerchandise: List<List<Merchandise>> = listOf(
            listOf(
                Merchandise("32x TNT".colored(0xfc4c1b)) { ItemStack.of(Material.TNT, 32) },
                Merchandise("16x TNT".colored(0xfc4c1b)) { ItemStack.of(Material.TNT, 16) },
            ),
            listOf(
                Merchandise("8x Pearl".colored(0x12a882)) { ItemStack.of(Material.ENDER_PEARL, 8) },
                Merchandise("4x Pearl".colored(0x12a882)) { ItemStack.of(Material.ENDER_PEARL, 4) }
            ),
            listOf(
                Merchandise("32x Arrows".component) { ItemStack.of(Material.ARROW, 32) },
                Merchandise("16x Arrows".component) { ItemStack.of(Material.ARROW, 16) },
                Merchandise("32x Mixed Arrow Quiver".component) { mixedQuiver(32) },
                Merchandise("16x Mixed Arrow Quiver".component) { mixedQuiver(16) },
            ),
        )
    }

    inner class MerchandiseBoxMenu : GuiHolder {
        override val layout = """
            X X X X X X X X X
            X X X / _ / X X X
            X X X X X X X X X
            X - - - - - - - X
            X - - - - - - - X
            X X X X X X X X X
        """.trimIndent()

        override val rows = 6
        override val name = "Merchandise Box".component

        var merchandiseIndex: Int = 0

        override fun onRender() {
            merchandiseIndex = 0
        }

        override fun getItemAt(ch: Char, slot: Int): ItemStack? {
            return when (ch) {
                '-' -> {
                    val merchandise = this@Mogul.merchandiseList.getOrNull(merchandiseIndex) ?: return null
                    val material = merchandise.itemProvider().type

                    ItemStacks.merchandise(merchandise.name, material, merchandiseIndex)
                }
                'X' -> ItemStacks.Gui.blackGlass
                '/' -> ItemStacks.Gui.yellowGlass
                else -> null
            }
        }

    }
}
