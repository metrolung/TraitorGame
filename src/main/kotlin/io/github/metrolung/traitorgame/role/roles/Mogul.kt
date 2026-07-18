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
import io.github.metrolung.traitorgame.role.PassiveNeutral
import io.github.metrolung.traitorgame.role.RoleSettings
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.PotionContents
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import xyz.xenondevs.invui.dsl.ExperimentalDslApi
import xyz.xenondevs.invui.dsl.gui
import xyz.xenondevs.invui.dsl.stonecutterWindow
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.set
import xyz.xenondevs.invui.item.Item
import kotlin.also
import kotlin.math.min
import kotlin.random.Random

class Mogul : PassiveNeutral {
    override val name: String
        get() = "Mogul"

    override val isEvil: Boolean
        get() = false

    override fun isWinner(session: Session, sessionPlayer: SessionPlayer, endGameReason: EndGameReason): Boolean {
        if (totalCash > session.settings.roleSettings.mogulMoneyGoal) {
            return true
        }
        return super.isWinner(session, sessionPlayer, endGameReason)
    }

    override fun getGoal(roleSettings: RoleSettings): String {
        return "You have no allegiances. Collect $${roleSettings.mogulMoneyGoal / 100.0} to win."
    }

    var cashAtLastSale = 0L
    var totalCash = 0L

    var meansToDoThis: Int = -1
    val merchandiseList: MutableList<Merchandise> = MutableList(14) { i ->
        possibleMerchandise.random().random()
    }.apply { sortByDescending {
        val item = it.itemProvider()
        item.type.name + it.amount
    } }

    private fun valueOf(stack: ItemStack?): Long {
        var sum = 0L

        stack?.getData(DataComponentTypes.BUNDLE_CONTENTS)?.let { contents ->
            for (itemStack in contents.contents()) {
                sum += valueOf(itemStack)
            }
        }

        stack?.getData(DataComponentTypes.CONTAINER)?.let { contents ->
            for (itemStack in contents.contents()) {
                sum += valueOf(itemStack)
            }
        }

        sum += when (stack?.type) {
            Material.AMETHYST_SHARD -> 1
            Material.COPPER_NUGGET -> 1

            Material.LAPIS_LAZULI -> 5
            Material.LAPIS_BLOCK -> 45

            Material.COPPER_INGOT -> 10
            Material.COPPER_BLOCK -> 90

            Material.GOLD_NUGGET -> 50
            Material.COAL -> 50
            Material.COAL_BLOCK -> 4_50

            Material.EMERALD -> 1_00
            Material.EMERALD_BLOCK -> 9_00
            Material.IRON_NUGGET -> 1_00

            Material.GOLD_INGOT -> 5_00
            Material.GOLD_BLOCK -> 45_00
            Material.IRON_INGOT -> 10_00
            Material.IRON_BLOCK -> 90_00
            Material.DIAMOND -> 40_00
            Material.DIAMOND_BLOCK -> 360_00
            Material.NETHERITE_INGOT -> 250_00
            Material.NETHERITE_BLOCK -> 2250_00

            else -> 0
        } * (stack?.amount ?: 0)

        return sum
    }

//    override fun handleInventoryClick(
//        session: Session,
//        sessionPlayer: OnlineSessionPlayer,
//        itemStack: ItemStack?,
//        cursor: ItemStack?,
//        guiHolder: GuiHolder?,
//        slot: Int,
//        action: InventoryAction
//    ): Boolean {
//        if (guiHolder is MerchandiseBoxMenu) {
//            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
//                val itemStack = itemStack ?: return true
//                val value = valueOf(itemStack) ?: return true
//
//                sessionPlayer.player.inventory.setItem(slot, null)
//                for (i in 0..<itemStack.amount) {
//                    session.server.scheduler.runTaskLater(session.plugin, { _ ->
//                        totalCash += value
//                        recentCash += value
//
//                        sessionPlayer.player.playSound(Sound.sound {
//                            it.source(Sound.Source.PLAYER)
//                            it.type(NamespacedKey.minecraft("block.note_block.bell"))
//                        })
//                    }, i.toLong())
//                }
//
//                return true
//            }
//        }
//
//        return super.handleInventoryClick(session, sessionPlayer, itemStack, cursor, guiHolder, slot, action)
//    }
//
//    override fun handleGuiClick(
//        session: Session,
//        sessionPlayer: OnlineSessionPlayer,
//        itemStack: ItemStack?,
//        cursor: ItemStack?,
//        guiHolder: GuiHolder,
//        slot: Int,
//        action: InventoryAction
//    ): Boolean {
//        if (guiHolder is MerchandiseBoxMenu) {
//            if (slot == 13) {
//                val cursor = sessionPlayer.player.itemOnCursor
//                val value = valueOf(cursor) ?: return true
//
//                sessionPlayer.player.setItemOnCursor(null)
//
//                for (i in 0..<cursor.amount) {
//                    session.server.scheduler.runTaskLater(session.plugin, { _ ->
//                        totalCash += value
//                        recentCash += value
//
//                        sessionPlayer.player.playSound(Sound.sound {
//                            it.source(Sound.Source.PLAYER)
//                            it.type(NamespacedKey.minecraft("block.note_block.bell"))
//                        })
//                    }, i.toLong())
//                }
//
//                return true
//            }
//
//
//            val index = itemStack?.persistentDataContainer?.get(ItemStacks.MERCHANDISE_INDEX_KEY, PersistentDataType.INTEGER) ?: return true
//            if (meansToDoThis != index) {
//                meansToDoThis = index
//                return true
//            }
//            meansToDoThis = -1
//
//            val merchandise = merchandiseList[index] ?: return true
//            merchandiseList[index] = null
//            boxMenu.update(session)
//
//            val dropped = sessionPlayer.player.dropItem(merchandise.itemProvider()) ?: return true
//            dropped.persistentDataContainer.set(TraitorGamePlugin.key("cannot_pickup"), PersistentDataType.STRING, sessionPlayer.uniqueId.toString())
//
//            return true
//        }
//
//        return super.handleGuiClick(session, sessionPlayer, itemStack, cursor, guiHolder, slot, action)
//    }

    override fun onRolePresented(session: Session, sessionPlayer: OnlineSessionPlayer) {
        sessionPlayer.player.give(ItemStacks.merchandiseBox)
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
                openGui(sessionPlayer.player)
//                boxMenu.update(session)
//                sessionPlayer.player.openInventory(boxMenu.inventory)
                return true
            }
        }

        return super.handleEquipmentUse(session, sessionPlayer, itemStack, equipmentType, block)
    }

    override fun onTickOnline(session: Session, sessionPlayer: OnlineSessionPlayer, tick: Int) {
        if (tick % 10 == 0) {
            updateCash(sessionPlayer.player)
        }


        if (session.roleShown) {
            sessionPlayer.player.sendActionBar(
                Component.text {
                    it.append("$${String.format("%.2f", totalCash/100.0)}".colored(Colors.VERY_YELLOW))
                    it.append(" (recent: $${String.format("%.2f", (totalCash-cashAtLastSale)/100.0)})".colored(Colors.MID_GRAY))
                }
            )
        }

        super.onTickOnline(session, sessionPlayer, tick)
    }

    companion object {
        data class Merchandise(val name: Component, val amount: Int = 1, val itemProvider: () -> ItemStack)

        private val arrows = listOf(
            ItemStack.of(Material.ARROW),
            ItemStack.of(Material.SPECTRAL_ARROW),
            tippedArrow(PotionType.WEAKNESS),
            tippedArrow(PotionType.LONG_WEAKNESS),
            tippedArrow(PotionType.SLOWNESS),
            tippedArrow(PotionType.LONG_SLOWNESS),
            tippedArrow(PotionType.STRONG_SLOWNESS),
            tippedArrow("Arrow of Levitation".component, PotionEffect(PotionEffectType.LEVITATION, 20*10, 0)),
            tippedArrow("Arrow of Levitation".component, PotionEffect(PotionEffectType.LEVITATION, 20*5, 2)),
            tippedArrow(PotionType.HARMING),
            tippedArrow(PotionType.STRONG_HARMING),
            tippedArrow(PotionType.POISON),
            tippedArrow(PotionType.LONG_POISON),
            tippedArrow(PotionType.STRONG_POISON),
            tippedArrow(PotionType.WIND_CHARGED),
            tippedArrow(PotionType.WEAVING),
            tippedArrow(PotionType.OOZING),
            tippedArrow(PotionType.INFESTED),
        )
        private fun tippedArrow(name: Component, effect: PotionEffect) =
            ItemStack.of(Material.TIPPED_ARROW).apply {
                this.setData(DataComponentTypes.POTION_CONTENTS, PotionContents.potionContents()
                    .addCustomEffect(effect)
                    .build())
                this.setData(DataComponentTypes.ITEM_NAME, name)
            }
        private fun tippedArrow(potionType: PotionType) =
            ItemStack.of(Material.TIPPED_ARROW).apply {
                this.setData(DataComponentTypes.POTION_CONTENTS, PotionContents.potionContents()
                    .potion(potionType)
                    .build())
            }

        private fun mixedQuiver(size: Int): ItemStack {
            var remaining = size
            val items = mutableListOf<ItemStack>()
            while (remaining > 0) {
                val arrow = arrows.random().clone()
                val amount = Random.nextInt(1, 1+min(8, remaining))
                arrow.amount = amount
                remaining -= amount
                items.add(arrow)
            }
            return ItemStacks.bundle(items)
        }

        val possibleMerchandise: List<List<Merchandise>> = listOf(
            listOf(
                Merchandise("32x TNT".colored(0xfc4c1b), 32) { ItemStack.of(Material.TNT, 32) },
                Merchandise("16x TNT".colored(0xfc4c1b), 16) { ItemStack.of(Material.TNT, 16) },
            ),
            listOf(
                Merchandise("4x Pearl".colored(0x12a882), 4) { ItemStack.of(Material.ENDER_PEARL, 4) },
                Merchandise("2x Pearl".colored(0x12a882), 2) { ItemStack.of(Material.ENDER_PEARL, 2) }
            ),
            listOf(
                Merchandise("32x Arrows".component, 32) { ItemStack.of(Material.ARROW, 32) },
                Merchandise("16x Arrows".component, 16) { ItemStack.of(Material.ARROW, 16) },
                Merchandise("32x Mixed Arrow Quiver".component, 32) { mixedQuiver(32) },
                Merchandise("16x Mixed Arrow Quiver".component, 16) { mixedQuiver(16) },
            ),
            listOf(
                Merchandise("4x Breeze Rod".component, 4) { ItemStack.of(Material.BREEZE_ROD, 4) },
                Merchandise("2x Breeze Rod".component, 2) { ItemStack.of(Material.BREEZE_ROD, 2) },
            ),
            listOf(
                Merchandise("Water Bucket".component) { ItemStack.of(Material.WATER_BUCKET) },
                Merchandise("Lava Bucket".component) { ItemStack.of(Material.LAVA_BUCKET) },
            ),
            listOf(
                Merchandise("4x Obsidian".component, 4) { ItemStack.of(Material.OBSIDIAN, 4) },
                Merchandise("2x Obsidian".component, 2) { ItemStack.of(Material.OBSIDIAN, 2) },
            ),
        )
    }

    fun updateCash(player: Player, purchaseMade: Boolean = false) {
        totalCash = player.inventory.sumOf { valueOf(it) }
        if (purchaseMade) {
            cashAtLastSale = totalCash
        }
    }

    @OptIn(ExperimentalDslApi::class)
    fun openGui(player: Player) {
        stonecutterWindow(player) {
            buttonsGui by Gui.empty(4, Math.ceilDiv(merchandiseList.size, 4)).also { gui ->
                for (i in merchandiseList.indices) {
                    val merchandise = merchandiseList[i]
                    val item = merchandise.itemProvider()

                    gui[i] = Item.simple(ItemStacks.merchandise(merchandise.name, merchandise.amount, item.type))
                }
            }
            upperGui by gui("ab") {
            }
        }.open()
    }

//    inner class MerchandiseBoxMenu : GuiHolder {
//        override var inventoryCache: Inventory? = null
//
//        override val key: NamespacedKey
//            get() = TraitorGamePlugin.key("merchandise_box")
//
//        override val layout = """
//            X X X X X X X X X
//            X X X / _ / X X X
//            X X X X X X X X X
//            X - - - - - - - X
//            X - - - - - - - X
//            X X X X X X X X X
//        """.trimIndent()
//
//        override val rows = 6
//        override val name = "Merchandise Box".component
//
//        var merchandiseIndex: Int = 0
//
//        override fun onRender() {
//            merchandiseIndex = 0
//        }
//
//        override fun getItemAt(ch: Char, slot: Int): ItemStack? {
//            return when (ch) {
//                '-' -> {
//                    val oldIndex = merchandiseIndex
//                    merchandiseIndex++
//
//                    val merchandise = this@Mogul.merchandiseList.getOrNull(oldIndex) ?: return null
//                    val material = merchandise.itemProvider().type
//
//                    ItemStacks.merchandise(merchandise.name, merchandise.amount, material, oldIndex)
//                }
//                'X' -> ItemStacks.Gui.blackGlass
//                '/' -> ItemStacks.Gui.yellowGlass
//                '_' -> ItemStacks.moneyBag
//                else -> null
//            }
//        }
//    }
}
