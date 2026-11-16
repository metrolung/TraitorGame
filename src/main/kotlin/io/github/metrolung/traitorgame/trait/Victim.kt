package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.TraitorGamePlugin
import org.bukkit.Material
import xyz.xenondevs.invui.item.ItemBuilder

object Victim : Trait {
    override fun onTakeDamage(event: Trait.Event, damage: Trait.DamageInfo) {
        if (damage.isKilling && damage.attackingPlayer != null) {
            event.session.forceEndSession(EndGameReason.JesterWin(event.player))
        }
    }

    override val settings: Trait.Settings
        get() = SETTINGS

    val SETTINGS = Trait.Settings(
        key = TraitorGamePlugin.key("victim"),
        name = "Victim",
        traitColor = Colors.LAST_CHANCE,
        itemProvider = ItemBuilder(Material.CHICKEN),
        descriptionProvider = { "Victim Provider" },
        provider = { Victim }
    )
}