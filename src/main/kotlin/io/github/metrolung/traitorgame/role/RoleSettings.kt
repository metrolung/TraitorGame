package io.github.metrolung.traitorgame.role

import io.github.metrolung.traitorgame.role.roles.Astral
import io.github.metrolung.traitorgame.role.roles.Detective
import io.github.metrolung.traitorgame.role.roles.Mogul
import io.github.metrolung.traitorgame.role.roles.DefaultSurvivor
import io.github.metrolung.traitorgame.role.roles.DefaultTraitor
import io.github.metrolung.traitorgame.role.roles.Jester
import org.bukkit.plugin.Plugin

data class RoleSettings(
    val traitorCount: Int,
    val neutralCount: Int,

    val traitorRolePool: List<Role.Setting>,
    val neutralRolePool: List<Role.Setting>,
    val survivorRolePool: List<Role.Setting>,

    val unassignedTraitor: Role.Generator,
    val unassignedSurvivor: Role.Generator,

    val detectiveNotebookCooldownTicks: Int,
    val traitorCodeWord: String,
    val mogulMoneyGoal: Long,
    val jesterPunishmentTicks: Int,
) {
    companion object {
        fun create(plugin: Plugin, traitorCount: Int, neutralCount: Int): RoleSettings {
            val config = plugin.config

            return RoleSettings(
                traitorCount,
                neutralCount,
                traitorRolePool = listOf(),
                neutralRolePool = listOf(
                    Role.Setting(
                        config.getInt("role.mogul.count"),
                        config.getDouble("role.mogul.chance")/100.0,
                    ) { Mogul() },
                    Role.Setting(
                        config.getInt("role.jester.count"),
                        config.getDouble("role.jester.chance")/100.0,
                    ) { Jester() }
                ),
                survivorRolePool = listOf(
                    Role.Setting(
                        config.getInt("role.detective.count"),
                        config.getDouble("role.detective.chance")/100.0,
                    ) { Detective() },
                    Role.Setting(
                        config.getInt("role.astral.count"),
                        config.getDouble("role.astral.chance")/100.0,
                    ) { Astral }
                ),
                unassignedTraitor = { DefaultTraitor },
                unassignedSurvivor = { DefaultSurvivor },
                detectiveNotebookCooldownTicks = (config.getDouble("role.detective.notebook-cooldown") * 20).toInt(),
                traitorCodeWord = Traitor.codeWords.random(),
                mogulMoneyGoal = (config.getDouble("role.mogul.money-goal") * 100).toLong(),
                jesterPunishmentTicks = (config.getDouble("role.jester.punishment") * 20).toInt()
            )
        }

    }
}