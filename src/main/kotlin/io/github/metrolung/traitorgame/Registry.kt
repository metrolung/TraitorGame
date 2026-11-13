package io.github.metrolung.traitorgame

import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.trait.Trait
import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File


class Registry<T>(
    val parseOneLiner: ((String) -> ConfigurationSection?)? = null
) {
    data class RegisteredRecord<T>(
        val value: T,
        var configurationSection: ConfigurationSection
    )

    val configurationFiles: Map<String, ConfigurationSection>
        field: MutableMap<String, ConfigurationSection> = mutableMapOf()
    val registered: Map<Key, RegisteredRecord<T>>
        field: MutableMap<Key, RegisteredRecord<T>> = mutableMapOf()

    fun addConfiguration(plugin: Plugin, configurationSection: ConfigurationSection) {
        configurationFiles[plugin.namespace()] = configurationSection
    }

    fun setConfigurationFile(plugin: Plugin, file: File) {
        val config = YamlConfiguration.loadConfiguration(file)
        configurationFiles[plugin.namespace()] = config
    }

    fun register(key: Key, value: T) {
        registered[key] = RegisteredRecord(value, MemoryConfiguration())
    }

    fun updateConfigs() {
        for ((namespace, config) in configurationFiles) {
            for (name in config.getKeys(false)) {
                val key = try {
                    if (name.contains(':')) Key.key(name) else Key.key(namespace, name)
                } catch (_: InvalidKeyException) {
                    continue
                }

                val record = registered[key] ?: continue
                var section = config.getConfigurationSection(name)

                if (section == null) {
                    val s = config.getString(name) ?: continue
                    section = parseOneLiner?.invoke(s) ?: continue
                }

                record.configurationSection = section
            }
        }
    }
}

fun traitOrRoleOneLiner(string: String): ConfigurationSection {
    var chance = 0.0
    var min = 0
    var max = 1

    val split = string.split(" ")
    for (sub in split) {
        try {
            if (sub.endsWith("%")) {
                chance = sub.substring(0, sub.length - 2).toDouble()
            } else if (sub.contains("-")) {
                val minAndMax = sub.split("-")
                min = minAndMax[0].toInt()
                max = minAndMax[1].toInt()
            } else {
                max = sub.toInt()
            }
        } catch (_: NumberFormatException) {
            continue
        }
    }

    val config = MemoryConfiguration()

    config.set("min", min)
    config.set("min", max)
    config.set("chance", chance)

    return config
}

val TraitRegistry = Registry<Trait.Settings>(::traitOrRoleOneLiner)
val RoleRegistry = Registry<Role.Settings>(::traitOrRoleOneLiner)
//class Registry {
//    val traits: MutableMap<Key, Trait.Settings> = mutableMapOf()
//    val roles: MutableMap<Key, Role.Settings> = mutableMapOf()
//
//    fun registerTrait() {
//
//    }
//
//    fun reload() {
//
//    }
//}