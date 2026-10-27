/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;

public final class InventoryResync {

    private InventoryResync() {
    }

    public static void resync(GrimPlayer player) {
        if (player == null || player.user == null) return;

        try {
            // 1. Immediately send server-side true items for hands and clear client cursor to prevent phantom items
            ItemStack mainHand = player.platformPlayer != null
                    ? player.platformPlayer.getInventory().getItemInHand()
                    : player.inventory.getHeldItem();
            ItemStack offHand = player.platformPlayer != null
                    ? player.platformPlayer.getInventory().getItemInOffHand()
                    : player.inventory.getOffHand();

            int stateId = player.inventory.stateID;
            // Offhand slot (slot 45)
            player.user.sendPacket(new WrapperPlayServerSetSlot(0, stateId, 45, offHand == null ? ItemStack.EMPTY : offHand));
            // Held hotbar slot
            player.user.sendPacket(new WrapperPlayServerSetSlot(0, stateId, 36 + player.packetStateData.lastSlotSelected, mainHand == null ? ItemStack.EMPTY : mainHand));
            // Cursor / Carried slot (-1)
            player.user.sendPacket(new WrapperPlayServerSetSlot(-1, stateId, -1, ItemStack.EMPTY));
            player.user.sendPacket(new WrapperPlayServerSetSlot(0, stateId, -1, ItemStack.EMPTY));
        } catch (Exception ignored) {
        }

        // 2. Schedule full Bukkit/Paper updateInventory on the entity thread (Folia safe)
        // This completely refreshes all slots from the server's authoritative state
        if (player.platformPlayer != null) {
            try {
                GrimAPI.INSTANCE.getScheduler().getEntityScheduler().execute(
                        player.platformPlayer,
                        GrimAPI.INSTANCE.getGrimPlugin(),
                        () -> {
                            try {
                                player.platformPlayer.updateInventory();
                            } catch (Exception ignored) {
                            }
                        },
                        null,
                        0
                );
            } catch (Exception ignored) {
            }
        }
    }
}
