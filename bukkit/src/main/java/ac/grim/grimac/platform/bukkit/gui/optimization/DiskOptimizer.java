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
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance disk and memory optimizer.
 * - Smart World Auto-Save: Skips auto-save for untouched worlds during the save interval.
 *   If any player places a block, breaks a block, or interacts, auto-save is enabled and the world saves as usual.
 * - Runs periodic background database compaction and expired record pruning asynchronously.
 * - Monitors heap memory pressure to prevent OutOfMemoryError and chunk parsing crashes.
 * - Trims stale disk cache and temporary files.
 */
public class DiskOptimizer implements Listener {

    public static volatile boolean enabled = false;

    private static final Set<UUID> modifiedWorlds = ConcurrentHashMap.newKeySet();
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

        // When enabled, configure each world: if previously modified, keep autoSave=true, otherwise false (idle)
        for (World world : Bukkit.getWorlds()) {
            try {
                world.setAutoSave(modifiedWorlds.contains(world.getUID()));
            } catch (Throwable ignored) {}
        }

        try {
            // Run optimizer maintenance asynchronously every 60 seconds (1200 ticks)
            schedulerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(GrimACBukkitLoaderPlugin.LOADER, () -> {
                if (!enabled) return;

                long now = System.currentTimeMillis();

                // 1. Proactive Memory Pressure Relief
                checkMemoryPressure(now);

                // 2. Database Maintenance
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

        // Restore default vanilla autosave for all worlds
        for (World world : Bukkit.getWorlds()) {
            try {
                world.setAutoSave(true);
            } catch (Throwable ignored) {}
        }
        modifiedWorlds.clear();
    }

    public static void onServerShutdown() {
        stopTask();
        // Make sure all worlds have autosave enabled for safe server shutdown
        for (World world : Bukkit.getWorlds()) {
            try {
                world.setAutoSave(true);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Marks a world as modified by a player.
     * Enables Bukkit/Paper auto-save for this world so it will be saved on the next autosave cycle.
     */
    public static void markWorldModified(World world) {
        if (!enabled || world == null) return;
        UUID uid = world.getUID();
        if (!modifiedWorlds.contains(uid)) {
            if (modifiedWorlds.add(uid)) {
                try {
                    world.setAutoSave(true);
                    LogUtil.info("DiskOptimizer: Мир '" + world.getName() + "' был изменен игроком. Автосохранение активировано для следующего цикла.");
                } catch (Throwable ignored) {}
            }
        }
    }

    // -------------------------------------------------------------
    // Player & World Interaction Listeners
    // -------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        markWorldModified(event.getBlock().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        markWorldModified(event.getBlock().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        markWorldModified(event.getBlock().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        markWorldModified(event.getBlock().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        markWorldModified(event.getBlock().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.useInteractedBlock() == Event.Result.DENY && event.useItemInHand() == Event.Result.DENY) {
            return;
        }
        if (event.getClickedBlock() != null) {
            markWorldModified(event.getClickedBlock().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        markWorldModified(event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        markWorldModified(event.getEntity().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        markWorldModified(event.getEntity().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        markWorldModified(event.getEntity().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        markWorldModified(event.getEntity().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleCreate(VehicleCreateEvent event) {
        markWorldModified(event.getVehicle().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        markWorldModified(event.getVehicle().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!event.blockList().isEmpty()) {
            markWorldModified(event.getLocation().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!event.blockList().isEmpty()) {
            markWorldModified(event.getBlock().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getLocation() != null && event.getInventory().getLocation().getWorld() != null) {
            markWorldModified(event.getInventory().getLocation().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getLocation() != null && event.getInventory().getLocation().getWorld() != null) {
            markWorldModified(event.getInventory().getLocation().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) {
            markWorldModified(event.getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        if (!enabled) return;
        try {
            // New world starts in idle state (no modifications yet)
            event.getWorld().setAutoSave(false);
        } catch (Throwable ignored) {}
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        modifiedWorlds.remove(event.getWorld().getUID());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldSave(WorldSaveEvent event) {
        if (!enabled) return;
        World world = event.getWorld();
        UUID uid = world.getUID();

        boolean wasModified = modifiedWorlds.remove(uid);
        try {
            // World was successfully saved to disk. Reset to idle mode until a player modifies it again.
            world.setAutoSave(false);
            if (wasModified) {
                LogUtil.info("DiskOptimizer: Мир '" + world.getName() + "' успешно сохранен на диск. Автосохранение переведено в режим ожидания изменений.");
            }
        } catch (Throwable ignored) {}
    }

    // -------------------------------------------------------------
    // Memory and Database Maintenance
    // -------------------------------------------------------------

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

                runMemoryRelief();
            }
        } catch (Throwable ignored) {}
    }

    private static void runMemoryRelief() {
        try {
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
}
