package io.github.metrolung.traitorgame

import io.papermc.paper.entity.Leashable
import org.bukkit.entity.Entity
import org.bukkit.entity.Interaction
import org.bukkit.entity.Mannequin
import java.util.UUID
import kotlin.time.Instant

data class SessionCorpse(
    val player: SessionPlayer,
    val mannequin: Mannequin,
    val interaction: Entity,
    val causeOfDeath: String,
    val timeOfDeath: Instant,
    val contaminators: MutableMap<UUID, SessionPlayer> = mutableMapOf()
) {
    companion object {
        val CORPSE_KEY = TraitorGamePlugin.key("corpse")
    }
}
