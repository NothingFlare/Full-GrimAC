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
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;

public final class InventoryResync {

    private InventoryResync() {
    }

    public static void resync(GrimPlayer player) {
        resyncAndRestore(player, 0, -1, player != null ? player.packetStateData.lastTotemTouchSlot : -1, null);
    }

    public static void resync(GrimPlayer player, int windowId, int slot, ItemStack fallbackItem) {
        resyncAndRestore(player, windowId, slot, player != null ? player.packetStateData.lastTotemTouchSlot : -1, fallbackItem);
    }

    public static void resyncAndRestore(GrimPlayer player, int windowId, int slot, int originSlot, ItemStack fallbackItem) {
        if (player == null || player.user == null) return;

        // 1. First priority: restore server cursor item back to its origin slot on the server thread
        if (player.platformPlayer != null) {
            try {
                GrimAPI.INSTANCE.getScheduler().getEntityScheduler().execute(
                        player.platformPlayer,
                        GrimAPI.INSTANCE.getGrimPlugin(),
                        () -> {
                            try {
                                player.platformPlayer.restoreCursorToSlot(originSlot);
                            } catch (Exception ignored) {
                            }
                        },
                        null,
                        0
                );
            } catch (Exception ignored) {
            }
        }

        try {
            int stateId = player.inventory.stateID;

            // 1. Hands (Main hand and Offhand)
            ItemStack mainHand = player.platformPlayer != null
                    ? player.platformPlayer.getInventory().getItemInHand()
                    : player.inventory.getHeldItem();
            ItemStack offHand = player.platformPlayer != null
                    ? player.platformPlayer.getInventory().getItemInOffHand()
                    : player.inventory.getOffHand();

            player.user.sendPacket(new WrapperPlayServerSetSlot(0, stateId, 45, offHand == null ? ItemStack.EMPTY : offHand));
            player.user.sendPacket(new WrapperPlayServerSetSlot(0, stateId, 36 + player.packetStateData.lastSlotSelected, mainHand == null ? ItemStack.EMPTY : mainHand));

            // 2. Cursor item - NEVER blindly clear to EMPTY!
            // Retrieve server's real cursor item. If player was carrying an item, restore it!
            ItemStack cursorItem = ItemStack.EMPTY;
            if (player.platformPlayer != null) {
                cursorItem = player.platformPlayer.getInventory().getItemOnCursor();
            }
            if ((cursorItem == null || cursorItem.isEmpty()) && player.inventory.menu != null) {
                cursorItem = player.inventory.menu.getCarried();
            }
            if ((cursorItem == null || cursorItem.isEmpty()) && fallbackItem != null && !fallbackItem.isEmpty()) {
                cursorItem = fallbackItem;
            }

            ItemStack finalCursor = (cursorItem != null && !cursorItem.isEmpty()) ? cursorItem : ItemStack.EMPTY;
            player.user.sendPacket(new WrapperPlayServerSetSlot(-1, stateId, -1, finalCursor));
            if (windowId != 0) {
                player.user.sendPacket(new WrapperPlayServerSetSlot(windowId, stateId, -1, finalCursor));
            }

            // 3. Specifically restore clicked slot if valid
            if (slot >= 0) {
                ItemStack slotItem = ItemStack.EMPTY;
                if (player.platformPlayer != null) {
                    slotItem = player.platformPlayer.getInventory().getOpenInventoryItem(slot);
                }
                if ((slotItem == null || slotItem.isEmpty()) && player.inventory.menu != null && slot < player.inventory.menu.getSlots().size()) {
                    slotItem = player.inventory.menu.getSlot(slot).getItem();
                }
                if (slotItem != null && !slotItem.isEmpty()) {
                    player.user.sendPacket(new WrapperPlayServerSetSlot(windowId, stateId, slot, slotItem));
                }
            }

            // 4. Forcefully refresh all totem slots to guarantee totems NEVER vanish into ghost items
            if (player.platformPlayer != null) {
                ItemStack[] contents = player.platformPlayer.getInventory().getContents();
                if (contents != null) {
                    for (int i = 0; i < contents.length; i++) {
                        ItemStack item = contents[i];
                        if (item != null && !item.isEmpty() && isTotem(item)) {
                            int vanillaSlot;
                            if (i < 9) {
                                vanillaSlot = i + 36; // Bukkit hotbar 0-8 -> vanilla 36-44
                            } else if (i < 36) {
                                vanillaSlot = i; // Bukkit inventory 9-35 -> vanilla 9-35
                            } else if (i < 40) {
                                vanillaSlot = 8 - (i - 36); // Bukkit armor 36-39 (boots->8, legs->7, chest->6, helm->5)
                            } else if (i == 40) {
                                vanillaSlot = 45; // Bukkit offhand 40 -> vanilla 45
                            } else {
                                continue;
                            }
                            player.user.sendPacket(new WrapperPlayServerSetSlot(0, stateId, vanillaSlot, item));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 5. Schedule full Bukkit/Paper updateInventory on the entity thread (Folia safe)
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

    public static boolean isTotem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getType() == null) return false;
        if (stack.getType() == ItemTypes.TOTEM_OF_UNDYING) return true;
        try {
            String name = stack.getType().getName().getKey().toLowerCase(java.util.Locale.ROOT);
            return name.contains("totem");
        } catch (Exception ignored) {
            return false;
        }
    }
}
