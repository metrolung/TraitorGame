package io.github.metrolung.traitorgame

import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BundleMeta
import org.bukkit.persistence.PersistentDataType


@Suppress("UnstableApiUsage")
object ItemStacks {
    val ROLE_EQUIPMENT_KEY = TraitorGamePlugin.key("equipment")
    val ROLE_MATERIAL_KEY = TraitorGamePlugin.key("material")

    val manifesto: ItemStack
        get() = ItemStack.of(Material.BOOK).apply {
            this.setData(DataComponentTypes.ITEM_NAME, Component.text("Traitor's Manifesto").color(Colors.TRAITOR_RED.textColor))
            this.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            this.lore(listOf("DO NOT LET ANYONE SEE THIS".colored(Colors.VERY_YELLOW).decorate(
                TextDecoration.BOLD)))
            this.editPersistentDataContainer { pdc ->
                pdc.set(ROLE_MATERIAL_KEY, PersistentDataType.STRING, "manifesto")
            }
        }

    val notebook: ItemStack
        get() = ItemStack.of(Material.BOOK).apply {
            this.setData(DataComponentTypes.ITEM_NAME, Component.text("Notebook"))
            this.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.minecraft("writable_book"))
            this.lore(listOf("Shift + click to open".colored(Colors.VERY_YELLOW)))
            this.editPersistentDataContainer { pdc ->
                pdc.set(ROLE_EQUIPMENT_KEY, PersistentDataType.STRING, "notebook")
            }
        }

    val merchandise_box: ItemStack
        get() = ItemStack.of(Material.CHEST).apply {
            this.setData(DataComponentTypes.ITEM_NAME, Component.text("Merchandise Box"))
            this.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            this.lore(listOf("Click to open".colored(Colors.VERY_YELLOW)))
            this.editPersistentDataContainer { pdc ->
                pdc.set(ROLE_EQUIPMENT_KEY, PersistentDataType.STRING, "merchandise_box")
            }
        }

    val swab: ItemStack
        get() = ItemStack.of(Material.WHITE_DYE).apply {
            this.setData(DataComponentTypes.ITEM_NAME, "Swab".component)
            this.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.minecraft("bone"))
            this.lore(listOf("Click a player to swab them".colored(Colors.VERY_YELLOW)))
            this.editPersistentDataContainer { pdc ->
                pdc.set(ROLE_MATERIAL_KEY, PersistentDataType.STRING, "swab")
            }
        }

    val USED_SWAB_KEY = TraitorGamePlugin.key("used_swab")
    fun usedSwab(name: Component, index: Int) = ItemStack.of(Material.WHITE_DYE).apply {
        this.setData(DataComponentTypes.ITEM_NAME, name)
        this.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
        this.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.minecraft("bone"))
        this.editPersistentDataContainer { pdc ->
            pdc.set(ROLE_MATERIAL_KEY, PersistentDataType.STRING, "used_swab")
            pdc.set(USED_SWAB_KEY, PersistentDataType.INTEGER, index)
        }
    }

    val swabChecker: ItemStack
        get() = ItemStack.of(Material.WHITE_BUNDLE).apply {
            this.setData(DataComponentTypes.ITEM_NAME, "Swab Checker".component)
            this.lore(listOf("Click to compare DNA of two swabs".colored(Colors.VERY_YELLOW)))
            this.editPersistentDataContainer { pdc ->
                pdc.set(ROLE_EQUIPMENT_KEY, PersistentDataType.STRING, "swab_checker")
            }
        }

    val moneyBag: ItemStack
        get() = ItemStack.of(Material.YELLOW_BUNDLE).apply {
            this.setData(DataComponentTypes.ITEM_NAME, "Money Bag".component)
            this.lore(listOf(Component.text("Put precious minerals in here for cash").color(Colors.VERY_YELLOW.textColor)))
            this.editPersistentDataContainer { pdc ->
                pdc.set(ROLE_EQUIPMENT_KEY, PersistentDataType.STRING, "money_bag")
            }
        }

    val detectiveCrossbow: ItemStack
        get() = ItemStack.of(Material.CROSSBOW).apply {
            this.editMeta { meta ->
                meta.addEnchant(Enchantment.QUICK_CHARGE, 2, true)
            }
        }

    fun bundle(arrows: MutableList<ItemStack>): ItemStack = ItemStack.of(Material.BUNDLE).apply {
        this.editMeta { meta ->
            (meta as BundleMeta).setItems(arrows)
        }
    }


    val MERCHANDISE_INDEX_KEY = TraitorGamePlugin.key("merchandise_index")
    fun merchandise(name: Component, material: Material, index: Int) = ItemStack.of(material).apply {
        this.setData(DataComponentTypes.ITEM_NAME, name)
        this.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
        this.editPersistentDataContainer { pdc ->
            pdc.set(ROLE_EQUIPMENT_KEY, PersistentDataType.STRING, "merchandise")
            pdc.set(MERCHANDISE_INDEX_KEY, PersistentDataType.INTEGER, index)
        }
    }

    object Gui {
        val blackGlass = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE).apply {
            this.setData(DataComponentTypes.ITEM_NAME, "".component)
        }
        val yellowGlass = ItemStack.of(Material.YELLOW_STAINED_GLASS_PANE).apply {
            this.setData(DataComponentTypes.ITEM_NAME, "".component)
        }
    }

}