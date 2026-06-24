package io.github.metrolung.traitorgame

import io.github.metrolung.traitorgame.role.RoleType
import net.kyori.adventure.text.TextComponent

interface EndGameReason {
    val winMessage: TextComponent
        get() = winningRoleType?.winMessage ?: "Draw...".colored(Colors.ORANGE)
    val winningRoleType: RoleType?

    data class RoleGroupWin(
        override val winningRoleType: RoleType,
    ) : EndGameReason {
        override val winMessage: TextComponent
            get() = winningRoleType.winMessage
    }

    object DragonDefeated : EndGameReason {
        override val winningRoleType: RoleType
            get() = RoleType.SURVIVOR
    }

    object Draw : EndGameReason {
        override val winningRoleType: Nothing?
            get() = null
    }
}