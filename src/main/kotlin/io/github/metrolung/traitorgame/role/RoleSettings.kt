package io.github.metrolung.traitorgame.role

import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.github.metrolung.traitorgame.role.roles.Survivor
import io.github.metrolung.traitorgame.role.roles.Traitor

data class RoleSettings(
    val traitorCount: Int,
    val passiveNeutralCount: Int,
    val evilNeutralCount: Int,

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
                detectiveNotebookCooldownTicks = (config.getDouble("role.detective.notebook-cooldown") * 20).toInt(),
                traitorCodeWord = plugin.getCodewords().random(),
                mogulMoneyGoal = (config.getDouble("role.mogul.money-goal") * 100).toLong(),
                jesterPunishmentTicks = (config.getDouble("role.jester.punishment") * 20).toInt()
            )
        }

    }
}