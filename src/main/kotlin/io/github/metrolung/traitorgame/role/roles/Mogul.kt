package io.github.metrolung.traitorgame.role.roles

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.ItemStacks
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.github.metrolung.traitorgame.colored
import io.github.metrolung.traitorgame.component
import io.github.metrolung.traitorgame.role.Neutral
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleAlignment
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.PotionContents
import io.papermc.paper.datacomponent.item.TooltipDisplay
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import xyz.xenondevs.invui.dsl.ExperimentalDslApi
import xyz.xenondevs.invui.dsl.gui
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.itemProvider
import xyz.xenondevs.invui.dsl.stonecutterWindow
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.set
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.item.ItemWrapper
import kotlin.also
import kotlin.math.min
import kotlin.random.Random

class Mogul : Neutral {
    override val settings: Role.Settings
        get() = SETTINGS

    override fun isWinner(session: Session, sessionPlayer: SessionPlayer, endGameReason: EndGameReason): Boolean {
        if (totalCash > session.settings.roleSettings.mogulMoneyGoal) {
            return true
        }
        return super.isWinner(session, sessionPlayer, endGameReason)
    }

    var cashAtLastSale = 0L
    var totalCash = 0L
    var depositedCash = 0L

    val merchandiseList: MutableList<Merchandise> = MutableList(14) { i ->
        Merchandise(possibleMerchandise.random().random())
    }.apply { sortByDescending {
        it.material.name + it.amount
    } }

    override fun itemPickup(
        session: Session,
        sessionPlayer: OnlineSessionPlayer,
        itemStack: ItemStack,
        entity: org.bukkit.entity.Item
    ): Boolean {

        if (entity.persistentDataContainer.get(TraitorGamePlugin.key("merchandise"), PersistentDataType.STRING) == sessionPlayer.player.uniqueId.toString()) {
            sessionPlayer.player.playPickupItemAnimation(entity)
            entity.remove()
            merchandiseList.add(Merchandise(itemStack))
            return true
        }

        return super.itemPickup(session, sessionPlayer, itemStack, entity)
    }

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
                openMerchandiseGui(sessionPlayer.player)
                return true
            }
        }

        return super.handleEquipmentUse(session, sessionPlayer, itemStack, equipmentType, block)
    }

    fun getBalanceComponent() = Component.text {
        it.append("$${String.format("%.2f", totalCash/100.0)}".colored(Colors.VERY_YELLOW))
        it.append(" (recent: $${String.format("%.2f", (totalCash-cashAtLastSale)/100.0)})".colored(Colors.MID_GRAY))
    }

    override fun onTickOnline(session: Session, sessionPlayer: OnlineSessionPlayer, tick: Int) {
        if (tick % 10 == 0) {
            updateCash(sessionPlayer.player)
        }

        if (session.roleShown) {
            sessionPlayer.statusBar.sendMessage(TraitorGamePlugin.key("mogul.balance"), getBalanceComponent())
        }

        super.onTickOnline(session, sessionPlayer, tick)
    }

    fun updateCash(player: Player, purchaseMade: Boolean = false) {
        totalCash = player.inventory.sumOf { valueOf(it) } + depositedCash
        if (purchaseMade) {
            cashAtLastSale = totalCash
        }
    }

    @OptIn(ExperimentalDslApi::class)
    fun openMerchandiseGui(player: Player) {
        stonecutterWindow(player) {
            val buttons = Gui.empty(4, Math.ceilDiv(merchandiseList.size, 4))

            fun updateButtons() {
                buttons.fill(null)

                for (i in merchandiseList.indices) {
                    val merchandise = merchandiseList[i]


                    buttons[i] = Item.simple(ItemStacks.merchandise(
                        if (merchandise.amount > 1)
                            "${merchandise.amount}x ".component.append(merchandise.name)
                        else
                            merchandise.name,
                        merchandise.amount,
                        merchandise.material
                    ))
                }
            }

            title by "Merchandise               ↓ ᴅʀᴏᴘ ↓".component

            buttonsGui by buttons.also { updateButtons() }

            upperGui by gui("ab") {
                'a' by item {
                    itemProvider by itemProvider(ItemType.YELLOW_BUNDLE) {
                        amount by 1
                        name by "Permanent Deposit".colored(Colors.VERY_YELLOW)
                        data[DataComponentTypes.TOOLTIP_DISPLAY] by TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.BUNDLE_CONTENTS).build()
                        lore by listOf(
                            "Warning! ".colored(Colors.VERY_RED).append("This action is irreversible!".colored(Colors.WHITE)),
                            "".component,
                            getBalanceComponent()
                        )
                    }

                    onClick {
                        val value = valueOf(player.itemOnCursor)
                        if (value > 0) {
                            player.setItemOnCursor(null)
                            depositedCash += value
                            updateCash(player, false)
                        }
                    }
                }
                'b' by item {
                    itemProvider by selectedSlot.map { slot ->
                        val merchandise =
                            merchandiseList.getOrNull(slot) ?: return@map ItemWrapper(ItemStacks.Gui.blackGlass)
                        ItemWrapper(merchandise.get())
                    }

                    onClick {
                        if (selectedSlot.get() >= merchandiseList.size)
                            return@onClick
                        val merchandise = merchandiseList.removeAt(selectedSlot.get())

                        val dropped = player.dropItem(merchandise.get()) ?: return@onClick
                        dropped.persistentDataContainer.set(TraitorGamePlugin.key("merchandise"), PersistentDataType.STRING, player.uniqueId.toString())

                        selectedSlot.set(selectedSlot.get())
                        updateButtons()
                    }
                }
            }
        }.open()
    }

    companion object {
        data class MerchandiseType(
            val name: Component,
            val amount: Int = 1,
            val material: Material = itemProvider(Random).type,
            val itemProvider: (Random) -> ItemStack
        )

        interface Merchandise {
            val name: Component
            val amount: Int
            val material: Material
            fun get(): ItemStack
        }

        fun Merchandise(item: ItemStack) = object : Merchandise {
            override val name: Component
                get() = item.effectiveName()
            override val amount: Int
                get() = item.amount
            override val material: Material
                get() = item.type

            override fun get(): ItemStack = item
        }

        fun Merchandise(type: MerchandiseType) = object : Merchandise {
            override val name: Component
                get() = type.name
            override val amount: Int
                get() = type.amount
            override val material: Material
                get() = type.material
            val seed = Random.nextInt()

            override fun get(): ItemStack = type.itemProvider(Random(seed))
        }

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

        private fun mixedQuiver(size: Int, random: Random): ItemStack {
            var remaining = size
            val items = mutableListOf<ItemStack>()
            while (remaining > 0) {
                val arrow = arrows.random(random).clone()
                val amount = random.nextInt(1, 1+min(8, remaining))
                arrow.amount = amount
                remaining -= amount
                items.add(arrow)
            }
            return ItemStacks.bundle(items)
        }

        val possibleMerchandise: List<List<MerchandiseType>> = listOf(
            listOf(
                MerchandiseType("TNT".colored(0xfc4c1b), 32) { ItemStack.of(Material.TNT, 32) },
                MerchandiseType("TNT".colored(0xfc4c1b), 16) { ItemStack.of(Material.TNT, 16) },
            ),
            listOf(
                MerchandiseType("Pearl".colored(0x12a882), 4) { ItemStack.of(Material.ENDER_PEARL, 4) },
                MerchandiseType("Pearl".colored(0x12a882), 2) { ItemStack.of(Material.ENDER_PEARL, 2) }
            ),
            listOf(
                MerchandiseType("Arrows".component, 32) { ItemStack.of(Material.ARROW, 32) },
                MerchandiseType("Arrows".component, 16) { ItemStack.of(Material.ARROW, 16) },
                MerchandiseType("Mixed Arrows".component, 32) { random -> mixedQuiver(32, random) },
                MerchandiseType("Mixed Arrows".component, 16) { random -> mixedQuiver(16, random) },
            ),
            listOf(
                MerchandiseType("Breeze Rod".component, 4) { ItemStack.of(Material.BREEZE_ROD, 4) },
                MerchandiseType("Breeze Rod".component, 2) { ItemStack.of(Material.BREEZE_ROD, 2) },
            ),
            listOf(
                MerchandiseType("Water Bucket".component) { ItemStack.of(Material.WATER_BUCKET) },
                MerchandiseType("Lava Bucket".component) { ItemStack.of(Material.LAVA_BUCKET) },
            ),
            listOf(
                MerchandiseType("Obsidian".component, 4) { ItemStack.of(Material.OBSIDIAN, 4) },
                MerchandiseType("Obsidian".component, 2) { ItemStack.of(Material.OBSIDIAN, 2) },
            ),
        )

        @JvmField
        val SETTINGS: Role.Settings = Role.Settings(
            key = TraitorGamePlugin.key("mogul"),
            name = "Mogul",
            roleColor = Colors.NEUTRAL_YELLOW,
            alignment = RoleAlignment.PASSIVE_NEUTRAL,
            itemProvider = ItemBuilder(Material.CROSSBOW),
            goalProvider = { roleSettings -> "You have no allegiances. Collect $${roleSettings.mogulMoneyGoal / 100.0} to win." },
            builder = { Mogul() }
        )
    }
}
