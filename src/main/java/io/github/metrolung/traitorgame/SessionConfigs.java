package io.github.metrolung.traitorgame;

import org.bukkit.Location;

public record SessionConfigs(
    int meetingCooldown,
    int discussionTime,
    int votingTime,
    int traitorCount,
    Location bellLocation
) {}