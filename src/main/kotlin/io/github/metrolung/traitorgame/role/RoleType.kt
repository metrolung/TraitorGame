package io.github.metrolung.traitorgame.role

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.colored
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent

enum class RoleType {
    TRAITOR,
    NEUTRAL,
    SURVIVOR;

    val winMessage: TextComponent
        get() = when (this) {
            TRAITOR -> "Traitors win!".colored(Colors.TRAITOR_RED)
            NEUTRAL -> "Neutral win!".colored(Colors.ORANGE)
            SURVIVOR -> "Survivors win!".colored(Colors.SURVIVOR_TEAL)
        }
}

interface Traitor : Role {
    override val type: RoleType
        get() = RoleType.TRAITOR

    override fun isWinner(session: Session, sessionPlayer: SessionPlayer, endGameReason: EndGameReason): Boolean {
        if (endGameReason is EndGameReason.RoleGroupWin && endGameReason.winningRoleType == RoleType.TRAITOR) {
            return true
        }

        return super.isWinner(session, sessionPlayer, endGameReason)
    }

    companion object {
        val codeWords = listOf(
            "weather",
            "butterfly",
            "octopus",
            "klutz",
            "asteroid",
            "canoe",
            "pickle",
            "firefly",
            "lemonade",
            "telephone",
            "prism",
            "shock",
            "shower",
            "bomber",
            "dialogue",
            "gravity",
            "pyramid",
            "implode",
        )
    }
}

interface Neutral : Role {
    override val type: RoleType
        get() = RoleType.NEUTRAL
}

interface Survivor : Role {
    override val type: RoleType
        get() = RoleType.SURVIVOR


    override fun isWinner(session: Session, sessionPlayer: SessionPlayer, endGameReason: EndGameReason): Boolean {
        if (endGameReason is EndGameReason.DragonDefeated) {
            return true
        }

        if (endGameReason is EndGameReason.RoleGroupWin && endGameReason.winningRoleType == this.type) {
            return true
        }

        return super.isWinner(session, sessionPlayer, endGameReason)
    }
}