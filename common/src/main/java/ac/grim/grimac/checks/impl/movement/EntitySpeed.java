/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.player.GameMode;

import java.util.Collections;

@CheckData(
        name = "EntitySpeed",
        stableKey = "grim.movement.entity_speed",
        description = "Moving faster than allowed while colliding with or passing through entities",
        setback = 3
)
public class EntitySpeed extends Check implements PostPredictionListener {

    private double buffer = 0.0;

    public EntitySpeed(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked() || player.disableGrim) return;
        if (player.gamemode == GameMode.SPECTATOR || player.gamemode == GameMode.CREATIVE) return;
        if (player.inVehicle() || player.isGliding || player.wasGliding) return;

        int colliding = player.uncertaintyHandler.collidingEntities.isEmpty()
                ? 0
                : Collections.max(player.uncertaintyHandler.collidingEntities);

        // Only check when player is intersecting or pushing through pushable entities
        if (colliding <= 0) {
            buffer = Math.max(0.0, buffer - 0.2);
            return;
        }

        // Exempt external forces that legitimately boost velocity
        if (player.likelyKB != null || player.firstBreadKB != null
                || player.likelyExplosions != null || player.firstBreadExplosion != null
                || player.wasTouchingWater || player.wasWasTouchingWater
                || player.riptideSpinAttackTicks > 0) {
            buffer = Math.max(0.0, buffer - 0.5);
            return;
        }

        // Horizontal displacement in world this tick
        double dx = player.x - player.lastX;
        double dz = player.z - player.lastZ;
        double speed = Math.hypot(dx, dz);

        // Speed potion effect multiplier (each level adds 20% to base speed)
        // Default player.speed is 0.1
        double speedFactor = Math.max(1.0, player.speed / 0.1);

        // Legitimate max speeds in vanilla:
        // Ground sprint: ~0.286 m/tick * speedFactor
        // Sprint jumping in air: peak ~0.36 m/tick * speedFactor
        // Ice: can reach ~0.55 m/tick
        double baseMax;
        if (player.uncertaintyHandler.isSteppingOnIce) {
            baseMax = 0.55 * speedFactor;
        } else if (!player.onGround && !player.lastOnGround) {
            baseMax = 0.36 * speedFactor;
        } else {
            baseMax = 0.286 * speedFactor;
        }

        // Maximum legitimate vanilla entity push from behind is at most 0.035 m/tick
        double maxLegitSpeed = baseMax + 0.035;

        // Tolerance before flagging: 0.04
        if (speed > maxLegitSpeed + 0.04) {
            double excess = speed - maxLegitSpeed;
            buffer += Math.min(2.0, excess * 10.0);

            if (buffer >= 2.0) {
                buffer = 1.0;
                if (flag("speed=" + String.format("%.3f", speed) + ", max=" + String.format("%.3f", maxLegitSpeed) + ", entities=" + colliding) && shouldModifyPackets()) {
                    executeViolationSetback();
                }
            }
        } else {
            buffer = Math.max(0.0, buffer - 0.3);
            reward();
        }
    }
}
