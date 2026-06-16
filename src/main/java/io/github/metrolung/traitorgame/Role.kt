package io.github.metrolung.traitorgame

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import org.bukkit.inventory.ItemStack


interface Role {
    val name: TextComponent
    val group: RoleGroup

    object Traitor : Role {
        override val name: TextComponent
            get() = Component
                .text("Traitor")
                .color(TextColor.color(0xFF0000))

        override val group: RoleGroup
            get() = RoleGroup.Traitors
    }

    object LuckyTraitor : Role {
        override val name: TextComponent
            get() = Component
                .text("Traitor")
                .color(TextColor.color(0xE01A7A))

        override val group: RoleGroup
            get() = RoleGroup.Traitors
    }

    object Detective : Role {
        override val name: TextComponent
            get() = Component
                .text("Detective")
                .color(TextColor.color(0x0000FF))

        override val group: RoleGroup
            get() = RoleGroup.Survivors
    }

    object Survivor : Role {
        override val name: TextComponent
            get() = Component
                .text("Survivor")
                .color(TextColor.color(0x01FFCC))

        override val group: RoleGroup
            get() = RoleGroup.Survivors
    }

    object LuckySurvivor : Role {
        override val name: TextComponent
            get() = Component
                .text("Survivor")
                .color(TextColor.color(0x00ff87))

        override val group: RoleGroup
            get() = RoleGroup.Survivors
    }
}