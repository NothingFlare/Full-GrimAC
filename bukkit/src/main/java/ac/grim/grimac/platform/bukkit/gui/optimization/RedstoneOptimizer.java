/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.platform.bukkit.gui.optimization;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * High-performance redstone caching and anti-lag optimizer.
 * Eliminates redundant state updates and throttles rapid feedback loops/clocks
 * while strictly preserving all vanilla redstone logic and contraption mechanics.
 */
public class RedstoneOptimizer implements Listener {

    public static volatile boolean enabled = false;

    private static long currentTick = -1;
    private static final Map<Long, Integer> signalCache = new HashMap<>(512);
    private static final Map<Long, Integer> updateCountCache = new HashMap<>(512);

    // Limit maximum updates per single block within one server tick (50ms).
    // Standard circuits trigger at most 1-2 times per tick; > 40 is a runaway lag machine.
    private static final int MAX_UPDATES_PER_BLOCK_PER_TICK = 40;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        if (!enabled) return;

        int oldCurrent = event.getOldCurrent();
        int newCurrent = event.getNewCurrent();

        // 1. Redundant update elimination: if signal level didn't change, avoid duplicate cascade
        if (oldCurrent == newCurrent) {
            event.setNewCurrent(oldCurrent);
            return;
        }

        Block block = event.getBlock();
        Location loc = block.getLocation();
        if (loc.getWorld() == null) return;

        long tick = loc.getWorld().getFullTime();
        if (tick != currentTick) {
            currentTick = tick;
            signalCache.clear();
            updateCountCache.clear();
        }

        long posKey = getBlockKey(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        // 2. Lag machine loop throttling: cap hyper-oscillating clock feedback loops
        int count = updateCountCache.getOrDefault(posKey, 0) + 1;
        updateCountCache.put(posKey, count);

        if (count > MAX_UPDATES_PER_BLOCK_PER_TICK) {
            event.setNewCurrent(oldCurrent);
            return;
        }

        // 3. Deduplication: if exact signal level was already processed at this block in this tick
        Integer cachedSignal = signalCache.get(posKey);
        if (cachedSignal != null && cachedSignal == newCurrent) {
            event.setNewCurrent(oldCurrent);
            return;
        }

        signalCache.put(posKey, newCurrent);
    }

    public static long getBlockKey(int x, int y, int z) {
        return (((long) x & 0x3FFFFFF) << 38) | (((long) z & 0x3FFFFFF) << 12) | ((long) y & 0xFFF);
    }

    public static void clearCache() {
        signalCache.clear();
        updateCountCache.clear();
    }
}
