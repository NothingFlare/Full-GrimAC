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
    private final Location originalLocation;
    private Location verificationLocation;
    private final long startTimeMs;
    private final int totalTicks;
    private int ticksElapsed;
    private volatile boolean completed;
    private boolean brandValidated;

    // Sonar Physics & Hitbox Collision State
    private double lastY;
    private double lastDeltaY;
    private double platformY = 100.0;
    private double blockHeight = 0.5; // Smooth stone slab height
    private int fallTicks = 0;
    private boolean gravityValidated = false;
    private boolean hitboxValidated = false;
    private boolean hasMoved = false;
    private boolean failed = false;
    private String failReason;

    public PlayerVerificationSession(Player player, String ip, int totalTicks, Location originalLocation) {
        this.player = player;
        this.uuid = player.getUniqueId();
        this.username = player.getName();
        this.ip = ip;
        this.originalLocation = originalLocation != null ? originalLocation.clone() : player.getLocation().clone();
        this.startTimeMs = System.currentTimeMillis();
        this.totalTicks = Math.max(20, totalTicks);
        this.ticksElapsed = 0;
        this.completed = false;
        this.brandValidated = false;
        this.lastY = player.getLocation().getY();
        this.lastDeltaY = 0;
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

    public Location getOriginalLocation() {
        return originalLocation;
    }

    public Location getVerificationLocation() {
        return verificationLocation;
    }

    public void setVerificationLocation(Location verificationLocation) {
        this.verificationLocation = verificationLocation;
        if (verificationLocation != null) {
            this.lastY = verificationLocation.getY();
        }
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

    public double getLastY() {
        return lastY;
    }

    public void setLastY(double lastY) {
        this.lastY = lastY;
    }

    public double getLastDeltaY() {
        return lastDeltaY;
    }

    public void setLastDeltaY(double lastDeltaY) {
        this.lastDeltaY = lastDeltaY;
    }

    public double getPlatformY() {
        return platformY;
    }

    public void setPlatformY(double platformY) {
        this.platformY = platformY;
    }

    public double getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(double blockHeight) {
        this.blockHeight = blockHeight;
    }

    public int getFallTicks() {
        return fallTicks;
    }

    public void incrementFallTicks() {
        this.fallTicks++;
    }

    public boolean isGravityValidated() {
        return gravityValidated;
    }

    public void setGravityValidated(boolean gravityValidated) {
        this.gravityValidated = gravityValidated;
    }

    public boolean isHitboxValidated() {
        return hitboxValidated;
    }

    public void setHitboxValidated(boolean hitboxValidated) {
        this.hitboxValidated = hitboxValidated;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed, String reason) {
        this.failed = failed;
        this.failReason = reason;
    }

    public String getFailReason() {
        return failReason;
    }
}
