package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.ItemStacks
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.github.metrolung.traitorgame.trait.event.TraitEvent
import org.bukkit.Material
import xyz.xenondevs.invui.item.ItemBuilder

class Detective : Trait {
    override fun onRolePresented(event: TraitEvent.RolePresentedEvent) {
        event.player.player?.give(ItemStacks.sampleChecker)
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
            traitColor = Colors.LAST_CHANCE,
            itemProvider = ItemBuilder(Material.BOOK),
            descriptionProvider = { "Detective description" },
            provider = { Detective() }
        )
    }
}