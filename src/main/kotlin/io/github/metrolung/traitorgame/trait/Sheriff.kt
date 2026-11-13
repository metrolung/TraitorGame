package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.ItemStacks
import io.github.metrolung.traitorgame.TraitorGamePlugin
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.ItemBuilder

class Sheriff : Trait {
    override fun onRolePresented(event: Trait.Event) {
        event.player.player?.give(ItemStacks.sheriffCrossbow)
        event.player.player?.give(ItemStack.of(Material.ARROW).apply { amount = 32 })
    }

    override val settings: Trait.Settings
        get() = SETTINGS

    companion object {
        val SETTINGS = Trait.Settings(
            key = TraitorGamePlugin.key("sheriff"),
            name = "Sheriff",
            traitColor = Colors.LAST_CHANCE,
            itemProvider = ItemBuilder(Material.CROSSBOW),
            builder = { LastChance() }
        )
    }
}