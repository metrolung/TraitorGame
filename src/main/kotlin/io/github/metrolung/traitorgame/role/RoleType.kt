package io.github.metrolung.traitorgame.role

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.colored
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.NamespacedKey

sealed interface RoleType {
    val name: String
    val collectiveName: String
    val color: Int
    val winSound: Sound

    val winMessage: TextComponent
        get() = "$collectiveName win!".colored(color)
    val stylized: TextComponent
        get() = name.colored(color)

    object Traitor : RoleType {
        override val name = "Traitor"
        override val collectiveName = "Traitors"

        override val color = Colors.TRAITOR_RED

        override val winSound = Sound.sound {
            it.type(NamespacedKey.minecraft("item.goat_horn.sound.2"))
            it.source(Sound.Source.AMBIENT)
        }
    }
    object Neutral : RoleType {
        override val name = "Neutral"
        override val collectiveName = "Neutral"

        override val color = Colors.NEUTRAL_YELLOW

        override val winSound: Sound = Sound.sound {
            it.type(NamespacedKey.minecraft("item.goat_horn.sound.0"))
            it.source(Sound.Source.AMBIENT)
        }
    }
    object Survivor : RoleType {
        override val name = "Survivor"
        override val collectiveName = "Survivors"

        override val color = Colors.SURVIVOR_TEAL

        override val winSound = Sound.sound {
                it.type(NamespacedKey.minecraft("item.goat_horn.sound.1"))
                it.source(Sound.Source.AMBIENT)
            }
    }
}

interface Traitor : Role {
    override val roleColor: Int
        get() = Colors.TRAITOR_RED

    override val type: RoleType
        get() = RoleType.Traitor

    override val isEvil: Boolean
        get() = true

    override fun isWinner(session: Session, sessionPlayer: SessionPlayer, endGameReason: EndGameReason): Boolean {
        if (endGameReason is EndGameReason.RoleGroupWin && endGameReason.winningRoleType == RoleType.Traitor) {
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
    override val roleColor: Int
        get() = Colors.NEUTRAL_YELLOW

    override val type: RoleType
        get() = RoleType.Neutral
}

interface Survivor : Role {
    override val roleColor: Int
        get() = Colors.SURVIVOR_TEAL

    override val type: RoleType
        get() = RoleType.Survivor

    override val isEvil: Boolean
        get() = false

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

interface EvilRole {}