/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.platform.bukkit.listener;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.impl.inventory.InventoryResync;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens to all Bukkit block placement and item usage events to catch server-side
 * rejections (e.g. entity in block hitbox, world protection, cancelled events)
 * and immediately resynchronizes the player inventory so items never disappear.
 */
public final class ItemDesyncFixListener implements Listener {

    private final Map<UUID, Long> lastResyncTick = new ConcurrentHashMap<>();

    private void resync(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        long currentTick = Bukkit.getCurrentTick();
        Long lastTick = lastResyncTick.get(uuid);
        if (lastTick != null && lastTick == currentTick) {
            return; // Already resynced in this tick
        }
        lastResyncTick.put(uuid, currentTick);

        // 1. Immediate resync on current thread
        try {
            player.updateInventory();
        } catch (Throwable ignored) {
        }

        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(uuid);
        if (grimPlayer != null) {
            try {
                InventoryResync.resync(grimPlayer);
            } catch (Throwable ignored) {
            }
        }

        // 2. Next tick delayed resync (guarantees server finished all internal state processing)
        try {
            PlatformPlayer platformPlayer = GrimACBukkitLoaderPlugin.LOADER.getPlatformPlayerFactory().getFromUUID(uuid);
            if (platformPlayer != null) {
                GrimAPI.INSTANCE.getScheduler().getEntityScheduler().execute(
                        platformPlayer,
                        GrimACBukkitLoaderPlugin.LOADER.getPlugin(),
                        () -> {
                            if (player.isOnline()) {
                                try {
                                    player.updateInventory();
                                } catch (Throwable ignored) {
                                }
                                if (grimPlayer != null) {
                                    try {
                                        InventoryResync.resync(grimPlayer);
                                    } catch (Throwable ignored) {
                                    }
                                }
                            }
                        },
                        null,
                        1
                );
            } else {
                Bukkit.getScheduler().runTaskLater(GrimACBukkitLoaderPlugin.LOADER, () -> {
                    if (player.isOnline()) {
                        player.updateInventory();
                    }
                }, 1L);
            }
        } catch (Throwable t) {
            try {
                Bukkit.getScheduler().runTaskLater(GrimACBukkitLoaderPlugin.LOADER, () -> {
                    if (player.isOnline()) {
                        player.updateInventory();
                    }
                }, 1L);
            } catch (Throwable ignored) {
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockCanBuild(BlockCanBuildEvent event) {
        if (!event.isBuildable()) {
            try {
                Player player = event.getPlayer();
                if (player != null) {
                    resync(player);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled() || !event.canBuild()) {
            resync(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.hasItem() && event.getAction().name().contains("RIGHT_CLICK")) {
            if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY ||
                    (event.isCancelled() && event.useItemInHand() != org.bukkit.event.Event.Result.ALLOW)) {
                resync(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.isCancelled()) {
            resync(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (event.isCancelled()) {
            resync(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled() && event.getWhoClicked() instanceof Player player) {
            resync(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.isCancelled()) {
            resync(event.getPlayer());
        }
    }
}
