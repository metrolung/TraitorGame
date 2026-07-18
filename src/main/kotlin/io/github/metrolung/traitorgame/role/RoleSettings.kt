package io.github.metrolung.traitorgame.role

import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.github.metrolung.traitorgame.role.roles.Survivor
import io.github.metrolung.traitorgame.role.roles.Traitor

data class RoleSettings(
    val traitorCount: Int,
    val passiveNeutralCount: Int,
    val evilNeutralCount: Int,

    val traitorRolePool: List<Role.Setting>,
    val passiveNeutralRolePool: List<Role.Setting>,
    val evilNeutralRolePool: List<Role.Setting>,
    val survivorRolePool: List<Role.Setting>,

    val unassignedTraitor: Role.Builder,
    val unassignedSurvivor: Role.Builder,

    val detectiveNotebookCooldownTicks: Int,
    val traitorCodeWord: String,
    val mogulMoneyGoal: Long,
    val jesterPunishmentTicks: Int,
) {
    companion object {
        fun create(
            plugin: TraitorGamePlugin,
            traitorCount: Int,
            passiveNeutralCount: Int,
            evilNeutralCount: Int,
        ): RoleSettings {
            val config = plugin.config

            return RoleSettings(
                traitorCount,
                passiveNeutralCount,
                evilNeutralCount,
                survivorRolePool = plugin.roles.values.mapNotNull { roleSetting ->
                    roleSetting.takeIf { it.alignment.isSurvivor }
                },
                passiveNeutralRolePool = plugin.roles.values.mapNotNull { roleSetting ->
                    roleSetting.takeIf { it.alignment.isPassiveNeutral }
                },
                evilNeutralRolePool = plugin.roles.values.mapNotNull { roleSetting ->
                    roleSetting.takeIf { it.alignment.isEvilNeutral }
                },
                traitorRolePool = plugin.roles.values.mapNotNull { roleSetting ->
                    roleSetting.takeIf { it.alignment.isTraitor }
                },
                unassignedTraitor = ::Traitor,
                unassignedSurvivor = ::Survivor,
                detectiveNotebookCooldownTicks = (config.getDouble("role.detective.notebook-cooldown") * 20).toInt(),
                traitorCodeWord = plugin.getCodewords().random(),
                mogulMoneyGoal = (config.getDouble("role.mogul.money-goal") * 100).toLong(),
                jesterPunishmentTicks = (config.getDouble("role.jester.punishment") * 20).toInt()
            )
        }

    }
}