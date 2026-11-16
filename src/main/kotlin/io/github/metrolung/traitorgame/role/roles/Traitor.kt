package io.github.metrolung.traitorgame.role.roles

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleAlignment
import org.bukkit.Material
import xyz.xenondevs.invui.item.ItemBuilder


interface Traitor : Role {
    override val settings: Role.Settings
        get() = SETTINGS

    companion object {
        @JvmField
        val SETTINGS: Role.Settings = Role.Settings(
            key = TraitorGamePlugin.key("traitor"),
            name = "Traitor",
            roleColor = Colors.TRAITOR_RED,
            alignment = RoleAlignment.TRAITOR,
            itemProvider = ItemBuilder(Material.REDSTONE),
            descriptionProvider = { "Kill all the players and end the game." },
            provider = { Traitor() }
        )
    }
}

fun Traitor() = object : Traitor {}