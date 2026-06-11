package io.github.metrolung.traitorgame;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;

public class Session {
    private final Server server;
    private final SessionManager sessionManager;
    private final Plugin plugin;
    private final Map<Player, SessionPlayer> players;

    private final SessionConfigs configs;
    private int meetingCooldown;
    private @Nullable ArmorStand bellLabel;
    private @Nullable Meeting activeMeeting;

    public Session(
        Server server,
        Plugin plugin,
        SessionManager sessionManager,
        SessionConfigs configs
    ) {
        Map<Player, SessionPlayer> players = new HashMap<>();

        List<Role> roles = Arrays.asList(new Role[server.getOnlinePlayers().size()]);
        Collections.fill(roles, Role.Innocent);
        for (int i = 0; i < configs.traitorCount(); i++) {
            if (i >= roles.size()) {
                break;
            }

            roles.set(i, Role.Traitor);
        }

        Collections.shuffle(roles);

        int roleIdx = 0;
        for (Player player : server.getOnlinePlayers()) {
            var sessionPlayer = new SessionPlayer(player, true, roles.get(roleIdx));
            players.put(player, sessionPlayer);

            roleIdx++;
        }

        this.bellLabel = null;
        this.server = server;
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.players = players;
        this.configs = configs;
    }

    public void onSessionStart() {
        var blockData = server.createBlockData(Material.BELL);
        configs.bellLocation().getWorld().setBlockData(configs.bellLocation(), blockData);

        this.bellLabel = configs.bellLocation().getWorld().createEntity(configs.bellLocation().add(0, 2, 0), ArmorStand.class);
        bellLabel.setInvisible(true);
        bellLabel.teleport(configs.bellLocation());
        bellLabel.setAI(false);
        bellLabel.setCustomNameVisible(true);
        bellLabel.customName(Component.text("Initiate Meeting"));

        for (SessionPlayer player : players.values()) {
            if (!(player.player instanceof Player onlinePlayer)) {
                continue;
            }

            onlinePlayer.sendTitlePart(
                TitlePart.TIMES,
                Title.Times.times(
                    Duration.ofSeconds(0),
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(0)
                )
            );

            onlinePlayer.sendTitlePart(
                TitlePart.TITLE,
                Component.text("")
            );

            onlinePlayer.sendTitlePart(
                TitlePart.SUBTITLE,
                Component.text("You are...")
            );

            server.getScheduler().runTaskLater(plugin, task -> {
                onlinePlayer.sendTitlePart(
                    TitlePart.TIMES,
                    Title.Times.times(
                        Duration.ofSeconds(0),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(1)
                    )
                );

                onlinePlayer.addPotionEffect(PotionEffectType.BLINDNESS.createEffect(60, 1));

                onlinePlayer.sendTitlePart(
                    TitlePart.TITLE,
                    player.role.getName().decorate(TextDecoration.BOLD)
                );
                onlinePlayer.sendTitlePart(
                    TitlePart.SUBTITLE,
                    Component.text("Shh!").decorate(TextDecoration.ITALIC).color(TextColor.color(0x777777))
                );

                onlinePlayer.sendTitlePart(
                    TitlePart.TITLE,
                    player.role.getName().decorate(TextDecoration.BOLD)
                );

                onlinePlayer.sendMessage(Component.text("Clear chat with F3 + D"));
                onlinePlayer.sendMessage(Component.text("You can view your role at any point with /role"));

            }, 100L);
        }
    }

    private void startMeeting() {
        this.meetingCooldown = configs.meetingCooldown();
        this.activeMeeting = new Meeting(this, configs.discussionTime(), configs.votingTime());
        this.activeMeeting.startMeeting();
    }

    public void onSessionEnd() {
        var blockData = server.createBlockData(Material.AIR);
        configs.bellLocation().getWorld().setBlockData(configs.bellLocation(), blockData);
        displayRoles();

        if (bellLabel != null) {
            bellLabel.remove();
            bellLabel = null;
        }
    }

    public void onServerTicked() {
        if (activeMeeting != null) {
            activeMeeting.onServerTicked();
            if (activeMeeting.state == Meeting.MeetingState.Finished) {
                activeMeeting = null;
            }
        } else {
            if (meetingCooldown > 0) {
                meetingCooldown--;
            }
        }
    }

    public void onPlayerDeath(Player player) {
        if (players.containsKey(player)) {
            players.get(player).alive = false;
        }
    }

    public void onPlayerInteractWithBlock(Player player, Location block) {
        boolean playerIsAlive = players.containsKey(player) && players.get(player).alive;
        boolean targetIsBell = block.equals(configs.bellLocation());
        boolean meetingNotOnCooldown = meetingCooldown <= 0;
        if (playerIsAlive && targetIsBell && meetingNotOnCooldown) {
            startMeeting();
        }
    }

    public boolean onBlockRemoved(Location blockLocation) {
        return blockLocation.equals(configs.bellLocation());
    }

    public Component onSkip(Player voter) {
        if (activeMeeting == null) {
            return Component.text("There is no meeting active");
        }

        boolean playerIsAlive = players.containsKey(voter) && players.get(voter).alive;

        if (!playerIsAlive) {
            return Component.text("You are not alive");
        }

        return activeMeeting.onSkip(voter);
    }

    public Component onVoteEndGame(Player voter) {
        if (activeMeeting == null) {
            return Component.text("There is no meeting active");
        }

        boolean playerIsAlive = players.containsKey(voter) && players.get(voter).alive;

        if (!playerIsAlive) {
            return Component.text("You are not alive");
        }

        return activeMeeting.onVoteEndGame(voter);
    }

    public Component onVote(Player voter, String name) {
        if (activeMeeting == null) {
            return Component.text("There is no meeting active");
        }

        boolean playerIsAlive = players.containsKey(voter) && players.get(voter).alive;

        if (!playerIsAlive) {
            return Component.text("You are not alive");
        }

        Player voted = null;

        for (Player target : players.keySet()) {
            boolean targetIsAlive = players.containsKey(target) && players.get(target).alive;
            if (targetIsAlive && target.getName().equalsIgnoreCase(name)) {
                voted = target;
                break;
            }
        }

        if (voted == null) {
            return Component.text("Target is not alive");
        }

        return activeMeeting.onVote(voter, voted);
    }

    public @Nullable Role getRole(Player player) {
        SessionPlayer sessionPlayer = this.players.get(player);
        if (sessionPlayer == null)
            return null;
        return sessionPlayer.role;
    }

    public void displayRoles() {
        int i = 0;
        for (var player : players.values()) {
            server.getScheduler().runTaskLater(plugin, task -> {
                server.sendMessage(
                    Component.text(Objects.requireNonNull(player.player.getName())).append(
                        Component.text(" was a ").append(
                            player.role.getName()
                        )
                    )
                );
            }, i * 10L);

            i++;
        }
    }

    public enum Role {
        Traitor,
        Innocent;

        public @NotNull TextComponent getName() {
            return switch (this) {
                case Traitor -> Component
                    .text("Traitor")
                    .color(TextColor.color(0x7A1D2E))
                    .shadowColor(ShadowColor.shadowColor(0x290A13));
                case Innocent -> Component
                    .text("Survivor")
                    .color(TextColor.color(0x1FFCC))
                    .shadowColor(ShadowColor.shadowColor(0xFFFFFF));
            };
        }
    }

    private static class SessionPlayer {
        private final @NotNull OfflinePlayer player;
        private boolean alive;
        private final @NotNull Role role;

        private SessionPlayer(@NotNull OfflinePlayer player, boolean alive, @NotNull Role role) {
            this.player = player;
            this.alive = alive;
            this.role = role;
        }
    }


    private static class Meeting {
        private final Session session;
        private final Server server;
        private MeetingState state;
        private int discussionTicks;
        private int votingTicks;

        private final Map<OfflinePlayer, Vote> votes;

        private Meeting(Session session, int discussionTicks, int votingTicks) {
            this.session = session;
            this.server = session.server;
            this.state = MeetingState.Uninit;
            this.discussionTicks = discussionTicks;
            this.votingTicks = votingTicks;
            this.votes = new HashMap<>();
        }

        private void startMeeting() {
            server.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500)));
            server.sendTitlePart(TitlePart.TITLE, Component.text("MEETING!").color(TextColor.color(255, 0, 0)));
            startDiscussion();
        }

        private void startDiscussion() {
            this.state = MeetingState.Discussion;
            server.sendMessage(Component.text("Discussion started"));
        }

        private void startVoting() {
            this.state = MeetingState.Voting;
            server.sendMessage(Component.text("Voting started"));
        }

        private void endMeeting() {
            this.state = MeetingState.Finished;

            Map<Vote, Integer> voteCounts = new HashMap<>();

            for (Vote vote : votes.values()) {
                voteCounts.put(vote, voteCounts.getOrDefault(vote, 0) + 1);
            }

            Vote highestVote = null;
            int highestVoteCount = 0;
            boolean tied = true;

            for (Map.Entry<Vote, Integer> entry : voteCounts.entrySet()) {
                if (highestVote == null) {
                    highestVote = entry.getKey();
                    highestVoteCount = entry.getValue();
                    tied = false;
                } else if (entry.getValue() > highestVoteCount) {
                    tied = false;
                    highestVote = entry.getKey();
                    highestVoteCount = entry.getValue();
                } else if (entry.getValue() == highestVoteCount) {
                    tied = true;
                }
            }

            if (tied) {
                server.sendMessage(Component.text("Vote tied (skip)"));
                return;
            }
            switch (highestVote) {
                case Vote.EndGame endGame -> {
                    server.sendMessage(Component.text("Session voted to end"));
                    session.sessionManager.endSession(session);
                }
                case Vote.PlayerVote playerVote -> {
                    playerVote.player().kill();
                }
                case Vote.Skip skip -> {
                    server.sendMessage(Component.text("Meeting skipped"));
                }
            }
        }

        private Component onVote(Player voter, Player voted) {
            if (votes.containsKey(voter)) {
                return Component.text("You've already voted");
            }

            votes.put(voter, new Vote.PlayerVote(voted));

            return Component
                .text("Your vote for ")
                .append(Component.text(Objects.requireNonNull(voted.getName())))
                .append(Component.text(" has been cast"));
        }

        private Component onSkip(Player voter) {
            if (votes.containsKey(voter)) {
                return Component.text("You've already voted");
            }

            votes.put(voter, new Vote.Skip());

            return Component.text("You have skipped");
        }

        private Component onVoteEndGame(Player voter) {
            if (votes.containsKey(voter)) {
                return Component.text("You've already voted");
            }

            votes.put(voter, new Vote.EndGame());

            return Component.text("You have skipped");
        }

        private void onServerTicked() {
            switch (state) {
                case Discussion -> {
                    discussionTicks--;

                    server.sendActionBar(Component
                        .text("Voting starts in ")
                        .append(Component.text(((discussionTicks - 1) / 20) + 1))
                        .append(Component.text(" seconds"))
                        .color(TextColor.color(0, 200,  200))
                    );

                    if (discussionTicks <= 0) {
                        this.startVoting();
                    }
                }
                case Voting -> {
                    votingTicks--;

                    server.sendActionBar(Component
                        .text("Voting ends in ")
                        .append(Component.text(((votingTicks - 1) / 20) + 1))
                        .append(Component.text(" seconds"))
                        .color(TextColor.color(0, 200,  200))
                    );

                    if (votingTicks <= 0) {
                        this.endMeeting();
                    }
                }
            }
        }

        private enum MeetingState {
            Uninit,
            Discussion,
            Voting,
            Finished
        }

        private sealed interface Vote {
            record PlayerVote(Player player) implements Vote {}
            record Skip() implements Vote {}
            record EndGame() implements Vote {}
        }
    }
}