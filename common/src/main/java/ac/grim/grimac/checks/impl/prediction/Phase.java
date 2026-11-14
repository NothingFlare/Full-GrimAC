package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.nmsutil.Collisions;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "Phase", stableKey = "grim.prediction.phase", description = "Moved into a solid block during movement prediction", setback = 1, decay = 0.005)
public class Phase extends Check implements PostPredictionListener {
    private SimpleCollisionBox oldBB;
    private int lastKnockbackTick = -100;

    public Phase(GrimPlayer player) {
        super(player);
        oldBB = player.boundingBox;
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!player.getSetbackTeleportUtil().blockOffsets && !predictionComplete.getData().isTeleport() && predictionComplete.isChecked()) { // Not falling through world
            SimpleCollisionBox newBB = player.boundingBox;

            List<SimpleCollisionBox> boxes = new ArrayList<>();
            Collisions.getCollisionBoxes(player, newBB, boxes, false);

            int currentTick = GrimAPI.INSTANCE.getTickManager().currentTick;
            boolean hasActiveKB = player.likelyKB != null || player.firstBreadKB != null
                    || (player.predictedVelocity != null && (player.predictedVelocity.isKnockback() || player.predictedVelocity.isExplosion()))
                    || player.checkManager.getKnockbackHandler().hasActiveOrPendingKnockback()
                    || player.checkManager.getExplosionHandler().hasActiveOrPendingExplosion();

            if (hasActiveKB) {
                lastKnockbackTick = currentTick;
            }

            boolean isRecentKnockback = Math.abs(currentTick - lastKnockbackTick) <= 25;
            boolean isRecentRiptide = player.isRiptidePose || player.pose == Pose.SPIN_ATTACK
                    || player.riptideSpinAttackTicks > 0
                    || player.packetStateData.tryingToRiptide
                    || Math.abs(currentTick - player.lastRiptideTick) <= 45
                    || Math.abs(currentTick - player.lastCollidedWithRiptideTick) <= 30;

            boolean isFluidOrSliding = player.wasTouchingWater || player.wasWasTouchingWater
                    || player.isSwimming || player.wasSwimming
                    || player.isGliding || player.wasGliding
                    || player.pose == Pose.SWIMMING || player.pose == Pose.FALL_FLYING
                    || player.pose == Pose.SPIN_ATTACK || player.isRiptidePose
                    || isRecentKnockback || isRecentRiptide;

            double minPenetration = isFluidOrSliding ? 0.08 : 0.04;

            for (SimpleCollisionBox box : boxes) {
                if (!newBB.isIntersected(box)) continue;

                double overlapX = Math.min(newBB.maxX, box.maxX) - Math.max(newBB.minX, box.minX);
                double overlapY = Math.min(newBB.maxY, box.maxY) - Math.max(newBB.minY, box.minY);
                double overlapZ = Math.min(newBB.maxZ, box.maxZ) - Math.max(newBB.minZ, box.minZ);

                if (overlapX <= 0 || overlapY <= 0 || overlapZ <= 0) continue;

                double penetrationDepth = Math.min(overlapX, Math.min(overlapY, overlapZ));
                if (penetrationDepth <= minPenetration) {
                    continue; // Surface grazing / edge contact, not an illegal block phase
                }

                if (oldBB != null && oldBB.isIntersected(box)) {
                    double oldOverlapX = Math.min(oldBB.maxX, box.maxX) - Math.max(oldBB.minX, box.minX);
                    double oldOverlapY = Math.min(oldBB.maxY, box.maxY) - Math.max(oldBB.minY, box.minY);
                    double oldOverlapZ = Math.min(oldBB.maxZ, box.maxZ) - Math.max(oldBB.minZ, box.minZ);
                    if (oldOverlapX > 0 && oldOverlapY > 0 && oldOverlapZ > 0) {
                        double oldPenetration = Math.min(oldOverlapX, Math.min(oldOverlapY, oldOverlapZ));
                        if (penetrationDepth <= oldPenetration + 0.02) {
                            continue; // Continuing legitimate contact along existing collision boundary
                        }
                    }
                }

                int bx = (int) Math.floor((box.minX + box.maxX) / 2.0);
                int by = (int) Math.floor((box.minY + box.maxY) / 2.0);
                int bz = (int) Math.floor((box.minZ + box.maxZ) / 2.0);

                    // If this block was recently broken/cleared by the player, do not flag phase setback
                    boolean recentlyBroken = player.blockHistory.getRecentModifications(mod ->
                            Math.abs(currentTick - mod.tick()) <= 15 &&
                            mod.location().getX() == bx &&
                            mod.location().getY() == by &&
                            mod.location().getZ() == bz &&
                            (mod.newBlockContents() == null || mod.newBlockContents().getType().isAir())
                    ).iterator().hasNext();

                    if (recentlyBroken) {
                        continue;
                    }

                    if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
                        // A bit of a hacky way to get the block state, but this is much faster to use the tuinity method for grabbing collision boxes
                        WrappedBlockState state = player.compensatedWorld.getBlock(bx, by, bz);
                        if (BlockTags.ANVIL.contains(state.getType()) || state.getType() == StateTypes.CHEST || state.getType() == StateTypes.TRAPPED_CHEST) {
                            continue; // 1.8 glitchy block, ignore
                        }
                    }
                    flagWithSetback();
                    return;
                }
            }

        oldBB = player.boundingBox;
        reward();
    }
}
