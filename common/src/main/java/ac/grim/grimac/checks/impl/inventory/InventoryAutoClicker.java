/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.manager.performance.PerformanceProtectionManager;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckData(
        name = "InventoryAutoClicker",
        stableKey = "grim.inventory.autoclicker",
        description = "High click speed or robotic consistency in inventory"
)
public class InventoryAutoClicker extends Check implements PacketReceiveListener {

    private static final int MAX_CPS = 24;
    private final Deque<Long> clickTimestamps = new ArrayDeque<>();
    private final Deque<Long> intervals = new ArrayDeque<>();
    private long lastClickTime = 0;
    private long lastInterval = 0;
    private int zeroDelayCount = 0;
    private int consecutiveDuplicates = 0;
    private double cpsBuffer = 0.0;

    public InventoryAutoClicker(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        // Skip on low server TPS to save resources
        if (PerformanceProtectionManager.shouldSkipInventory()) return;

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
            long now = System.currentTimeMillis();
            WindowClickType clickType = click.getWindowClickType();

            // 1. Closed container check (clicking container window when none was opened)
            int windowId = click.getWindowId();
            if (windowId > 0 && player.inventory.getOpenWindowID() == 0 && !player.serverOpenedInventoryThisTick) {
                if (flag("closed_window=" + windowId) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                    InventoryResync.resync(player);
                }
                return;
            }

            // Exclude vanilla multi-packet operations:
            // QUICK_CRAFT (drag crafting sends start, paint, end packets in 0ms)
            // PICKUP_ALL (double click to collect items sends pickup + pickup_all in 0ms)
            boolean isVanillaMultiAction = (clickType == WindowClickType.QUICK_CRAFT || clickType == WindowClickType.PICKUP_ALL);

            // 2. Zero-delay / double click check for standard individual clicks (diff <= 5ms)
            if (!isVanillaMultiAction && lastClickTime > 0) {
                long diff = now - lastClickTime;
                if (diff <= 5) {
                    zeroDelayCount++;
                    if (zeroDelayCount >= 6) {
                        zeroDelayCount = 0;
                        if (flag("zero_delay=" + diff + "ms") && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                            InventoryResync.resync(player);
                        }
                    }
                } else {
                    zeroDelayCount = Math.max(0, zeroDelayCount - 1);
                }
            }

            // 3. CPS Check in sliding 1-second window with burst buffer allowance
            if (!isVanillaMultiAction) {
                clickTimestamps.addLast(now);
                while (!clickTimestamps.isEmpty() && now - clickTimestamps.peekFirst() > 1000) {
                    clickTimestamps.pollFirst();
                }

                int cps = clickTimestamps.size();
                if (cps > MAX_CPS) {
                    cpsBuffer += (cps - MAX_CPS);
                    if (cpsBuffer >= 4.0) {
                        cpsBuffer = 0;
                        if (flag("cps=" + cps) && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                            InventoryResync.resync(player);
                        }
                    }
                } else {
                    cpsBuffer = Math.max(0, cpsBuffer - 1.0);
                }
            }

            // 4. Robotic interval & duplicate consistency check
            if (!isVanillaMultiAction && lastClickTime > 0) {
                long interval = now - lastClickTime;
                if (interval >= 25 && interval <= 500) {
                    // Exact duplicate check
                    if (lastInterval > 0 && Math.abs(interval - lastInterval) <= 1) {
                        consecutiveDuplicates++;
                        if (consecutiveDuplicates >= 9) {
                            consecutiveDuplicates = 0;
                            flag("constant_delay: " + interval + "ms (x9)");
                        }
                    } else if (Math.abs(interval - lastInterval) > 2) {
                        consecutiveDuplicates = 0;
                    }
                    lastInterval = interval;

                    intervals.addLast(interval);
                    if (intervals.size() > 20) {
                        intervals.pollFirst();
                    }

                    if (intervals.size() >= 16) {
                        double mean = calculateMean(intervals);
                        double sd = calculateSD(intervals, mean);
                        double maxSd = (mean < 100.0) ? 1.4 : 1.8;
                        if (sd < maxSd && mean >= 35.0) {
                            flag("robotic_clicks: sd=" + String.format("%.2f", sd) + "ms, mean=" + String.format("%.1f", mean) + "ms");
                        }
                    }
                } else if (interval > 500) {
                    consecutiveDuplicates = 0;
                    lastInterval = 0;
                }
            }
            lastClickTime = now;
        }
    }

    private static double calculateMean(Iterable<Long> values) {
        long sum = 0;
        int count = 0;
        for (long v : values) {
            sum += v;
            count++;
        }
        return count == 0 ? 0 : (double) sum / count;
    }

    private static double calculateSD(Iterable<Long> values, double mean) {
        double sum = 0;
        int count = 0;
        for (long v : values) {
            double diff = v - mean;
            sum += diff * diff;
            count++;
        }
        return count == 0 ? 0 : Math.sqrt(sum / count);
    }
}
