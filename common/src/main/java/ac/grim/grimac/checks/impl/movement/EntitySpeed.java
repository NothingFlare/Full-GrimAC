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
import ac.grim.grimac.utils.enums.Pose;
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
    private int lastKnockbackTick = -100;

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
        if (player.wasTouchingWater || player.wasWasTouchingWater
                || player.isRiptidePose || player.pose == Pose.SPIN_ATTACK
                || player.riptideSpinAttackTicks > 0
                || isRiptideMovement()) {
            buffer = Math.max(0.0, buffer - 0.5);
            return;
        }

        // Exempt knockback from sword hits, mob attacks, arrows, and explosions (including 25-tick inertial flight)
        if (isKnockbackMovement()) {
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
        // Sprint jumping forward: peak ~0.62 m/tick * speedFactor
        // 45-degree angle sprint jumping (W+A / W+D, strafing): peak ~0.72 m/tick * speedFactor
        // Low ceiling / head-hitter jump sprint (caves, 2-block tunnels): peak ~0.88 m/tick * speedFactor
        // Ice sprint jumping: can reach ~1.05 m/tick * speedFactor
        double baseMax;
        if (player.uncertaintyHandler.isSteppingOnIce) {
            baseMax = 1.05 * speedFactor;
        } else if (!player.onGround || !player.lastOnGround || player.isSprinting || player.lastSprinting || player.lastSprintingForSpeed) {
            // In air, jumping, sprinting, strafe jumping, 2-block ceiling head-hitters, or sprint reset inertia after attack
            baseMax = 0.88 * speedFactor;
        } else {
            // Normal walking on ground
            baseMax = 0.35 * speedFactor;
        }

        // Vanilla entity collision push can add up to ~0.08 m/tick per entity (capped at 0.40 m/tick)
        double pushAllowance = Math.min(0.40, colliding * 0.08);
        double maxLegitSpeed = baseMax + pushAllowance;

        // If speed is within legitimate vanilla limits, immediately decay buffer and return
        if (speed <= maxLegitSpeed + 0.05) {
            buffer = Math.max(0.0, buffer - 0.5);
            reward();
            return;
        }

        // Only flag sustained impossible speeds (e.g. EntitySpeed / PushBoost cheats reaching 1.5 - 50 m/tick)
        double excess = speed - maxLegitSpeed;
        buffer += Math.min(2.0, excess * 2.0);

        if (buffer >= 5.0) {
            buffer = 3.0;
            if (flag("speed=" + String.format("%.3f", speed) + ", max=" + String.format("%.3f", maxLegitSpeed) + ", entities=" + colliding) && shouldModifyPackets()) {
                executeViolationSetback();
            }
        }
    }

    private boolean isKnockbackMovement() {
        int currentTick = GrimAPI.INSTANCE.getTickManager().currentTick;

        // 1. Direct knockback or explosion active this tick
        if (player.likelyKB != null || player.firstBreadKB != null
                || player.likelyExplosions != null || player.firstBreadExplosion != null
                || (player.predictedVelocity != null && (player.predictedVelocity.isKnockback() || player.predictedVelocity.isExplosion()))) {
            lastKnockbackTick = currentTick;
            return true;
        }

        // 2. Pending unacknowledged knockback or explosion packets sent from server
        if (player.checkManager.getKnockbackHandler().hasActiveOrPendingKnockback()
                || player.checkManager.getExplosionHandler().hasActiveOrPendingExplosion()) {
            lastKnockbackTick = currentTick;
            return true;
        }

        // 3. Post-knockback inertial flight grace period (25 ticks = 1.25 seconds)
        if (Math.abs(currentTick - lastKnockbackTick) <= 25) {
            return true;
        }

        return false;
    }

    private boolean isRiptideMovement() {
        int currentTick = GrimAPI.INSTANCE.getTickManager().currentTick;

        // 1. Direct riptide active this tick
        if (player.isRiptidePose || player.pose == Pose.SPIN_ATTACK
                || player.riptideSpinAttackTicks > 0
                || player.packetStateData.tryingToRiptide
                || (player.predictedVelocity != null && player.predictedVelocity.isTrident())) {
            player.lastRiptideTick = currentTick;
            return true;
        }

        // 2. Ballistic flight after riptide launch (up to 45 ticks = 2.25 seconds)
        if (Math.abs(currentTick - player.lastRiptideTick) <= 45) {
            return true;
        }

        // 3. Target player recently collided with or pushed by a riptiding player (up to 30 ticks = 1.5 seconds)
        if (Math.abs(currentTick - player.lastCollidedWithRiptideTick) <= 30) {
            return true;
        }

        return false;
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
                || player.pointThreeEstimator.isNearBubbleColumn()
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
