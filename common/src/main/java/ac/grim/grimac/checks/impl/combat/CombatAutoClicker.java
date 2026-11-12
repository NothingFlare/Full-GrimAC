/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockBreakListener;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.manager.performance.PerformanceProtectionManager;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckData(
        name = "CombatAutoClicker",
        stableKey = "grim.combat.autoclicker",
        description = "High CPS, zero delay, or robotic click consistency in combat"
)
public class CombatAutoClicker extends Check implements PacketReceiveListener, BlockBreakListener {

    private static final int MAX_CPS = 20;
    private final Deque<Long> clickTimestamps = new ArrayDeque<>();
    private final Deque<Long> intervals = new ArrayDeque<>();
    private long lastClickTime = 0;
    private long lastSwingTime = 0;
    private long lastAttackTime = 0;
    private long lastInterval = 0;
    private int zeroDelayCount = 0;
    private int consecutiveDuplicateIntervals = 0;
    private double cpsBuffer = 0.0;
    private double sdBuffer = 0.0;

    // Block mining / placement tracking
    private boolean isDiggingBlock = false;
    private long lastDiggingStartTime = 0;
    private long lastDiggingEndTime = 0;
    private long lastDiggingPacketTime = 0;
    private long lastRightClickTime = 0;

    public CombatAutoClicker(GrimPlayer player) {
        super(player);
    }

    private void notifyBot() {
        KillAuraBot bot = player.checkManager.get(KillAuraBot.class);
        if (bot != null) {
            bot.addSuspicion(2);
        }
    }

    private void decayClickHistory() {
        if (!intervals.isEmpty()) {
            intervals.pollFirst();
        }
        cpsBuffer = Math.max(0.0, cpsBuffer - 1.0);
        sdBuffer = Math.max(0.0, sdBuffer - 0.5);
        consecutiveDuplicateIntervals = Math.max(0, consecutiveDuplicateIntervals - 2);
    }

    private boolean isPlayerDigging(long now) {
        if (player.packetOrderProcessor.isDigging()) {
            return true;
        }
        if (isDiggingBlock && (now - lastDiggingStartTime < 10000)) {
            return true;
        }
        if (now - lastDiggingEndTime < 600) {
            return true;
        }
        if (now - lastDiggingPacketTime < 600) {
            return true;
        }
        if (now - player.lastBlockBreak < 600) {
            return true;
        }
        return false;
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        long now = System.currentTimeMillis();
        lastDiggingPacketTime = now;
        switch (blockBreak.action) {
            case START_DIGGING -> {
                isDiggingBlock = true;
                lastDiggingStartTime = now;
            }
            case FINISHED_DIGGING, CANCELLED_DIGGING -> {
                isDiggingBlock = false;
                lastDiggingEndTime = now;
            }
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        // Block digging packets
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);
            DiggingAction action = dig.getAction();
            long now = System.currentTimeMillis();
            if (action == DiggingAction.START_DIGGING) {
                isDiggingBlock = true;
                lastDiggingStartTime = now;
                lastDiggingPacketTime = now;
            } else if (action == DiggingAction.FINISHED_DIGGING || action == DiggingAction.CANCELLED_DIGGING) {
                isDiggingBlock = false;
                lastDiggingEndTime = now;
                lastDiggingPacketTime = now;
            }
            return;
        }

        // Block placement / item usage packets (right-click actions)
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT || event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            lastRightClickTime = System.currentTimeMillis();
            return;
        }

        // Slot change resets active digging
        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            isDiggingBlock = false;
            lastDiggingEndTime = System.currentTimeMillis();
            return;
        }

        // 1. Attack packets: check illegal attack while using items (bow, food, shield)
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            isDiggingBlock = false;
            handleAttackPacket(event);
            return;
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                isDiggingBlock = false;
                handleAttackPacket(event);
            }
            return;
        }

        // 2. Arm swing packets: Canonical single source of truth for physical clicks
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) return;

            // In 1.9+, ignore offhand animations (e.g. placing offhand blocks / eating)
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
                try {
                    WrapperPlayClientAnimation anim = new WrapperPlayClientAnimation(event);
                    if (anim.getHand() == InteractionHand.OFF_HAND) {
                        return;
                    }
                } catch (Exception ignored) {
                }
            }

            long now = System.currentTimeMillis();

            // Ignore swings while managing inventory or container
            if (player.inventory.isContainerOpen() || (now - player.packetStateData.lastWindowClickTime < 1000)) {
                decayClickHistory();
                return;
            }

            // Ignore swings caused by placing blocks or right-clicking
            if (player.packetOrderProcessor.isRightClicking() || (now - lastRightClickTime < 350)) {
                return;
            }

            // Ignore swings while legitimately mining blocks
            if (isPlayerDigging(now)) {
                return;
            }

            lastSwingTime = now;
            handleClick(event);
        }
    }

    private void handleAttackPacket(PacketReceiveEvent event) {
        if (player.packetStateData.isSlowedByUsingItem()) {
            notifyBot();
            if (flag("action=attack_while_using_item") && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
                return;
            }
        }

        long now = System.currentTimeMillis();
        if (player.inventory.isContainerOpen() || (now - player.packetStateData.lastWindowClickTime < 1000)) {
            return;
        }
        lastAttackTime = now;

        // If an animation already accompanied this attack in the last 150ms, do not register a duplicate click!
        if (now - lastSwingTime <= 150) {
            return;
        }

        // Attack occurred without animation (e.g. NoSwing cheat) -> register as click
        handleClick(event);
    }

    private void handleClick(PacketReceiveEvent event) {
        long now = System.currentTimeMillis();

        // 1. Zero-delay clicks:
        // Consecutive packets with <= 6ms difference indicate macro / packet flood
        // Requires 6 consecutive zero-delay packets to prevent false flags on butterfly double-clicks / TCP bursts
        if (lastClickTime > 0) {
            long diff = now - lastClickTime;
            if (diff <= 6) {
                zeroDelayCount++;
                if (zeroDelayCount >= 6) {
                    zeroDelayCount = 3;
                    notifyBot();
                    if (flag("zero_delay=" + diff + "ms (x6)") && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                        return;
                    }
                }
            } else if (diff > 25) {
                zeroDelayCount = Math.max(0, zeroDelayCount - 2);
            }
        }

        // 2. Sliding 1-second CPS check:
        // Blatant high CPS (>= 25) triggers immediately; 21-24 requires sustained buffer
        clickTimestamps.addLast(now);
        while (!clickTimestamps.isEmpty() && now - clickTimestamps.peekFirst() > 1000) {
            clickTimestamps.pollFirst();
        }

        int cps = clickTimestamps.size();
        if (cps > MAX_CPS) {
            if (cps >= 25) {
                notifyBot();
                if (flag("cps=" + cps) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                    return;
                }
            } else {
                cpsBuffer += (cps - MAX_CPS) * 0.5;
                if (cpsBuffer >= 4.0) {
                    cpsBuffer = 2.0;
                    notifyBot();
                    if (flag("cps=" + cps) && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                        return;
                    }
                }
            }
        } else {
            cpsBuffer = Math.max(0.0, cpsBuffer - 0.5);
        }

        // 3. Robotic consistency & interval pattern checks:
        if (!PerformanceProtectionManager.shouldSkipInventory() && lastClickTime > 0) {
            long interval = now - lastClickTime;

            if (interval >= 25 && interval <= 500) {
                // Filter out Minecraft tick quantization (50ms, 100ms) and Windows OS 15.6ms timer steps
                boolean isTickQuantized = (Math.abs(interval - 50) <= 4
                        || Math.abs(interval - 100) <= 4
                        || Math.abs(interval - 15) <= 2
                        || Math.abs(interval - 31) <= 2
                        || Math.abs(interval - 47) <= 2);

                if (!isTickQuantized) {
                    // 3a. Constant delay check across 12 consecutive identical intervals
                    if (lastInterval > 0 && Math.abs(interval - lastInterval) <= 1) {
                        consecutiveDuplicateIntervals++;
                        if (consecutiveDuplicateIntervals >= 12) {
                            consecutiveDuplicateIntervals = 6;
                            notifyBot();
                            flag("constant_delay: " + interval + "ms (x12)");
                        }
                    } else if (Math.abs(interval - lastInterval) > 3) {
                        consecutiveDuplicateIntervals = Math.max(0, consecutiveDuplicateIntervals - 1);
                    }
                    lastInterval = interval;

                    // 3b. Rolling standard deviation across 16 samples
                    intervals.addLast(interval);
                    if (intervals.size() > 18) {
                        intervals.pollFirst();
                    }

                    if (intervals.size() >= 16) {
                        double mean = calculateMean(intervals);
                        double sd = calculateSD(intervals, mean);
                        player.packetStateData.currentClickStdDev = sd;
                        player.packetStateData.currentMeanInterval = mean;

                        // Autoclickers with constant delay produce standard deviation < 1.0ms.
                        // Human clicking naturally produces standard deviation > 8ms.
                        double maxAllowedSd = (mean < 70.0) ? 1.0 : (mean < 120.0 ? 1.4 : 1.8);
                        if (sd < maxAllowedSd && mean >= 35.0) {
                            sdBuffer += 1.0;
                            if (sdBuffer >= 3.0) {
                                sdBuffer = 1.5;
                                notifyBot();
                                flag("robotic_interval: sd=" + String.format("%.2f", sd) + "ms, mean=" + String.format("%.1f", mean) + "ms");
                            }
                        } else if (sd > maxAllowedSd + 3.0) {
                            sdBuffer = Math.max(0.0, sdBuffer - 0.5);
                        }
                    }
                }
            } else if (interval > 500) {
                decayClickHistory();
                lastInterval = 0;
                intervals.clear();
            }
        }

        lastClickTime = now;
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
