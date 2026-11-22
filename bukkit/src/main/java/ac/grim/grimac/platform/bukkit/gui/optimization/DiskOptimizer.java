/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.platform.bukkit.gui.optimization;

import ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance disk and memory optimizer.
 *
 * Smart World Auto-Save:
 * - Keeps world autoSave=true during normal gameplay so Paper's ChunkMap.processUnloads()
 *   is NEVER blocked, eliminating memory leaks and OutOfMemoryError in Moonrise chunk system.
 * - On the exact tick when server auto-save executes, temporarily suspends auto-save
 *   ONLY for worlds where no players interacted, skipping unnecessary disk I/O and level.dat writes.
 * - Restores autoSave=true immediately on the very next tick.
 * - If any player modifies a world, it saves normally as always.
 *
 * Maintenance:
 * - Off-main thread database compaction and pruning.
 * - Proactive JVM memory pressure relief.
 * - Safe log rotation.
 */
public class DiskOptimizer implements Listener {

    public static volatile boolean enabled = false;

    private static final Set<UUID> modifiedWorlds = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> skippedWorldsInCurrentCycle = ConcurrentHashMap.newKeySet();

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();
    private static ExecutorService asyncDiskExecutor;

    private static BukkitTask mainTickTask;
    private static BukkitTask maintenanceTask;

    private static volatile long lastMaintenanceTime = 0;
    private static volatile long lastMemoryReliefTime = 0;

    // Reflection cache for DedicatedServer.ticksUntilAutosave
    private static Method getServerMethod;
    private static Field ticksUntilAutosaveField;
    private static boolean reflectionInitialized = false;

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

    /**
     * Submits an asynchronous disk/database query or computation.
     */
    public static <T> CompletableFuture<T> submitDiskQuery(Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        ExecutorService exec = asyncDiskExecutor;
        if (exec == null || exec.isShutdown()) {
            try {
                future.complete(callable.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
            return future;
        }
        exec.execute(() -> {
            try {
                future.complete(callable.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Submits an asynchronous disk I/O, cleanup, or maintenance task.
     */
    public static CompletableFuture<Void> submitDiskTask(Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        ExecutorService exec = asyncDiskExecutor;
        if (exec == null || exec.isShutdown()) {
            try {
                runnable.run();
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
            return future;
        }
        exec.execute(() -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Asynchronously saves a world.
     * All disk I/O, level data serialization and region file chunk writes are handled
     * through non-blocking asynchronous pipelines, eliminating main thread freeze.
     */
    public static CompletableFuture<Void> saveWorldAsync(World world) {
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        submitDiskTask(() -> {
            try {
                // Schedule non-blocking chunk save on tick thread.
                // In Paper/Moonrise, CraftWorld.save() executes non-blocking chunk queueing (flush=false),
                // pushing chunk region data to background I/O threads.
                Bukkit.getScheduler().runTask(GrimACBukkitLoaderPlugin.LOADER, () -> {
                    try {
                        world.save();
                        future.complete(null);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Asynchronously saves all modified worlds.
     */
    public static CompletableFuture<Void> saveModifiedWorldsAsync() {
        if (modifiedWorlds.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        Set<UUID> toSave = new HashSet<>(modifiedWorlds);
        modifiedWorlds.removeAll(toSave);

        CompletableFuture<?>[] futures = toSave.stream()
            .map(Bukkit::getWorld)
            .filter(Objects::nonNull)
            .map(DiskOptimizer::saveWorldAsync)
            .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }

    public static void startTask() {
        stopTask();
        if (!enabled) return;

        if (asyncDiskExecutor == null || asyncDiskExecutor.isShutdown()) {
            asyncDiskExecutor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 4),
                r -> {
                    Thread t = new Thread(r, "FullGrim-AsyncDisk-" + THREAD_COUNTER.incrementAndGet());
                    t.setDaemon(true);
                    t.setPriority(Thread.NORM_PRIORITY - 1);
                    return t;
                }
            );
        }

        // Ensure all worlds start with autoSave=true so chunk unloads run smoothly
        for (World world : Bukkit.getWorlds()) {
            try {
                world.setAutoSave(true);
            } catch (Throwable ignored) {}
        }

        try {
            // Main thread 1-tick monitor: handles safe, 1-tick auto-save suppression for untouched worlds
            // and triggers asynchronous saving for modified worlds
            mainTickTask = Bukkit.getScheduler().runTaskTimer(GrimACBukkitLoaderPlugin.LOADER, () -> {
                if (!enabled) return;

                int ticksUntilSave = getTicksUntilAutosave();
                boolean isAutosaveApproaching;
                if (ticksUntilSave >= 0) {
                    isAutosaveApproaching = (ticksUntilSave <= 1);
                } else {
                    // Fallback: check modulo 6000 ticks (default Paper autosave interval)
                    isAutosaveApproaching = (Bukkit.getCurrentTick() % 6000 == 5999);
                }

                if (isAutosaveApproaching) {
                    // Autosave is about to run on the next tick!
                    // 1. For worlds where no player interacted, temporarily disable autoSave for 1 tick to skip saving
                    for (World world : Bukkit.getWorlds()) {
                        if (!modifiedWorlds.contains(world.getUID())) {
                            try {
                                world.setAutoSave(false);
                                skippedWorldsInCurrentCycle.add(world.getUID());
                            } catch (Throwable ignored) {}
                        }
                    }
                    // 2. For modified worlds: execute asynchronous non-blocking save
                    if (!modifiedWorlds.isEmpty()) {
                        saveModifiedWorldsAsync();
                    }
                } else if (!skippedWorldsInCurrentCycle.isEmpty()) {
                    // Autosave tick has finished! Restore autoSave=true immediately so chunk unloads continue
                    for (UUID uid : skippedWorldsInCurrentCycle) {
                        World world = Bukkit.getWorld(uid);
                        if (world != null) {
                            try {
                                world.setAutoSave(true);
                            } catch (Throwable ignored) {}
                        }
                    }
                    skippedWorldsInCurrentCycle.clear();
                }
            }, 1L, 1L);

            // Off-main asynchronous maintenance task (every 60s)
            maintenanceTask = Bukkit.getScheduler().runTaskTimerAsynchronously(GrimACBukkitLoaderPlugin.LOADER, () -> {
                if (!enabled) return;

                long now = System.currentTimeMillis();

                // 1. Proactive Memory Pressure Relief
                submitDiskTask(() -> checkMemoryPressure(now));

                // 2. Database Maintenance
                if (now - lastMaintenanceTime > 900000L) {
                    lastMaintenanceTime = now;
                    submitDiskTask(() -> {
                        runDatabaseMaintenance();
                        trimOldLogFiles();
                    });
                }

            }, 600L, 1200L);
        } catch (Throwable ignored) {}
    }

    public static void stopTask() {
        if (mainTickTask != null) {
            try {
                mainTickTask.cancel();
            } catch (Throwable ignored) {}
            mainTickTask = null;
        }
        if (maintenanceTask != null) {
            try {
                maintenanceTask.cancel();
            } catch (Throwable ignored) {}
            maintenanceTask = null;
        }
        if (asyncDiskExecutor != null) {
            try {
                asyncDiskExecutor.shutdown();
            } catch (Throwable ignored) {}
            asyncDiskExecutor = null;
        }

        // Restore default vanilla autosave for all worlds
        for (World world : Bukkit.getWorlds()) {
            try {
                world.setAutoSave(true);
            } catch (Throwable ignored) {}
        }
        modifiedWorlds.clear();
        skippedWorldsInCurrentCycle.clear();
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
     * Guarantees the world will be saved on the next auto-save cycle.
     */
    public static void markWorldModified(World world) {
        if (!enabled || world == null) return;
        // If there are no players in this world, ignore: cannot be a player modification
        if (world.getPlayers().isEmpty()) return;

        UUID uid = world.getUID();
        if (modifiedWorlds.add(uid)) {
            try {
                world.setAutoSave(true);
            } catch (Throwable ignored) {}
        }
    }

    private static int getTicksUntilAutosave() {
        if (!reflectionInitialized) {
            reflectionInitialized = true;
            try {
                Class<?> craftServerClass = Bukkit.getServer().getClass();
                getServerMethod = craftServerClass.getMethod("getServer");
                Object mcServer = getServerMethod.invoke(Bukkit.getServer());
                if (mcServer != null) {
                    Class<?> clazz = mcServer.getClass();
                    while (clazz != null && clazz != Object.class) {
                        try {
                            Field f = clazz.getDeclaredField("ticksUntilAutosave");
                            f.setAccessible(true);
                            ticksUntilAutosaveField = f;
                            break;
                        } catch (NoSuchFieldException ignored) {
                            clazz = clazz.getSuperclass();
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (getServerMethod != null && ticksUntilAutosaveField != null) {
            try {
                Object mcServer = getServerMethod.invoke(Bukkit.getServer());
                return ticksUntilAutosaveField.getInt(mcServer);
            } catch (Throwable ignored) {}
        }
        return -1;
    }

    // -------------------------------------------------------------
    // Player Interaction Listeners
    // -------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getPlayer() != null) {
            markWorldModified(event.getBlock().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        if (event.getPlayer() != null) {
            markWorldModified(event.getBlock().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer() != null) {
            markWorldModified(event.getBlock().getWorld());
        }
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
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        markWorldModified(event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        markWorldModified(event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        markWorldModified(event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            markWorldModified(event.getEntity().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        markWorldModified(event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            markWorldModified(event.getEntity().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        if (event.getPlayer() != null) {
            markWorldModified(event.getEntity().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getPlayer() != null) {
            markWorldModified(event.getEntity().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (event instanceof HangingBreakByEntityEvent) {
            HangingBreakByEntityEvent entityEvent = (HangingBreakByEntityEvent) event;
            if (entityEvent.getRemover() instanceof Player) {
                markWorldModified(event.getEntity().getWorld());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (event.getAttacker() instanceof Player) {
            markWorldModified(event.getVehicle().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!event.blockList().isEmpty() && !event.getLocation().getWorld().getPlayers().isEmpty()) {
            markWorldModified(event.getLocation().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!event.blockList().isEmpty() && !event.getBlock().getWorld().getPlayers().isEmpty()) {
            markWorldModified(event.getBlock().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player && event.getInventory().getLocation() != null) {
            World world = event.getInventory().getLocation().getWorld();
            if (world != null && !world.getPlayers().isEmpty()) {
                markWorldModified(world);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player && event.getInventory().getLocation() != null) {
            World world = event.getInventory().getLocation().getWorld();
            if (world != null && !world.getPlayers().isEmpty()) {
                markWorldModified(world);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        if (!enabled) return;
        try {
            event.getWorld().setAutoSave(true);
        } catch (Throwable ignored) {}
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        modifiedWorlds.remove(event.getWorld().getUID());
        skippedWorldsInCurrentCycle.remove(event.getWorld().getUID());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldSave(WorldSaveEvent event) {
        if (!enabled) return;
        World world = event.getWorld();
        modifiedWorlds.remove(world.getUID());
    }

    // -------------------------------------------------------------
    // Memory and Database Maintenance (Silent)
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
                runMemoryRelief();
            }
        } catch (Throwable ignored) {}
    }

    private static void runMemoryRelief() {
        try {
            runDatabaseMaintenance();
        } catch (Throwable ignored) {}
    }

    // Clean up Sonar verified players database (https://github.com/jonesdevelopment/sonar/)
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
