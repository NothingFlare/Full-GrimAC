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
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

@CheckData(
        name = "ItemDisappearanceCheck",
        stableKey = "grim.inventory.item_disappearance",
        description = "Detects and recovers disappearing, ghost, or desynced items during inventory and AutoTotem operations"
)
public class ItemDisappearanceCheck extends Check implements PacketReceiveListener {

    private int lastTotemCount = -1;
    private long lastThrowPacketTime = 0;

    public ItemDisappearanceCheck(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim || player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) return;

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            handleWindowClick(event);
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            handlePlayerDigging(event);
        } else if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            handleCloseWindow(event);
        }
    }

    private void handleWindowClick(PacketReceiveEvent event) {
        long now = System.currentTimeMillis();
        if (now < player.packetStateData.inventoryThrottleUntil) {
            event.setCancelled(true);
            player.onPacketCancel();
            return;
        }

        WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
        int windowId = click.getWindowId();
        int slot = click.getSlot();
        WindowClickType clickType = click.getWindowClickType();
        ItemStack carried = click.getCarriedItemStack();

        if (clickType == WindowClickType.THROW) {
            lastThrowPacketTime = now;
        }

        // 1. If another check cancelled this inventory click (e.g. AutoTotem, InventoryAction, InventoryAutoClicker):
        // The client has already locally applied the prediction (item picked up or moved to slot).
        // Since the packet was cancelled and rejected by the server, an authoritative resync is
        // required immediately to restore both the slot and cursor so items never vanish.
        if (event.isCancelled()) {
            player.packetStateData.inventoryThrottleUntil = Math.max(player.packetStateData.inventoryThrottleUntil, now + 400L);
            int originSlot = player.packetStateData.lastTotemTouchSlot;
            InventoryResync.resyncAndRestore(player, windowId, slot, originSlot, carried);
            lastTotemCount = countTotems();
            return;
        }

        // 2. Track totem count across inventory actions
        int currentTotems = countTotems();
        if (lastTotemCount >= 0 && currentTotems >= 0 && currentTotems < lastTotemCount) {
            int loss = lastTotemCount - currentTotems;
            boolean legitThrow = (now - lastThrowPacketTime < 300);
            boolean legitContainerDeposit = (windowId != 0);
            boolean legitTotemPop = (player.packetStateData.serverTotemPopTime > 0 && (now - player.packetStateData.serverTotemPopTime < 3000))
                    || (player.packetStateData.lastTotemPopTime > 0 && (now - player.packetStateData.lastTotemPopTime < 3000));

            if (!legitThrow && !legitContainerDeposit && !legitTotemPop) {
                // Unexpected drop in totem count without legitimate throw, pop, or container storage!
                // This indicates ghost item desynchronization or client-server discrepancy.
                flag("unexpected_totem_loss=" + loss + ", remaining=" + currentTotems);
                InventoryResync.resync(player, windowId, slot, carried);
            }
        }
        lastTotemCount = currentTotems;
    }

    private void handlePlayerDigging(PacketReceiveEvent event) {
        WrapperPlayClientPlayerDigging digging = new WrapperPlayClientPlayerDigging(event);
        DiggingAction action = digging.getAction();

        if (action == DiggingAction.DROP_ITEM || action == DiggingAction.DROP_ITEM_STACK) {
            lastThrowPacketTime = System.currentTimeMillis();
        } else if (action == DiggingAction.SWAP_ITEM_WITH_OFFHAND) {
            if (event.isCancelled()) {
                // SWAP was cancelled by anticheat (e.g. AutoTotem). Force resync offhand and hotbar.
                InventoryResync.resync(player, 0, Inventory.SLOT_OFFHAND, null);
                lastTotemCount = countTotems();
            }
        }
    }

    private void handleCloseWindow(PacketReceiveEvent event) {
        player.packetStateData.inventoryThrottleUntil = 0;
        // If player had an item on cursor when closing a window, ensure it is safely restored to inventory
        if (player.platformPlayer != null) {
            try {
                ac.grim.grimac.GrimAPI.INSTANCE.getScheduler().getEntityScheduler().execute(
                        player.platformPlayer,
                        ac.grim.grimac.GrimAPI.INSTANCE.getGrimPlugin(),
                        () -> {
                            try {
                                player.platformPlayer.restoreCursorToSlot(-1);
                            } catch (Exception ignored) {
                            }
                        },
                        null,
                        0
                );
            } catch (Exception ignored) {
            }
        }
        lastTotemCount = countTotems();
    }

    private int countTotems() {
        if (player.platformPlayer == null) return -1;
        try {
            ItemStack[] contents = player.platformPlayer.getInventory().getContents();
            if (contents == null) return -1;
            int count = 0;
            for (ItemStack item : contents) {
                if (item != null && !item.isEmpty() && isTotem(item)) {
                    count += item.getAmount();
                }
            }
            ItemStack cursor = player.platformPlayer.getInventory().getItemOnCursor();
            if (cursor != null && !cursor.isEmpty() && isTotem(cursor)) {
                count += cursor.getAmount();
            }
            return count;
        } catch (Exception ignored) {
            return -1;
        }
    }

    public static boolean isTotem(ItemStack stack) {
        return InventoryResync.isTotem(stack);
    }
}
