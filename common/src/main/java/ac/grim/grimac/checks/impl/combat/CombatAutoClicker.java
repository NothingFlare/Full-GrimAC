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

    private static final int MAX_CPS = 22;
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

    // Block mining / placement tracking to prevent false flags when digging blocks
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

    private void resetClickHistory() {
        clickTimestamps.clear();
        intervals.clear();
        lastClickTime = 0;
        lastSwingTime = 0;
        lastAttackTime = 0;
        lastInterval = 0;
        lastTickDiff = 0;
        zeroDelayCount = 0;
        consecutiveDuplicateIntervals = 0;
        identicalTickCount = 0;
        cpsBuffer = 0;
        sdBuffer = 0;
    }

    private boolean isPlayerDigging(long now) {
        if (player.packetOrderProcessor.isDigging()) {
            return true;
        }
        if (isDiggingBlock && (now - lastDiggingStartTime < 15000)) {
            return true;
        }
        if (now - lastDiggingEndTime < 800) {
            return true;
        }
        if (now - lastDiggingPacketTime < 800) {
            return true;
        }
        if (now - player.lastBlockBreak < 800) {
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
                resetClickHistory();
            }
            case FINISHED_DIGGING, CANCELLED_DIGGING -> {
                isDiggingBlock = false;
                lastDiggingEndTime = now;
                resetClickHistory();
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
                resetClickHistory();
            } else if (action == DiggingAction.FINISHED_DIGGING || action == DiggingAction.CANCELLED_DIGGING) {
                isDiggingBlock = false;
                lastDiggingEndTime = now;
                lastDiggingPacketTime = now;
                resetClickHistory();
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
            resetClickHistory();
            return;
        }

        // 1. Attack packets: check illegal attack while using items (bow, food, shield)
        // Also register as combat click if no swing preceded it
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            isDiggingBlock = false;
            lastDiggingEndTime = 0;
            lastDiggingPacketTime = 0;
            handleAttackPacket(event);
            return;
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                isDiggingBlock = false;
                lastDiggingEndTime = 0;
                lastDiggingPacketTime = 0;
                handleAttackPacket(event);
            }
            return;
        }

        // 2. Arm swing packets: track true physical click CPS and intervals in combat
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            // Exempt creative mode (insta-breaking blocks / rapid building)
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

            // Ignore swings caused by placing blocks or right-clicking
            if (player.packetOrderProcessor.isRightClicking() || (now - lastRightClickTime < 300)) {
                return;
            }

            // Completely ignore swings while digging or breaking blocks
            if (isPlayerDigging(now)) {
                resetClickHistory();
                return;
            }

            // Deduplicate: if an attack packet was processed within the last 45ms, this swing belongs to it
            lastSwingTime = now;
            if (now - lastAttackTime <= 45) {
                return;
            }

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
        lastAttackTime = now;
        // Deduplicate: if a swing packet was processed within the last 45ms, this attack belongs to it
        if (now - lastSwingTime <= 45) {
            return;
        }

        handleClick(event);
    }

    private void handleClick(PacketReceiveEvent event) {
        long now = System.currentTimeMillis();

        // 1. Zero-delay clicks:
        // Butterfly clicking can produce legitimate double clicks (~8-15ms), but consecutive
        // zero-delay packets (diff <= 5ms) without any gap indicate an unthrottled macro or packet flood.
        if (lastClickTime > 0) {
            long diff = now - lastClickTime;
            if (diff <= 5) {
                zeroDelayCount++;
                if (zeroDelayCount >= 6) {
                    zeroDelayCount = 0;
                    notifyBot();
                    if (flag("zero_delay=" + diff + "ms") && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                        return;
                    }
                }
            } else if (diff > 25) {
                zeroDelayCount = 0;
            }
        }

        // 2. Sliding 1-second CPS check with buffer for human burst allowance
        clickTimestamps.addLast(now);
        while (!clickTimestamps.isEmpty() && now - clickTimestamps.peekFirst() > 1000) {
            clickTimestamps.pollFirst();
        }

        int cps = clickTimestamps.size();
        if (cps > MAX_CPS) {
            cpsBuffer += (cps - MAX_CPS);
            if (cpsBuffer >= 4.0) {
                cpsBuffer = 0;
                notifyBot();
                if (flag("cps=" + cps) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                    return;
                }
            }
        } else {
            cpsBuffer = Math.max(0, cpsBuffer - 1.0);
        }

        // 3. Robotic consistency & interval pattern checks
        if (!PerformanceProtectionManager.shouldSkipInventory() && lastClickTime > 0) {
            long interval = now - lastClickTime;

            if (interval >= 25 && interval <= 500) {
                // 3a. Exact constant delay check (e.g. 50ms, 50ms, 50ms)
                // Real autoclickers have exact 0-1ms variation across many clicks.
                // Require at least 8 consecutive identical intervals.
                if (lastInterval > 0 && Math.abs(interval - lastInterval) <= 1) {
                    consecutiveDuplicateIntervals++;
                    if (consecutiveDuplicateIntervals >= 8) {
                        consecutiveDuplicateIntervals = 4;
                        notifyBot();
                        flag("constant_delay: " + interval + "ms");
                    }
                } else if (Math.abs(interval - lastInterval) > 2) {
                    consecutiveDuplicateIntervals = 0;
                }
                lastInterval = interval;

                // 3b. Fixed tick delta check (strictly 1 tick = 50ms or 2 ticks = 100ms)
                // Cheats only lock to 1 tick (20 CPS) or 2 ticks (10 CPS).
                // Never check 3+ ticks (150ms+), which is completely normal human rhythmic clicking!
                int tickDiff = (int) Math.round((double) interval / 50.0);
                if (lastTickDiff > 0 && tickDiff == lastTickDiff && (tickDiff == 1 || tickDiff == 2) && Math.abs(interval - (tickDiff * 50)) <= 3) {
                    identicalTickCount++;
                    if (identicalTickCount >= 10) {
                        identicalTickCount = 6;
                        notifyBot();
                        flag("tick_locked: " + tickDiff + " ticks (~" + (tickDiff * 50) + "ms)");
                    }
                } else {
                    identicalTickCount = 0;
                }
                lastTickDiff = tickDiff;

                // 3c. Standard deviation across a statistically significant rolling window (20+ samples)
                intervals.addLast(interval);
                if (intervals.size() > 20) {
                    intervals.pollFirst();
                }

                if (intervals.size() >= 16) {
                    double mean = calculateMean(intervals);
                    double sd = calculateSD(intervals, mean);
                    player.packetStateData.currentClickStdDev = sd;
                    player.packetStateData.currentMeanInterval = mean;

                    // Unrandomized software autoclickers maintain sd < 1.5ms across 16+ intervals.
                    // Legitimate jitter and butterfly clicking naturally produce sd > 2.5ms across 16 intervals.
                    double maxAllowedSd = (mean < 80.0) ? 1.6 : (mean < 150.0 ? 2.0 : 2.5);
                    if (sd < maxAllowedSd && mean >= 35.0) {
                        sdBuffer += 1.0;
                        if (sdBuffer >= 2.0) {
                            sdBuffer = 1.0;
                            notifyBot();
                            flag("robotic_interval: sd=" + String.format("%.2f", sd) + "ms, mean=" + String.format("%.1f", mean) + "ms");
                        }
                    } else if (sd > maxAllowedSd + 2.0) {
                        sdBuffer = Math.max(0, sdBuffer - 0.5);
                    }
                }
            } else if (interval > 500) {
                // Reset consecutive streak after pause
                consecutiveDuplicateIntervals = 0;
                identicalTickCount = 0;
                lastInterval = 0;
                lastTickDiff = 0;
                sdBuffer = 0;
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
            sum += Math.pow(v - mean, 2);
            count++;
        }
        return count == 0 ? 0 : Math.sqrt(sum / count);
    }
}
