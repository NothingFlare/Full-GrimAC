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

    private static final int MAX_CPS = 18;
    private final Deque<Long> clickTimestamps = new ArrayDeque<>();
    private final Deque<Long> intervals = new ArrayDeque<>();
    private long lastClickTime = 0;
    private long lastSwingTime = 0;
    private long lastAttackTime = 0;
    private long lastInterval = 0;
    private int lastTickDiff = 0;
    private int zeroDelayCount = 0;
    private int consecutiveDuplicateIntervals = 0;
    private int identicalTickCount = 0;
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
        identicalTickCount = Math.max(0, identicalTickCount - 2);
    }

    private boolean isPlayerDigging(long now) {
        if (player.packetOrderProcessor.isDigging()) {
            return true;
        }
        if (isDiggingBlock && (now - lastDiggingStartTime < 10000)) {
            return true;
        }
        if (now - lastDiggingEndTime < 400) {
            return true;
        }
        if (now - lastDiggingPacketTime < 400) {
            return true;
        }
        if (now - player.lastBlockBreak < 400) {
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
            if (player.gamemode == GameMode.CREATIVE) return;

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
            if (player.packetOrderProcessor.isRightClicking() || (now - lastRightClickTime < 250)) {
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
        // Consecutive packets with <= 8ms difference indicate macro / packet flood
        if (lastClickTime > 0) {
            long diff = now - lastClickTime;
            if (diff <= 8) {
                zeroDelayCount++;
                if (zeroDelayCount >= 3) {
                    zeroDelayCount = 0;
                    notifyBot();
                    if (flag("zero_delay=" + diff + "ms (x3)") && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                        return;
                    }
                }
            } else if (diff > 30) {
                zeroDelayCount = 0;
            }
        }

        // 2. Sliding 1-second CPS check
        clickTimestamps.addLast(now);
        while (!clickTimestamps.isEmpty() && now - clickTimestamps.peekFirst() > 1000) {
            clickTimestamps.pollFirst();
        }

        int cps = clickTimestamps.size();
        if (cps > MAX_CPS) {
            // Blatant high CPS (>= 22) triggers immediately; 19-21 requires small buffer
            if (cps >= 22) {
                notifyBot();
                if (flag("cps=" + cps) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                    return;
                }
            } else {
                cpsBuffer += (cps - MAX_CPS) * 0.5;
                if (cpsBuffer >= 2.0) {
                    cpsBuffer = 1.0;
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

        // 3. Robotic consistency & interval pattern checks
        if (!PerformanceProtectionManager.shouldSkipInventory() && lastClickTime > 0) {
            long interval = now - lastClickTime;

            if (interval >= 20 && interval <= 500) {
                // 3a. Constant delay check with 3ms network ping tolerance (e.g. 50ms +/- 3ms)
                if (lastInterval > 0 && Math.abs(interval - lastInterval) <= 3) {
                    consecutiveDuplicateIntervals++;
                    if (consecutiveDuplicateIntervals >= 5) {
                        consecutiveDuplicateIntervals = 3;
                        notifyBot();
                        flag("constant_delay: " + interval + "ms (x5)");
                    }
                } else if (Math.abs(interval - lastInterval) > 4) {
                    consecutiveDuplicateIntervals = Math.max(0, consecutiveDuplicateIntervals - 1);
                }
                lastInterval = interval;

                // 3b. Fixed tick delta check (1 tick = 50ms or 2 ticks = 100ms)
                int tickDiff = (int) Math.round((double) interval / 50.0);
                if (lastTickDiff > 0 && tickDiff == lastTickDiff && (tickDiff == 1 || tickDiff == 2) && Math.abs(interval - (tickDiff * 50)) <= 4) {
                    identicalTickCount++;
                    if (identicalTickCount >= 6) {
                        identicalTickCount = 4;
                        notifyBot();
                        flag("tick_locked: " + tickDiff + " ticks (~" + (tickDiff * 50) + "ms)");
                    }
                } else {
                    identicalTickCount = Math.max(0, identicalTickCount - 1);
                }
                lastTickDiff = tickDiff;

                // 3c. Rolling standard deviation across 10-14 samples
                intervals.addLast(interval);
                if (intervals.size() > 14) {
                    intervals.pollFirst();
                }

                if (intervals.size() >= 10) {
                    double mean = calculateMean(intervals);
                    double sd = calculateSD(intervals, mean);
                    player.packetStateData.currentClickStdDev = sd;
                    player.packetStateData.currentMeanInterval = mean;

                    // Autoclickers (even randomized ones) produce standard deviation < 3.5ms.
                    // Legitimate human clicking naturally produces standard deviation > 12ms.
                    double maxAllowedSd = (mean < 70.0) ? 2.5 : (mean < 120.0 ? 3.2 : 3.8);
                    if (sd < maxAllowedSd && mean >= 30.0) {
                        sdBuffer += 1.0;
                        if (sdBuffer >= 1.5) {
                            sdBuffer = 0.8;
                            notifyBot();
                            flag("robotic_interval: sd=" + String.format("%.2f", sd) + "ms, mean=" + String.format("%.1f", mean) + "ms");
                        }
                    } else if (sd > maxAllowedSd + 3.0) {
                        sdBuffer = Math.max(0.0, sdBuffer - 0.4);
                    }
                }
            } else if (interval > 500) {
                decayClickHistory();
                lastInterval = 0;
                lastTickDiff = 0;
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
