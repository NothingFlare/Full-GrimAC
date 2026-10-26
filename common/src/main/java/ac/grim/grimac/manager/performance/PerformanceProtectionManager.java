/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.manager.performance;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;

import java.lang.reflect.Method;

public final class PerformanceProtectionManager {

    private static boolean enabled = true;
    private static double lowTpsThreshold = 15.0;
    private static double recoverTpsThreshold = 17.0;
    private static boolean disableInventoryChecks = true;
    private static boolean disableKillAuraBots = true;
    private static boolean simplifyAntiXray = true;
    private static boolean throttleSecondaryChecks = true;
    private static boolean broadcastToOps = true;

    private static volatile boolean lowTpsActive = false;
    private static double lastTps = 20.0;
    private static long lastStateChangeTime = 0;

    private PerformanceProtectionManager() {
    }

    public static void init() {
        reload();
    }

    public static void reload() {
        try {
            var config = GrimAPI.INSTANCE.getConfigManager().getConfig();
            enabled = config.getBooleanElse("performance-protection.enabled", true);
            lowTpsThreshold = config.getDoubleElse("performance-protection.low-tps-threshold", 15.0);
            recoverTpsThreshold = config.getDoubleElse("performance-protection.recover-tps-threshold", 17.0);
            disableInventoryChecks = config.getBooleanElse("performance-protection.disable-inventory-checks", true);
            disableKillAuraBots = config.getBooleanElse("performance-protection.disable-killaura-bots", true);
            simplifyAntiXray = config.getBooleanElse("performance-protection.simplify-antixray", true);
            throttleSecondaryChecks = config.getBooleanElse("performance-protection.throttle-secondary-checks", true);
            broadcastToOps = config.getBooleanElse("performance-protection.broadcast-to-ops", true);
        } catch (Throwable ignored) {
        }
    }

    public static void update(int currentTick) {
        if (!enabled) {
            lowTpsActive = false;
            return;
        }

        // Check TPS every 20 ticks (1 second)
        if (currentTick % 20 != 0) return;

        double tps = GrimAPI.INSTANCE.getPlatformServer().getTPS();
        if (Double.isNaN(tps) || tps <= 0.0) return;

        double prevTps = lastTps;
        lastTps = tps;
        long now = System.currentTimeMillis();

        if (!lowTpsActive) {
            // Drop below 15.0
            if (tps < lowTpsThreshold) {
                lowTpsActive = true;
                lastStateChangeTime = now;
                String logMsg = String.format("[PerformanceProtection] Server TPS dropped to %.2f (< %.1f). Enabling performance protection mode (throttling checks, disabling bots/inventory).", tps, lowTpsThreshold);
                LogUtil.warn(logMsg);
                if (broadcastToOps) {
                    broadcastToStaff(String.format("&e[PerformanceProtection] &7Server TPS dropped to &c%.2f &7(< %.1f). Enabling performance protection mode (throttling checks, disabling bots/inventory).", tps, lowTpsThreshold));
                }
            }
        } else {
            // Recover: TPS >= 17.0 AND rising/stable (tps >= prevTps) AND at least 3 seconds in recovery
            if (tps >= recoverTpsThreshold && tps >= prevTps && (now - lastStateChangeTime > 3000)) {
                lowTpsActive = false;
                lastStateChangeTime = now;
                String logMsg = String.format("[PerformanceProtection] Server TPS recovered to %.2f (>= %.1f). Performance protection mode disabled, all checks re-enabled.", tps, recoverTpsThreshold);
                LogUtil.info(logMsg);
                if (broadcastToOps) {
                    broadcastToStaff(String.format("&e[PerformanceProtection] &7Server TPS recovered to &a%.2f &7(>= %.1f). Performance protection mode disabled, all checks re-enabled.", tps, recoverTpsThreshold));
                }
            }
        }
    }

    private static void broadcastToStaff(String text) {
        try {
            Component component = MessageUtil.miniMessage("%prefix% " + text);
            for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                if (isOperatorOrStaff(player)) {
                    player.sendMessage(component);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isOperatorOrStaff(GrimPlayer player) {
        if (player.hasPermission("grim.performance") || player.hasPermission("grim.alerts") || player.hasPermission("grim.admin")) {
            return true;
        }
        if (player.platformPlayer != null) {
            try {
                Object nativeObj = player.platformPlayer.getNative();
                if (nativeObj != null) {
                    Method isOpMethod = nativeObj.getClass().getMethod("isOp");
                    return (boolean) isOpMethod.invoke(nativeObj);
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    public static boolean isLowTpsActive() {
        return enabled && lowTpsActive;
    }

    public static boolean shouldSkipInventory() {
        return enabled && lowTpsActive && disableInventoryChecks;
    }

    public static boolean shouldSkipKillAuraBots() {
        return enabled && lowTpsActive && disableKillAuraBots;
    }

    public static boolean shouldSimplifyAntiXray() {
        return enabled && lowTpsActive && simplifyAntiXray;
    }

    public static boolean shouldThrottleSecondary(int tick) {
        return enabled && lowTpsActive && throttleSecondaryChecks && (tick % 2 != 0);
    }

    public static double getLastTps() {
        return lastTps;
    }
}
