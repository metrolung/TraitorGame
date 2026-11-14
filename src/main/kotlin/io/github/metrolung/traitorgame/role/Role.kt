package io.github.metrolung.traitorgame.role

import io.github.metrolung.traitorgame.ConfigurableItemRegistry
import io.github.metrolung.traitorgame.SessionSettings
import io.github.metrolung.traitorgame.colored
import io.github.metrolung.traitorgame.trait.Trait
import net.kyori.adventure.text.TextComponent
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import xyz.xenondevs.invui.item.ItemProvider

interface Role {
    val settings: Settings

    val roleTraits: List<Trait.Settings>
        get() = listOf()

    fun interface Builder {
        fun build(config: ConfigurationSection): Role
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
    ) {
        fun register(configurableItemRegistry: ConfigurableItemRegistry<Role.Settings>) {
            configurableItemRegistry.register(this.key, this)
        }
    }
}