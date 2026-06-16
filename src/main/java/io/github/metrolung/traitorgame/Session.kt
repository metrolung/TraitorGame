package io.github.metrolung.traitorgame

import io.papermc.paper.entity.LookAnchor
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.TitlePart
import org.bukkit.Color
import org.bukkit.EntityEffect
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.entity.Creeper
import org.bukkit.entity.Display
import org.bukkit.entity.EnderDragon
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.entity.TextDisplay
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.BlockIterator
import org.joml.Vector3f
import java.time.Duration
import java.util.*
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class Session(
    val server: Server,
    val plugin: Plugin,
    val sessionManager: SessionManager,
    val settings: SessionSettings
) {
    private val mutableAlivePlayers: MutableMap<UUID, SessionPlayer>
    private var meetingCooldown = settings.meetingCooldownTicks / 2
    private var bellLabel: TextDisplay
    private var activeMeeting: Meeting? = null

    val allPlayers: Map<UUID, SessionPlayer>
    val alivePlayers: Map<UUID, SessionPlayer>
        get() = mutableAlivePlayers

    init {
        val players: MutableMap<UUID, SessionPlayer> = mutableMapOf()

        val roles: List<Role> = List(server.onlinePlayers.size) { i ->
            if (i < settings.traitorCount) {
                if (Random.nextDouble() > 0.9)
                    return@List Role.LuckyTraitor

                return@List Role.Traitor
            }

            if (i == settings.traitorCount) {
                return@List Role.Detective
            }

            if (Random.nextDouble() > 0.9)
                return@List Role.LuckySurvivor

            return@List Role.Survivor
        }

        roles.shuffled().let { roles ->
            for ((roleIdx, player) in server.onlinePlayers.withIndex()) {
                val sessionPlayer = SessionPlayer(
                    player.uniqueId,
                    player.name,
                    roles[roleIdx],
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
        val sessionPlayer = getSessionPlayer(player) ?: return Component.text("You are not alive")
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
            it.type(Key.key("minecraft:entity.player.teleport"))
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

        for (sessionPlayer in this.alivePlayers.values) {
            val player = sessionPlayer.player ?: continue

            player.resetStats()

            if (player.gameMode == GameMode.SPECTATOR)
                player.gameMode = GameMode.SURVIVAL

            for ((idx, equipment) in sessionPlayer.role.equipment) {
                player.inventory.setItem(idx, equipment)
            }

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

            server.scheduler.runTaskLater(plugin, { _ ->
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
                    sessionPlayer.role.name.decorate(TextDecoration.BOLD)
                )
                player.sendTitlePart(
                    TitlePart.SUBTITLE,
                    Component.text("Shh!").decorate(TextDecoration.ITALIC).color(TextColor.color(0x777777))
                )
                player.sendTitlePart(
                    TitlePart.TITLE,
                    sessionPlayer.role.name.decorate(TextDecoration.BOLD)
                )

                player.sendMessage(Component.text("You are a ").append(sessionPlayer.role.name))
                player.sendMessage(Component.text("Clear chat with F3 + D"))
                player.sendMessage(Component.text("You can view your role at any point with /role"))
            }, 100L)
        }
    }

    fun onSessionEnd(reason: EndGameReason?) {
        val blockData = server.createBlockData(Material.AIR)
        settings.bellLocation.getWorld().setBlockData(settings.bellLocation, blockData)
        bellLabel.remove()

        for (player in this.alivePlayers.values) {
            val player = player.player ?: continue
            if (player.gameMode == GameMode.SPECTATOR)
                player.gameMode = GameMode.SURVIVAL
        }

        calculateEndGameText(reason)

        server.sendMessage(Component.empty())
        for (line in endGameText!!) {
            server.sendMessage(line)
        }
        server.sendMessage(Component.empty())
    }

    private fun determineDefaultEndGameReason(): EndGameReason {
        if (mutableAlivePlayers.isEmpty()) {
            return EndGameReason.Draw
        }

        for (player in mutableAlivePlayers.values) {
            if (player.role.group is RoleGroup.Traitors) {
                return EndGameReason.RoleGroupWin(RoleGroup.Traitors)
            }
        }

        return EndGameReason.RoleGroupWin(RoleGroup.Survivors)
    }

    fun startMeeting() {
        for (sessionPlayer in mutableAlivePlayers.values) {
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

    fun onServerTicked() {
        if (activeMeeting?.state == Meeting.State.Finished) {
            activeMeeting = null
        }

        val activeMeeting = activeMeeting
        if (activeMeeting == null) {
            if (meetingCooldown > 0) {
                meetingCooldown--

                val seconds = ((meetingCooldown - 1) / 20) + 1

                bellLabel.text(
                    Component
                        .text("Meeting on cooldown for ${seconds.seconds} more seconds")
                        .color(TextColor.color(0xFFFFFF))
                )
                bellLabel.backgroundColor = Color.fromARGB(100, 20, 20, 20)
                bellLabel.transformation = Transformation()
            } else {
                bellLabel.text(
                    Component
                        .text("Call meeting")
                        .color(TextColor.color(0xAAFFAA))
                )
                bellLabel.backgroundColor = Color.fromARGB(100, 0, 20, 0)
                bellLabel.transformation = Transformation(scale = Vector3f(2f, 2f, 2f))
            }
        } else {
            activeMeeting.onServerTicked()
        }
    }

    fun isPlayerAlive(player: SessionPlayer): Boolean {
        return isPlayerAlive(player.playerUuid)
    }

    fun isPlayerAlive(player: Player): Boolean {
        return isPlayerAlive(player.uniqueId)
    }

    fun isPlayerAlive(player: UUID): Boolean {
        return mutableAlivePlayers.containsKey(player)
    }

    fun getSessionPlayer(player: UUID): SessionPlayer? {
        return mutableAlivePlayers[player]
    }

    fun onPlayerKilled(player: UUID) {
        mutableAlivePlayers.remove(player)
        if (mutableAlivePlayers.isEmpty() || (mutableAlivePlayers.size == 1 && settings.onePlayerEndsGame)) {
            sessionManager.endSession()
        }
    }

    fun onPlayerKilled(player: SessionPlayer) {
        onPlayerKilled(player.playerUuid)
    }

    private fun applyDeadModifiers(player: Player) {
        player.gameMode = GameMode.SPECTATOR
    }

    private fun qualifiesForLastChance(sessionPlayer: SessionPlayer, event: PlayerDeathEvent): Boolean {
        val damage = event.player.lastDamageCause ?: return false

        if (damage.cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            if (damage.damageSource.directEntity is Player || damage.damageSource.directEntity is TNTPrimed) {
                if (Random.nextDouble() > 0.5) {
                    return false
                }
            }

            if (damage.damageSource.directEntity is Creeper) {
                return true
            }
        }

        if (damage.finalDamage > 10.0) {
            return true
        }

        if (damage.cause == EntityDamageEvent.DamageCause.FALL) {
            return true
        }

        if (sessionPlayer.role is Role.LuckySurvivor || sessionPlayer.role is Role.LuckyTraitor) {
            return true
        }

        return false
    }

    fun onEntityDeath(event: EntityDeathEvent) {
        if (event.entity is EnderDragon) {
            sessionManager.endSession(EndGameReason.RoleGroupWin(RoleGroup.Survivors))
        }
    }

    fun onPlayerChat(event: AsyncChatEvent) {
        event.isCancelled = true

        val bubble = settings.chatRange - settings.chatFalloff
        val falloff = settings.chatFalloff

        val message = PlainTextComponentSerializer.plainText().serialize(event.message())
        val player = event.player

        if (player.gameMode == GameMode.SPECTATOR) {
            server.consoleSender.sendMessage(Component.text("☠ <${player.name}> $message").color(TextColor.color(0x888888)))
            for (listener in server.onlinePlayers) {
                if (listener.gameMode == GameMode.SPECTATOR) {
                    player.sendMessage(Component.text("☠ <${player.name}> $message").color(TextColor.color(0x888888)))
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

    fun onPlayerDeath(event: PlayerDeathEvent) {
        event.showDeathMessages = false

        val player = event.player

        val sessionPlayer: SessionPlayer = mutableAlivePlayers[player.uniqueId] ?: run {
            return
        }

        if (!sessionPlayer.lastChanceUsed && qualifiesForLastChance(sessionPlayer, event)) {
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
            player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 100, 2, true, true))
            player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 100, 1, true, true))

            sessionPlayer.lastChanceUsed = true

            return
        }

        onPlayerKilled(player.uniqueId)
    }

    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.damage > 1) {
            getSessionPlayer(event.entity.uniqueId)?.let { sessionPlayer ->
                if (sessionPlayer.returnLocation != null) {
                    sessionPlayer.canReturn = false
                    sessionPlayer.player?.sendMessage(Component
                        .text("You may no longer use /back")
                        .color(TextColor.color(0xFF0000))
                    )
                }
            }
        }

        if (event.entity !is Player) {
            event.damage *= settings.entityDamageFactor
        }
    }

    fun onEntityTakeDamage(event: EntityDamageEvent) {
        if (event.entity is Player && activeMeeting != null) {
            event.isCancelled = true
        }
    }

    fun onPlayerJoin(player: Player) {
        if (!isPlayerAlive(player)) {
            applyDeadModifiers(player)
        }
    }

    fun onPlayerRespawn(player: Player) {
        if (!isPlayerAlive(player)) {
            applyDeadModifiers(player)
        }
    }

    fun onPlayerDrop(event: PlayerDropItemEvent) {
        val sessionPlayer: SessionPlayer = mutableAlivePlayers[event.player.uniqueId] ?: run {
            return
        }

        if (sessionPlayer.role.equipment.containsKey(event.player.inventory.heldItemSlot)) {
            event.isCancelled = true
        }
    }

    fun onPlayerSwapHands(event: PlayerSwapHandItemsEvent) {
        val sessionPlayer: SessionPlayer = mutableAlivePlayers[event.player.uniqueId] ?: run {
            return
        }

        if (
            sessionPlayer.role.equipment.containsKey(event.player.inventory.heldItemSlot) ||
            sessionPlayer.role.equipment.containsKey(40)
        ) {
            event.isCancelled = true
        }
    }

    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: run {
            return
        }

        val sessionPlayer: SessionPlayer = mutableAlivePlayers[player.uniqueId] ?: run {
            return
        }

        if (sessionPlayer.role.equipment.containsKey(event.slot) || sessionPlayer.role.equipment.containsKey(event.rawSlot)) {
            event.isCancelled = true
        }
    }

    fun onRightClickItem(player: Player, slot: Int): Boolean {
        val sessionPlayer: SessionPlayer = mutableAlivePlayers[player.uniqueId] ?: run {
            return false
        }

        if (sessionPlayer.role.equipment.containsKey(slot)) {
            sessionPlayer.role.handleEquipmentInteraction(sessionPlayer, slot)
            return true
        }

        return false
    }

    fun onPlayerRightClickBlock(player: Player, block: Location): Boolean {
        val targetIsBell = block.isSameBlockAs(settings.bellLocation)
        val meetingNotOnCooldown = meetingCooldown <= 0
        val playerAlive = isPlayerAlive(player)

        if (playerAlive && targetIsBell) {
            if (activeMeeting == null && meetingNotOnCooldown) {
                startMeeting()
            } else {
                activeMeeting!!.onBellClicked(player)
            }
            return true
        }
        return false
    }

    fun onBlockRemoved(blockLocation: Location): Boolean {
        return blockLocation == settings.bellLocation
    }

    fun onVote(voter: Player, vote: Vote): Component {
        val meeting = activeMeeting ?: run {
            return Component.text("There is no meeting active")
        }

        if (!isPlayerAlive(voter)) {
            return Component.text("Voter is not alive")
        }

        if (vote is Vote.PlayerVote && !isPlayerAlive(vote.playerUuid)) {
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

    fun calculateEndGameText(reason: EndGameReason?) {
        val text = mutableListOf<TextComponent>()

        val reason = reason ?: determineDefaultEndGameReason()
        text.add(reason.winMessage)

        val crown = Component
            .text("\uD83D\uDC51")
            .color(TextColor.color(0xF0B600))

        val skull = Component
            .text("☠")
            .color(TextColor.color(0x555555))

        val winners = mutableListOf<TextComponent>()
        val aliveButLost = mutableListOf<TextComponent>()
        val dead = mutableListOf<TextComponent>()

        for (player in mutableAlivePlayers.values) {
            if (reason.isWinner(this, player)) {
                winners.add(
                    Component.empty()
                        .append(crown)
                        .append(Component.text(" ${player.name}"))
                        .append(Component
                            .text(" ... ")
                            .color(TextColor.color(0x888888))
                        )
                        .append(player.role.name)
                )
            } else {
                aliveButLost.add(
                    Component.empty()
                        .append(Component
                            .text(" ${player.name}")
                            .color(TextColor.color(0x888888))
                        )
                        .append(Component
                            .text(" ... ")
                            .color(TextColor.color(0x888888))
                        )
                        .append(player.role.name)
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
                                .color(TextColor.color(0x555555))
                                .decorate(TextDecoration.ITALIC)
                        )
                        .append(Component
                            .text(" ... ")
                            .color(TextColor.color(0x888888))
                        )
                        .append(
                            player.role.name
                                .decorate(TextDecoration.ITALIC)
                        )
                )
            }
        }

        if (winners.isNotEmpty()) {
            text.add(Component.empty())

            text.add(Component
                .text("Winners:")
                .color(TextColor.color(0xF0B600))
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
                .color(TextColor.color(0x888888))
            )

            for (component in aliveButLost) {
                text.add(component)
            }
        }

        if (dead.isNotEmpty()) {
            text.add(Component.empty())

            text.add(Component
                .text("Dead:")
                .color(TextColor.color(0x555555))
            )

            for (component in dead) {
                text.add(component)
            }
        }

        endGameText = text
    }
}