package io.github.metrolung.traitorgame.role.roles

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.ItemStacks
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleAlignment
import io.github.metrolung.traitorgame.role.RoleSettings


interface Traitor : Role {
    override val roleColor: Int
        get() = Colors.TRAITOR_RED

    override val alignment: RoleAlignment
        get() = RoleAlignment.TRAITOR

    override val isEvil: Boolean
        get() = true

    override fun getGoal(roleSettings: RoleSettings): String {
        if (roleSettings.traitorCount == 1) {
            return "Kill all the players and end the game."
        }
        return "Kill all the players and end the game. Find your teammates with the codeword: ${roleSettings.traitorCodeWord}"
    }

    override fun isWinner(session: Session, sessionPlayer: SessionPlayer, endGameReason: EndGameReason): Boolean {
        if (endGameReason is EndGameReason.TraitorWin) {
            return true
        }

        return super.isWinner(session, sessionPlayer, endGameReason)
    }

    override fun onRolePresented(session: Session, sessionPlayer: OnlineSessionPlayer) {
        sessionPlayer.player.inventory.setItem(9, ItemStacks.manifesto(session.settings.roleSettings.traitorCodeWord))

        super.onRolePresented(session, sessionPlayer)
    }
}

fun Traitor() = object : Traitor {
    override val name: String
        get() = "Traitor"
}