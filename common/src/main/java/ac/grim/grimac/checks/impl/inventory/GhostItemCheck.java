/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3i;

@CheckData(
        name = "GhostItemCheck",
        stableKey = "grim.inventory.ghost_item",
        description = "Detects and prevents disappearing items when placing blocks into entities or obstacles"
)
public class GhostItemCheck extends Check implements BlockPlaceListener {

    public GhostItemCheck(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (!isEnabled() || player.disableGrim || player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) {
            return;
        }

        if (!place.isBlock || place.material == null || !place.material.isSolid() || place.material.isAir()) {
            return;
        }

        Vector3i placedPos = place.getPlacedBlockPos();
        SimpleCollisionBox blockBox = new SimpleCollisionBox(
                placedPos.getX(), placedPos.getY(), placedPos.getZ(),
                placedPos.getX() + 1, placedPos.getY() + 1, placedPos.getZ() + 1
        );

        // Check if placed block collides with player's own bounding box or any entity's bounding box
        boolean entityCollides = false;
        if (player.boundingBox != null && player.boundingBox.isIntersected(blockBox)) {
            entityCollides = true;
        } else if (player.compensatedEntities != null && player.compensatedEntities.entityMap != null) {
            for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
                if (entity != null && entity.getPossibleCollisionBoxes() != null && entity.getPossibleCollisionBoxes().isIntersected(blockBox)) {
                    entityCollides = true;
                    break;
                }
            }
        }

        if (entityCollides) {
            // Player attempted to place a block into an entity or player bounding box.
            // When this occurs, the server rejects placement (entity collision / isBuildable=false),
            // but the client has already locally deducted the item from the hand.
            // We forcefully resync inventory to guarantee the item never disappears as a ghost item!
            InventoryResync.resync(player);
        }
    }
}
