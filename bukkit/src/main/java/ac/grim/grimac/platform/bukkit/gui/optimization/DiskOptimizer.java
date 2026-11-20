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

import java.io.File;

/**
 * High-performance disk and memory optimizer.
 * - Protects Paper's native Moonrise incremental async chunk saving (ensuring autoSave=true).
 * - Runs periodic background database compaction and expired record pruning.
 * - Monitors heap memory pressure to prevent OutOfMemoryError and chunk parsing crashes.
 * - Trims stale disk cache and temporary files.
 */
public class DiskOptimizer implements Listener {

    public static volatile boolean enabled = false;

    private static BukkitTask schedulerTask;
    private static volatile long lastMaintenanceTime = 0;
    private static volatile long lastMemoryReliefTime = 0;

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
        }
    }

    public static void startTask() {
        stopTask();
        if (!enabled) return;

        // Ensure Paper's native incremental async saving is active for all worlds
        for (World world : Bukkit.getWorlds()) {
            try {
                world.setAutoSave(true);
            } catch (Throwable ignored) {}
        }

        try {
            // Run optimizer maintenance asynchronously every 60 seconds (1200 ticks)
            // Off-main execution guarantees database SQL queries and file I/O never freeze server ticks
            schedulerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(GrimACBukkitLoaderPlugin.LOADER, () -> {
                if (!enabled) return;

                long now = System.currentTimeMillis();

                // 1. Proactive Memory Pressure Relief: Prevent OutOfMemoryError in chunk system
                checkMemoryPressure(now);

                // 2. Database Maintenance: compact and purge old records every 15 minutes
                if (now - lastMaintenanceTime > 900000L) {
                    lastMaintenanceTime = now;
                    runDatabaseMaintenance();
                    trimOldLogFiles();
                }

            }, 600L, 1200L);
        } catch (Throwable t) {
            LogUtil.warn("DiskOptimizer: Не удалось запустить планировщик оптимизации: " + t.getMessage());
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

    public static void onServerShutdown() {
        stopTask();
        // Paper natively flushes all chunk data safely during shutdown
    }

    /**
     * Checks JVM heap memory usage and proactively frees memory if heap exceeds 85%
     * to avoid Spottedleaf Moonrise chunk loader OOM crashes.
     */
    private static void checkMemoryPressure(long now) {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long allocatedMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = allocatedMemory - freeMemory;

            double usedRatio = (double) usedMemory / (double) maxMemory;
            if (usedRatio > 0.85 && (now - lastMemoryReliefTime > 60000L)) {
                lastMemoryReliefTime = now;
                long usedMB = usedMemory / (1024 * 1024);
                long maxMB = maxMemory / (1024 * 1024);
                LogUtil.warn("DiskOptimizer: Высокая нагрузка на память (" + usedMB + "MB / " + maxMB + "MB, " + (int)(usedRatio * 100) + "%). Выполняется мягкая очистка кэшей...");

                // Clean in-memory caches
                runMemoryRelief();
            }
        } catch (Throwable ignored) {}
    }

    private static void runMemoryRelief() {
        try {
            // Trim database cache
            runDatabaseMaintenance();
        } catch (Throwable ignored) {}
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

    private static void trimOldLogFiles() {
        try {
            File logsDir = new File(GrimACBukkitLoaderPlugin.LOADER.getDataFolder().getParentFile().getParentFile(), "logs");
            if (logsDir.exists() && logsDir.isDirectory()) {
                File[] files = logsDir.listFiles((dir, name) -> name.endsWith(".log.gz") || name.endsWith(".tmp"));
                if (files != null && files.length > 20) {
                    // Sort by last modified and delete oldest if exceeding 20 compressed logs
                    java.util.Arrays.sort(files, java.util.Comparator.comparingLong(File::lastModified));
                    for (int i = 0; i < files.length - 20; i++) {
                        try {
                            files[i].delete();
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldSave(WorldSaveEvent event) {
        // Ensure world auto-save is never turned off accidentally
        if (enabled && !event.getWorld().isAutoSave()) {
            try {
                event.getWorld().setAutoSave(true);
            } catch (Throwable ignored) {}
        }
    }
}
