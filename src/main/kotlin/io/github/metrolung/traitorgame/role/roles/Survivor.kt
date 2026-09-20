package io.github.metrolung.traitorgame.role.roles

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleAlignment
import org.bukkit.Material
import xyz.xenondevs.invui.item.ItemBuilder

interface Survivor : Role {

    override fun isWinner(session: Session, sessionPlayer: SessionPlayer, endGameReason: EndGameReason): Boolean {
        if (endGameReason is EndGameReason.SurvivorWin) {
            return true
        }

        if (endGameReason is EndGameReason.DragonDefeated) {
            return true
        }

        return super.isWinner(session, sessionPlayer, endGameReason)
    }

    override val settings: Role.Settings
        get() = SETTINGS

    companion object {
        @JvmField
        val SETTINGS: Role.Settings = Role.Settings(
            key = TraitorGamePlugin.key("survivor"),
            name = "Survivor",
            roleColor = Colors.SURVIVOR_TEAL,
            alignment = RoleAlignment.SURVIVOR,
            itemProvider = ItemBuilder(Material.DIAMOND),
            goalProvider = { "Defeat the Ender Dragon and don't die!" },
            builder = { Survivor() }
        )
    }
}

fun Survivor() = object : Survivor {}