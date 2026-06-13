package io.github.metrolung.traitorgame;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
    private final Map<UUID, SessionPlayer> livingPlayers;
    private final Map<UUID, SessionPlayer> deadPlayers;

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
        Map<UUID, SessionPlayer> players = new HashMap<>();

        List<Role> roles = Arrays.asList(new Role[server.getOnlinePlayers().size()]);
        Collections.fill(roles, new Role.Survivor());
        for (int i = 0; i < configs.traitorCount(); i++) {
            if (i >= roles.size()) {
                break;
            }

            roles.set(i, new Role.Traitor());
        }

        Collections.shuffle(roles);

        int roleIdx = 0;
        for (Player player : server.getOnlinePlayers()) {
            var sessionPlayer = new SessionPlayer(
                player.getUniqueId(),
                player.getName(),
                roles.get(roleIdx),
                server
            );
            players.put(player.getUniqueId(), sessionPlayer);

            roleIdx++;
        }

        this.bellLabel = null;
        this.server = server;
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.livingPlayers = players;
        this.deadPlayers = new HashMap<>();
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

        for (SessionPlayer player : livingPlayers.values()) {
            Player onlinePlayer = player.getPlayer();

            if (onlinePlayer == null) {
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

    private @Nullable RoleGroup determineWinner() {
        if (livingPlayers.isEmpty()) {
            return null;
        }

        for (SessionPlayer player : livingPlayers.values()) {
            if (player.role.getRoleGroup() instanceof RoleGroup.Traitors) {
                return player.role.getRoleGroup();
            }
        }

        return livingPlayers.values().iterator().next().role.getRoleGroup();
    }

    private void startMeeting() {
        this.meetingCooldown = configs.meetingCooldown();
        this.activeMeeting = new Meeting(this, configs.discussionTime(), configs.votingTime());
        this.activeMeeting.startMeeting();
    }

    public void onSessionEnd() {
        var blockData = server.createBlockData(Material.AIR);
        configs.bellLocation().getWorld().setBlockData(configs.bellLocation(), blockData);
        endGameText();

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

    public boolean isPlayerAlive(Player player) {
        return isPlayerAlive(player.getUniqueId());
    }

    public boolean isPlayerAlive(UUID player) {
        return livingPlayers.containsKey(player);
    }

    public void onPlayerDeath(Player player) {
        if (livingPlayers.containsKey(player.getUniqueId())) {
            deadPlayers.put(player.getUniqueId(), livingPlayers.remove(player.getUniqueId()));
        }
    }

    public void onPlayerInteractWithBlock(Player player, Location block) {
        boolean targetIsBell = block.equals(configs.bellLocation().toBlockLocation());
        boolean meetingNotOnCooldown = meetingCooldown <= 0;
        if (isPlayerAlive(player) && targetIsBell && meetingNotOnCooldown) {
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

        if (!isPlayerAlive(voter)) {
            return Component.text("You are not alive");
        }

        return activeMeeting.onSkip(voter);
    }

    public Component onVoteEndGame(Player voter) {
        if (activeMeeting == null) {
            return Component.text("There is no meeting active");
        }

        if (!isPlayerAlive(voter)) {
            return Component.text("You are not alive");
        }

        return activeMeeting.onVoteEndGame(voter);
    }

    public Component onVote(Player voter, String name) {
        if (activeMeeting == null) {
            return Component.text("There is no meeting active");
        }

        if (!isPlayerAlive(voter)) {
            return Component.text("You are not alive");
        }

        SessionPlayer voted = null;

        for (SessionPlayer player : livingPlayers.values()) {
            if (player.name().equalsIgnoreCase(name)) {
                voted = player;
                break;
            }
        }

        if (voted == null) {
            return Component.text("Target is not alive");
        }

        return activeMeeting.onVote(voter, voted);
    }

    public @Nullable Role getRole(Player player) {
        SessionPlayer sessionPlayer = this.livingPlayers.get(player.getUniqueId());
        if (sessionPlayer == null)
            return null;
        return sessionPlayer.role;
    }

    public void endGameText() {
        int i = 0;

        var winner = determineWinner();
        if (winner != null) {
            server.sendMessage(winner.winMessage());
        } else {
            server.sendMessage(Component.text("Draw...").color(TextColor.color(0xFF8E63)));
        }

        for (var player : livingPlayers.values()) {
            if (player.getPlayer() == null) {
                continue;
            }

            server.getScheduler().runTaskLater(plugin, task -> {
                server.sendMessage(
                    Component.text(Objects.requireNonNull(player.getPlayer().getName())).append(
                        Component.text(" was a ").append(
                            player.role.getName()
                        )
                    )
                );
            }, i * 10L);

            i++;
        }
    }

    public sealed interface RoleGroup {
        @NotNull TextComponent winMessage();

        final class Traitors implements RoleGroup {
            private Traitors() {}
            public static final Traitors INSTANCE = new Traitors();

            @Override
            public @NotNull TextComponent winMessage() {
                return Component
                    .text("Traitors win...")
                    .color(TextColor.color(0x7A1D2E));
            }
        }

        final class Survivors implements RoleGroup {
            private Survivors() {}
            public static final Survivors INSTANCE = new Survivors();

            @Override
            public @NotNull TextComponent winMessage() {
                return Component
                    .text("Survivors win!")
                    .color(TextColor.color(0x01FFCC));
            }
        }
    }

    public interface Role {
        @NotNull TextComponent getName();
        @NotNull RoleGroup getRoleGroup();

        final class Traitor implements Role {
            @Override
            public @NotNull TextComponent getName() {
                return Component
                        .text("Traitor")
                        .color(TextColor.color(0x7A1D2E));
            }

            @Override
            public @NotNull RoleGroup getRoleGroup() {
                return RoleGroup.Survivors.INSTANCE;
            }
        }

        final class Survivor implements Role {
            @Override
            public @NotNull TextComponent getName() {
                return Component
                    .text("Survivor")
                    .color(TextColor.color(0x01FFCC));
            }

            @Override
            public @NotNull RoleGroup getRoleGroup() {
                return RoleGroup.Survivors.INSTANCE;
            }
        }
//        Traitor,
//        Innocent;
//
//        public @NotNull TextComponent getName() {
//            return switch (this) {
//                case Traitor -> Component
//                    .text("Traitor")
//                    .color(TextColor.color(0x7A1D2E))
//                    .shadowColor(ShadowColor.shadowColor(0x290A13));
//                case Innocent -> Component
//                    .text("Survivor")
//                    .color(TextColor.color(0x1FFCC))
//                    .shadowColor(ShadowColor.shadowColor(0xFFFFFF));
//            };
//        }
    }

    private record SessionPlayer(
        @NotNull UUID player,
        @NotNull String name,
        @NotNull Role role,
        @NotNull Server server
    ) {
        public @Nullable Player getPlayer() {
            return server.getPlayer(player);
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
                    if (playerVote.player().getPlayer() != null) {
                        playerVote.player().getPlayer().kill();
                    }

                    server.sendMessage(
                        Component.text(playerVote.player.name)
                            .append(Component.text(" has been voted out"))
                    );
                }
                case Vote.Skip skip -> {
                    server.sendMessage(Component.text("Meeting skipped"));
                }
            }
        }

        private Component onVote(Player voter, SessionPlayer voted) {
            if (votes.containsKey(voter)) {
                return Component.text("You've already voted");
            }

            votes.put(voter, new Vote.PlayerVote(voted));

            return Component
                .text("Your vote for ")
                .append(Component.text(voted.name()))
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
            record PlayerVote(SessionPlayer player) implements Vote {}
            record Skip() implements Vote {}
            record EndGame() implements Vote {}
        }
    }
}