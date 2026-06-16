package io.github.metrolung.traitorgame

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor


sealed interface RoleGroup {
    val winMessage: TextComponent

    object Traitors : RoleGroup {
        override val winMessage: TextComponent
            get() = Component
                .text("Traitors win!")
                .color(TextColor.color(0xE8294C))
    }

    object Survivors : RoleGroup {
        override val winMessage: TextComponent
            get() = Component
                .text("Survivors win!")
                .color(TextColor.color(0x01FFCC))
    }
}