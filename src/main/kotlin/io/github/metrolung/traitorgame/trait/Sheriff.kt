package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.ItemStacks
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.TraitorGamePlugin
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.ItemBuilder

class Sheriff : Trait {
    override fun onRolePresented(session: Session, sessionPlayer: OnlineSessionPlayer) {
        sessionPlayer.player.give(ItemStacks.sheriffCrossbow)
        sessionPlayer.player.give(ItemStack.of(Material.ARROW).apply { amount = 32 })
    }

    override val settings: Trait.Settings
        get() = SETTINGS

    companion object {
        val SETTINGS = Trait.Settings(
            key = TraitorGamePlugin.key("sheriff"),
            name = "Sheriff",
            activeWhileDead = false,
            traitColor = Colors.LAST_CHANCE,
            itemProvider = ItemBuilder(Material.CROSSBOW),
            builder = { LastChance() }
        )
    }
}