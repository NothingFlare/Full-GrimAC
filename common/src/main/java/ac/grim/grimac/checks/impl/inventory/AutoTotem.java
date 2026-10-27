/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.multiactions.MultiActionsC;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.checks.type.PrePredictionPacketReceiveListener;
import ac.grim.grimac.manager.performance.PerformanceProtectionManager;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckData(
        name = "AutoTotem",
        stableKey = "grim.inventory.autototem",
        description = "Equipped totem with impossible timing, sequence, or consistency"
)
public class AutoTotem extends Check implements PrePredictionPacketReceiveListener, PacketReceiveListener {

    private static final long MIN_LEGIT_REACTION_MS = 120L;
    private static final long MAX_EXPLOIT_SEQUENCE_MS = 60L;

    private int fastSwapSequence = 0;
    private long firstSwapTime = 0;
    private long lastSwapPacketTime = 0;

    // Silent totem swap tracking: HELD_ITEM_CHANGE -> SWAP -> HELD_ITEM_CHANGE back
    private boolean silentSwapCandidate = false;
    private long silentSwapTime = 0;
    private int silentSwapPrevSlot = -1;

    private final Deque<Long> swapIntervals = new ArrayDeque<>();

    public AutoTotem(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPrePredictionPacketReceive(PacketReceiveEvent event) {
        processIncomingPacket(event);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!event.isCancelled()) {
            processIncomingPacket(event);
        }
    }

    private void processIncomingPacket(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        // 1. Offhand swap packet (F key)
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging digging = new WrapperPlayClientPlayerDigging(event);
            if (digging.getAction() == DiggingAction.SWAP_ITEM_WITH_OFFHAND) {
                handleOffhandSwap(event);
            }
        } else if (event.getPacketType() == PacketType.Play.Client.PICK_ITEM) {
            handlePickItem();
        } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
            handleWindowClick(event, click);
        } else if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            handleHeldItemChange(event);
        }
    }

    private void handleHeldItemChange(PacketReceiveEvent event) {
        long now = System.currentTimeMillis();
        WrapperPlayClientHeldItemChange held = new WrapperPlayClientHeldItemChange(event);

        // Detect silent totem swap return: switched to totem, swapped with F, now immediately switching back
        if (silentSwapCandidate && (now - silentSwapTime <= 45)) {
            if (held.getSlot() == silentSwapPrevSlot) {
                silentSwapCandidate = false;
                long totalDuration = now - player.packetStateData.lastHeldItemChangeTime;
                if (flag("exploit=silent_totem_swap, duration=" + totalDuration + "ms") && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                    resyncHands();
                }
            }
        }
    }

    private void handleOffhandSwap(PacketReceiveEvent event) {
        long now = System.currentTimeMillis();
        player.packetStateData.lastOffhandSwapTime = now;

        // 1. Swap while using item (shield, food, bow)
        if (player.packetStateData.isSlowedByUsingItem()) {
            if (flag("exploit=swap_while_using_item") && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
                resyncHands();
                return;
            }
        }

        // 2. Sub-tick multi-swap (two swaps in <= 25ms)
        if (lastSwapPacketTime > 0) {
            long diff = now - lastSwapPacketTime;
            if (diff <= 25 && diff >= 0) {
                if (flag("exploit=subtick_multiswap, diff=" + diff + "ms") && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                    resyncHands();
                    return;
                }
            }
        }
        lastSwapPacketTime = now;

        // 3. Silent totem swap initiation: swap within 35ms of slot switch
        if (player.packetStateData.lastHeldItemChangeTime > 0) {
            long slotDiff = now - player.packetStateData.lastHeldItemChangeTime;
            if (slotDiff <= 35 && slotDiff >= 0) {
                silentSwapCandidate = true;
                silentSwapTime = now;
                silentSwapPrevSlot = player.packetStateData.previousSlot;
                if (flag("exploit=instant_hotbar_swap, diff=" + slotDiff + "ms") && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                    resyncHands();
                    return;
                }
            }
        }

        // 4. Instant damage-triggered swap (< 60ms from entity hurt)
        if (player.packetStateData.lastDamageTime > 0) {
            long dmgDiff = now - player.packetStateData.lastDamageTime;
            if (dmgDiff < 60 && dmgDiff >= 0 && player.packetStateData.serverTotemPopTime == 0) {
                if (flag("exploit=damage_instant_swap, diff=" + dmgDiff + "ms") && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                    resyncHands();
                    return;
                }
            }
        }

        // 5. PickItem exploit sequence: SWAP -> PICK -> SWAP in <= 60ms
        if (fastSwapSequence == 0) {
            fastSwapSequence = 1;
            firstSwapTime = now;
        } else if (fastSwapSequence == 2) {
            long totalTime = now - firstSwapTime;
            fastSwapSequence = 0;
            if (totalTime <= MAX_EXPLOIT_SEQUENCE_MS && totalTime >= 0) {
                if (flag("exploit=pick_item_swap, total=" + totalTime + "ms") && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                    resyncHands();
                    return;
                }
            }
        }

        checkTotemSwapTiming(event, now);
    }

    private void handlePickItem() {
        if (fastSwapSequence == 1) {
            fastSwapSequence = 2;
        } else {
            fastSwapSequence = 0;
        }
    }

    private void handleWindowClick(PacketReceiveEvent event, WrapperPlayClientClickWindow click) {
        long now = System.currentTimeMillis();
        int slot = click.getSlot();
        int button = click.getButton();
        WindowClickType clickType = click.getWindowClickType();

        boolean isOffhandSlot = (click.getWindowId() == 0 && slot == Inventory.SLOT_OFFHAND);
        boolean isOffhandSwap = (clickType == WindowClickType.SWAP && button == 40);
        boolean clickedIsTotem = isTotem(click.getCarriedItemStack());
        if (!clickedIsTotem && click.getSlots().isPresent()) {
            for (ItemStack s : click.getSlots().get().values()) {
                if (isTotem(s)) {
                    clickedIsTotem = true;
                    break;
                }
            }
        }
        boolean currentOffhandIsTotem = isTotem(player.inventory.getOffHand())
                || (player.platformPlayer != null && isTotem(player.platformPlayer.getInventory().getItemInOffHand()));
        boolean involvesTotem = isOffhandSlot || isOffhandSwap || clickedIsTotem || currentOffhandIsTotem;

        // 1. Moving / Sprinting Inventory Click Check (Vanilla clients cannot move while inventory GUI is open)
        if (isMoving(player)) {
            // Any inventory interaction while physically moving/sprinting is impossible in vanilla
            if (flag("exploit=moving_inventory_click, slot=" + slot + ", button=" + button) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
                resyncHands();
                return;
            }
        }

        // 2. Track when a totem is clicked or picked up from the inventory
        if (!isOffhandSlot && (clickedIsTotem || isTotem(getInventoryItem(slot)))) {
            player.packetStateData.lastTotemTouchTime = now;
            player.packetStateData.lastTotemTouchSlot = slot;
        }

        // 3. TotemGuard AutoTotemA: Instant totem-to-offhand transfer (< 75ms)
        if (isOffhandSlot || isOffhandSwap) {
            if (player.packetStateData.lastTotemTouchTime > 0) {
                long touchDiff = now - player.packetStateData.lastTotemTouchTime;
                if (touchDiff >= 0 && touchDiff < 75) {
                    if (flag("exploit=instant_totem_transfer, diff=" + touchDiff + "ms") && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                        resyncHands();
                        return;
                    }
                }
            }
        }

        // 4. In-world combat attack within 50ms of inventory click
        long attackDiff = Math.abs(now - player.packetStateData.lastAttackPacketTime);
        if (attackDiff < 50 && player.packetStateData.lastAttackPacketTime > 0) {
            if (flag("exploit=combat_while_inventory, diff=" + attackDiff + "ms") && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
                resyncHands();
                return;
            }
        }

        // 5. Check totem pop reaction timing
        if (involvesTotem) {
            checkTotemSwapTiming(event, now);
        }
    }

    private void checkTotemSwapTiming(PacketReceiveEvent event, long now) {
        // A. Primary check based on serverTotemPopTime (timestamp when the server popped the player's totem)
        if (player.packetStateData.serverTotemPopTime > 0) {
            long elapsed = now - player.packetStateData.serverTotemPopTime;
            long ping = Math.max(0, (long) player.getTransactionPing());
            long minLegitTime = Math.max(75L, ping + 60L);

            if (elapsed >= 0 && elapsed < minLegitTime) {
                player.packetStateData.serverTotemPopTime = 0;
                player.packetStateData.lastTotemPopTime = 0;
                player.packetStateData.lastReactionTime = elapsed;
                if (flag("fast_swap=" + elapsed + "ms, min=" + minLegitTime + "ms, ping=" + ping + "ms") && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                    resyncHands();
                }
                return;
            } else if (elapsed >= 2000) {
                player.packetStateData.serverTotemPopTime = 0;
            }
        }

        // B. Secondary check based on real-time task arrival
        if (player.packetStateData.lastTotemPopTime > 0) {
            long elapsed = now - player.packetStateData.lastTotemPopTime;

            if (elapsed >= 0 && elapsed < 1500) {
                player.packetStateData.lastReactionTime = elapsed;

                if (elapsed < MIN_LEGIT_REACTION_MS) {
                    player.packetStateData.lastTotemPopTime = 0;
                    if (flag("fast_swap=" + elapsed + "ms") && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                        resyncHands();
                    }
                    return;
                }

                // If TPS protection is active, skip standard deviation calculations
                if (PerformanceProtectionManager.shouldSkipInventory()) {
                    return;
                }

                swapIntervals.addLast(elapsed);
                if (swapIntervals.size() > 8) {
                    swapIntervals.pollFirst();
                }

                if (swapIntervals.size() >= 3) {
                    double mean = calculateMean(swapIntervals);
                    double sd = calculateSD(swapIntervals, mean);
                    if (sd < 6.0 && mean < 180.0) {
                        flag("consistent_sd=" + String.format("%.2f", sd) + "ms, mean=" + String.format("%.1f", mean) + "ms");
                    }
                }
            } else if (elapsed >= 1500) {
                player.packetStateData.lastTotemPopTime = 0;
            }
        }
    }

    public static boolean isMoving(GrimPlayer player) {
        if (player.isSprinting && (!player.isSwimming || !player.clientClaimsLastOnGround)) return true;
        if (MultiActionsC.isVerboseInput(player)) return true;
        double dx = player.x - player.lastX;
        double dz = player.z - player.lastZ;
        if ((dx * dx + dz * dz) > 0.0004) return true;
        if (player.actualMovement != null) {
            double ax = player.actualMovement.getX();
            double az = player.actualMovement.getZ();
            if ((ax * ax + az * az) > 0.0004) return true;
        }
        return false;
    }

    private ItemStack getInventoryItem(int slot) {
        if (slot >= 0 && slot < player.inventory.inventory.getInventoryStorage().getSize()) {
            ItemStack item = player.inventory.inventory.getInventoryStorage().getItem(slot);
            if (item != null) return item;
        }
        return ItemStack.EMPTY;
    }

    private void resyncHands() {
        if (player.user == null) return;
        ItemStack mainHand = player.platformPlayer != null ? player.platformPlayer.getInventory().getItemInHand() : player.inventory.getHeldItem();
        ItemStack offHand = player.platformPlayer != null ? player.platformPlayer.getInventory().getItemInOffHand() : player.inventory.getOffHand();
        player.user.sendPacket(new WrapperPlayServerSetSlot(0, player.inventory.stateID, 45, offHand == null ? ItemStack.EMPTY : offHand));
        player.user.sendPacket(new WrapperPlayServerSetSlot(0, player.inventory.stateID, 36 + player.packetStateData.lastSlotSelected, mainHand == null ? ItemStack.EMPTY : mainHand));
    }

    private static boolean isTotem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getType() == null) return false;
        String name = stack.getType().getName().getKey();
        return name != null && name.contains("totem");
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
