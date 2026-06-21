package io.github.metrolung.traitorgame

import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType


@Suppress("UnstableApiUsage")
object ItemStacks {

    val SPECIAL_EQUIPMENT_KEY = TraitorGamePlugin.key("equipment")


    val notebook = ItemStack.of(Material.BOOK).apply {
        this.setData(DataComponentTypes.ITEM_NAME, Component.text("Notebook"))
        this.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.minecraft("writable_book"))
        this.editPersistentDataContainer { pdc ->
            pdc.set(SPECIAL_EQUIPMENT_KEY, PersistentDataType.STRING, "notebook")
        }
    }

    val detective_crossbow = ItemStack.of(Material.CROSSBOW).apply {
        this.editMeta { meta ->
            meta.addEnchant(Enchantment.QUICK_CHARGE, 2, true)
        }
    }

}