package io.github.metrolung.traitorgame

import org.bukkit.Location
import org.bukkit.plugin.Plugin

@JvmRecord
data class SessionSettings(
    val traitorCount: Int,
    val bellLocation: Location,

    val meetingCooldownTicks: Int,
    val discussionTimeTicks: Int,
    val votingTimeTicks: Int,
    val entityDamageFactor: Double,
    val onePlayerEndsGame: Boolean,

    val chatRange: Double,
    val chatFalloff: Double,
    val blockDampening: Double,
) {


    companion object {
        fun create(plugin: Plugin, traitorCount: Int, bellLocation: Location): SessionSettings {
            val config = plugin.config

            return SessionSettings(
                traitorCount,
                bellLocation,
                meetingCooldownTicks = (config.getDouble("gameplay.meeting-cooldown") * 20).toInt(),
                discussionTimeTicks = (config.getDouble("gameplay.discussion-time") * 20).toInt(),
                votingTimeTicks = (config.getDouble("gameplay.voting-time") * 20).toInt(),
                entityDamageFactor = config.getDouble("gameplay.entity-damage-factor"),
                onePlayerEndsGame = config.getBoolean("gameplay.one-player-ends-game"),
                chatRange = config.getDouble("chat.range"),
                chatFalloff = config.getDouble("chat.falloff"),
                blockDampening = config.getDouble("chat.block-dampening")
            )
        }
    }
}