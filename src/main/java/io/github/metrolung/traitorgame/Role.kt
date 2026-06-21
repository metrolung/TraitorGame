package io.github.metrolung.traitorgame

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


interface Role {
    val name: TextComponent
    val goal: String
    val group: RoleGroup
    fun handleEquipmentUse(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, equipmentType: String) {}
    fun onMeetingStart(session: Session, meeting: Meeting, sessionPlayer: SessionPlayer) {}
    fun onMeetingEnd(session: Session, meeting: Meeting, sessionPlayer: SessionPlayer) {}
    fun onRolePresented(session: Session, sessionPlayer: OnlineSessionPlayer) {}
    fun onPlayerDied(session: Session, sessionPlayer: SessionPlayer, deadPlayer: SessionPlayer) {}

    object Traitor : Role {
        override val name: TextComponent
            get() = Component
                .text("Traitor")
                .color(TextColor.color(0xFF0000))

        override val goal: String
            get() = "Kill all the players and end the game."

        override val group: RoleGroup
            get() = RoleGroup.Traitors
    }

    object LuckyTraitor : Role {
        override val name: TextComponent
            get() = Component
                .text("Traitor")
                .color(TextColor.color(0xE01A7A))

        override val goal: String
            get() = "Kill all the players and end the game."

        override val group: RoleGroup
            get() = RoleGroup.Traitors
    }

    class Detective : Role {
        val notes: MutableList<MutableList<Pair<LocalDateTime, DetectiveNote>>> = mutableListOf(mutableListOf())

        override val name: TextComponent
            get() = Component
                .text("Detective")
                .color(TextColor.color(0x0000FF))

        override val goal: String
            get() = "Track down and kill the traitors by any means."

        override val group: RoleGroup
            get() = RoleGroup.Survivors

        override fun onRolePresented(session: Session, sessionPlayer: OnlineSessionPlayer) {
            sessionPlayer.player.inventory.heldItemSlot = 8
            sessionPlayer.player.give(ItemStacks.notebook)
            sessionPlayer.player.give(ItemStacks.detective_crossbow)
            sessionPlayer.player.give(ItemStack.of(Material.ARROW).apply { amount = 16 })
        }

        override fun handleEquipmentUse(session: Session, sessionPlayer: OnlineSessionPlayer, itemStack: ItemStack, equipmentType: String) {
            if (equipmentType == "notebook") {
                if (sessionPlayer.player.getCooldown(TraitorGamePlugin.key("notebook_cooldown")) > 0) {
                    sessionPlayer.player.playSound(Sound.sound {
                        it.source(Sound.Source.PLAYER)
                        it.type(NamespacedKey.minecraft("item.book.put"))
                    })

                    return
                }

                sessionPlayer.player.setCooldown(Material.BOOK, session.settings.noteCooldownTicks)

                sessionPlayer.player.setCooldown(TraitorGamePlugin.key("notebook_cooldown"), session.settings.noteCooldownTicks)

                val nearby: Collection<Player> = sessionPlayer.player.location.getNearbyPlayers(12.0)
                val nearbySessionPlayers: MutableList<String> = mutableListOf()
                for (nearbyPlayer in nearby) {
                    if (nearbyPlayer.uniqueId == sessionPlayer.playerUuid) {
                        continue
                    }

                    if (!session.isPlayerAlive(nearbyPlayer.uniqueId)) {
                        continue
                    }

                    nearbySessionPlayers.add(nearbyPlayer.name)
                }

                addNote(sessionPlayer.sessionPlayer, DetectiveNote.SeenNear(nearbySessionPlayers))
            }
        }

        override fun onMeetingEnd(session: Session, meeting: Meeting, sessionPlayer: SessionPlayer) {
            val voter = session.alivePlayers.entries.random().value
            val vote = meeting.votes[voter.playerUuid]
            addNote(sessionPlayer, DetectiveNote.CaughtVote(voter.name, vote))
        }

        override fun onMeetingStart(session: Session, meeting: Meeting, sessionPlayer: SessionPlayer) {
            val roundNotes = notes.last()

            notes.add(mutableListOf())

            if (roundNotes.isNotEmpty()) {
                val book = ItemStack.of(Material.WRITTEN_BOOK)
                book.editMeta { meta ->
                    val bookMeta = meta as? BookMeta ?: return@editMeta

                    bookMeta.itemName(Component.text("Case ${notes.size-1}"))
                    bookMeta.author = sessionPlayer.name

                    for (page in roundNotes.chunked(4)) {
                        var component = Component.empty()

                        for ((time, note) in page) {
                            val timeMessage = time.format(DateTimeFormatter.ofPattern("mm:ss"))
                            component = component
                                .append(Component.text("[$timeMessage] "))
                                .append(note.message).append(Component.text("\n\n"))
                        }

                        bookMeta.addPages(component)
                    }
                }

                sessionPlayer.whenOnline { player ->
                    player.player.give(book)
                }
            }
        }

        override fun onPlayerDied(session: Session, sessionPlayer: SessionPlayer, deadPlayer: SessionPlayer) {
            addNote(sessionPlayer, DetectiveNote.Death(deadPlayer.name))
        }

        fun addNote(sessionPlayer: SessionPlayer, note: DetectiveNote) {
            sessionPlayer.player?.playSound(Sound.sound {
                it.source(Sound.Source.PLAYER)
                it.type(NamespacedKey.minecraft("item.book.page_turn"))
            })
            sessionPlayer.player?.sendMessage(Component.text("Note written.").decorate(TextDecoration.ITALIC).color(TextColor.color(0x777777)))
            notes.last().add(LocalDateTime.now() to note)
        }

        interface DetectiveNote {
            val message: Component

            data class CaughtVote(val name: String, val vote: Vote?) : DetectiveNote {
                override val message: Component
                    get() = when (vote) {
                        Vote.EndGame ->
                            Component.text("I saw $name's ballot. They voted to end the game")
                        is Vote.PlayerVote -> {
                            Component.text("I saw $name's ballot. They voted for ${vote.player.name}")
                        }
                        Vote.Skip -> Component.text("I saw $name's ballot. They voted to skip")
                        null -> Component.text("$name didn't vote.")
                    }

            }
            data class SeenNear(val names: List<String>) : DetectiveNote {
                override val message: Component
                    get() {
                        if (names.isEmpty()) {
                            return Component.text("I was alone")
                        }

                        if (names.size == 1) {
                            return Component.text("I was alone with ${names.first()}")
                        }

                        if (names.size == 2) {
                            return Component.text("${names.first()} was with ${names[1]}")
                        }

                        val namesJoined = buildString {
                            for ((index, name) in names.withIndex()) {
                                if (index == names.size-1) {
                                    append("and ")
                                    append(name)
                                } else {
                                    append(name)
                                    append(", ")
                                }
                            }
                        }

                        return Component.text("$namesJoined were together")
                    }
            }
            data class Death(val name: String) : DetectiveNote {
                override val message: Component
                    get() {
                        return Component.text("$name died")
                    }
            }
        }
    }

    object Survivor : Role {
        override val name: TextComponent
            get() = Component
                .text("Survivor")
                .color(TextColor.color(0x01FFCC))

        override val goal: String
            get() = "Defeat the enderdragon and don't die!"

        override val group: RoleGroup
            get() = RoleGroup.Survivors
    }

    object LuckySurvivor : Role {
        override val name: TextComponent
            get() = Component
                .text("Survivor")
                .color(TextColor.color(0x00ff87))

        override val goal: String
            get() = "Defeat the enderdragon and don't die!"

        override val group: RoleGroup
            get() = RoleGroup.Survivors
    }
}