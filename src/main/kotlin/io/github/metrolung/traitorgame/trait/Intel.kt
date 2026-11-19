package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.ItemStacks
import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.github.metrolung.traitorgame.trait.event.TraitEvent
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.ItemBuilder

class Intel : Trait {
    override val settings: Trait.Settings
        get() = SETTINGS

    companion object {
        val SETTINGS = Trait.Settings(
            key = TraitorGamePlugin.key("intel"),
            name = "Intel",
            traitColor = Colors.LAST_CHANCE,
            itemProvider = ItemBuilder(Material.RED_DYE),
            descriptionProvider = { "Intel description" },
            provider = { Intel() }
        )
    }
}