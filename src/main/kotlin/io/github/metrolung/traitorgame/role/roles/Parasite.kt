package io.github.metrolung.traitorgame.role.roles

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.github.metrolung.traitorgame.role.Neutral
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleAlignment
import io.github.metrolung.traitorgame.trait.Pacifist
import io.github.metrolung.traitorgame.trait.Trait
import io.github.metrolung.traitorgame.trait.Victim
import org.bukkit.Material
import xyz.xenondevs.invui.item.ItemBuilder

class Parasite : Traitor {
    override val settings: Role.Settings get() = SETTINGS

    companion object {
        @JvmField
        val SETTINGS: Role.Settings = Role.Settings(
            key = TraitorGamePlugin.key("parasite"),
            name = "Parasite",
            roleColor = Colors.TRAITOR_RED,
            alignment = RoleAlignment.TRAITOR,
            itemProvider = ItemBuilder(Material.ROTTEN_FLESH),
            descriptionProvider = { "Parasite description" },
            provider = { Parasite() }
        )
    }
}