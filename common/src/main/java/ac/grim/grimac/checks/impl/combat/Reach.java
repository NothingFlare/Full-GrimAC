/*
 * Copyright (C) 2020-2024 GrimAC Team
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntitySizeable;
import ac.grim.grimac.utils.data.packetentity.dragon.PacketEntityEnderDragonPart;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import ac.grim.grimac.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemAttackRange;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.viaversion.viaversion.api.Via;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// You may not copy the check unless you are licensed under GPL
@CheckData(name = "Reach", stableKey = "grim.combat.reach", description = "Attacked an entity from too far away")
public class Reach extends Check implements PacketReceiveListener {
    private static final Verbose V = Verbose.of("{f64:%.5f} blocks, type={entity}");

    private static final List<EntityType> blacklisted = Arrays.asList(
            EntityTypes.BOAT,
            EntityTypes.CHEST_BOAT,
            EntityTypes.SHULKER);
    private static final CheckResult NONE = new CheckResult(ResultType.NONE, 0, 0, false);
    // Only one flag per reach attack, per entity, per tick.
    // We store position because lastX isn't reliable on teleports.
    private final Int2ObjectMap<InteractionData> playerAttackQueue = new Int2ObjectOpenHashMap<>();
    private boolean cancelImpossibleHits;
    private double threshold;
    private double cancelBuffer; // For the next 4 hits after using reach, we aggressively cancel reach

    public Reach(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (!player.disableGrim && event.getPacketType() == PacketType.Play.Client.ATTACK) {
            WrapperPlayClientAttack packet = new WrapperPlayClientAttack(event);
            onInteract(event, packet.getEntityId(), InteractionHand.MAIN_HAND);
        }

        if (!player.disableGrim && event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                onInteract(event, packet.getEntityId(), InteractionHand.MAIN_HAND);
            }
        }

        // If the player set their look, or we know they have a new tick
        if (isUpdate(event.getPacketType())) {
            tickBetterReachCheckWithAngle();
        }
    }

    private void onInteract(PacketReceiveEvent event, int entityId, InteractionHand hand) {
        // Don't let the player teleport to bypass reach
        if (player.getSetbackTeleportUtil().shouldBlockMovement()) {
            event.setCancelled(true);
            player.onPacketCancel();
            return;
        }

        PacketEntity entity = player.compensatedEntities.entityMap.get(entityId);
        // Stop people from freezing transactions before an entity spawns to bypass reach
        // TODO: implement dragon parts?
        if (entity == null || entity instanceof PacketEntityEnderDragonPart) {
            // Only cancel if and only if we are tracking this entity
            // This is because we don't track paintings.
            if (shouldModifyPackets() && player.compensatedEntities.serverPositionsMap.containsKey(entityId)) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
            return;
        }

        // Dead entities cause false flags (https://github.com/GrimAnticheat/Grim/issues/546)
        if (entity.isDead) return;

        // TODO: Remove when in front of via
        if (entity.getType() == EntityTypes.ARMOR_STAND && player.getClientVersion().isOlderThan(ClientVersion.V_1_8))
            return;
        // Prevents Happy Ghast Reach false on 1.21.6+ servers with ViaBackwards set up
        if (entity.getType() == EntityTypes.HAPPY_GHAST && player.getClientVersion().isOlderThan(ClientVersion.V_1_21_6))
            return;
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR)
            return;
        if (player.inVehicle()) return;
        if (entity.riding != null) return;

        ItemStack currentStack = player.inventory.getItemInHand(hand);
        ItemStack startStack = player.inventory.getStartOfTickStack();

        boolean hasRange = false;
        float maxReach = 0f;
        float hitboxMargin = 0f;
        Vector3dm attackRangeMovement = null;

        boolean clientAttackRangeExists = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_11);
        boolean clientAndServerAgrees = clientAttackRangeExists && ATTACK_RANGE_COMPONENT_EXISTS;

        boolean viaVersionAvailable = false;
        if (USE_1_8_HITBOX_MARGIN && ViaVersionUtil.isAvailable) {
            viaVersionAvailable = Via.getConfig().getValues().containsKey("use-1_8-hitbox-margin") && Via.getConfig().use1_8HitboxMargin();
        }

        boolean clientAndViaVersion = clientAttackRangeExists && viaVersionAvailable;
        if (clientAndServerAgrees || clientAndViaVersion) {
            ItemAttackRange startRange = startStack.getComponentOr(ComponentTypes.ATTACK_RANGE, null);
            ItemAttackRange currentRange = currentStack.getComponentOr(ComponentTypes.ATTACK_RANGE, null);

            if (clientAndViaVersion) {
                if (startStack != ItemStack.EMPTY) {
                    startRange = new ItemAttackRange(0F, 3F, 0F, 4F, 0.1F, 1F);
                }

                if (currentStack != ItemStack.EMPTY) {
                    currentRange = new ItemAttackRange(0F, 3F, 0F, 4F, 0.1F, 1F);
                }
            }

            // If the start stack has no range component, the client defaults to vanilla reach behavior,
            // regardless of what the current stack is (No Range -> X = No Range used).
            if (startRange != null) {
                hasRange = true;
                if (currentRange == null) {
                    // Range (Start) -> No Range (Current)
                    // Client logic uses Start Range, including the attack_range movement projection.
                    if (!clientAndViaVersion) {
                        attackRangeMovement = player.clientVelocity.clone();
                    }
                    maxReach = startRange.getMaxRange();
                    hitboxMargin = startRange.getHitboxMargin();
                } else {
                    // Range (Start) -> Range (Current)
                    // Client logic requires satisfying BOTH constraints
                    maxReach = Math.min(startRange.getMaxRange(), currentRange.getMaxRange());
                    hitboxMargin = Math.min(startRange.getHitboxMargin(), currentRange.getHitboxMargin());
                }
            }
        }

        // Real-time reach verification and cancellation
        CheckResult result = checkReach(entity, player.x, player.y, player.z,
                player.yaw, player.pitch, player.lastYaw, player.lastPitch,
                hasRange, maxReach, hitboxMargin, attackRangeMovement, true);

        if (result.type() == ResultType.REACH) {
            cancelBuffer = 1;
            flag(
                    V.write(verbose()).f64(result.minDistance()).uint(Math.max(0, entity.getType().getId(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()))),
                    () -> {
                        String added = ", type=" + entity.getType().getName().getKey();
                        if (entity instanceof PacketEntitySizeable sizeable) {
                            added += ", size=" + sizeable.size;
                        }
                        return result.verbose() + added;
                    });
            KillAuraBot bot = player.checkManager.get(KillAuraBot.class);
            if (bot != null) {
                bot.addSuspicion(2);
            }
            if (shouldModifyPackets() && cancelImpossibleHits) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
            return;
        }

        boolean tooManyAttacks = playerAttackQueue.size() > 10;
        if (!tooManyAttacks) {
            playerAttackQueue.put(entityId, new InteractionData(
                    player.x, player.y, player.z,
                    player.yaw, player.pitch, player.lastYaw, player.lastPitch,
                    hasRange, maxReach, hitboxMargin, attackRangeMovement
            )); // Queue for next tick for very precise check
        } else if (shouldModifyPackets() && cancelImpossibleHits) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }

    private void tickBetterReachCheckWithAngle() {
        for (Int2ObjectMap.Entry<InteractionData> attack : playerAttackQueue.int2ObjectEntrySet()) {
            PacketEntity reachEntity = player.compensatedEntities.entityMap.get(attack.getIntKey());
            if (reachEntity == null) continue;

            InteractionData interactionData = attack.getValue();
            CheckResult result = checkReach(reachEntity,
                    interactionData.x, interactionData.y, interactionData.z,
                    interactionData.yaw, interactionData.pitch, interactionData.lastYaw, interactionData.lastPitch,
                    interactionData.hasAttackRange, interactionData.maxReach, interactionData.hitboxMargin,
                    interactionData.attackRangeMovement, false);
            switch (result.type()) {
                case REACH -> {
                    flag(
                            V.write(verbose()).f64(result.minDistance()).uint(Math.max(0, reachEntity.getType().getId(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()))),
                            () -> {
                                String added = ", type=" + reachEntity.getType().getName().getKey();
                                if (reachEntity instanceof PacketEntitySizeable sizeable) {
                                    added += ", size=" + sizeable.size;
                                }
                                return result.verbose() + added;
                            });
                    KillAuraBot bot = player.checkManager.get(KillAuraBot.class);
                    if (bot != null) {
                        bot.addSuspicion(2);
                    }
                }
                case HITBOX -> {
                    String added = "type=" + reachEntity.getType().getName().getKey();
                    if (reachEntity instanceof PacketEntitySizeable sizeable) {
                        added += ", size=" + sizeable.size;
                    }
                    player.checkManager.get(Hitboxes.class).flag(result.verbose() + added);
                    KillAuraBot bot = player.checkManager.get(KillAuraBot.class);
                    if (bot != null) {
                        bot.addSuspicion(2);
                    }
                }
            }
        }

        playerAttackQueue.clear();
    }

    @NotNull
    private CheckResult checkReach(PacketEntity reachEntity, double x, double y, double z,
                                   float yaw, float pitch, float lastYaw, float lastPitch,
                                   boolean hasAttackRange, float itemMaxReach, float itemHitboxMargin,
                                   Vector3dm attackRangeMovement, boolean isPrediction) {
        SimpleCollisionBox targetBox = getTargetBox(reachEntity);

        double movementAllowance = attackRangeMovement == null ? 0 : getAttackRangeMovementAllowance(attackRangeMovement);
        double maxReach = applyReachModifiers(targetBox, hasAttackRange, itemMaxReach, itemHitboxMargin, !player.packetStateData.didLastLastMovementIncludePosition) + movementAllowance;
        double minDistance = Double.MAX_VALUE;

        // https://bugs.mojang.com/browse/MC-67665
        List<Vector3dm> possibleLookDirs = new ArrayList<>(Collections.singletonList(ReachUtils.getLook(player, yaw, pitch)));

        // If we are a tick behind, we don't know their next look so don't bother doing this
        if (!isPrediction) {
            // 1.7 players do not have any of these issues! They are always on the latest look vector
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {
                possibleLookDirs.add(ReachUtils.getLook(player, lastYaw, pitch));

                // 1.9+ players could be a tick behind because we don't get skipped ticks
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
                    possibleLookDirs.add(ReachUtils.getLook(player, lastYaw, lastPitch));
                }
            }
        } else {
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {
                possibleLookDirs.add(ReachUtils.getLook(player, lastYaw, pitch));
            }
        }

        // +3 would be 3 + 3 = 6, which is the pre-1.20.5 behaviour, preventing "Missed Hitbox"
        final double distance = maxReach + 3;

        final double[] possibleEyeHeights = player.getPossibleEyeHeights();
        for (Vector3dm lookVec : possibleLookDirs) {
            Vector3dm scaledLook = lookVec.clone().multiply(distance);

            for (double eye : possibleEyeHeights) {
                final Vector3d eyePos = new Vector3d(x, y + eye, z);
                Vector3d endReachPos = eyePos.add(scaledLook.getX(), scaledLook.getY(), scaledLook.getZ());

                Vector3d intercept = ReachUtils.calculateIntercept(targetBox, eyePos, endReachPos).first();

                if (ReachUtils.isVecInside(targetBox, eyePos)) {
                    minDistance = 0;
                    break;
                }

                if (intercept != null) {
                    minDistance = Math.min(eyePos.distance(intercept), minDistance);
                }
            }
        }

        // if the entity is not exempt and the entity is alive
        if ((!blacklisted.contains(reachEntity.getType()) && reachEntity.isLivingEntity) || reachEntity.getType() == EntityTypes.END_CRYSTAL) {
            double minBoxDist = ReachUtils.getMinReachToBox(x, y, z, possibleEyeHeights, targetBox);

            if (minDistance == Double.MAX_VALUE) {
                cancelBuffer = 1;
                if (minBoxDist > maxReach) {
                    return new CheckResult(ResultType.REACH, minBoxDist, movementAllowance, attackRangeMovement != null);
                }
                return new CheckResult(ResultType.HITBOX, minBoxDist, 0, false);
            } else if (minDistance > maxReach || minBoxDist > maxReach) {
                cancelBuffer = 1;
                double reportedDistance = minDistance != Double.MAX_VALUE ? minDistance : minBoxDist;
                return new CheckResult(ResultType.REACH, reportedDistance, movementAllowance, attackRangeMovement != null);
            } else {
                cancelBuffer = Math.max(0, cancelBuffer - 0.25);
            }
        }

        return NONE;
    }

    private SimpleCollisionBox getTargetBox(PacketEntity reachEntity) {
        if (reachEntity.getType() == EntityTypes.END_CRYSTAL) { // Hardcode end crystal box
            return new SimpleCollisionBox(reachEntity.trackedServerPosition.getPos().subtract(1, 0, 1), reachEntity.trackedServerPosition.getPos().add(1, 2, 1));
        }
        SimpleCollisionBox possibleBoxes = reachEntity.getPossibleCollisionBoxes();
        Vector3d serverPos = reachEntity.trackedServerPosition.getPos();
        SimpleCollisionBox serverBox = GetBoundingBox.getPacketEntityBoundingBox(player, serverPos.x, serverPos.y, serverPos.z, reachEntity);

        double maxExtra = 1.0;
        double minX = Math.max(possibleBoxes.minX, serverBox.minX - maxExtra);
        double maxX = Math.min(possibleBoxes.maxX, serverBox.maxX + maxExtra);
        double minY = Math.max(possibleBoxes.minY, serverBox.minY - maxExtra);
        double maxY = Math.min(possibleBoxes.maxY, serverBox.maxY + maxExtra);
        double minZ = Math.max(possibleBoxes.minZ, serverBox.minZ - maxExtra);
        double maxZ = Math.min(possibleBoxes.maxZ, serverBox.maxZ + maxExtra);

        if (minX <= maxX && minY <= maxY && minZ <= maxZ) {
            return new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
        }
        return possibleBoxes;
    }

    private double getAttackRangeMovementAllowance(Vector3dm attackRangeMovement) {
        if (attackRangeMovement.lengthSquared() == 0) {
            return 0;
        }

        return getForwardMovement(attackRangeMovement, player.yaw, player.pitch);
    }

    private double getForwardMovement(Vector3dm movement, float yaw, float pitch) {
        return Math.max(0, movement.dot(ReachUtils.getLook(player, yaw, pitch)));
    }

    private static final boolean ATTACK_RANGE_COMPONENT_EXISTS = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_11);
    private static final boolean USE_1_8_HITBOX_MARGIN = PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8);

    private double applyReachModifiers(SimpleCollisionBox targetBox, boolean hasAttackRange, float itemMaxReach, float itemHitboxMargin, boolean giveMovementThreshold) {
        double maxReach;
        double hitboxMargin = threshold;

        if (hasAttackRange) {
            maxReach = itemMaxReach;
            hitboxMargin += itemHitboxMargin;
        } else {
            maxReach = player.compensatedEntities.self.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
            // 1.7 and 1.8 players get a bit of extra hitbox (this is why you should use 1.8 on cross version servers)
            // Yes, this is vanilla and not uncertainty.  All reach checks have this or they are wrong.
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
                hitboxMargin += 0.1f;
            }
        }

        // This is better than adding to the reach, as 0.03 can cause a player to miss their target
        // Adds some more than 0.03 uncertainty in some cases, but a good trade off for simplicity
        //
        // Just give the uncertainty on 1.9+ clients as we have no way of knowing whether they had 0.03 movement
        // However, on 1.21.2+ we do know if they had 0.03 movement
        if (giveMovementThreshold || player.canSkipTicks()) {
            hitboxMargin += player.getMovementThreshold();
        }

        targetBox.expand(hitboxMargin);

        return maxReach;
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        this.cancelImpossibleHits = config.getBooleanElse("Reach.block-impossible-hits", true);
        this.threshold = config.getDoubleElse("Reach.threshold", 0.0005);
    }

    private enum ResultType {
        REACH, HITBOX, NONE
    }

    private record CheckResult(ResultType type, double minDistance, double extraMovement, boolean hasExtraMovement) {
        public boolean isFlag() {
            return type != ResultType.NONE;
        }

        public String verbose() {
            if (type == ResultType.REACH) {
                String verbose = String.format("%.5f", minDistance) + " blocks";
                if (hasExtraMovement) {
                    verbose += String.format(", extraMovement=%.5f", extraMovement);
                }
                return verbose;
            } else if (type == ResultType.HITBOX) {
                return String.format("dist=%.2f, missed_raycast, ", minDistance);
            }
            return "";
        }
    }

    private record InteractionData(double x, double y, double z,
                                   float yaw, float pitch, float lastYaw, float lastPitch,
                                   boolean hasAttackRange, float maxReach, float hitboxMargin,
                                   Vector3dm attackRangeMovement) {}
}
