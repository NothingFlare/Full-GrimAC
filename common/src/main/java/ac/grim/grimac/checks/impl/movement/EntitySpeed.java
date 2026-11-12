/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.Collections;

@CheckData(
        name = "EntitySpeed",
        stableKey = "grim.movement.entity_speed",
        description = "Moving faster than allowed while colliding with or passing through entities",
        setback = 3
)
public class EntitySpeed extends Check implements PostPredictionListener {

    private double buffer = 0.0;
    private int lastTechnicalMovementTick = -100;

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

        // Exempt technical redstone / block movements (pistons, slime launchers, piston bolts, honey conveyors, etc.)
        if (isTechnicalMovement()) {
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

    private boolean isTechnicalMovement() {
        int currentTick = GrimAPI.INSTANCE.getTickManager().currentTick;

        // 1. Direct piston displacement in uncertainty queues
        if (Collections.max(player.uncertaintyHandler.pistonX) > 0.001
                || Collections.max(player.uncertaintyHandler.pistonY) > 0.001
                || Collections.max(player.uncertaintyHandler.pistonZ) > 0.001) {
            lastTechnicalMovementTick = currentTick;
            return true;
        }

        // 2. Active pistons in compensated world or slime piston bounces
        if (!player.compensatedWorld.activePistons.isEmpty()
                || !player.uncertaintyHandler.slimePistonBounces.isEmpty()) {
            lastTechnicalMovementTick = currentTick;
            return true;
        }

        // 3. Slime blocks, honey blocks, and bouncy block interactions
        if (player.uncertaintyHandler.isSteppingOnSlime
                || player.uncertaintyHandler.wasSteppingOnSlime
                || player.uncertaintyHandler.isSteppingOnHoney
                || player.uncertaintyHandler.isSteppingOnBouncyBlock
                || player.uncertaintyHandler.wasSteppingOnBouncyBlock
                || player.uncertaintyHandler.thisTickSlimeBlockUncertainty != 0
                || player.uncertaintyHandler.nextTickSlimeBlockUncertainty != 0) {
            lastTechnicalMovementTick = currentTick;
            return true;
        }

        // 4. Bubble columns, shulker boxes/entities, and step movements
        if (player.uncertaintyHandler.isSteppingNearBubbleColumn
                || player.uncertaintyHandler.isSteppingNearShulker
                || player.uncertaintyHandler.isNearGlitchyBlock
                || player.uncertaintyHandler.isOrWasNearGlitchyBlock
                || player.uncertaintyHandler.isStepMovement) {
            lastTechnicalMovementTick = currentTick;
            return true;
        }

        // 5. Proximity to piston or slime/honey mechanisms in world
        if (isNearPistonMechanism()) {
            lastTechnicalMovementTick = currentTick;
            return true;
        }

        // 6. Inertial flight grace period (25 ticks = 1.25 seconds after last technical push)
        if (Math.abs(currentTick - lastTechnicalMovementTick) <= 25) {
            return true;
        }

        return false;
    }

    private boolean isNearPistonMechanism() {
        int px = (int) Math.floor(player.x);
        int py = (int) Math.floor(player.y);
        int pz = (int) Math.floor(player.z);

        for (int x = px - 2; x <= px + 2; x++) {
            for (int y = py - 1; y <= py + 2; y++) {
                for (int z = pz - 2; z <= pz + 2; z++) {
                    WrappedBlockState state = player.compensatedWorld.getBlock(x, y, z);
                    if (state != null && state.getType() != null) {
                        StateType type = state.getType();
                        if (type == StateTypes.PISTON
                                || type == StateTypes.STICKY_PISTON
                                || type == StateTypes.PISTON_HEAD
                                || type == StateTypes.MOVING_PISTON
                                || type == StateTypes.SLIME_BLOCK
                                || type == StateTypes.HONEY_BLOCK) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
