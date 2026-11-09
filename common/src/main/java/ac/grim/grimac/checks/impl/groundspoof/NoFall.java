/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.checks.impl.groundspoof;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.checks.type.PostPredictionListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.GhostBlockDetector;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

@CheckData(
        name = "NoFall",
        stableKey = "grim.groundspoof.no_fall",
        description = "Sent an on-ground packet while not colliding with the ground, or spoofed ground state to avoid fall damage",
        setback = 2,
        decay = 0.05
)
public class NoFall extends Check implements PacketReceiveListener, PostPredictionListener {

    public boolean flipPlayerGroundStatus = false;
    private double trackedFallDistance = 0.0;
    private double fakeGroundBuffer = 0.0;
    private double noGroundBuffer = 0.0;
    private boolean flaggedDuringCurrentFall = false;

    public NoFall(GrimPlayer player) {
        super(player);
    }

    private boolean isExemptFromNoFall() {
        if (player.disableGrim) return true;
        if (player.gamemode == GameMode.SPECTATOR || player.gamemode == GameMode.CREATIVE) return true;
        if (player.inVehicle() || player.isGliding || player.wasGliding) return true;
        if (player.isFlying || player.wasFlying || player.canFly) return true;
        if (player.wasTouchingWater || player.wasWasTouchingWater || player.wasTouchingLava) return true;
        if (player.isClimbing || player.uncertaintyHandler.wasAffectedByStuckSpeed()) return true;
        if (player.packetStateData.lastPacketWasTeleport) return true;
        if (player.getSetbackTeleportUtil().insideUnloadedChunk() || player.getSetbackTeleportUtil().blockOffsets) return true;
        if (player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.SLOW_FALLING).isPresent()
                || player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.LEVITATION).isPresent()) return true;
        if (player.riptideSpinAttackTicks > 0) return true;
        return false;
    }

    public boolean isNearGround(double x, double y, double z) {
        SimpleCollisionBox feetBB = GetBoundingBox.getBoundingBoxFromPosAndSize(player, x, y, z, 0.6f, 0.001f);
        // Expand slightly downwards and horizontally to account for step / block edges
        feetBB.expand(Math.max(0.04, player.getMovementThreshold()));

        List<SimpleCollisionBox> boxes = new ArrayList<>();
        Collisions.getCollisionBoxes(player, feetBB, boxes, false);

        for (SimpleCollisionBox box : boxes) {
            if (feetBB.collidesVertically(box)) {
                return true;
            }
        }

        return player.compensatedWorld.isNearHardEntity(feetBB.copy().expand(1));
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);

        // Synchronize with external prediction flip if requested
        if (flipPlayerGroundStatus) {
            flipPlayerGroundStatus = false;
            if (shouldModifyPackets()) {
                wrapper.setOnGround(!wrapper.isOnGround());
                event.markForReEncode(true);
            }
        }

        if (isExemptFromNoFall()) {
            trackedFallDistance = 0.0;
            flaggedDuringCurrentFall = false;
            fakeGroundBuffer = Math.max(0.0, fakeGroundBuffer - 0.2);
            noGroundBuffer = Math.max(0.0, noGroundBuffer - 0.2);
            return;
        }

        double targetX = wrapper.hasPositionChanged() ? wrapper.getLocation().getX() : player.x;
        double targetY = wrapper.hasPositionChanged() ? wrapper.getLocation().getY() : player.y;
        double targetZ = wrapper.hasPositionChanged() ? wrapper.getLocation().getZ() : player.z;

        boolean claimedOnGround = wrapper.isOnGround();
        boolean physicallyNearGround = isNearGround(targetX, targetY, targetZ);

        // 1. Packet NoFall: Client claims onGround=true while suspended in mid-air
        if (claimedOnGround && !physicallyNearGround) {
            if (!GhostBlockDetector.isGhostBlock(player)) {
                double currentFall = Math.max(player.fallDistance, trackedFallDistance);
                if (currentFall > 0.3 || (player.y - targetY > 0.05) || player.clientVelocity.getY() < -0.1) {
                    fakeGroundBuffer += 1.0;
                    flaggedDuringCurrentFall = true;

                    flag("type=Packet, fall=" + String.format("%.2f", currentFall) + ", y=" + String.format("%.2f", targetY));

                    if (shouldModifyPackets()) {
                        // Force packet onGround to false so server does NOT reset fall distance!
                        wrapper.setOnGround(false);
                        event.markForReEncode(true);
                    }

                    if (fakeGroundBuffer >= 2.0) {
                        executeViolationSetback();
                    }
                }
            }
            return;
        }

        // 2. NoGround NoFall: Client lands on solid ground after a fall but claims onGround=false
        // in order to evade server fall damage
        if (!claimedOnGround && physicallyNearGround) {
            double currentFall = Math.max(player.fallDistance, trackedFallDistance);
            double verticalDelta = Math.abs(targetY - player.lastY);

            if (currentFall > 2.5 && verticalDelta < 0.05) {
                noGroundBuffer += 1.0;
                flaggedDuringCurrentFall = true;

                flag("type=NoGround, fall=" + String.format("%.2f", currentFall) + ", y=" + String.format("%.2f", targetY));

                if (shouldModifyPackets()) {
                    // Force packet onGround to true so server executes causeFallDamage!
                    wrapper.setOnGround(true);
                    event.markForReEncode(true);
                }

                // Also enforce fall damage on main thread in case server missed it
                enforceFallDamage(currentFall);

                trackedFallDistance = 0.0;
                return;
            }
        }

        // Decay buffers when movement is normal
        fakeGroundBuffer = Math.max(0.0, fakeGroundBuffer - 0.05);
        noGroundBuffer = Math.max(0.0, noGroundBuffer - 0.05);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (isExemptFromNoFall() || !predictionComplete.isChecked()) {
            trackedFallDistance = 0.0;
            flaggedDuringCurrentFall = false;
            return;
        }

        // Accumulate real fall distance while player is falling in the air
        if (!player.onGround && player.y < player.lastY) {
            trackedFallDistance += (player.lastY - player.y);
        }

        // Player legitimately landed on ground
        if (player.onGround) {
            if (flaggedDuringCurrentFall && trackedFallDistance > 3.2) {
                enforceFallDamage(trackedFallDistance);
            }
            trackedFallDistance = 0.0;
            flaggedDuringCurrentFall = false;
        }
    }

    private void enforceFallDamage(double fallDist) {
        if (player.platformPlayer == null) return;

        final OptionalInt jumpBoost = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.JUMP_BOOST);
        double effectiveDist = fallDist - (jumpBoost.orElse(0));
        final double expectedDamage = Math.max(0.0, Math.ceil(effectiveDist - 3.0));

        if (expectedDamage <= 0.0) return;

        GrimAPI.INSTANCE.getScheduler().getEntityScheduler().execute(
                player.platformPlayer,
                GrimAPI.INSTANCE.getGrimPlugin(),
                () -> {
                    try {
                        Object nativeObj = player.platformPlayer.getNative();
                        Method getFallDist = nativeObj.getClass().getMethod("getFallDistance");
                        float serverFall = ((Number) getFallDist.invoke(nativeObj)).floatValue();

                        // Only apply damage directly if server's fall distance was spoofed/reset to < 3.0
                        // This prevents any double damage when the server applies it natively
                        if (serverFall < 3.0f) {
                            Method damageMethod = nativeObj.getClass().getMethod("damage", double.class);
                            damageMethod.invoke(nativeObj, expectedDamage);
                        }
                    } catch (Exception ignored) {
                    }
                },
                null,
                0
        );
    }
}
