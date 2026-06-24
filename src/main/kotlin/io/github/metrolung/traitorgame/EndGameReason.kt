package io.github.metrolung.traitorgame

import io.github.metrolung.traitorgame.role.RoleType
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.TextComponent
import org.bukkit.NamespacedKey
import java.util.UUID

interface EndGameReason {
    val winMessage: TextComponent
        get() = winningRoleType?.winMessage ?: "Draw...".colored(Colors.ORANGE)

    val winSound: Sound
        get() = winningRoleType?.winSound ?: Sound.sound {
            it.type(NamespacedKey.minecraft("item.goat_horn.sound.3"))
            it.source(Sound.Source.AMBIENT)
        }

    val winningRoleType: RoleType?

    data class RoleGroupWin(override val winningRoleType: RoleType) : EndGameReason {
        override val winMessage: TextComponent
            get() = winningRoleType.winMessage
    }

    object DragonDefeated : EndGameReason {
        override val winningRoleType: RoleType
            get() = RoleType.Survivor

        override val winMessage: TextComponent
            get() = "Dragon defeated!".colored(Colors.DRAGON_PURPLE)
    }

    data class JesterWin(val sessionPlayer: SessionPlayer) : EndGameReason {
        override val winningRoleType: RoleType
            get() = RoleType.Neutral

        override val winSound: Sound
            get() = Sound.sound {
                it.type(NamespacedKey.minecraft("item.goat_horn.sound.7"))
                it.source(Sound.Source.AMBIENT)
            }

        override val winMessage: TextComponent
            get() = "Jester win!".colored(Colors.JESTER_PEACH)
    }

    object Draw : EndGameReason {
        override val winningRoleType: Nothing?
            get() = null
    }
}