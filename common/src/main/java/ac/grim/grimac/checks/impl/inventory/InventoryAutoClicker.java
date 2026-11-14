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

            // Quick transfer operations: Shift-click (QUICK_MOVE) and hotbar number key swapping (SWAP)
            boolean isQuickTransfer = (clickType == WindowClickType.QUICK_MOVE
                    || clickType == WindowClickType.SWAP);

            // Reset stale state if more than 1 second elapsed since last click
            if (lastClickTime > 0 && now - lastClickTime > 1000) {
                zeroDelayCount = 0;
                consecutiveDuplicates = 0;
                cpsBuffer = 0.0;
                intervals.clear();
            }

            // 1. Zero-delay packet flood detection (ChestStealer / Nuker / Macro):
            // Normal human clicking over TCP with Netty batching may group packets in a single network flush.
            // When looting a double chest (54 slots) or using shift-double-click, up to 54-60 packets can arrive in a single flush.
            if (!isVanillaMultiAction && lastClickTime > 0) {
                long diff = now - lastClickTime;
                if (diff <= 5) {
                    zeroDelayCount++;
                    // QUICK_MOVE (shift-click) and SWAP (hotbar 1-9 keys) in containers allow up to 64 packets in a batch
                    int maxAllowedZeroDelay = isQuickTransfer ? 64 : 10;

                    if (zeroDelayCount >= maxAllowedZeroDelay) {
                        zeroDelayCount = maxAllowedZeroDelay - 10;
                        if (flag("zero_delay=" + diff + "ms (x" + maxAllowedZeroDelay + ")") && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                            InventoryResync.resync(player);
                        }
                    }
                } else if (diff > 15) {
                    // Distinct click/frame, reset zero-delay batch accumulator
                    zeroDelayCount = 0;
                }
            }

            // 2. Sliding 1-second CPS check:
            // Allows rapid human sweeps across chests (up to 54-60 slots/sec for double chests) while blocking Cheats (80-120+ CPS)
            if (!isVanillaMultiAction) {
                clickTimestamps.addLast(now);
                while (!clickTimestamps.isEmpty() && now - clickTimestamps.peekFirst() > 1000) {
                    clickTimestamps.pollFirst();
                }

                int cps = clickTimestamps.size();
                int allowedCps = isQuickTransfer ? 64 : MAX_CPS;

                if (cps > allowedCps) {
                    cpsBuffer += (cps - allowedCps);
                    if (cpsBuffer >= 8.0) {
                        cpsBuffer = 4.0;
                        if (flag("cps=" + cps + " (allowed=" + allowedCps + ")") && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                            InventoryResync.resync(player);
                        }
                    }
                } else {
                    cpsBuffer = Math.max(0.0, cpsBuffer - 2.0);
                }
            }

            // 3. Robotic interval & duplicate consistency check:
            // Only applies to single-click interactions (PICKUP / THROW). Quick transfers (QUICK_MOVE / SWAP)
            // across a uniform geometric grid naturally have low variance when sweeping with mouse.
            if (!isVanillaMultiAction && !isQuickTransfer && lastClickTime > 0) {
                long interval = now - lastClickTime;
                if (interval >= 25 && interval <= 500) {
                    boolean isTickQuantized = (Math.abs(interval - 50) <= 4
                            || Math.abs(interval - 100) <= 4
                            || Math.abs(interval - 15) <= 2
                            || Math.abs(interval - 16) <= 2
                            || Math.abs(interval - 31) <= 2
                            || Math.abs(interval - 33) <= 2);

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
