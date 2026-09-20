package io.github.metrolung.traitorgame

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import com.destroystokyo.paper.event.server.ServerTickEndEvent
import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketListenerCommon
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import io.github.metrolung.traitorgame.achievements.Achievement
import io.github.metrolung.traitorgame.achievements.TheFinalStage
import io.github.metrolung.traitorgame.api.Floodgate
import io.github.metrolung.traitorgame.role.roles.Detective
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleManager
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.papermc.paper.datacomponent.item.ResolvableProfile
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.HoverEvent
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
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.EnderDragon
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.Item
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.entity.ItemMergeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scoreboard.Team
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
import kotlin.random.nextInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class Session(
    val server: Server,
    val plugin: Plugin,
    val manager: SessionManager,
    val roleManager: RoleManager,
    val settings: SessionSettings,
) : Listener, PacketListener {
    private var meetingCooldown = settings.meetingCooldownTicks / 2
    private var bellLabel: TextDisplay
    private var bellBlock: BlockDisplay
    private var activeMeeting: Meeting? = null
    private val achievements: MutableSet<Achievement> = mutableSetOf()

    private var active: Boolean = false

    var listenerCommon: PacketListenerCommon? = null

    var roleShown = false
        private set

    val samples = mutableListOf<Detective.DNASample>()

    val allPlayers: Map<UUID, SessionPlayer>
        field: MutableMap<UUID, SessionPlayer>
    val alivePlayers: Map<UUID, SessionPlayer>
        field: MutableMap<UUID, SessionPlayer>
    val deadPlayers: Map<UUID, SessionCorpse>
        field: MutableMap<UUID, SessionCorpse>

    init {
        val roles = roleManager.generatePool(
            server.onlinePlayers.size,
            settings.roleSettings.traitorCount,
            settings.roleSettings.passiveNeutralCount,
            settings.roleSettings.evilNeutralCount
        )

        val players: MutableMap<UUID, SessionPlayer> = mutableMapOf()
        for ((roleIdx, player) in server.onlinePlayers.withIndex()) {
            val sessionPlayer = SessionPlayer(
                player.uniqueId,
                player.name,
                roles[roleIdx].settings.builder.build(),
                server,
                StatusBar(" | ".colored(Colors.LIGHT_GRAY))
            )
            players[player.uniqueId] = sessionPlayer
        }

        alivePlayers = players
        allPlayers = players.toMutableMap()
        deadPlayers = mutableMapOf()
        this.bellLabel = settings.bellLocation.getWorld()
            .createEntity(settings.bellLocation, TextDisplay::class.java)
        this.bellBlock = settings.bellLocation.getWorld()
            .createEntity(settings.bellLocation, BlockDisplay::class.java)
    }

    fun deinit() {
        forceEndSession(null)

        activeMeeting?.deinit()

        val blockData = server.createBlockData(Material.AIR)
        this.settings.bellLocation.getWorld().setBlockData(settings.bellLocation, blockData)
        this.bellLabel.remove()
        this.bellBlock.remove()


        for ((_, mannequin, interaction) in deadPlayers.values) {
            mannequin.remove()
            interaction.remove()
        }

        for (player in server.onlinePlayers) {
            listAll(player)
        }
    }

    fun sendBack(player: UUID): String {
        val sessionPlayer = alivePlayers[player] ?: return "You are not alive"
        val locationPreMeeting = sessionPlayer.returnLocation ?: return "Nowhere to return to"

        if (!sessionPlayer.canReturn) {
            return "You may not return at this time"
        }

        if (!isPlayerAlive(sessionPlayer)) {
            return "You are not alive"
        }

        sessionPlayer.player?.teleport(locationPreMeeting)
        sessionPlayer.player?.playSound(Sound.sound {
            it.source(Sound.Source.PLAYER)
            it.type(NamespacedKey.minecraft("entity.player.teleport"))
        })

        sessionPlayer.returnLocation = null
        return "Teleported"
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

    fun circleBell(player: Player, radians: Double, distance: Double) {
        val pos = settings.bellLocation.toCenterLocation()
        pos.yaw = (radians*180/Math.PI).toFloat()
        pos.pitch = 0f
        pos.add(
            sin(radians)*distance,
            0.0,
            -cos(radians)*distance
        )

        val posY = pos.world.getHighestBlockYAt(pos) + 1
        pos.y = posY.toDouble()

        player.teleport(pos)
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

            circleBell(player, Random.nextDouble(-Math.PI, Math.PI), 4.0)
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

    fun initPlayer(sessionPlayer: OnlineSessionPlayer, tp: Boolean = true) {
        val player = sessionPlayer.player

        updatePlayerList(player)
        player.resetStats()

        if (tp)
            circleBell(player, Random.nextDouble(-Math.PI, Math.PI), 4.0)

        if (player.gameMode == GameMode.SPECTATOR) {
            player.gameMode = GameMode.SURVIVAL
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
                sessionPlayer.role.settings.stylized.decorate(TextDecoration.BOLD)
            )
            player.sendTitlePart(
                TitlePart.SUBTITLE,
                Component.text("Shh!").decorate(TextDecoration.ITALIC).color(Colors.MID_GRAY.textColor)
            )

            player.stopSound(sound)
            player.playSound(Sound.sound {
                it.source(Sound.Source.PLAYER)
                it.type(NamespacedKey.minecraft("entity.evoker.prepare_summon"))
            })

            player.sendMessage(
                Component.text {
                    it.append("You are: ".component)
                    it.append(sessionPlayer.role.settings.stylized)
                    it.append(" [".component)
                    it.append(sessionPlayer.role.settings.alignment.stylized)
                    it.append("]".component)
                }
            )

            player.sendMessage(
                Component.empty()
                    .append(Component.text("GOAL").decorate(TextDecoration.BOLD).color(Colors.VERY_YELLOW.textColor))
                    .append(": ${sessionPlayer.role.settings.goalProvider(settings.roleSettings)}")
            )

            player.sendMessage("")

            player.sendMessage(Component.text("Clear chat with F3 + D").color(Colors.LIGHT_GRAY.textColor))
            player.sendMessage(Component.text("You can view your role at any point with /role").color(Colors.LIGHT_GRAY.textColor))

            player.inventory.heldItemSlot = 8
            sessionPlayer.role.onRolePresented(this, sessionPlayer)
        }, 60L)
    }

    fun start() {
        onSessionStart()
    }

    private fun onSessionStart() {
        if (active) {
            return
        }

        this.active = true

        val labelLocation = settings.bellLocation.toCenterLocation().add(0.0, 1.0, 0.0)
        this.bellLabel.billboard = Display.Billboard.CENTER
        this.bellLabel.isSeeThrough = true
        this.bellLabel.viewRange = Float.MAX_VALUE
        this.bellLabel.lineWidth = 150
        this.bellLabel.spawnAt(labelLocation)

        val bell = server.createBlockData(Material.BELL)
        val bellBlockLocation = settings.bellLocation.toBlockLocation()
        bellBlockLocation.pitch = 0f
        bellBlockLocation.yaw = 0f
        this.bellBlock.block = bell
        this.bellLabel.viewRange = Float.MAX_VALUE

        val bellBlockData = server.createBlockData(Material.BELL)
        this.bellBlock.block = bellBlockData
        this.bellBlock.spawnAt(bellBlockLocation)

        for (world in server.worlds) {
            for (entity in world.entities) {
                if (entity is Item) {
                    entity.remove()
                }
            }
        }

        for (sessionPlayer in this.alivePlayers.values) {
            sessionPlayer.whenOnline { sessionPlayer ->
                sessionPlayer.player.setNametagVisibility(false)
                initPlayer(sessionPlayer)
            }
        }

        gracePeriod()
    }

    private fun onSessionEnd(reason: EndGameReason) {
        if (!this.active) {
            return
        }

        this.active = false

        val reason = reason

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
            player.setNametagVisibility(true)

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

    fun forceEndSession(reason: EndGameReason?) {
        onSessionEnd(reason ?: determineDefaultEndGameReason())
    }

    private fun determineDefaultEndGameReason(): EndGameReason {
        if (alivePlayers.isEmpty()) {
            return EndGameReason.Draw
        }

        if (alivePlayers.values.any { it.role.settings.alignment.isTraitor && it.role.isWinner(this, it, EndGameReason.TraitorWin) }) {
            return EndGameReason.TraitorWin
        }

        if (alivePlayers.values.any { it.role.settings.alignment.isSurvivor && it.role.isWinner(this, it, EndGameReason.SurvivorWin) }) {
            return EndGameReason.SurvivorWin
        }

        if (alivePlayers.values.any { it.role.settings.alignment.isNeutral && it.role.isWinner(this, it, EndGameReason.NeutralWin) }) {
            return EndGameReason.NeutralWin
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
        for ((_, mannequin, interaction, _, _, contaminators) in deadPlayers.values) {
            interaction.teleport(mannequin.location)

            val nearby = mannequin.location.getNearbyPlayers(1.5)
            for (nearbyPlayer in nearby) {
                val player = alivePlayers[nearbyPlayer.uniqueId]?.onlineSessionPlayer ?: continue

                if (player.player.isSneaking && nearbyPlayer.location.distanceSquared(mannequin.location) >= 0.5*0.5) {
                    val movement = player.player.location.subtract(mannequin.location).toVector().normalize().multiply(0.03)

                    mannequin.velocity = movement
                }

                if (player.role !is Detective && !contaminators.containsKey(player.uniqueId)) {
                    contaminators[player.uniqueId] = player.sessionPlayer
                    player.player.sendMessage(Component.text("You have contaminated the corpse.").color(Colors.VERY_RED.textColor))
                }
            }
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

        val interaction = location.world.spawnEntity(location, EntityType.INTERACTION) as Interaction
        interaction.persistentDataContainer.set(
            INTERACTION_PASSTHROUGH_KEY,
            PersistentDataType.STRING,
            mannequin.uniqueId.toString()
        )
        interaction.interactionWidth = 1.5f
        interaction.interactionHeight = 0.5f

        for (contaminator in contaminators.values) {
            if (contaminator.role.settings.alignment.isEvil) {
                contaminator.player?.sendMessage(Component.text("Hide the body by crouching.").color(Colors.VERY_RED.textColor))
            } else {
                contaminator.player?.sendMessage(Component.text("You have contaminated the corpse.").color(Colors.VERY_RED.textColor))
            }
        }

        deadPlayers[player.uniqueId] = SessionCorpse(
            sessionPlayer,
            mannequin,
            interaction,
            causeOfDeath,
            Clock.System.now(),
            contaminators
        )
    }

    fun onPlayerKilled(playerUuid: UUID, drops: MutableList<ItemStack>, causeOfDeath: String, contaminators: MutableMap<UUID, SessionPlayer>) {
        val deadPlayer = alivePlayers.remove(playerUuid)
        if (alivePlayers.isEmpty()) {
            manager.endSession()
            return
        }

        if (!onFinalStage() && alivePlayers.values.all { !it.role.settings.alignment.isEvil } && settings.endIfNoEvil) {
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

        return !event.player.combatTracker.isInCombat
    }

    fun revive(player: Player, roleIfNotPresent: Role.Builder): String {
        if (alivePlayers.containsKey(player.uniqueId)) {
            return "Player already alive"
        }

        deadPlayers[player.uniqueId]?.let { corpse ->
            val sessionPlayer = corpse.player
            val deadLoc = corpse.mannequin.location

            deadPlayers.remove(sessionPlayer.uniqueId)
            corpse.mannequin.remove()

            alivePlayers[sessionPlayer.uniqueId] = sessionPlayer
            allPlayers[sessionPlayer.uniqueId] = sessionPlayer

            initPlayer(sessionPlayer.onlineSessionPlayer!!)
            player.teleport(deadLoc)

            return "Player brought back"
        }

        val sessionPlayer = SessionPlayer(
            uniqueId = player.uniqueId,
            name = player.name,
            role = roleIfNotPresent.build(),
            server = server,
            statusBar = StatusBar(" | ".colored(Colors.LIGHT_GRAY))
        )

        allPlayers[player.uniqueId] = sessionPlayer
        alivePlayers[player.uniqueId] = sessionPlayer

        initPlayer(sessionPlayer.onlineSessionPlayer!!)

        return "Player brought back with role"
    }

    // Returns true if handled
    fun onRightClickItem(player: Player, item: ItemStack, hand: EquipmentSlot): Boolean {
        val sessionPlayer: SessionPlayer = alivePlayers[player.uniqueId] ?: run {
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
            if (activeMeeting == null && meetingNotOnCooldown && !onFinalStage()) {
                startMeeting()
                player.swingHand(EquipmentSlot.HAND)
            } else if (activeMeeting != null) {
                activeMeeting!!.onBellClicked(player)
                player.swingHand(EquipmentSlot.HAND)
            }
            return true
        }

        val sessionPlayer: SessionPlayer = alivePlayers[player.uniqueId] ?: run {
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

    // Returns true if handled
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
        val sessionPlayer = alivePlayers[player.uniqueId] ?: return null
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
                        .append(player.role.settings.stylized)
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
                        .append(player.role.settings.stylized)
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
                            player.role.settings.stylized
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

    fun updateBell() {
        val bellBlockData = server.createBlockData(Material.BELL)
        val barrierBlockData = server.createBlockData(Material.BARRIER)
        for (player in server.onlinePlayers) {
            if (Floodgate.isFloodgatePlayer(player.uniqueId)) {
                player.sendBlockChange(settings.bellLocation, bellBlockData)
            } else {
                player.sendBlockChange(settings.bellLocation, barrierBlockData)
            }
        }

        val activeMeeting = activeMeeting
        if (activeMeeting != null) {
            return
        }

        if (onFinalStage()) {
            bellLabel.text("Meetings are no longer possible".colored(Colors.VERY_RED))
            bellLabel.backgroundColor = Colors.ALMOST_BLACK.color(100)
            bellLabel.transformation = Transformation()

            return
        }

        if (meetingCooldown > 0) {
            meetingCooldown--

            val seconds = ((meetingCooldown - 1) / 20) + 1

            bellLabel.text("Meeting on cooldown for ${seconds.seconds} more seconds".colored(Colors.WHITE))
            bellLabel.backgroundColor = Colors.ALMOST_BLACK.color(100)
            bellLabel.transformation = Transformation()

            return
        }

        bellLabel.text(
            Component
                .text("Call meeting")
                .color(Colors.MEETING_GREEN.textColor)
        )
        bellLabel.backgroundColor = Colors.MIDNIGHT_GREEN.color(100)
        bellLabel.transformation = Transformation(scale = Vector3f(2f, 2f, 2f))
    }

    fun hasAchieved(achievement: Achievement) = achievements.contains(achievement)

    fun achieve(achievement: Achievement) {
        if (achievements.contains(achievement))
            return

        achievements.add(achievement)

        server.scheduler.runTaskLater(plugin, { _ ->
            server.sendMessage(Component.text {
                it.append("\n".component)
                it.append("Global achievement reached! ".component)
                it.append(Component.text { innerIt ->
                    innerIt.append("[".component)
                    innerIt.append(achievement.name)
                    innerIt.append("]".component)
                    achievement.description?.let { description ->
                        innerIt.hoverEvent(HoverEvent.showText(description))
                    }
                    innerIt.color(Colors.VERY_GREEN.textColor)
                })
                it.append("\n".component)
                if (achievement.secondsBeforeExposure.last == achievement.secondsBeforeExposure.first) {
                    it.append("Achievement reached ${achievement.secondsBeforeExposure.first.seconds} ago".colored(Colors.MID_GRAY))
                } else if (achievement.secondsBeforeExposure.first <= 0) {
                    it.append("Achievement reached less than ${achievement.secondsBeforeExposure.last.seconds} ago".colored(Colors.MID_GRAY))
                } else {
                    it.append("Achievement reached at least ${achievement.secondsBeforeExposure.first.seconds} ago".colored(Colors.MID_GRAY))
                }
                it.append("\n".component)
            })
        }, Random.nextInt(achievement.secondsBeforeExposure).toLong())
    }

    fun onFinalStage() = hasAchieved(TheFinalStage)

    //
    // EVENT
    //

    @EventHandler
    fun onServerTicked(event: ServerTickEndEvent) {
        val tickNumber = event.tickNumber
        if (activeMeeting?.state == Meeting.State.Finished) {
            activeMeeting?.deinit()
            activeMeeting = null
        }

        checkPlayerNearCorpse()

        val pistonBlockData = server.createBlockData(Material.MOVING_PISTON)
        settings.bellLocation.getWorld().setBlockData(settings.bellLocation, pistonBlockData)

        for (player in server.onlinePlayers) {
            val alivePlayer = alivePlayers[player.uniqueId]
            if (alivePlayer != null) {
                val onlinePlayer = alivePlayer.onlineSessionPlayer!!
                alivePlayer.role.onTickOnline(this, onlinePlayer, tickNumber)

                val message = alivePlayer.statusBar.tick()
                player.sendActionBar(message)
            }
        }

        updateBell()

        activeMeeting?.onServerTicked()
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        if (event.entity is EnderDragon) {
            manager.endSession(EndGameReason.DragonDefeated)
        }
    }

    @EventHandler
    fun onItemDespawn(event: ItemDespawnEvent) {
        if (event.entity.itemStack.persistentDataContainer.get(ItemStacks.ROLE_MATERIAL_KEY, PersistentDataType.STRING) == "manifesto") {
            event.isCancelled = true
        }
    }

    @EventHandler
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
                    listener.sendMessage(Component.text("☠ <${player.name}> $message").color(Colors.LIGHT_GRAY.textColor))
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

    @EventHandler
    fun endPortalOpened(event: PlayerChangedWorldEvent) {
        if (event.player.world.key == NamespacedKey.minecraft("the_end")) {
            achieve(TheFinalStage)
        }
    }

    @EventHandler
    fun onPlayerAdvancement(event: PlayerAdvancementDoneEvent) {
        event.message(null)
    }

    override fun onPacketSend(event: PacketSendEvent) {
        if (event.packetType == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            var changeTracked = false
            val wrapper = WrapperPlayServerEntityEquipment(event)

            for ((i, equipment) in wrapper.equipment.withIndex()) {
                if (equipment == null) continue
                val item = SpigotConversionUtil.toBukkitItemStack(equipment.item)
                if (item.persistentDataContainer.get(ItemStacks.OBSCURED_KEY, PersistentDataType.BOOLEAN) == true) {
                    changeTracked = true
                    wrapper.equipment[i] = Equipment(equipment.slot, com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY)
                }
            }

            if (changeTracked) {
                event.markForReEncode(true)
            }
        }
    }

    @EventHandler
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

    @EventHandler
    fun equipmentChanged(event: EntityEquipmentChangedEvent) {
        val sessionPlayer = allPlayers[event.entity.uniqueId] ?: return

        event.entity.equipment?.let { equipment ->
            var anyHidden = false
            for (slot in EquipmentSlot.entries) {
                if (equipment.getItem(slot).persistentDataContainer.get(ItemStacks.OBSCURED_KEY, PersistentDataType.BOOLEAN) == true) {
                    anyHidden = true
                    break
                }
            }
            if (anyHidden) {
                sessionPlayer.statusBar.sendMessage(TraitorGamePlugin.key("obscured_item"), "Item obscured".component, important = false)
            } else {
                sessionPlayer.statusBar.hideMessage(TraitorGamePlugin.key("obscured_item"))
            }
        }
    }

    @EventHandler
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
                    sessionPlayer.player.sendMessage(Component
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

    @EventHandler
    fun onEntityTakeDamage(event: EntityDamageEvent) {
        if (event.entity is Player && activeMeeting != null) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val sessionPlayer = alivePlayers[player.uniqueId]

        for (player in server.onlinePlayers) {
            updatePlayerList(player)
        }

        if (sessionPlayer == null) {
            applyDeadModifiers(player)
        } else {
            sessionPlayer.onlineSessionPlayer!!.doOnlineActions()
        }
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerPostRespawnEvent) {
        if (!isPlayerAlive(event.player)) {
            applyDeadModifiers(event.player)
        }
    }

    @EventHandler
    fun onEntityPickup(event: EntityPickupItemEvent) {
        alivePlayers[event.entity.uniqueId]?.onlineSessionPlayer?.let { sessionPlayer ->
            if (sessionPlayer.role.itemPickup(this, sessionPlayer, event.item.itemStack, event.item)) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler
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

        alivePlayers[event.player.uniqueId]?.onlineSessionPlayer?.let { sessionPlayer ->
            if (sessionPlayer.role.itemDropped(this, sessionPlayer, event.itemDrop.itemStack, event.itemDrop)) {
                event.isCancelled = true
                return
            }
        }
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

        val handled = this.onPlayerRightClickEntity(
            player,
            player.inventory.getItem(event.hand),
            event.hand,
            event.rightClicked
        )
        if (handled) {
            event.isCancelled = true
            return
        }
    }

    @EventHandler
    fun onItemMerge(event: ItemMergeEvent) {
        if (event.entity.persistentDataContainer.has(TraitorGamePlugin.key("merchandise"))) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!event.action.isRightClick) {
            return
        }

        val player = event.player

        event.clickedBlock?.let { clickedBlock ->
            val handled = this.onPlayerRightClickBlock(
                player,
                event.item,
                clickedBlock.location.toBlockLocation(),
                event.hand ?: EquipmentSlot.HAND
            )

            if (handled) {
                event.isCancelled = true
                return
            }
        }

        event.item?.let { item ->
            val handled = this.onRightClickItem(player, item, event.hand ?: EquipmentSlot.HAND)
            if (handled) {
                event.isCancelled = true
                return
            }
        }
    }
}