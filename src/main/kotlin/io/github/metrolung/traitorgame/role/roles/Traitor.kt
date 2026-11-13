package io.github.metrolung.traitorgame.role.roles

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.ItemStacks
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.SessionSettings
import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleAlignment
import io.github.metrolung.traitorgame.trait.Trait
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
            goalProvider = { sessionSettings ->
                if (sessionSettings.traitorCount == 1)
                    "Kill all the players and end the game."
                else
                    "Kill all the players and end the game. Find your teammates with the codeword: ${"placeholder"}"
            },
            builder = { Traitor() }
        )
    }
}

fun Traitor() = object : Traitor {}