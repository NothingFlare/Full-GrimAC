/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.platform.bukkit.antibot;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerVerificationSession {

    private final Player player;
    private final UUID uuid;
    private final String username;
    private final String ip;
    private final Location startLocation;
    private final long startTimeMs;
    private final int totalTicks;
    private int ticksElapsed;
    private volatile boolean completed;
    private boolean brandValidated;

    public PlayerVerificationSession(Player player, String ip, int totalTicks) {
        this.player = player;
        this.uuid = player.getUniqueId();
        this.username = player.getName();
        this.ip = ip;
        this.startLocation = player.getLocation().clone();
        this.startTimeMs = System.currentTimeMillis();
        this.totalTicks = Math.max(20, totalTicks);
        this.ticksElapsed = 0;
        this.completed = false;
        this.brandValidated = false;
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getIp() {
        return ip;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    public int getTicksElapsed() {
        return ticksElapsed;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isBrandValidated() {
        return brandValidated;
    }

    public void setBrandValidated(boolean brandValidated) {
        this.brandValidated = brandValidated;
    }

    public void incrementTick() {
        ticksElapsed++;
    }

    public double getSecondsRemaining() {
        int left = Math.max(0, totalTicks - ticksElapsed);
        return left / 20.0;
    }
}
