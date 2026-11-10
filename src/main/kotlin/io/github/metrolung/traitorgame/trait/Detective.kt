package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.ItemStacks
import io.github.metrolung.traitorgame.MiniMessage
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.TraitorGamePlugin
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.ItemBuilder

class Detective : Trait {
    override fun onRolePresented(session: Session, sessionPlayer: OnlineSessionPlayer) {
        sessionPlayer.player.give(ItemStacks.sampleChecker)
    }

    sealed interface DNASample {
        data class IdentifiableSample(val player: SessionPlayer) : DNASample
        data class IdentifiableCorpseSample(val player: SessionPlayer) : DNASample
        data class CorpseSample(val corpse: SessionPlayer, val potentials: List<SessionPlayer>) : DNASample
    }


    override val settings: Trait.Settings
        get() = SETTINGS

    companion object {
        val SETTINGS = Trait.Settings(
            key = TraitorGamePlugin.key("detective"),
            name = "Detective",
            activeWhileDead = false,
            traitColor = Colors.LAST_CHANCE,
            itemProvider = ItemBuilder(Material.BOOK),
            builder = { LastChance() }
        )
    }
}