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

    private static final int MAX_CPS = 26;
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

        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            clickTimestamps.clear();
            intervals.clear();
            lastClickTime = 0;
            lastInterval = 0;
            zeroDelayCount = 0;
            consecutiveDuplicates = 0;
            cpsBuffer = 0.0;
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
            long now = System.currentTimeMillis();
            WindowClickType clickType = click.getWindowClickType();

            // Exclude vanilla multi-packet operations and creative operations:
            // QUICK_CRAFT (drag crafting sends start, paint, end packets in 0ms)
            // PICKUP_ALL (double click to collect items sends pickup + pickup_all in 0ms)
            // CLONE (creative middle click)
            boolean isVanillaMultiAction = (clickType == WindowClickType.QUICK_CRAFT
                    || clickType == WindowClickType.PICKUP_ALL
                    || clickType == WindowClickType.CLONE);

            // 1. Zero-delay packet flood detection (ChestStealer / Nuker / Macro):
            // Normal human clicking over TCP with Netty batching may group 3-6 packets in a single flush.
            // ChestStealer cheats loot 15-27 slots in 0ms.
            if (!isVanillaMultiAction && lastClickTime > 0) {
                long diff = now - lastClickTime;
                if (diff <= 5) {
                    zeroDelayCount++;
                    // QUICK_MOVE (shift-click) and SWAP (hotbar 1-9 keys) allow higher batch allowance (up to 14 packets)
                    int maxAllowedZeroDelay = (clickType == WindowClickType.QUICK_MOVE || clickType == WindowClickType.SWAP) ? 14 : 8;

                    if (zeroDelayCount >= maxAllowedZeroDelay) {
                        zeroDelayCount = maxAllowedZeroDelay - 4;
                        if (flag("zero_delay=" + diff + "ms (x" + maxAllowedZeroDelay + ")") && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                            InventoryResync.resync(player);
                        }
                    }
                } else if (diff > 25) {
                    zeroDelayCount = Math.max(0, zeroDelayCount - 2);
                }
            }

            // 2. Sliding 1-second CPS check:
            // Allows rapid human bursts (shift-clicking 10-15 stacks in a chest) while blocking Cheats (40-60 CPS)
            if (!isVanillaMultiAction) {
                clickTimestamps.addLast(now);
                while (!clickTimestamps.isEmpty() && now - clickTimestamps.peekFirst() > 1000) {
                    clickTimestamps.pollFirst();
                }

                int cps = clickTimestamps.size();
                if (cps > MAX_CPS) {
                    cpsBuffer += (cps - MAX_CPS);
                    if (cpsBuffer >= 6.0) {
                        cpsBuffer = 3.0;
                        if (flag("cps=" + cps) && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                            InventoryResync.resync(player);
                        }
                    }
                } else {
                    cpsBuffer = Math.max(0.0, cpsBuffer - 1.0);
                }
            }

            // 3. Robotic interval & duplicate consistency check:
            // Filters out Minecraft tick quantization (50ms, 100ms) and Windows OS 15.6ms timer steps
            if (!isVanillaMultiAction && lastClickTime > 0) {
                long interval = now - lastClickTime;
                if (interval >= 25 && interval <= 500) {
                    boolean isTickQuantized = (Math.abs(interval - 50) <= 4
                            || Math.abs(interval - 100) <= 4
                            || Math.abs(interval - 15) <= 2
                            || Math.abs(interval - 31) <= 2);

                    if (!isTickQuantized) {
                        if (lastInterval > 0 && Math.abs(interval - lastInterval) <= 1) {
                            consecutiveDuplicates++;
                            if (consecutiveDuplicates >= 14) {
                                consecutiveDuplicates = 8;
                                flag("constant_delay: " + interval + "ms (x14)");
                            }
                        } else if (Math.abs(interval - lastInterval) > 3) {
                            consecutiveDuplicates = Math.max(0, consecutiveDuplicates - 1);
                        }
                        lastInterval = interval;

                        intervals.addLast(interval);
                        if (intervals.size() > 20) {
                            intervals.pollFirst();
                        }

                        if (intervals.size() >= 16) {
                            double mean = calculateMean(intervals);
                            double sd = calculateSD(intervals, mean);
                            if (sd < 1.2 && mean >= 35.0) {
                                flag("robotic_clicks: sd=" + String.format("%.2f", sd) + "ms, mean=" + String.format("%.1f", mean) + "ms");
                            }
                        }
                    }
                } else if (interval > 500) {
                    consecutiveDuplicates = 0;
                    lastInterval = 0;
                    intervals.clear();
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
