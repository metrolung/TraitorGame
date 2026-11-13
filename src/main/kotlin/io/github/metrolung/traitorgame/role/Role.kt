package io.github.metrolung.traitorgame.role

import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.Meeting
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.SessionSettings
import io.github.metrolung.traitorgame.Vote
import io.github.metrolung.traitorgame.colored
import io.github.metrolung.traitorgame.trait.Trait
import net.kyori.adventure.text.TextComponent
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.ItemProvider

interface Role {
    val settings: Settings

    val roleTraits: List<Trait.Settings>
        get() = listOf()

    fun interface Builder {
        fun build(): Role
    }

    data class Settings(
        val key: NamespacedKey,
        val name: String,
        val roleColor: Int,
        val stylized: TextComponent = name.colored(roleColor),
        val alignment: RoleAlignment,
        val itemProvider: ItemProvider,
        val goalProvider: (SessionSettings) -> String,
        val builder: Builder,
    )
}