package io.github.metrolung.traitorgame

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor

interface EndGameReason {

//    fun determineWinners(session: Session): List<>
    val winMessage: TextComponent

    fun isWinner(session: Session, sessionPlayer: SessionPlayer): Boolean

    data class RoleGroupWin(
        val roleGroup: RoleGroup,
    ) : EndGameReason {
        override val winMessage: TextComponent
            get() = roleGroup.winMessage

        override fun isWinner(session: Session, sessionPlayer: SessionPlayer): Boolean {
            return session.isPlayerAlive(sessionPlayer) && sessionPlayer.role.group == roleGroup
        }
    }

    object Draw : EndGameReason {
        override val winMessage: TextComponent
            get() = Component.text("Draw...").color(TextColor.color(0xFF8E63))

        override fun isWinner(session: Session, sessionPlayer: SessionPlayer): Boolean {
            return false
        }
    }
}