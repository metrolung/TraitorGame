package io.github.metrolung.traitorgame

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.TitlePart
import org.bukkit.NamespacedKey
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.geysermc.cumulus.form.ModalForm
import org.geysermc.cumulus.form.SimpleForm
import org.geysermc.cumulus.util.FormImage
import org.geysermc.floodgate.api.player.FloodgatePlayer
import org.geysermc.floodgate.util.LinkedPlayer
import java.time.Duration
import java.util.*
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds


class Meeting(
    val session: Session,
    val bellLabel: TextDisplay
) {
    private var timing = 0xBEEF
    var state: State? = null
        private set
    val server: Server = session.server
    val votes: MutableMap<UUID, Vote> = mutableMapOf()

    private var onMeetingEnd = {}
    private var turnout: MutableMap<Vote, Int>? = null

    fun startMeeting() {
        server.sendTitlePart(
            TitlePart.TIMES,
            Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))
        )
        server.sendTitlePart(TitlePart.TITLE, Component.text("MEETING!").color(Colors.VERY_RED.textColor))

        val meetingWorldBorder = server.createWorldBorder()
        meetingWorldBorder.size = 21.0
        meetingWorldBorder.center = session.settings.bellLocation.toCenterLocation()

        for (player in server.onlinePlayers) {
            player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 50, 1, false, false, false))
            player.worldBorder = meetingWorldBorder
        }

        for (player in session.alivePlayers.values) {
            player.role.onMeetingStart(session, this, player)
        }

        session.gatherAroundBell()
        server.playSound(Sound.sound {
            it.source(Sound.Source.BLOCK)
            it.type(Key.key("minecraft:block.bell.use"))
        })
        startDiscussion()
    }

    private fun javaVotingMenu(player: Player) {
        player.sendMessage(Component.empty())

        for ((_, alivePlayer) in session.alivePlayers) {
            player.sendMessage(
                Component.empty()
                    .append(Component
                        .text("→ ")
                        .color(Colors.LIGHT_GRAY.textColor)
                    )
                    .append(Component
                        .text(alivePlayer.name)
                    )
                    .append(Component
                        .text(" - ")
                        .color(Colors.LIGHT_GRAY.textColor)
                    )
                    .append(Component
                        .text("[ VOTE ]")
                        .color(Colors.VERY_GREEN.textColor)
                        .clickEvent(ClickEvent.runCommand("${TraitorGamePlugin.namespace}:vote player ${alivePlayer.name}"))
                    )
            )
        }

        player.sendMessage(Component.empty())

        player.sendMessage(Component
            .empty()
            .append(Component
                .text("[ SKIP ]")
                .color(Colors.ORANGE.textColor)
                .clickEvent(ClickEvent.runCommand("${TraitorGamePlugin.namespace}:vote skip"))
            )
            .append(Component.text("   |   ").color(Colors.LIGHT_GRAY.textColor))
            .append(Component
                .text("[ END GAME ]")
                .color(Colors.VERY_RED.textColor)
                .clickEvent(ClickEvent.runCommand("${TraitorGamePlugin.namespace}:vote end"))
            )
        )

        player.sendMessage(Component.empty())
    }

    private fun bedrockVotingMenu(player: Player, connection: FloodgatePlayer) {
        val form = SimpleForm
            .builder()
            .title("Voting Menu")
            .content("Actions")
            .button("Vote to Skip")
            .button("Vote to End Game")
            .content("Players")

        val commands = mutableListOf(
            "${TraitorGamePlugin.namespace}:vote skip",
            "${TraitorGamePlugin.namespace}:vote end",
        )

        for ((_, alivePlayer) in session.alivePlayers) {
            val voteText = "Vote ${alivePlayer.name}"

            if (FloodgateApi.isFloodgatePlayer(alivePlayer.playerUuid)) {
                val linked: LinkedPlayer? = FloodgateApi.getPlayer(alivePlayer.playerUuid).linkedPlayer

                if (linked == null) {
                    form.button(voteText, FormImage.Type.URL,
                        "https://minecraft.wiki/images/thumb/Bedrock_JE2_BE2.png/150px-Bedrock_JE2_BE2.png?5ea94"
                    )
                } else {
                    form.button(voteText, FormImage.Type.URL, "https://cravatar.eu/head/${linked.javaUniqueId}")
                }
            } else {
                form.button(voteText, FormImage.Type.URL, "https://cravatar.eu/head/${alivePlayer.playerUuid}")
            }
            commands.add("${TraitorGamePlugin.namespace}:vote player ${alivePlayer.name}")
        }

        form.validResultHandler { response ->
            val command = commands.getOrNull(response.clickedButtonId())
            if (command != null)
                player.performCommand(command)
        }

        form.closedOrInvalidResultHandler { ->
            player.sendMessage(Component.text("Click the bell to vote!").color(Colors.VERY_YELLOW.textColor))
        }

        connection.sendForm(form)
    }

    private fun javaBackMenu(player: Player) {
        player.sendMessage(
            Component
                .newline()
                .append(
                    Component
                        .text("Return back to your previous location? ")
                        .color(Colors.LAVENDER.textColor)
                        .clickEvent(ClickEvent.runCommand("${TraitorGamePlugin.namespace}:back"))
                        .append(
                            Component
                                .text("[click]")
                                .color(Colors.DARK_GRAY.textColor)
                        )
                )
        )

        player.sendMessage(Component.empty())
    }

    private fun bedrockBackMenu(player: Player, connection: FloodgatePlayer) {
        val form = ModalForm
            .builder()
            .title("Back Menu")
            .content("Return back to your previous location?")
            .button1("Yes")
            .button2("No")

        form.validResultHandler { response ->
            if (response.clickedFirst()) {
                player.performCommand("${TraitorGamePlugin.namespace}:back")
            }
        }

        form.closedOrInvalidResultHandler { ->
            player.sendMessage(
                Component
                    .text("Return back to your previous location with ")
                    .color(Colors.LAVENDER.textColor)
                    .append(
                        Component
                            .text("/back")
                            .color(Colors.DARK_GRAY.textColor)
                    )
            )
        }

        connection.sendForm(form)
    }

    fun onBellClicked(player: Player) {
        val bedrockConnection = FloodgateApi.getPlayer(player.uniqueId)

        if (bedrockConnection != null && state == State.Voting) {
            bedrockVotingMenu(player, bedrockConnection)
        }
    }

    private fun startDiscussion() {
        this.state = State.Discussion
        this.timing = session.settings.discussionTimeTicks

        server.sendMessage(Component.text("Discussion started"))
    }

    private fun startVoting() {
        this.state = State.Voting
        this.timing = session.settings.votingTimeTicks

        server.sendMessage(Component.text("Voting started"))

        for (player in server.onlinePlayers) {
            val bedrockConnection = FloodgateApi.getPlayer(player.uniqueId)

            if (bedrockConnection == null) {
                javaVotingMenu(player)
            } else {
                bedrockVotingMenu(player, bedrockConnection)
            }
        }
    }

    private fun startSuspense() {
        this.state = State.Suspense
        this.timing = 50

        server.sendMessage(Component.text("The votes have been cast....").color(Colors.VERY_YELLOW.textColor))

        val turnout: MutableMap<Vote, Int> = mutableMapOf()

        for (vote in votes.values) {
            turnout[vote] = turnout.getOrDefault(vote, 0) + 1
        }

        this.turnout = turnout

        session.gracePeriod()

        var highestVote: Vote? = null
        var highestVoteCount = 0
        var tied = true

        for (entry in turnout.entries) {
            if (highestVote == null) {
                highestVote = entry.key
                highestVoteCount = entry.value
                tied = false
            } else if (entry.value > highestVoteCount) {
                tied = false
                highestVote = entry.key
                highestVoteCount = entry.value
            } else if (entry.value == highestVoteCount) {
                tied = true
            }
        }

        if (tied) {
            onMeetingEnd = { server.sendMessage(Component.text("Vote tied (skip)")) }
            return
        }

        when (highestVote) {
            is Vote.PlayerVote -> {
                onMeetingEnd = a@{
                    val sessionPlayer = highestVote.player

                    val player = sessionPlayer.player
                    if (player == null) {
                        server.sendMessage(Component.text("${sessionPlayer.name} has been voted. "))
                        session.onPlayerKilled(sessionPlayer, mutableListOf(), "Voted", mutableMapOf())
                    } else {
                        server.sendMessage(
                            Component.text("${sessionPlayer.name} has been voted. ")
                                .append(
                                    Component
                                        .text("FINISH THEM!!!")
                                        .color(Colors.VERY_RED.textColor)
                                        .decorate(TextDecoration.BOLD)
                                )
                        )

                        server.playSound(Sound.sound {
                            it.source(Sound.Source.AMBIENT)
                            it.pitch(1f)
                            it.type(NamespacedKey.minecraft("ambient.crimson_forest.mood"))
                        })

                        player.clearActivePotionEffects()
                        player.addPotionEffect(
                            PotionEffect(
                                PotionEffectType.SLOWNESS,
                                session.settings.meetingCooldownTicks,
                                2,
                                false,
                                false,
                                true
                            )
                        )
                        player.addPotionEffect(
                            PotionEffect(
                                PotionEffectType.WEAKNESS,
                                session.settings.meetingCooldownTicks,
                                2,
                                false,
                                false,
                                true
                            )
                        )
                        player.addPotionEffect(
                            PotionEffect(
                                PotionEffectType.GLOWING,
                                session.settings.meetingCooldownTicks,
                                2,
                                false,
                                false,
                                true
                            )
                        )
                        player.playHurtAnimation(0f)
                        player.absorptionAmount = 0.0
                        player.health = 5.0
                        player.foodLevel = min(player.foodLevel, 10)
                        sessionPlayer.returnLocation = null
                    }
                }
            }

            is Vote.EndGame -> {
                onMeetingEnd = {
                    server.sendMessage(Component.text("Session voted to end"))
                    session.sessionManager.endSession()
                }
            }

            is Vote.Skip -> {
                onMeetingEnd = {
                    server.sendMessage(Component.text("Meeting skipped"))
                }
            }

            else -> error("unreachable")
        }
    }

    private fun endMeeting() {
        this.state = State.Finished
        onMeetingEnd()

        for (player in server.onlinePlayers) {
            player.worldBorder = player.world.worldBorder
        }

        for ((_, sessionPlayer) in session.alivePlayers) {
            sessionPlayer.role.onMeetingEnd(this.session, this, sessionPlayer, turnout ?: mapOf())

            if (sessionPlayer.returnLocation != null) {
                sessionPlayer.canReturn = true

                sessionPlayer.player?.let { player ->
                    val bedrockConnection = FloodgateApi.getPlayer(player.uniqueId)

                    if (bedrockConnection == null) {
                        javaBackMenu(player)
                    } else {
                        bedrockBackMenu(player, bedrockConnection)
                    }
                }
            }
        }
    }

    fun onVote(voter: Player, vote: Vote): Component {
        if (state != State.Voting) {
            return Component.text("You may not vote yet")
        }

        if (votes.containsKey(voter.uniqueId)) {
            return Component.text("You've already voted")
        }

        votes[voter.uniqueId] = vote


        server.sendMessage(
            Component
                .text("${voter.name} has voted ")
                .append(
                    Component
                        .text("[${votes.size}/${session.alivePlayers.size}]")
                        .color(Colors.LIGHT_GRAY.textColor)
                )
        )

        if (votes.size == session.alivePlayers.size) {
            this.startSuspense()
        }

        return when (vote) {
            is Vote.PlayerVote -> {
                Component.text("You have voted ${vote.player.name}")
            }
            Vote.Skip ->
                Component.text("You have voted to skip")
            Vote.EndGame ->
                Component.text("You have voted to end the game")
        }
    }

    fun onServerTicked() {
        timing--

        when (state) {
            State.Discussion -> {
                val seconds = ((timing - 1) / 20) + 1

                bellLabel.text(
                    Component
                        .text("Voting starts in ${seconds.seconds}")
                        .color(Colors.SURVIVOR_TEAL.textColor)
                )
                bellLabel.backgroundColor = Colors.MIDNIGHT_CYAN.color(100)
                bellLabel.transformation = Transformation()

                if (timing <= 0) {
                    this.startVoting()
                }
            }

            State.Voting -> {
                val seconds = ((timing - 1) / 20) + 1

                bellLabel.text(
                    Component
                        .text("Voting ends in ${seconds.seconds}")
                        .color(Colors.SURVIVOR_TEAL.textColor)
                )
                bellLabel.backgroundColor = Colors.MIDNIGHT_CYAN.color(100)
                bellLabel.transformation = Transformation()

                if (timing <= 0) {
                    this.startSuspense()
                }
            }

            State.Suspense -> {
                timing--

                if (timing <= 0) {
                    this.endMeeting()
                }
            }

            else -> {}
        }
    }

    enum class State {
        Discussion,
        Voting,
        Suspense,
        Finished
    }
}