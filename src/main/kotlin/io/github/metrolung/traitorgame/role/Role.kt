package io.github.metrolung.traitorgame.role

import io.github.metrolung.traitorgame.ConfigurableItemRegistry
import io.github.metrolung.traitorgame.RoleRegistry
import io.github.metrolung.traitorgame.TraitRegistry
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

    data class Settings(
        val key: NamespacedKey,
        val name: String,
        val roleColor: Int,
        val stylized: TextComponent = name.colored(roleColor),
        val alignment: RoleAlignment,
        val itemProvider: ItemProvider,
        val descriptionProvider: () -> String,
        val provider: () -> Role,
    ) {
        fun register(configurableItemRegistry: ConfigurableItemRegistry<Role.Settings> = RoleRegistry) {
            configurableItemRegistry.register(this.key, this)
        }

        fun getConfig(configurableItemRegistry: ConfigurableItemRegistry<Role.Settings> = RoleRegistry): ConfigurationSection? {
            return configurableItemRegistry.getConfig(this.key)
        }
    }
}