package io.github.metrolung.traitorgame

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

interface GuiHolder : InventoryHolder {
    val key: Key

    val layout: String
    val rows: Int
    val name: Component

    fun onRender()
    fun getItemAt(ch: Char, slot: Int): ItemStack?

    var inventoryCache: Inventory?

    fun update(session: Session) {
        if (inventoryCache == null)
            inventoryCache = session.server.createInventory(this, 9*rows, name)

        onRender()
        var slot = 0
        for (ch in layout) {
            if (ch.isWhitespace() || ch == '\n') {
                continue
            }

            if (slot >= 9*rows) {
                break
            }

            val item = getItemAt(ch, slot)
            inventoryCache!!.setItem(slot, item)
            slot++
        }
    }

    override fun getInventory(): Inventory {
        return inventoryCache!!
    }
}