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
public class AutoTotem extends Check implements PacketReceiveListener {

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
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim || event.isCancelled()) return;

        // 1. Offhand swap packet (F key in-world)
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

        ItemStack mainHand = player.inventory.getHeldItem();
        ItemStack offHand = player.inventory.getOffHand();
        boolean mainIsTotem = isTotem(mainHand);
        boolean offIsTotem = isTotem(offHand);

        // If neither hand involves a totem, this is a legitimate weapon or item swap (spear, sword, tool, shield, food).
        // AutoTotem must NEVER interfere with non-totem weapon swaps or legitimate rapid F-key hotkeys.
        if (!mainIsTotem && !offIsTotem) {
            return;
        }

        // Sub-tick multi-swap (two totem swaps in <= 25ms)
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

        // If mainHand is a totem, the player is equipping a totem into offhand via F
        if (mainIsTotem) {
            // 1. Silent totem swap initiation: swap within 35ms of slot switch
            if (player.packetStateData.lastHeldItemChangeTime > 0) {
                long slotDiff = now - player.packetStateData.lastHeldItemChangeTime;
                if (slotDiff <= 35 && slotDiff >= 0) {
                    silentSwapCandidate = true;
                    silentSwapTime = now;
                    silentSwapPrevSlot = player.packetStateData.previousSlot;
                }
            }

            // 2. PickItem exploit sequence: SWAP -> PICK -> SWAP in <= 60ms
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

            // 3. Reaction timing check after totem pop
            checkTotemSwapTiming(event, now);
        }
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

        ItemStack slotItem = getInventoryItem(slot);
        boolean slotIsTotem = isTotem(slotItem);
        boolean carriedIsTotem = isTotem(click.getCarriedItemStack());

        // 1. Mouse Drag Detection (TotemGuard AutoTotemA):
        // Detects cheats that pick up a totem with mouse (PICKUP on slot X) and immediately drop it
        // into offhand (PICKUP on slot 45) in < 75ms (impossible human mouse travel).
        // This does NOT apply to the 'F' key (SWAP with button 40), which is a native 1-packet hotkey!
        if (clickType == WindowClickType.PICKUP) {
            if (!isOffhandSlot && (slotIsTotem || carriedIsTotem)) {
                player.packetStateData.lastTotemTouchTime = now;
                player.packetStateData.lastTotemTouchSlot = slot;
            } else if (isOffhandSlot) {
                if (player.packetStateData.lastTotemTouchTime > 0 && player.packetStateData.lastTotemTouchSlot != slot) {
                    long touchDiff = now - player.packetStateData.lastTotemTouchTime;
                    player.packetStateData.lastTotemTouchTime = 0;
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
        }

        // 2. Check totem pop reaction timing ONLY if this click actually equips a totem to offhand
        boolean equipsTotemToOffhand = (isOffhandSwap && slotIsTotem) || (isOffhandSlot && (carriedIsTotem || slotIsTotem));
        if (equipsTotemToOffhand) {
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
