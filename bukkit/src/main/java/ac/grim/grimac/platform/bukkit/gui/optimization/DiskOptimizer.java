/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.platform.bukkit.gui.optimization;

import ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance disk and world auto-save optimizer.
 * Eliminates periodic disk I/O lag spikes by intelligently staggering world auto-saves,
 * deferring heavy disk flushes during low TPS, and running background database maintenance.
 */
public class DiskOptimizer implements Listener {

    public static volatile boolean enabled = false;

    private static BukkitTask schedulerTask;
    private static final AtomicInteger worldIndex = new AtomicInteger(0);
    private static volatile long lastMaintenanceTime = 0;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean state) {
        if (enabled == state) return;
        enabled = state;
        if (enabled) {
            startTask();
        } else {
            stopTask();
            restoreVanillaAutoSave();
        }
    }

    public static void startTask() {
        stopTask();
        if (!enabled) return;

        // Take over auto-save for all worlds to stagger them smoothly
        for (World world : Bukkit.getWorlds()) {
            try {
                world.setAutoSave(false);
            } catch (Throwable ignored) {}
        }

        // Run smart saving staggered every 40 seconds (800 ticks)
        try {
            schedulerTask = Bukkit.getScheduler().runTaskTimer(GrimACBukkitLoaderPlugin.LOADER, () -> {
                if (!enabled) return;

                // 1. TPS Check: Do NOT run disk saves during low TPS, heavy combat or bot attacks
                double tps = getRecentTps();
                if (tps < 18.5) {
                    // Defer saving until TPS stabilizes
                    return;
                }

                List<World> worlds = Bukkit.getWorlds();
                if (worlds.isEmpty()) return;

                // Save one world at a time to spread out disk I/O load evenly
                int idx = Math.abs(worldIndex.getAndIncrement() % worlds.size());
                World targetWorld = worlds.get(idx);

                long start = System.currentTimeMillis();
                try {
                    targetWorld.save();
                    long duration = System.currentTimeMillis() - start;
                    if (duration > 500) {
                        LogUtil.warn("DiskOptimizer: Мир '" + targetWorld.getName() + "' сохранялся на диск " + duration + "мс.");
                    }
                } catch (Throwable t) {
                    LogUtil.warn("DiskOptimizer: Ошибка при сохранении мира " + targetWorld.getName() + ": " + t.getMessage());
                }

                // 2. Database Maintenance: trigger background cleanup once every 30 minutes
                if (System.currentTimeMillis() - lastMaintenanceTime > 1800000L) {
                    lastMaintenanceTime = System.currentTimeMillis();
                    runDatabaseMaintenance();
                }

            }, 600L, 800L); // first run after 30s, then every 40s
        } catch (Throwable t) {
            LogUtil.warn("DiskOptimizer: Не удалось запустить планировщик сохранения: " + t.getMessage());
        }
    }

    public static void stopTask() {
        if (schedulerTask != null) {
            try {
                schedulerTask.cancel();
            } catch (Throwable ignored) {}
            schedulerTask = null;
        }
    }

    public static void restoreVanillaAutoSave() {
        for (World world : Bukkit.getWorlds()) {
            try {
                world.setAutoSave(true);
            } catch (Throwable ignored) {}
        }
    }

    public static void onServerShutdown() {
        stopTask();
        // Guaranteed flush of all worlds before shutdown to prevent any data loss
        for (World world : Bukkit.getWorlds()) {
            try {
                world.save();
            } catch (Throwable ignored) {}
        }
    }

    private static double getRecentTps() {
        try {
            double[] tps = Bukkit.getTPS();
            if (tps != null && tps.length > 0) {
                return tps[0];
            }
        } catch (Throwable ignored) {}
        return 20.0;
    }

    private static void runDatabaseMaintenance() {
        try {
            xyz.jonesdev.sonar.api.Sonar sonar = xyz.jonesdev.sonar.api.Sonar.get0();
            if (sonar != null && sonar.getVerifiedPlayerController() != null) {
                sonar.getVerifiedPlayerController().clearOld(
                    sonar.getVerifiedPlayerController().getMaximumAge(), false);
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldSave(WorldSaveEvent event) {
        // Monitored manual / external saves
    }
}
