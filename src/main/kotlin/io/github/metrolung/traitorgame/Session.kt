package io.github.metrolung.traitorgame

import io.github.metrolung.traitorgame.role.roles.Detective
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RolePicker
import io.github.metrolung.traitorgame.role.RoleType
import io.papermc.paper.datacomponent.item.ResolvableProfile
import io.papermc.paper.entity.LookAnchor
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.TitlePart
import org.bukkit.EntityEffect
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Server
import org.bukkit.entity.Creeper
import org.bukkit.entity.Display
import org.bukkit.entity.EnderDragon
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.Item
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.entity.TNTPrimed
import org.bukkit.entity.TextDisplay
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.BlockIterator
import org.joml.Vector3f
import java.time.Duration
import java.util.*
import kotlin.collections.component1
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class Session(
    val server: Server,
    val plugin: Plugin,
    val manager: SessionManager,
    val settings: SessionSettings,
) {
    private val mutableAlivePlayers: MutableMap<UUID, SessionPlayer>
    private var meetingCooldown = settings.meetingCooldownTicks / 2
    private var bellLabel: TextDisplay
    private var activeMeeting: Meeting? = null

    var roleShown = false
        private set

    // this will not be cleaned up until the game ends which isnt ideal but probably not a big deal
    val swabs = mutableListOf<Pair<String?, SessionPlayer>>()

    private val mutableDeadPlayers = mutableMapOf<UUID, SessionCorpse>()

    val allPlayers: Map<UUID, SessionPlayer>
    val alivePlayers: Map<UUID, SessionPlayer>
        get() = mutableAlivePlayers
    val deadPlayers: Map<UUID, SessionCorpse>
        get() = mutableDeadPlayers

    init {
        val roles = RolePicker.newRoleSelection(server.onlinePlayers.size, settings.roleSettings)

        val players: MutableMap<UUID, SessionPlayer> = mutableMapOf()
        roles.let { roles ->
            for ((roleIdx, player) in server.onlinePlayers.withIndex()) {
                val sessionPlayer = SessionPlayer(
                    player.uniqueId,
                    player.name,
                    roles[roleIdx].generate(),
                    server
                )
                players[player.uniqueId] = sessionPlayer
            }
        }

        this.mutableAlivePlayers = players
        this.allPlayers = players.toMap()
        this.bellLabel = settings.bellLocation.getWorld()
            .createEntity(settings.bellLocation, TextDisplay::class.java)
    }

    fun sendBack(player: UUID): TextComponent {
        val sessionPlayer = getLivingPlayer(player) ?: return Component.text("You are not alive")
        val locationPreMeeting = sessionPlayer.returnLocation ?: return Component.text("Nowhere to return to")

        if (!sessionPlayer.canReturn) {
            return Component.text("You may not return at this time")
        }

        if (!isPlayerAlive(sessionPlayer)) {
            return Component.text("You are not alive")
        }

        sessionPlayer.player?.teleport(locationPreMeeting)
        sessionPlayer.player?.playSound(Sound.sound {
            it.source(Sound.Source.PLAYER)
            it.type(NamespacedKey.minecraft("entity.player.teleport"))
        })

        sessionPlayer.returnLocation = null
        return Component.text("Teleported")
    }

    fun gracePeriod() {
        for (player in alivePlayers.values) {
            player.player?.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 20*30, 255, false, false, true))
        }
    }

    fun playerNearBell(player: Player): Boolean {
        val center = settings.bellLocation.toCenterLocation()

        return player.world.key == center.world.key && player.location.distanceSquared(center) < 6*6
    }

    fun gatherAroundBell() {
        val center = settings.bellLocation.toCenterLocation()
        val spectatorLocation = center.add(0.0, 2.0, 0.0)
        for (player in server.onlinePlayers) {
            if (playerNearBell(player)) {
                continue
            }

            if (player.gameMode == GameMode.SPECTATOR) {
                player.teleport(spectatorLocation)
                continue
            }

            val angle = Random.nextDouble(-Math.PI, Math.PI)
            player.teleport(center)

            val offset = center.add(cos(angle)*4, 0.0, sin(angle)*4)
            val offsetY = offset.world.getHighestBlockYAt(offset) + 1
            offset.y = offsetY.toDouble()
            player.teleport(offset)
            player.lookAt(bellLabel, LookAnchor.EYES, LookAnchor.FEET)
        }
    }

    private fun updatePlayerList(seer: Player) {
        for (player in server.onlinePlayers) {
            if (!isPlayerAlive(seer.uniqueId)) {
                seer.listPlayer(player)
                continue
            }

            if (seer.uniqueId == player.uniqueId) {
                seer.listPlayer(player)
                continue
            }

            if (allPlayers.containsKey(player.uniqueId)) {
                seer.unlistPlayer(player)
            } else {
                seer.listPlayer(player)
            }
        }
    }

    private fun listAll(seer: Player) {
        for (player in server.onlinePlayers) {
            seer.listPlayer(player)
        }
    }

    fun onSessionStart() {
        val blockData = server.createBlockData(Material.BELL)
        settings.bellLocation.getWorld().setBlockData(settings.bellLocation, blockData)

        val labelLocation = settings.bellLocation.toCenterLocation().add(0.0, 1.0, 0.0)
        this.bellLabel.billboard = Display.Billboard.CENTER
        this.bellLabel.isSeeThrough = true
        this.bellLabel.viewRange = Float.MAX_VALUE
        this.bellLabel.lineWidth = 150
        this.bellLabel.spawnAt(labelLocation)

        gatherAroundBell()
        gracePeriod()

        for (world in server.worlds) {
            for (entity in world.entities) {
                if (entity is Item) {
                    entity.remove()
                }
            }
        }

        for (sessionPlayer in this.alivePlayers.values) {
            val player = sessionPlayer.player ?: continue

            updatePlayerList(player)

            player.resetStats()

            if (player.gameMode == GameMode.SPECTATOR)
                player.gameMode = GameMode.SURVIVAL

            player.sendTitlePart(
                TitlePart.TIMES,
                Title.Times.times(
                    Duration.ofSeconds(0),
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(0)
                )
            )

            player.sendTitlePart(
                TitlePart.TITLE,
                Component.text("")
            )

            player.sendTitlePart(
                TitlePart.SUBTITLE,
                Component.text("You are...")
            )

            val sound = Sound.sound {
                it.source(Sound.Source.AMBIENT)
                it.volume(0.8f)
                it.type(NamespacedKey.minecraft("ambient.soul_sand_valley.mood"))
            }
            server.playSound(sound)

            server.scheduler.runTaskLater(plugin, { _ ->
                roleShown = true

                player.sendTitlePart(
                    TitlePart.TIMES,
                    Title.Times.times(
                        Duration.ofSeconds(0),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(1)
                    )
                )
                player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 60, 1, false, false, false))

                player.sendTitlePart(
                    TitlePart.TITLE,
                    sessionPlayer.role.stylized.decorate(TextDecoration.BOLD)
                )
                player.sendTitlePart(
                    TitlePart.SUBTITLE,
                    Component.text("Shh!").decorate(TextDecoration.ITALIC).color(Colors.MID_GRAY.textColor)
                )

                sessionPlayer.player?.stopSound(sound)
                sessionPlayer.player?.playSound(Sound.sound {
                    it.source(Sound.Source.PLAYER)
                    it.type(NamespacedKey.minecraft("entity.evoker.prepare_summon"))
                })

                sessionPlayer.whenOnline { sessionPlayer, player ->
                    player.sendMessage(
                        Component.text {
                            it.append("You are: ".component)
                            it.append(sessionPlayer.role.stylized)
                            it.append(" [".component)
                            it.append(sessionPlayer.role.type.stylized)
                            it.append("]".component)
                        }
                    )
                    player.sendMessage(
                        Component.empty()
                            .append(Component.text("GOAL").decorate(TextDecoration.BOLD).color(Colors.VERY_YELLOW.textColor))
                            .append(": ${sessionPlayer.role.getGoal(settings.roleSettings)}")
                    )

                    player.sendMessage("")

                    player.sendMessage(Component.text("Clear chat with F3 + D").color(Colors.LIGHT_GRAY.textColor))
                    player.sendMessage(Component.text("You can view your role at any point with /role").color(Colors.LIGHT_GRAY.textColor))

                    sessionPlayer.player.inventory.heldItemSlot = 8
                    sessionPlayer.role.onRolePresented(this, sessionPlayer)
                }
            }, 60L)
        }
    }

    fun onSessionEnd(reason: EndGameReason?) {
        val reason = reason ?: determineDefaultEndGameReason()

        val blockData = server.createBlockData(Material.AIR)
        settings.bellLocation.getWorld().setBlockData(settings.bellLocation, blockData)
        bellLabel.remove()

        for (corpse in deadPlayers.values) {
            corpse.mannequin.remove()
            corpse.interaction.remove()
        }

        for (player in this.alivePlayers.values) {
            val player = player.player ?: continue
            if (player.gameMode == GameMode.SPECTATOR)
                player.gameMode = GameMode.SURVIVAL
        }

        for (player in server.onlinePlayers) {
            updatePlayerList(player)
        }

        calculateEndGameText(reason)

        server.sendMessage(Component.empty())
        for (line in endGameText!!) {
            server.sendMessage(line)
        }
        server.sendMessage(Component.empty())

        for (player in server.onlinePlayers) {
            player.sendTitlePart(
                TitlePart.TIMES,
                Title.Times.times(
                    Duration.ofSeconds(0),
                    Duration.ofSeconds(2),
                    Duration.ofSeconds(1)
                )
            )
            player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 60, 1, false, false, false))

            player.sendTitlePart(
                TitlePart.TITLE,
                endGameTitle!!
            )
            player.sendTitlePart(
                TitlePart.SUBTITLE,
                Component.text("GG").decorate(TextDecoration.BOLD).color(Colors.VERY_YELLOW.textColor)
            )

            player.playSound(reason.winSound)
        }
    }

    private fun determineDefaultEndGameReason(): EndGameReason {
        if (alivePlayers.isEmpty()) {
            return EndGameReason.Draw
        }

        val traitorWinGameReason = EndGameReason.RoleGroupWin(RoleType.Traitor)
        if (alivePlayers.values.any { it.role.type == RoleType.Traitor && it.role.isWinner(this, it, traitorWinGameReason) }) {
            return traitorWinGameReason
        }

        val survivorWinGameReason = EndGameReason.RoleGroupWin(RoleType.Survivor)
        if (alivePlayers.values.any { it.role.type == RoleType.Survivor && it.role.isWinner(this, it, survivorWinGameReason) }) {
            return survivorWinGameReason
        }

        val neutralWinGameReason = EndGameReason.RoleGroupWin(RoleType.Neutral)
        if (alivePlayers.values.any { it.role.type == RoleType.Neutral && it.role.isWinner(this, it, neutralWinGameReason) }) {
            return neutralWinGameReason
        }

        return EndGameReason.Draw
    }

    fun startMeeting() {
        for (sessionPlayer in alivePlayers.values) {
            sessionPlayer.player?.let { player ->
                if (playerNearBell(player)) {
                    sessionPlayer.returnLocation = null
                } else {
                    sessionPlayer.returnLocation = player.location
                }
                sessionPlayer.canReturn = false
            }
        }
        this.meetingCooldown = settings.meetingCooldownTicks
        this.activeMeeting = Meeting(this, bellLabel)
        this.activeMeeting!!.startMeeting()
    }

    private fun checkPlayerNearCorpse() {
        for (deadPlayer in deadPlayers.values) {

            deadPlayer.interaction.teleport(deadPlayer.mannequin.location)

            val nearby = deadPlayer.mannequin.location.getNearbyPlayers(1.5)
            for (nearbyPlayer in nearby) {
                if (nearbyPlayer.location.distanceSquared(deadPlayer.mannequin.location) < 0.1*0.1) {
                    continue
                }

                val player = alivePlayers[nearbyPlayer.uniqueId]?.onlineSessionPlayer ?: continue

                if (player.player.isSneaking) {
                    val movement = player.player.location.subtract(deadPlayer.mannequin.location).toVector().normalize().multiply(0.03)

                    deadPlayer.mannequin.velocity = movement
                }

                if (player.role !is Detective && !deadPlayer.contaminators.containsKey(player.uniqueId)) {
                    deadPlayer.contaminators[player.uniqueId] = player.sessionPlayer
                    player.player.sendMessage(Component.text("You have contaminated the corpse.").color(Colors.VERY_RED.textColor))
                }
            }
        }
    }

    fun onServerTicked(tickNumber: Int) {
        if (activeMeeting?.state == Meeting.State.Finished) {
            activeMeeting = null
        }

        checkPlayerNearCorpse()

        for (alivePlayer in alivePlayers.values) {
            val onlinePlayer = alivePlayer.onlineSessionPlayer ?: continue
            alivePlayer.role.onTickOnline(this, onlinePlayer, tickNumber)
        }

        val activeMeeting = activeMeeting
        if (activeMeeting == null) {
            if (meetingCooldown > 0) {
                meetingCooldown--

                val seconds = ((meetingCooldown - 1) / 20) + 1

                bellLabel.text(
                    Component
                        .text("Meeting on cooldown for ${seconds.seconds} more seconds")
                        .color(Colors.WHITE.textColor)
                )
                bellLabel.backgroundColor = Colors.ALMOST_BLACK.color(100)
                bellLabel.transformation = Transformation()
            } else {
                bellLabel.text(
                    Component
                        .text("Call meeting")
                        .color(Colors.MEETING_GREEN.textColor)
                )
                bellLabel.backgroundColor = Colors.MIDNIGHT_GREEN.color(100)
                bellLabel.transformation = Transformation(scale = Vector3f(2f, 2f, 2f))
            }
        } else {
            activeMeeting.onServerTicked()
        }
    }

    fun isPlayerAlive(player: SessionPlayer): Boolean {
        return isPlayerAlive(player.uniqueId)
    }

    fun isPlayerAlive(player: Player): Boolean {
        return isPlayerAlive(player.uniqueId)
    }

    fun isPlayerAlive(player: UUID): Boolean {
        return alivePlayers.containsKey(player)
    }

    // this is not necessarily the same as !isPlayerAlive(...)
    //   since it specifically targets players who have died, instead
    //   of just players who might not have joined in time
    fun isPlayerDead(player: SessionPlayer): Boolean {
        return isPlayerDead(player.uniqueId)
    }

    fun isPlayerDead(player: Player): Boolean {
        return isPlayerDead(player.uniqueId)
    }

    fun isPlayerDead(player: UUID): Boolean {
        return deadPlayers.containsKey(player)
    }

    fun getLivingPlayer(player: UUID): SessionPlayer? {
        return alivePlayers[player]
    }

    private fun spawnCorpse(location: Location, player: Player, sessionPlayer: SessionPlayer, causeOfDeath: String, contaminators: MutableMap<UUID, SessionPlayer>) {
        val mannequin = location.world.spawnEntity(location, EntityType.MANNEQUIN) as Mannequin
        mannequin.persistentDataContainer.set(
            SessionCorpse.CORPSE_KEY,
            PersistentDataType.STRING,
            player.uniqueId.toString()
        )
        mannequin.isInvulnerable = true
        mannequin.profile = ResolvableProfile.resolvableProfile(player.playerProfile)
        mannequin.setPose(Pose.SLEEPING, true)
        mannequin.spawnAt(location)

        val interaction = location.world.spawnEntity(location, EntityType.INTERACTION) as Interaction
        interaction.persistentDataContainer.set(
            INTERACTION_PASSTHROUGH_KEY,
            PersistentDataType.STRING,
            mannequin.uniqueId.toString()
        )
        interaction.interactionWidth = 1.5f
        interaction.interactionHeight = 0.5f
        interaction.spawnAt(location)

        for (contaminator in contaminators.values) {
            if (contaminator.role.type == RoleType.Traitor) {
                contaminator.player?.sendMessage(Component.text("Hide the body by crouching.").color(Colors.VERY_RED.textColor))
            } else {
                contaminator.player?.sendMessage(Component.text("You have contaminated the corpse.").color(Colors.VERY_RED.textColor))
            }
        }

        mutableDeadPlayers[player.uniqueId] = SessionCorpse(
            sessionPlayer,
            mannequin,
            interaction,
            causeOfDeath,
            Clock.System.now(),
            contaminators
        )
    }

    fun onPlayerKilled(playerUuid: UUID, drops: MutableList<ItemStack>, causeOfDeath: String, contaminators: MutableMap<UUID, SessionPlayer>) {
        val deadPlayer = mutableAlivePlayers.remove(playerUuid)
        if (alivePlayers.isEmpty()) {
            manager.endSession()
            return
        }

        if (deadPlayer == null) {
            return
        }

        drops.removeAll { it.persistentDataContainer.has(ItemStacks.ROLE_EQUIPMENT_KEY) }

        deadPlayer.player?.let { player ->
            spawnCorpse(player.location, player, deadPlayer, causeOfDeath, contaminators)
        }

        for ((_, player) in alivePlayers) {
            player.role.onPlayerDied(this, player, deadPlayer)
        }
    }

    fun onPlayerKilled(player: SessionPlayer, drops: MutableList<ItemStack>, causeOfDeath: String, contaminators: MutableMap<UUID, SessionPlayer>) {
        onPlayerKilled(player.uniqueId, drops, causeOfDeath, contaminators)
    }

    private fun applyDeadModifiers(player: Player) {
        player.gameMode = GameMode.SPECTATOR
    }

    private fun qualifiesForLastChance(event: PlayerDeathEvent): Boolean {
        val damage = event.player.lastDamageCause ?: return false

        if (damage.damageSource.directEntity is Player) {
            return false
        }

        if (damage.cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            if (damage.damageSource.directEntity is TNTPrimed) {
                return false
            }

            if (damage.damageSource.directEntity is Creeper) {
                return true
            }
        }

        if (damage.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return true
        }

        if (damage.finalDamage > 7.0) {
            return true
        }

        if (damage.cause == EntityDamageEvent.DamageCause.FALL) {
            return true
        }

        return false
    }

    fun onEntityDeath(event: EntityDeathEvent) {
        if (event.entity is EnderDragon) {
            manager.endSession(EndGameReason.DragonDefeated)
        }
    }

    fun onPlayerChat(event: AsyncChatEvent) {
        event.isCancelled = true

        val bubble = settings.chatRange - settings.chatFalloff
        val falloff = settings.chatFalloff

        val message = PlainTextComponentSerializer.plainText().serialize(event.message())
        val player = event.player

        if (player.gameMode == GameMode.SPECTATOR) {
            server.consoleSender.sendMessage(Component.text("☠ <${player.name}> $message").color(Colors.LIGHT_GRAY.textColor))
            for (listener in server.onlinePlayers) {
                if (listener.gameMode == GameMode.SPECTATOR) {
                    player.sendMessage(Component.text("☠ <${player.name}> $message").color(Colors.LIGHT_GRAY.textColor))
                }
            }
        } else {
            server.consoleSender.sendMessage(Component.text("<${player.name}> $message"))
            for (listener in server.onlinePlayers) {
                if (listener.world.key != player.world.key) {
                    continue
                }

                var volume = 1.0

                if (player.uniqueId != listener.uniqueId) {
                    val distance = player.eyeLocation.distance(listener.eyeLocation)

                    val start = player.eyeLocation.toVector()
                    val end = listener.eyeLocation.toVector()
                    val direction = end.subtract(start)

                    val ray = BlockIterator(player.world, start, direction, 0.0, ceil(distance).toInt())

                    var dampening = 0.0
                    for (block in ray) {
                        if (block.isSolid) {
                            dampening += settings.blockDampening
                        }
                    }

                    volume = (1-(distance-bubble+dampening)/(falloff+dampening/2)).coerceIn(0.0..1.0)
                }

                if (listener.gameMode == GameMode.SPECTATOR) {
                    volume = max(0.2, volume)
                }

                if (volume == 0.0) {
                    continue
                }

                val message = if (volume > 0.5f || listener.gameMode == GameMode.SPECTATOR) {
                    message
                } else {
                    message.map {
                        if (volume < Random.nextDouble(0.0, 0.6)) {
                            "$%#*&@][(){}".random()
                        } else {
                            it
                        }
                    }.joinToString("")
                }

                listener.sendMessage(Component.text("<${player.name}> $message").color(TextColor.color(volume.toFloat(), volume.toFloat(), volume.toFloat())))
            }
        }
    }

    fun onPlayerAdvancement(event: PlayerAdvancementDoneEvent) {
        event.message(null)
    }

    fun onInventoryClickEvent(event: InventoryClickEvent) {
        val sessionPlayer = alivePlayers[event.whoClicked.uniqueId] ?: return
        val item = event.currentItem
        val cursor = event.cursor

        (event.clickedInventory?.holder as? GuiHolder)?.let { guiHolder ->
            if (sessionPlayer.role.handleGuiClick(this, sessionPlayer.onlineSessionPlayer!!, item, cursor, guiHolder, event.slot, event.action)) {
                event.isCancelled = true
            }

            return
        }

        item?.persistentDataContainer?.get(ItemStacks.ROLE_EQUIPMENT_KEY, PersistentDataType.STRING)?.let { equipmentType ->
            if (sessionPlayer.role.handleEquipmentInventoryClick(this, sessionPlayer.onlineSessionPlayer!!, item, equipmentType, event.action)) {
                event.isCancelled = true
            }
        }

        if (event.clickedInventory == event.whoClicked.inventory) {
            val guiHolder = event.whoClicked.openInventory.topInventory.holder as? GuiHolder
            sessionPlayer.role.handleInventoryClick(this, sessionPlayer.onlineSessionPlayer!!, item, cursor, guiHolder, event.slot, event.action)
            return
        }

        fun canBeMoved(itemStack: ItemStack)
            = !itemStack.persistentDataContainer.has(ItemStacks.ROLE_EQUIPMENT_KEY)

        if (!canBeMoved(event.cursor)) {
            event.isCancelled = true
            return
        }

        if (event.whoClicked.openInventory.getInventory(event.rawSlot)?.getItem(event.rawSlot)?.let { !canBeMoved(it) } ?: false) {
            event.isCancelled = true
            return
        }

        if (event.whoClicked.openInventory.getInventory(event.slot)?.getItem(event.slot)?.let { !canBeMoved(it) } ?: false) {
            event.isCancelled = true
            return
        }

        if (event.action == InventoryAction.HOTBAR_SWAP) {
            val slot = if (event.hotbarButton < 0) {
                40
            } else {
                event.hotbarButton
            }

            if (event.whoClicked.inventory.getItem(slot)?.let { !canBeMoved(it) } ?: false) {
                event.isCancelled = true
                return
            }
        }
    }

    fun onPlayerDeath(event: PlayerDeathEvent) {
        event.showDeathMessages = false

        val player = event.player

        val sessionPlayer = alivePlayers[player.uniqueId]?.onlineSessionPlayer ?: run {
            return
        }

        var contaminators = mutableMapOf<UUID, SessionPlayer>()

        val killer = event.player.lastDamageCause?.damageSource?.directEntity
        if (killer != null) {
            val killerSessionPlayer = alivePlayers[killer.uniqueId]?.onlineSessionPlayer
            if (killerSessionPlayer != null) {
                contaminators = mutableMapOf(killerSessionPlayer.uniqueId to killerSessionPlayer.sessionPlayer)
                if (killerSessionPlayer.role.onKilling(this, killerSessionPlayer, sessionPlayer)) {
                    event.isCancelled = true
                    return
                }
                if (sessionPlayer.role.onKilled(this, sessionPlayer, killerSessionPlayer)) {
                    event.isCancelled = true
                    return
                }
            }
        }

        if (!sessionPlayer.lastChanceUsed && qualifiesForLastChance(event)) {
            player.sendTitlePart(
                TitlePart.TIMES,
                Title.Times.times(
                    Duration.ofMillis(100),
                    Duration.ofMillis(1500),
                    Duration.ofSeconds(1)
                )
            )

            player.sendTitlePart(
                TitlePart.TITLE,
                MiniMessage.deserialize("<gradient:#88f0fc:#ccffff><b>Last chance!</b></gradient>")
            )

            player.playEffect(EntityEffect.PROTECTED_FROM_DEATH)

            event.isCancelled = true

            player.health = 5.0
            player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 200, 2, true, true))
            player.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 200, 2, true, true))
            player.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, 200, 0, true, true))
            player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 100, 0, true, true))

            sessionPlayer.lastChanceUsed = true

            return
        }

        val causeOfDeath = when (event.player.lastDamageCause?.cause) {
            EntityDamageEvent.DamageCause.KILL -> "Divine Retribution"
            EntityDamageEvent.DamageCause.WORLD_BORDER -> "Divine Retribution"
            EntityDamageEvent.DamageCause.CONTACT -> "Exsanguination, Environmental"
            EntityDamageEvent.DamageCause.ENTITY_ATTACK -> "Exsanguination, Direct"
            EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK -> "Exsanguination, Direct"
            EntityDamageEvent.DamageCause.PROJECTILE -> "Exsanguination, Indirect"
            EntityDamageEvent.DamageCause.SUFFOCATION -> "Asphyxia"
            EntityDamageEvent.DamageCause.FALL -> "Blunt Force"
            EntityDamageEvent.DamageCause.FIRE -> "Severe Burns"
            EntityDamageEvent.DamageCause.FIRE_TICK -> "Severe Burns"
            EntityDamageEvent.DamageCause.MELTING -> "Hyperthermia"
            EntityDamageEvent.DamageCause.LAVA -> "Severe Burns"
            EntityDamageEvent.DamageCause.DROWNING -> "Asphyxia"
            EntityDamageEvent.DamageCause.BLOCK_EXPLOSION -> "Blast Injury"
            EntityDamageEvent.DamageCause.ENTITY_EXPLOSION -> "Blast Injury"
            EntityDamageEvent.DamageCause.VOID -> "Asphyxia"
            EntityDamageEvent.DamageCause.LIGHTNING -> "Electric Shock"
            EntityDamageEvent.DamageCause.SUICIDE -> "Suicide"
            EntityDamageEvent.DamageCause.STARVATION -> "Starvation"
            EntityDamageEvent.DamageCause.POISON -> "Intoxication"
            EntityDamageEvent.DamageCause.MAGIC -> "Intoxication"
            EntityDamageEvent.DamageCause.WITHER -> "Intoxication"
            EntityDamageEvent.DamageCause.FALLING_BLOCK -> "Asphyxia"
            EntityDamageEvent.DamageCause.THORNS -> "Exsanguination, Direct"
            EntityDamageEvent.DamageCause.DRAGON_BREATH -> "Intoxication"
            EntityDamageEvent.DamageCause.FLY_INTO_WALL -> "Blunt Force"
            EntityDamageEvent.DamageCause.HOT_FLOOR -> "Severe Burns"
            EntityDamageEvent.DamageCause.CAMPFIRE -> "Severe Burns"
            EntityDamageEvent.DamageCause.CRAMMING -> "Asphyxia"
            EntityDamageEvent.DamageCause.DRYOUT -> "Hyperthermia"
            EntityDamageEvent.DamageCause.FREEZE -> "Hypothermia"
            EntityDamageEvent.DamageCause.SONIC_BOOM -> "Blast Injury"
            else -> "Unknown"
        }

        onPlayerKilled(player.uniqueId, event.drops, causeOfDeath, contaminators)
    }

    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.damage > 0.001) {
            alivePlayers[event.entity.uniqueId]?.onlineSessionPlayer?.let { sessionPlayer ->
                val attackerSessionPlayer = alivePlayers[event.damager.uniqueId]?.onlineSessionPlayer
                if (attackerSessionPlayer != null) {
                    if (attackerSessionPlayer.role.onAttacking(this, attackerSessionPlayer, sessionPlayer)) {
                        event.isCancelled = true
                        return
                    }
                    if (sessionPlayer.role.onAttacked(this, sessionPlayer, attackerSessionPlayer)) {
                        event.isCancelled = true
                        return
                    }
                }

                if (sessionPlayer.returnLocation != null && sessionPlayer.canReturn) {
                    sessionPlayer.canReturn = false
                    sessionPlayer.player?.sendMessage(Component
                        .text("You may no longer use /back")
                        .color(Colors.VERY_RED.textColor)
                    )
                }
            }
        }

        if (event.damager !is Player && event.entity is Player) {
            event.damage *= settings.entityDamageFactor
        }
    }

    fun onEntityTakeDamage(event: EntityDamageEvent) {
        if (event.entity is Player && activeMeeting != null) {
            event.isCancelled = true
        }
    }

    fun onPlayerJoin(player: Player) {
        val sessionPlayer = getLivingPlayer(player.uniqueId)

        for (player in server.onlinePlayers) {
            updatePlayerList(player)
        }

        if (sessionPlayer == null) {
            applyDeadModifiers(player)
        } else {
            sessionPlayer.onlineSessionPlayer!!.doOnlineActions()
        }
    }

    fun onPlayerRespawn(player: Player) {
        if (!isPlayerAlive(player)) {
            applyDeadModifiers(player)
        }
    }

    fun onEntityPickup(event: EntityPickupItemEvent) {
        val cannotPickup = event.item.persistentDataContainer.get(TraitorGamePlugin.key("cannot_pickup"), PersistentDataType.STRING) ?: return

        if (event.entity.uniqueId == UUID.fromString(cannotPickup)) {
            event.isCancelled = true
        }
    }

    fun onPlayerDrop(event: PlayerDropItemEvent) {
        alivePlayers[event.player.uniqueId]?.let { sessionPlayer ->
            val item = event.itemDrop.itemStack

            val equipmentType = item.persistentDataContainer.get(ItemStacks.ROLE_EQUIPMENT_KEY, PersistentDataType.STRING) ?: return@let
            if (sessionPlayer.role.handleEquipmentDropped(this, sessionPlayer.onlineSessionPlayer!!, item, equipmentType, event.itemDrop)) {
                event.isCancelled = true
            }
            return
        }

        if (event.itemDrop.itemStack.persistentDataContainer.has(ItemStacks.ROLE_EQUIPMENT_KEY)) {
            event.isCancelled = true
        }
    }

    fun onRightClickItem(player: Player, item: ItemStack, hand: EquipmentSlot): Boolean {
        val sessionPlayer: SessionPlayer = mutableAlivePlayers[player.uniqueId] ?: run {
            return false
        }

        item.persistentDataContainer.get(ItemStacks.ROLE_EQUIPMENT_KEY, PersistentDataType.STRING)?.let { equipmentType ->
            if (sessionPlayer.role.handleEquipmentUse(this, sessionPlayer.onlineSessionPlayer!!, item, equipmentType, null)) {
                player.swingHand(hand)
            }
            return true
        }

        item.persistentDataContainer.get(ItemStacks.ROLE_MATERIAL_KEY, PersistentDataType.STRING)?.let { materialType ->
            if (sessionPlayer.role.handleMaterialUse(this, sessionPlayer.onlineSessionPlayer!!, item, materialType, null)) {
                player.swingHand(hand)
            }
            return true
        }

        return false
    }

    fun onPlayerRightClickBlock(player: Player, item: ItemStack?, block: Location, hand: EquipmentSlot): Boolean {
        val targetIsBell = block.isSameBlockAs(settings.bellLocation)
        val meetingNotOnCooldown = meetingCooldown <= 0
        val playerAlive = isPlayerAlive(player)

        if (playerAlive && targetIsBell) {
            if (activeMeeting == null && meetingNotOnCooldown) {
                startMeeting()
                player.swingHand(hand)
            } else if (activeMeeting != null) {
                activeMeeting!!.onBellClicked(player)
                player.swingHand(hand)
            }
            return true
        }

        val sessionPlayer: SessionPlayer = mutableAlivePlayers[player.uniqueId] ?: run {
            return false
        }

        item?.persistentDataContainer?.get(ItemStacks.ROLE_EQUIPMENT_KEY, PersistentDataType.STRING)?.let { equipmentType ->
            if (sessionPlayer.role.handleEquipmentUse(this, sessionPlayer.onlineSessionPlayer!!, item, equipmentType, block)) {
                player.swingHand(hand)
            }
            return true
        }

        item?.persistentDataContainer?.get(ItemStacks.ROLE_MATERIAL_KEY, PersistentDataType.STRING)?.let { materialType ->
            if (sessionPlayer.role.handleEquipmentUse(this, sessionPlayer.onlineSessionPlayer!!, item, materialType, block)) {
                player.swingHand(hand)
            }
            return true
        }

        return false
    }

    fun onPlayerRightClickEntity(player: Player, item: ItemStack?, hand: EquipmentSlot, entity: Entity): Boolean {
        val sessionPlayer: SessionPlayer = alivePlayers[player.uniqueId] ?: run {
            return false
        }

        item?.persistentDataContainer?.get(ItemStacks.ROLE_EQUIPMENT_KEY, PersistentDataType.STRING)?.let { equipmentType ->
            if (sessionPlayer.role.handleEquipmentUseOnEntity(this, sessionPlayer.onlineSessionPlayer!!, item, equipmentType, entity)) {
                player.swingHand(hand)
            }
            return true
        }

        item?.persistentDataContainer?.get(ItemStacks.ROLE_MATERIAL_KEY, PersistentDataType.STRING)?.let { materialType ->
            if (sessionPlayer.role.handleMaterialUseOnEntity(this, sessionPlayer.onlineSessionPlayer!!, item, materialType, entity)) {
                player.swingHand(hand)
            }
            return true
        }

        return false
    }

    fun blockCanBeChanged(blockLocation: Location): Boolean {
        if (blockLocation.isSameBlockAs(settings.bellLocation))
            return false

        return true
    }

    fun onVote(voter: Player, vote: Vote): Component {
        val meeting = activeMeeting ?: run {
            return Component.text("There is no meeting active")
        }

        if (!isPlayerAlive(voter)) {
            return Component.text("Voter is not alive")
        }

        if (vote is Vote.PlayerVote && !isPlayerAlive(vote.player)) {
            return Component.text("Voted is not alive")
        }

        return meeting.onVote(voter, vote)
    }

    fun getRole(player: Player): Role? {
        val sessionPlayer = this.mutableAlivePlayers[player.uniqueId] ?: return null
        return sessionPlayer.role
    }

    var endGameText: List<TextComponent>? = null
        private set

    var endGameTitle: TextComponent? = null
        private set

    fun calculateEndGameText(reason: EndGameReason) {
        val text = mutableListOf<TextComponent>()


        text.add(reason.winMessage)
        endGameTitle = reason.winMessage

        val crown = Component
            .text("\uD83D\uDC51")
            .color(Colors.ORANGE.textColor)

        val skull = Component
            .text("☠")
            .color(Colors.DARK_GRAY.textColor)

        val winners = mutableListOf<TextComponent>()
        val aliveButLost = mutableListOf<TextComponent>()
        val dead = mutableListOf<TextComponent>()

        for (player in alivePlayers.values) {
            if (player.role.isWinner(this, player, reason)) {
                winners.add(
                    Component.empty()
                        .append(crown)
                        .append(Component.text(" ${player.name}"))
                        .append(Component
                            .text(" ... ")
                            .color(Colors.LIGHT_GRAY.textColor)
                        )
                        .append(player.role.stylized)
                )
            } else {
                aliveButLost.add(
                    Component.empty()
                        .append(Component
                            .text(" ${player.name}")
                            .color(Colors.LIGHT_GRAY.textColor)
                        )
                        .append(Component
                            .text(" ... ")
                            .color(Colors.LIGHT_GRAY.textColor)
                        )
                        .append(player.role.stylized)
                )
            }
        }

        for (player in allPlayers.values) {
            if (!isPlayerAlive(player)) {
                dead.add(
                    Component.empty()
                        .append(skull)
                        .append(
                            Component
                                .text(" ${player.name}")
                                .color(Colors.DARK_GRAY.textColor)
                                .decorate(TextDecoration.ITALIC)
                        )
                        .append(Component
                            .text(" ... ")
                            .color(Colors.LIGHT_GRAY.textColor)
                        )
                        .append(
                            player.role.stylized
                                .decorate(TextDecoration.ITALIC)
                        )
                )
            }
        }

        if (winners.isNotEmpty()) {
            text.add(Component.empty())

            text.add(Component
                .text("Winners:")
                .color(Colors.ORANGE.textColor)
                .decorate(TextDecoration.BOLD)
            )

            for (component in winners) {
                text.add(component)
            }
        }

        if (aliveButLost.isNotEmpty()) {
            text.add(Component.empty())

            text.add(Component
                .text("Close but no cigar:")
                .color(Colors.LIGHT_GRAY.textColor)
            )

            for (component in aliveButLost) {
                text.add(component)
            }
        }

        if (dead.isNotEmpty()) {
            text.add(Component.empty())

            text.add(Component
                .text("Dead:")
                .color(Colors.DARK_GRAY.textColor)
            )

            for (component in dead) {
                text.add(component)
            }
        }

        endGameText = text
    }

    fun onItemDespawn(event: ItemDespawnEvent) {
        if (event.entity.itemStack.persistentDataContainer.get(ItemStacks.ROLE_MATERIAL_KEY, PersistentDataType.STRING) == "manifesto") {
            event.isCancelled = true
        }
    }
}