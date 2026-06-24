package io.github.metrolung.traitorgame.role.roles

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.ItemStacks
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleSettings
import io.github.metrolung.traitorgame.role.Traitor


object DefaultTraitor : Traitor {
    override val roleColor: Int
        get() = Colors.TRAITOR_RED

    override val name: String
        get() = "Traitor"

    override fun getGoal(roleSettings: RoleSettings): String {
        if (roleSettings.traitorCount == 1) {
            return "Kill all the players and end the game."
        } else {
            return "Kill all the players and end the game. Find your teammates with the codeword: ${roleSettings.traitorCodeWord}"
        }
    }

    override fun onRolePresented(session: Session, sessionPlayer: OnlineSessionPlayer) {
        sessionPlayer.player.inventory.setItem(9, ItemStacks.manifesto)
    }
}