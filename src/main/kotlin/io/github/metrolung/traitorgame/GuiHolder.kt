package io.github.metrolung.traitorgame

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

interface GuiHolder : InventoryHolder {
    val layout: String
    val rows: Int
    val name: Component

    fun onRender()
    fun getItemAt(ch: Char, slot: Int): ItemStack?

//    @JvmField
//    val inventory = Bukkit.createInventory(this, 54, Component.text("Merchandise Box"))
    override fun getInventory(): Inventory {
        val inventory = Bukkit.createInventory(this, 9*rows, name)

        onRender()
        for ((slot, ch) in layout.filterNot { it.isWhitespace() }.withIndex()) {
            inventory.setItem(slot, getItemAt(ch, slot))
        }

        return inventory
    }
}