package io.github.metrolung.traitorgame;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;

public class Session implements Listener {
    private final Server server;
    private final SessionManager sessionManager;
    private final Plugin plugin;
    private final Map<OfflinePlayer, SessionPlayer> players;

    private final Random random = new Random();


    public Session(Server server, Plugin plugin, SessionManager sessionManager, int traitorCount) {
        Map<OfflinePlayer, SessionPlayer> players = new HashMap<>();

        List<Role> roles = Arrays.asList(new Role[server.getOnlinePlayers().size()]);
        Collections.fill(roles, Role.Innocent);
        for (int i = 0; i < traitorCount; i++) {
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

        this.server = server;
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.players = players;
    }

    public void onSessionStart() {
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

    public void onSessionEnd() {
        displayRoles();
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent death) {
        if (players.containsKey(death.getPlayer())) {
            players.get(death.getPlayer()).alive = false;
        }
    }

    public @Nullable Role getRole(OfflinePlayer player) {
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
        private @NotNull Role role;

        private SessionPlayer(@NotNull OfflinePlayer player, boolean alive, @NotNull Role role) {
            this.player = player;
            this.alive = alive;
            this.role = role;
        }
    }
}