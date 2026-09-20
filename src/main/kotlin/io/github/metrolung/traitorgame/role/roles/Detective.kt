package io.github.metrolung.traitorgame.role.roles

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.ItemStacks
import io.github.metrolung.traitorgame.Meeting
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionCorpse
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.github.metrolung.traitorgame.Vote
import io.github.metrolung.traitorgame.api.Floodgate
import io.github.metrolung.traitorgame.colored
import io.github.metrolung.traitorgame.component
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleAlignment
import io.github.metrolung.traitorgame.textColor
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.invui.dsl.ExperimentalDslApi
import xyz.xenondevs.invui.dsl.gui
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.set
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.UpdateReason
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.collections.iterator
import kotlin.math.min
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class Detective : Survivor {
    val notes = mutableListOf<Pair<Instant, DetectiveNote>>()
//    val scannerInventory = VirtualInventory(3*3)
    val identifiedPlayers = mutableSetOf<UUID>()
    val resultGui = Gui.empty(5, 2)

    override fun onRolePresented(session: Session, sessionPlayer: OnlineSessionPlayer) {
        sessionPlayer.player.give(ItemStacks.notebook)
        sessionPlayer.player.give(ItemStacks.detectiveCrossbow)
        sessionPlayer.player.give(ItemStack.of(Material.ARROW).apply { amount = 32 })
        sessionPlayer.player.give(ItemStacks.swab.apply { amount = 64 })
        sessionPlayer.player.give(ItemStacks.sampleChecker)
        identifiedPlayers.add(sessionPlayer.uniqueId)
    }

    override fun handleEquipmentUse(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, equipmentType: String, block: Location?): Boolean {
        when (equipmentType) {
            "notebook" -> {
                if (sessionPlayer.player.isSneaking) {
                    val book = Book.builder()
                    book.title(Component.text("Notebook"))
                    book.author(Component.text(sessionPlayer.name))

                    for ((i, pair) in notes.withIndex()) {
                        val (time, note) = pair

                        val timeMessage = LocalDateTime.ofInstant(time.toJavaInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("mm:ss"))
                        book.addPage(
                            Component
                                .text("Note ${i+1} ($timeMessage)\n---------------\n")
                                .append(note.message(time))
                        )
                    }

                    sessionPlayer.player.openBook(book)

                    return true
                }

                if (sessionPlayer.player.getCooldown(TraitorGamePlugin.key("notebook_cooldown")) > 0) {
                    return false
                }

                val nearbySessionPlayers: MutableList<String> = mutableListOf()
                for (nearbyPlayer in sessionPlayer.player.location.getNearbyEntities(8.0, 8.0, 8.0)) {
                    if (nearbyPlayer.uniqueId == sessionPlayer.uniqueId) {
                        continue
                    }

                    if (session.isPlayerAlive(nearbyPlayer.uniqueId)) {
                        nearbySessionPlayers.add(nearbyPlayer.name)
                    }

                    val corpse = nearbyPlayer.persistentDataContainer.get(
                        SessionCorpse.CORPSE_KEY,
                        PersistentDataType.STRING
                    )

                    if (corpse != null) {
                        val corpseName = session.deadPlayers[UUID.fromString(corpse)]?.player?.name ?: continue
                        nearbySessionPlayers.add("☠ $corpseName")
                    }
                }

                if (nearbySessionPlayers.isEmpty()) {
                    sessionPlayer.player.sendMessage(Component.text("Nothing to take note of.").color(Colors.MID_GRAY.textColor))
                    return false
                }

                sessionPlayer.player.setCooldown(Material.BOOK, session.settings.roleSettings.detectiveNotebookCooldownTicks)
                sessionPlayer.player.setCooldown(TraitorGamePlugin.key("notebook_cooldown"), session.settings.roleSettings.detectiveNotebookCooldownTicks)

                addNote(sessionPlayer.player, DetectiveNote.SeenNear(
                    sessionPlayer.player.location,
                    nearbySessionPlayers
                ))
                return true
            }
            "sample_checker" -> {
                openScannerGui(session, sessionPlayer.player)
                return true
            }
        }

        return super.handleEquipmentUse(session, sessionPlayer, itemStack, equipmentType, block)
    }

    override fun handleMaterialUseOnEntity(
        session: Session,
        sessionPlayer: OnlineSessionPlayer,
        itemStack: ItemStack,
        materialType: String,
        entity: Entity
    ): Boolean {
        when (materialType) {
            "swab" -> {
                if (session.isPlayerAlive(entity.uniqueId)) {
                    val swabbed = session.alivePlayers[entity.uniqueId]!!
                    session.samples.add(DNASample.IdentifiableSample(swabbed))
                    itemStack.amount -= 1

                    sessionPlayer.player.give(
                        listOf(ItemStacks.usedSwab(Component.text("Swab of ${swabbed.name}"), session.samples.size - 1)),
                        true
                    )
                } else {
                    val playerUuid = entity.persistentDataContainer.get(
                        SessionCorpse.CORPSE_KEY,
                        PersistentDataType.STRING
                    ) ?: return false

                    val corpsePlayer = session.deadPlayers[UUID.fromString(playerUuid)] ?: return false

                    itemStack.amount -= 1

                    val contaminators = corpsePlayer.contaminators.values.toList()
                    if (contaminators.isNotEmpty()) {
                        session.samples.add(DNASample.CorpseSample(corpsePlayer.player, contaminators))
                    } else {
                        session.samples.add(DNASample.IdentifiableCorpseSample(corpsePlayer.player))
                    }

                    sessionPlayer.player.give(
                        listOf(
                            ItemStacks.usedSwab(
                                Component.text("Swab of ${corpsePlayer.player.name}'s Corpse"),
                                session.samples.size - 1
                            )
                        ),
                        true
                    )
                }

                for (audience in sessionPlayer.player.world.audiences()) {
                    audience.playSound(Sound.sound {
                        it.source(Sound.Source.PLAYER)
                        it.pitch(-0.5f)
                        it.type(NamespacedKey.minecraft("entity.pig.saddle"))
                    }, entity.location.x, entity.location.y, entity.location.z)
                }

                return true
            }
        }

        return super.handleMaterialUseOnEntity(session, sessionPlayer, itemStack, materialType, entity)
    }

    override fun handleEquipmentUseOnEntity(
        session: Session,
        sessionPlayer: OnlineSessionPlayer,
        itemStack: ItemStack,
        equipmentType: String,
        entity: Entity
    ): Boolean {
        when (equipmentType) {
            "notebook" -> {
                if (sessionPlayer.player.getCooldown(TraitorGamePlugin.key("notebook_cooldown")) > 0) {
                    return false
                }

                val corpse = entity.persistentDataContainer.get(
                    SessionCorpse.CORPSE_KEY,
                    PersistentDataType.STRING
                )

                if (corpse == null) {
                    return false
                }

                val sessionCorpse = session.deadPlayers[UUID.fromString(corpse)] ?: return false

                sessionPlayer.player.setCooldown(Material.BOOK, session.settings.roleSettings.detectiveNotebookCooldownTicks)
                sessionPlayer.player.setCooldown(TraitorGamePlugin.key("notebook_cooldown"), session.settings.roleSettings.detectiveNotebookCooldownTicks)

                addNote(sessionPlayer.player, DetectiveNote.Death(
                    sessionCorpse.player.name,
                    sessionCorpse.causeOfDeath,
                    sessionCorpse.timeOfDeath,
                ))
                return true
            }
        }

        return super.handleEquipmentUseOnEntity(session, sessionPlayer, itemStack, equipmentType, entity)
    }

    override fun onMeetingEnd(session: Session, meeting: Meeting, sessionPlayer: SessionPlayer, turnout: Map<Vote, Int>) {
        addNote(sessionPlayer.player, DetectiveNote.VoterTurnout(turnout))
    }

    override fun onMeetingStart(session: Session, meeting: Meeting, sessionPlayer: SessionPlayer) {}

    fun addNote(player: Player?, note: DetectiveNote) {
        player?.playSound(Sound.sound {
            it.source(Sound.Source.PLAYER)
            it.type(NamespacedKey.minecraft("item.book.page_turn"))
        })
        player?.sendMessage(
            Component.text("Note written.").decorate(TextDecoration.ITALIC).color(
                Colors.MID_GRAY.textColor))
        notes.add(Clock.System.now() to note)
        if (notes.size >= 100)
            notes.removeFirst()
    }


    @OptIn(ExperimentalDslApi::class)
    fun openScannerGui(session: Session, player: Player) {
        val itemSample: MutableProvider<DNASample?> = mutableProvider { null }

        val useSlot = VirtualInventory(1)
        useSlot.addPreUpdateHandler { event ->
            val item = event.newItem
            if (item == null) {
                itemSample.set(null)
                return@addPreUpdateHandler
            }
            val sample = item.persistentDataContainer.get(ItemStacks.SAMPLE_KEY, PersistentDataType.INTEGER)
            if (sample == null) {
                event.isCancelled = true
                return@addPreUpdateHandler
            }
            itemSample.set(session.samples[sample])
        }

        window(player) {
            upperGui by gui(
                "# # # # # # # # #",
                "# . # , , , , , #",
                "# s # , , , , , #",
                "# # # # # # # # #"
            ) {
                title by " ↓ sᴡᴀʙ ↓         Sample Checker".component

                '#' by ItemStacks.Gui.whiteGlass
                '.' by useSlot
                ',' by resultGui
                's' by item {
                    itemProvider by itemSample.map { sample ->
                        if (sample == null) {
                            ItemBuilder(ItemType.RED_STAINED_GLASS_PANE)
                                .setName("Cannot scan".colored(Colors.VERY_RED))
                        } else {
                            ItemBuilder(ItemType.LIME_STAINED_GLASS_PANE)
                                .setName("Scan".colored(Colors.VERY_GREEN))
                                .setLore(listOf(
                                    "Warning! ".colored(Colors.VERY_RED).append("This consumes the swab!".colored(Colors.WHITE)),
                                ))
                        }
                    }

                    onClick {
                        itemSample.get()?.let { sample ->
                            useSlot.setItem(UpdateReason.SUPPRESSED, 0, null)
                            when (sample) {
                                is DNASample.IdentifiableSample -> {
                                    identifiedPlayers.add(sample.player.uniqueId)
                                    resultGui.fill(null)
                                    resultGui[0] = Item.simple(ItemStacks.Gui.identifiedPlayer(
                                        sample.player.name.colored(Colors.VERY_GREEN),
                                        sample.player.uniqueId
                                    ))
                                }
                                is DNASample.IdentifiableCorpseSample -> {
                                    identifiedPlayers.add(sample.player.uniqueId)
                                    resultGui.fill(null)
                                    resultGui[0] = Item.simple(ItemStacks.Gui.identifiedPlayer(
                                        sample.player.name.colored(Colors.VERY_GREEN),
                                        sample.player.uniqueId
                                    ))
                                }
                                is DNASample.CorpseSample -> {
                                    resultGui.fill(null)
                                    val backing = (sample.potentials + sample.corpse).shuffled()
                                    val players = backing.subList(0, min(backing.size, 10))
                                    player.sendMessage("seen near corpse")
                                    addNote(player, DetectiveNote.SeenNearCorpse(
                                        sample.corpse.name,
                                        sample.potentials.map {
                                            if (identifiedPlayers.contains(it.uniqueId))
                                                it.name
                                            else
                                                "???"
                                        }
                                    ))
                                    for ((i, value) in players.withIndex()) {
                                        resultGui[i] = if (identifiedPlayers.contains(value.uniqueId)) {
                                            Item.simple(ItemStacks.Gui.identifiedPlayer(
                                                value.name.component,
                                                value.uniqueId
                                            ))
                                        } else {
                                            if (Floodgate.api?.isFloodgatePlayer(player.uniqueId) == true) {
                                                Item.simple(ItemStacks.Gui.unidentifiedPlayerBedrock)
                                            } else {
                                                Item.simple(ItemStacks.Gui.unidentifiedPlayer)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            onClose {
                useSlot.items.first()?.let {
                    player.give(it)
                }
            }
        }.open()
    }

    interface DetectiveNote {
        fun message(noteTime: Instant): Component

        data class VoterTurnout(val turnout: Map<Vote, Int>) : DetectiveNote {
            override fun message(noteTime: Instant): Component {
                return Component.text {
                    for ((vote, count) in turnout) {
                        it.append(
                            Component
                                .text("[$count] ")
                                .append(
                                    when (vote) {
                                        Vote.EndGame -> Component.text("End Game")
                                        Vote.Skip -> Component.text("Skip Vote")
                                        is Vote.PlayerVote -> Component.text(vote.player.name)
                                    }
                                )
                        )
                    }
                    it.append(Component.newline())
                }
            }
        }

        data class SeenNear(val location: Location, val names: List<String>) : DetectiveNote {
            override fun message(noteTime: Instant): Component {
                return Component.text {
                    it.append("Surroundings at \n${location.blockX}, ${location.blockY}, ${location.blockZ}:\n".colored(Colors.DARK_GRAY))
                    for ((i, name) in names.withIndex()) {
                        it.append(" ${i+1}. $name".component)
                    }
                }
            }
        }

        data class SeenNearCorpse(val corpseName: String, val names: List<String>) : DetectiveNote {
            override fun message(noteTime: Instant): Component {
                return Component.text {
                    it.append("Contaminated corpse \nof $corpseName:\n".colored(Colors.DARK_GRAY))
                    for ((i, name) in names.withIndex()) {
                        it.append(" ${i+1}. $name\n".component)
                    }
                }
            }
        }

        data class Death(val name: String, val causeOfDeath: String, val deathTime: Instant) : DetectiveNote {
            override fun message(noteTime: Instant): Component {
                return Component.text {
                    it.append(Component.text("$name died\n"))
                    it.append(Component.text("Cause: $causeOfDeath\n\n"))
                    it.append(Component.text("body was found ${(noteTime-deathTime).inWholeSeconds.seconds} old"))
                }
            }
        }
    }

    sealed interface DNASample {
        data class IdentifiableSample(val player: SessionPlayer) : DNASample
        data class IdentifiableCorpseSample(val player: SessionPlayer) : DNASample
        data class CorpseSample(val corpse: SessionPlayer, val potentials: List<SessionPlayer>) : DNASample
    }

    override val settings: Role.Settings
        get() = SETTINGS

    companion object {
        @JvmField
        val SETTINGS: Role.Settings = Role.Settings(
            key = TraitorGamePlugin.key("detective"),
            name = "Detective",
            roleColor = Colors.DETECTIVE_BLUE,
            alignment = RoleAlignment.SURVIVOR,
            itemProvider = ItemBuilder(Material.CROSSBOW),
            goalProvider = { "Track down and kill the traitors by any means." },
            builder = { Detective() }
        )
    }
}