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
import io.github.metrolung.traitorgame.role.RoleSettings
import io.github.metrolung.traitorgame.textColor
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BundleMeta
import org.bukkit.persistence.PersistentDataType
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.collections.iterator
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class Detective : Survivor {
    val notes = mutableListOf<Pair<Instant, DetectiveNote>>()

    override val roleColor: Int
        get() = Colors.DETECTIVE_BLUE

    override val name: String
        get() = "Detective"

    override fun getGoal(roleSettings: RoleSettings): String {
        return "Track down and kill the traitors by any means."
    }

    override fun onRolePresented(session: Session, sessionPlayer: OnlineSessionPlayer) {
        sessionPlayer.player.give(ItemStacks.notebook)
        sessionPlayer.player.give(ItemStacks.detectiveCrossbow)
        sessionPlayer.player.give(ItemStack.of(Material.ARROW).apply { amount = 32 })
        sessionPlayer.player.give(ItemStacks.swab.apply { amount = 64 })
        sessionPlayer.player.give(ItemStacks.swabChecker)
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
                        nearbySessionPlayers.add("☠ $corpseName's corpse")
                    }
                }

                if (nearbySessionPlayers.isEmpty()) {
                    sessionPlayer.player.sendMessage(Component.text("Nothing to take note of.").color(Colors.MID_GRAY.textColor))
                    return false
                }

                sessionPlayer.player.setCooldown(Material.BOOK, session.settings.roleSettings.detectiveNotebookCooldownTicks)
                sessionPlayer.player.setCooldown(TraitorGamePlugin.key("notebook_cooldown"), session.settings.roleSettings.detectiveNotebookCooldownTicks)

                addNote(sessionPlayer.sessionPlayer, DetectiveNote.SeenNear(
                    sessionPlayer.player.location,
                    nearbySessionPlayers
                ))
                return true
            }
            "swab_checker" -> {
                val bundleContents = (itemStack.itemMeta as BundleMeta).items

                if (bundleContents.size != 2) {
                    sessionPlayer.player.sendMessage(
                        Component
                            .text("Swab checker requires exactly two used swabs!")
                            .color(Colors.MID_GRAY.textColor)
                    )
                    return false
                }

                val swabIndex1 = bundleContents[0].persistentDataContainer.get(ItemStacks.USED_SWAB_KEY, PersistentDataType.INTEGER)
                val swabIndex2 = bundleContents[1].persistentDataContainer.get(ItemStacks.USED_SWAB_KEY, PersistentDataType.INTEGER)

                if (swabIndex1 == null || swabIndex2 == null) {
                    sessionPlayer.player.sendMessage(
                        Component
                            .text("Swab checker requires used swabs!")
                            .color(Colors.MID_GRAY.textColor)
                    )
                    return false
                }

                val (swabbedName1, swabbedPlayer1) = session.swabs[swabIndex1]
                val (swabbedName2, swabbedPlayer2) = session.swabs[swabIndex2]

                if (swabbedPlayer1.uniqueId == swabbedPlayer2.uniqueId) {
                    session.server.playSound(Sound.sound {
                        it.source(Sound.Source.PLAYER)
                        it.pitch(2f)
                        it.type(NamespacedKey.minecraft("block.note_block.bit"))
                    })
                    sessionPlayer.player.sendMessage(
                        Component
                            .text("A match is found between $swabbedName1 and $swabbedName2!")
                            .color(Colors.VERY_GREEN.textColor)
                    )
                } else {
                    sessionPlayer.player.sendMessage(
                        Component
                            .text("Inconclusive match between $swabbedName1 and $swabbedName2")
                            .color(Colors.MID_GRAY.textColor)
                    )
                }

                session.server.playSound(Sound.sound {
                    it.source(Sound.Source.PLAYER)
                    it.type(NamespacedKey.minecraft("item.bottle.fill"))
                })

                itemStack.editMeta { (it as BundleMeta).setItems(null) }

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
                    session.swabs.add(swabbed.name to swabbed)

                    itemStack.amount -= 1

                    sessionPlayer.player.give(
                        listOf(ItemStacks.usedSwab(Component.text("Swab of ${swabbed.name}"), session.swabs.size - 1)),
                        true
                    )
                } else {
                    val playerUuid = entity.persistentDataContainer.get(
                        SessionCorpse.CORPSE_KEY,
                        PersistentDataType.STRING
                    ) ?: return false

                    val corpsePlayer = session.deadPlayers[UUID.fromString(playerUuid)] ?: return false

                    itemStack.amount -= 1

                    val swabbed = if (Random.nextBoolean() || corpsePlayer.contaminators.isEmpty()) {
                        corpsePlayer.player
                    } else {
                        corpsePlayer.contaminators.values.random()
                    }
                    session.swabs.add("unknown DNA" to swabbed)

                    sessionPlayer.player.give(
                        listOf(
                            ItemStacks.usedSwab(
                                Component.text("Swab of ${swabbed.name}'s Corpse"),
                                session.swabs.size - 1
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

                addNote(sessionPlayer.sessionPlayer, DetectiveNote.Death(
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
        addNote(sessionPlayer, DetectiveNote.VoterTurnout(turnout))
    }

    override fun onMeetingStart(session: Session, meeting: Meeting, sessionPlayer: SessionPlayer) {}

    fun addNote(sessionPlayer: SessionPlayer, note: DetectiveNote) {
        sessionPlayer.player?.playSound(Sound.sound {
            it.source(Sound.Source.PLAYER)
            it.type(NamespacedKey.minecraft("item.book.page_turn"))
        })
        sessionPlayer.player?.sendMessage(
            Component.text("Note written.").decorate(TextDecoration.ITALIC).color(
                Colors.MID_GRAY.textColor))
        notes.add(Clock.System.now() to note)
        if (notes.size >= 100)
            notes.removeFirst()
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
                    it.append(Component.text("Surroundings at \n${location.blockX}, ${location.blockY}, ${location.blockZ}:\n\n"))
                    for (name in names) {
                        it.append(Component.text(name))
                        it.append(Component.newline())
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
}