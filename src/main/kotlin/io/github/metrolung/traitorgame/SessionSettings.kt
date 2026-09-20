package io.github.metrolung.traitorgame

import io.github.metrolung.traitorgame.role.RoleSettings
import org.bukkit.Location
import org.bukkit.plugin.Plugin

@JvmRecord
data class SessionSettings(
    val bellLocation: Location,

    val testMode: Boolean,

    val meetingCooldownTicks: Int,
    val discussionTimeTicks: Int,
    val votingTimeTicks: Int,
    val entityDamageFactor: Double,
    val endIfNoEvil: Boolean,

    val chatRange: Double,
    val chatFalloff: Double,
    val blockDampening: Double,

    val roleSettings: RoleSettings
) {
    companion object {
        fun create(
            plugin: TraitorGamePlugin,
            traitorCount: Int,
            passiveNeutralCount: Int,
            evilNeutralCount: Int,
            bellLocation: Location
        ): SessionSettings {
            val config = plugin.config

            return SessionSettings(
                bellLocation,
                testMode = config.getBoolean("test-mode"),
                meetingCooldownTicks = (config.getDouble("gameplay.meeting-cooldown") * 20).toInt(),
                discussionTimeTicks = (config.getDouble("gameplay.discussion-time") * 20).toInt(),
                votingTimeTicks = (config.getDouble("gameplay.voting-time") * 20).toInt(),
                entityDamageFactor = config.getDouble("gameplay.entity-damage-factor"),
                endIfNoEvil = config.getBoolean("gameplay.end-if-no-evil"),
                chatRange = config.getDouble("chat.range"),
                chatFalloff = config.getDouble("chat.falloff"),
                blockDampening = config.getDouble("chat.block-dampening"),
                roleSettings = RoleSettings.create(plugin, traitorCount, passiveNeutralCount, evilNeutralCount)
            )
        }
    }
}