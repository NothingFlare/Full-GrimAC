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
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;

@CheckData(
        name = "AutoTotem",
        stableKey = "grim.inventory.autototem",
        description = "Equipped totem with impossible timing, sequence, or consistency"
)
public class AutoTotem extends Check implements PacketReceiveListener {

    private static final long MAX_EXPLOIT_SEQUENCE_MS = 40L;

    private int fastSwapSequence = 0;
    private long firstSwapTime = 0;
    private long lastSwapPacketTime = 0;

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
        // If player swapped offhand within 45ms, and now immediately switches hotbar slot back
        if (player.packetStateData.lastOffhandSwapTime > 0) {
            long swapDiff = now - player.packetStateData.lastOffhandSwapTime;
            if (swapDiff <= 45 && swapDiff >= 0) {
                if (flag("exploit=silent_swap_return, diff=" + swapDiff + "ms") && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                    InventoryResync.resync(player);
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
                    InventoryResync.resync(player);
                    return;
                }
            }
        }
        lastSwapPacketTime = now;

        // If mainHand is a totem, the player is equipping a totem into offhand via F
        if (mainIsTotem) {
            // 1. Silent totem swap: player switched to this totem slot in under 45ms
            if (player.packetStateData.lastHeldItemChangeTime > 0) {
                long slotDiff = now - player.packetStateData.lastHeldItemChangeTime;
                if (slotDiff <= 45 && slotDiff >= 0) {
                    if (flag("exploit=silent_totem_swap, diff=" + slotDiff + "ms") && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                        InventoryResync.resync(player);
                        return;
                    }
                }
            }

            // 2. PickItem exploit sequence: SWAP -> PICK -> SWAP in <= 40ms
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
                        InventoryResync.resync(player);
                        return;
                    }
                }
            }

            // 3. Reaction timing check after totem pop
            if (checkTotemSwapTiming(event, now)) {
                return;
            }
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

        Optional<Map<Integer, ItemStack>> changedSlots = click.getSlots();
        boolean changedHasTotem = changedSlots.isPresent() && changedSlots.get().values().stream().anyMatch(AutoTotem::isTotem);
        boolean isOffhandSlot = (click.getWindowId() == 0 && slot == Inventory.SLOT_OFFHAND);
        boolean isOffhandSwap = (clickType == WindowClickType.SWAP && button == 40);

        ItemStack slotItem = getInventoryItem(slot);
        ItemStack carried = click.getCarriedItemStack();
        boolean slotIsTotem = isTotem(slotItem) || changedHasTotem;
        boolean carriedIsTotem = isTotem(carried);

        // 1. Mouse Drag Detection (TotemGuard AutoTotemA):
        // Detects cheats that pick up a totem with mouse (PICKUP on slot X) and immediately drop it
        // into offhand (PICKUP on slot 45) in < 65ms (impossible human mouse travel).
        if (clickType == WindowClickType.PICKUP) {
            if (!isOffhandSlot && (slotIsTotem || carriedIsTotem)) {
                player.packetStateData.lastTotemTouchTime = now;
                player.packetStateData.lastTotemTouchSlot = slot;
            } else if (isOffhandSlot) {
                if (player.packetStateData.lastTotemTouchTime > 0 && player.packetStateData.lastTotemTouchSlot != slot) {
                    long touchDiff = now - player.packetStateData.lastTotemTouchTime;
                    player.packetStateData.lastTotemTouchTime = 0;
                    if (touchDiff >= 0 && touchDiff < 65) {
                        if (flag("exploit=instant_totem_transfer, diff=" + touchDiff + "ms") && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                            InventoryResync.resync(player);
                            return;
                        }
                    }
                }
            }
        }

        // 2. Any offhand action (SWAP button 40, or click into offhand slot 45):
        if (isOffhandSwap || isOffhandSlot) {
            // A. Check reaction timing if a totem popped on the player
            if (checkTotemSwapTiming(event, now)) {
                return;
            }

            // B. Combat Totem Swap: Player cannot attack an entity and swap totem into offhand in < 60ms
            if (player.packetStateData.lastAttackPacketTime > 0) {
                long attackDelta = now - player.packetStateData.lastAttackPacketTime;
                if (attackDelta >= 0 && attackDelta < 60) {
                    if (flag("exploit=combat_totem_swap, delta=" + attackDelta + "ms") && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                        InventoryResync.resync(player);
                        return;
                    }
                }
            }
        }
    }

    private boolean checkTotemSwapTiming(PacketReceiveEvent event, long now) {
        // A. Primary check based on serverTotemPopTime (timestamp when the server popped the player's totem)
        if (player.packetStateData.serverTotemPopTime > 0) {
            long elapsed = now - player.packetStateData.serverTotemPopTime;
            long ping = Math.max(0, (long) player.getTransactionPing());
            long minLegitTime = ping + 90L;

            if (elapsed >= 0 && elapsed < minLegitTime) {
                player.packetStateData.serverTotemPopTime = 0;
                player.packetStateData.lastTotemPopTime = 0;
                player.packetStateData.lastReactionTime = elapsed;
                if (flag("fast_swap=" + elapsed + "ms, min=" + minLegitTime + "ms, ping=" + ping + "ms") && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                    InventoryResync.resync(player);
                }
                return true;
            } else if (elapsed >= 2500) {
                player.packetStateData.serverTotemPopTime = 0;
            }
        }

        // B. Secondary check based on real-time task arrival
        if (player.packetStateData.lastTotemPopTime > 0) {
            long elapsed = now - player.packetStateData.lastTotemPopTime;

            if (elapsed >= 0 && elapsed < 2500) {
                player.packetStateData.lastReactionTime = elapsed;

                if (elapsed < 90L) {
                    player.packetStateData.lastTotemPopTime = 0;
                    player.packetStateData.serverTotemPopTime = 0;
                    if (flag("fast_swap_rtt=" + elapsed + "ms") && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                        InventoryResync.resync(player);
                    }
                    return true;
                }

                // If TPS protection is active, skip standard deviation calculations
                if (PerformanceProtectionManager.shouldSkipInventory()) {
                    return false;
                }

                swapIntervals.addLast(elapsed);
                if (swapIntervals.size() > 8) {
                    swapIntervals.pollFirst();
                }

                if (swapIntervals.size() >= 4) {
                    double mean = calculateMean(swapIntervals);
                    double sd = calculateSD(swapIntervals, mean);
                    if (sd < 2.0 && mean < 90.0) {
                        flag("consistent_sd=" + String.format("%.2f", sd) + "ms, mean=" + String.format("%.1f", mean) + "ms");
                    }
                }
            } else if (elapsed >= 2500) {
                player.packetStateData.lastTotemPopTime = 0;
            }
        }
        return false;
    }

    private ItemStack getInventoryItem(int slot) {
        if (player.inventory.menu != null && slot >= 0 && slot < player.inventory.menu.getSlots().size()) {
            try {
                ItemStack item = player.inventory.menu.getSlot(slot).getItem();
                if (item != null && !item.isEmpty()) return item;
            } catch (Exception ignored) {}
        }
        if (slot >= 0 && slot < player.inventory.inventory.getInventoryStorage().getSize()) {
            ItemStack item = player.inventory.inventory.getInventoryStorage().getItem(slot);
            if (item != null && !item.isEmpty()) return item;
        }
        return ItemStack.EMPTY;
    }

    private static boolean isTotem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getType() == null) return false;
        if (stack.getType() == ItemTypes.TOTEM_OF_UNDYING) return true;
        String name = stack.getType().getName().getKey();
        return name != null && name.toLowerCase().contains("totem");
    }

    private static double calculateMean(Iterable<Long> values) {
        long sum = 0;
        int count = 0;
        for (long v : values) {
            sum += v;
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
