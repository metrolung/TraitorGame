package io.github.metrolung.traitorgame

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import com.destroystokyo.paper.event.server.ServerTickEndEvent
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.Server
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID

class SessionManager : Listener {
    var oldSession: Session? = null
        private set
    var session: Session? = null
        private set
    val isSessionActive: Boolean
        get() = session != null

    fun startSession(server: Server, plugin: Plugin, configs: SessionSettings) {
        if (this.session == null) {
            this.session = Session(server, plugin, this, configs)
            this.session!!.onSessionStart()
        } else {
            error("Session already active")
        }
    }

    @JvmOverloads
    fun endSession(reason: EndGameReason? = null) {
        this.session?.let { session ->
            this.oldSession = session
            this.session = null
            session.onSessionEnd(reason)
        }
    }

    @EventHandler
    @Suppress("UNUSED")
    private fun onServerTicked(event: ServerTickEndEvent) {
        session?.onServerTicked(event.tickNumber)
    }

    @EventHandler
    private fun onPlayerDeath(event: PlayerDeathEvent) {
        session?.onPlayerDeath(event)
    }

    @EventHandler
    private fun onPlayerChat(event: AsyncChatEvent) {
        session?.onPlayerChat(event)
    }

    @EventHandler
    private fun onEntityDeath(event: EntityDeathEvent) {
        session?.onEntityDeath(event)
    }

    @EventHandler
    private fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        session?.onEntityDamageByEntity(event)
    }

    @EventHandler
    private fun onItemDespawn(event: ItemDespawnEvent) {
        session?.onItemDespawn(event)
    }

    @EventHandler
    private fun onEntityTakeDamage(event: EntityDamageEvent) {
        session?.onEntityTakeDamage(event)
    }

    @EventHandler
    private fun onPlayerJoinGame(event: PlayerJoinEvent) {
        session?.onPlayerJoin(event.player)
    }

    @EventHandler
    private fun onPlayerRespawn(event: PlayerPostRespawnEvent) {
        session?.onPlayerRespawn(event.player)
    }

    @EventHandler
    private fun onPlayerPickup(event: EntityPickupItemEvent) {
        session?.onEntityPickup(event)
    }

    @EventHandler
    private fun onPlayerDrop(event: PlayerDropItemEvent) {
        session?.onPlayerDrop(event)
    }

    @EventHandler
    private fun onPlayerAdvancement(event: PlayerAdvancementDoneEvent) {
        session?.onPlayerAdvancement(event)
    }

    @EventHandler
    private fun onInventoryClicked(event: InventoryClickEvent) {
        session?.onInventoryClickEvent(event)
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player

        val passthrough = event.rightClicked.persistentDataContainer.get(INTERACTION_PASSTHROUGH_KEY, PersistentDataType.STRING)
        if (passthrough != null) {
            val entity = player.server.getEntity(UUID.fromString(passthrough))

            if (entity != null) {
                val passthroughEvent = PlayerInteractEntityEvent(
                    player,
                    entity,
                    event.hand
                )
                player.server.pluginManager.callEvent(passthroughEvent)
                event.isCancelled = passthroughEvent.isCancelled

                return
            }
        }

        session?.let { session ->
            if (session.onPlayerRightClickEntity(
                player,
                player.inventory.getItem(event.hand),
                event.hand,
                event.rightClicked
            )) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!event.action.isRightClick) {
            return
        }

        val player = event.player

        session?.let { session ->
            event.clickedBlock?.let { clickedBlock ->
                if (session.onPlayerRightClickBlock(
                    player,
                    event.item,
                    clickedBlock.location.toBlockLocation(),
                    event.hand ?: EquipmentSlot.HAND
                )) {
                    event.isCancelled = true
                    return
                }
            }

            event.item?.let { item ->
                if (session.onRightClickItem(player, item, event.hand ?: EquipmentSlot.HAND)) {
                    event.isCancelled = true
                    return
                }
            }
        }
    }

//    @EventHandler
//    private fun onBlockBroken(event: BlockBreakEvent) {
//        session?.let { session ->
//            if (!session.blockCanBeChanged(event.block.location.toBlockLocation())) {
//                event.isCancelled = true
//            }
//        }
//    }
//
//    @EventHandler
//    private fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
//        session?.let { session ->
//            if (!session.blockCanBeChanged(event.block.location.toBlockLocation())) {
//                event.isCancelled = true
//            }
//        }
//    }
//
//    @EventHandler
//    private fun onPistonExtend(event: BlockPistonExtendEvent) {
//        session?.let { session ->
//            if (event.blocks.any { !session.blockCanBeChanged(it.location) }) {
//                event.isCancelled = true
//            }
//        }
//    }
//
//    @EventHandler
//    private fun onEntityExploded(event: EntityExplodeEvent) {
//        session?.let { session ->
//            event.blockList().removeAll {
//                !session.blockCanBeChanged(it.location)
//            }
//        }
//    }
//
//    @EventHandler
//    private fun onBlockExploded(event: BlockExplodeEvent) {
//        session?.let { session ->
//            event.blockList().removeAll {
//                !session.blockCanBeChanged(it.location)
//            }
//        }
//    }
}
