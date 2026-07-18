package io.github.metrolung.traitorgame.role.roles

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleAlignment
import io.github.metrolung.traitorgame.role.RoleSettings

interface Survivor : Role {
    override val roleColor: Int
        get() = Colors.SURVIVOR_TEAL

    override val alignment: RoleAlignment
        get() = RoleAlignment.SURVIVOR

    override val isEvil: Boolean
        get() = false

    override fun getGoal(roleSettings: RoleSettings): String {
        return "Defeat the enderdragon and don't die!"
    }

    override fun isWinner(session: Session, sessionPlayer: SessionPlayer, endGameReason: EndGameReason): Boolean {
        if (endGameReason is EndGameReason.SurvivorWin) {
            return true
        }

        if (endGameReason is EndGameReason.DragonDefeated) {
            return true
        }

        return super.isWinner(session, sessionPlayer, endGameReason)
    }
}

fun Survivor() = object : Survivor {
    override val name: String
        get() = "Survivor"
}