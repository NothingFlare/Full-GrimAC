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
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(
        name = "InventoryAction",
        stableKey = "grim.inventory.invalid_action",
        description = "Interacted in world or attacked while interacting with inventory"
)
public class InventoryAction extends Check implements PacketReceiveListener {

    public InventoryAction(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        // Skip on low server TPS to save resources
        if (PerformanceProtectionManager.shouldSkipInventory()) return;

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            long now = System.currentTimeMillis();

            // Check if player attacked an entity in the same tick / recent window
            long attackDelta = Math.abs(now - player.packetStateData.lastAttackPacketTime);
            if (player.packetStateData.lastAttackPacketTime > 0 && attackDelta < 30) {
                if (flag("action=attack, delta=" + attackDelta + "ms") && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }

            // Check if player is currently using an item (charging bow, eating, blocking)
            if (player.packetStateData.isSlowedByUsingItem()) {
                if (flag("action=using_item") && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            handleAttackAfterClick(event);
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                handleAttackAfterClick(event);
            }
        }
    }

    private void handleAttackAfterClick(PacketReceiveEvent event) {
        long now = System.currentTimeMillis();
        long clickDelta = Math.abs(now - player.packetStateData.lastWindowClickTime);
        if (player.packetStateData.lastWindowClickTime > 0 && clickDelta < 30) {
            if (flag("action=attack_after_click, delta=" + clickDelta + "ms") && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }
    }
}
