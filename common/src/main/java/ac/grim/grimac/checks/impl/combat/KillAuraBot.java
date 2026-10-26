/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.manager.performance.PerformanceProtectionManager;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

@CheckData(
        name = "KillAuraBot",
        stableKey = "grim.combat.killaura_bot",
        description = "Attacked invisible anti-killaura bot entity"
)
public class KillAuraBot extends Check implements PacketReceiveListener {

    private boolean botActive = false;
    private int currentBotId = 0;
    private UUID botUUID = null;
    private boolean isPlayerBot = false;
    private int botTicks = 0;
    private int spawnCooldown = 0;
    private int orbitSide = 1;

    private int suspicionScore = 0;
    private int lastTargetId = -1;
    private long lastTargetHitTime = 0;
    private float previousYaw = 0f;
    private int consecutiveHits = 0;

    public KillAuraBot(GrimPlayer player) {
        super(player);
    }

    public void addSuspicion(int amount) {
        suspicionScore += amount;
        if (suspicionScore >= 2 && !botActive && spawnCooldown <= 0 && !PerformanceProtectionManager.shouldSkipKillAuraBots()) {
            spawnBot(lastTargetId != -1 ? lastTargetId : 0);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        // Skip bot spawning if server TPS dropped below threshold
        if (PerformanceProtectionManager.shouldSkipKillAuraBots()) {
            if (botActive) despawnBot();
            return;
        }

        // 1. Attack packets
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            WrapperPlayClientAttack attack = new WrapperPlayClientAttack(event);
            handleAttack(event, attack.getEntityId());
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                handleAttack(event, interact.getEntityId());
            }
        }

        // 2. Flying / rotation / movement ticks: maintain bot position & lifetime
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            handleTick();
        }
    }

    private void handleAttack(PacketReceiveEvent event, int targetId) {
        long now = System.currentTimeMillis();

        // Check if player attacked the invisible bot
        if (botActive && targetId == currentBotId) {
            if (flag("exploit=bot_hit, botId=" + currentBotId) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
            despawnBot();
            suspicionScore = 0;
            spawnCooldown = 25;
            return;
        }

        long diff = now - lastTargetHitTime;

        // Suspicion heuristic: Multi-target switching in < 400ms
        if (lastTargetId != -1 && lastTargetId != targetId) {
            if (diff < 400) {
                suspicionScore += 2;
            }
        }

        // Suspicion heuristic: rapid consecutive hits
        if (diff < 350) {
            consecutiveHits++;
            if (consecutiveHits >= 2) {
                suspicionScore += 1;
            }
        } else {
            consecutiveHits = 1;
        }

        // Suspicion heuristic: Angular head snap (> 25 degrees)
        float yawDiff = Math.abs(player.yaw - previousYaw);
        if (yawDiff > 25.0f && yawDiff < 335.0f) {
            suspicionScore += 1;
        }

        lastTargetId = targetId;
        lastTargetHitTime = now;
        previousYaw = player.yaw;

        // Trigger bot when suspicion reaches threshold or combat is actively ongoing
        if (suspicionScore >= 2 && !botActive && spawnCooldown <= 0 && !PerformanceProtectionManager.shouldSkipKillAuraBots()) {
            spawnBot(targetId);
        }
    }

    private void handleTick() {
        if (spawnCooldown > 0) {
            spawnCooldown--;
        }

        if (botActive) {
            if (PerformanceProtectionManager.shouldSkipKillAuraBots()) {
                despawnBot();
                return;
            }

            botTicks++;
            if (botTicks > 30) { // 1.5 seconds lifetime
                despawnBot();
                suspicionScore = Math.max(0, suspicionScore - 1);
                spawnCooldown = 35; // cooldown before next test
            } else {
                // Update bot position smoothly orbiting in peripheral FOV (50-70 degrees)
                double angle = player.yaw + (orbitSide * (55.0 + (botTicks * 1.5)));
                double rad = Math.toRadians(angle);
                double dist = 2.4;
                double botX = player.x + (-Math.sin(rad) * dist);
                double botY = player.y + 0.3;
                double botZ = player.z + (Math.cos(rad) * dist);

                try {
                    player.user.sendPacket(new WrapperPlayServerEntityTeleport(
                            currentBotId, new Vector3d(botX, botY, botZ), (float) angle, 0f, false
                    ));
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void spawnBot(int targetId) {
        if (botActive) return;

        currentBotId = 910000 + (Math.abs(player.entityID) % 40000);
        botUUID = UUID.randomUUID();
        botActive = true;
        botTicks = 0;
        orbitSide = -orbitSide;

        // Match entity type to what player is attacking (Player vs Monster)
        PacketEntity target = player.compensatedEntities.entityMap.get(targetId);
        EntityType botType = EntityTypes.ZOMBIE;
        isPlayerBot = false;
        if (target != null && target.getType() != null) {
            botType = target.getType();
            if (botType == EntityTypes.PLAYER) {
                isPlayerBot = true;
            }
        }

        double baseAngle = player.yaw + (orbitSide * 55.0);
        double rad = Math.toRadians(baseAngle);
        double dist = 2.4;
        Vector3d botPos = new Vector3d(
                player.x + (-Math.sin(rad) * dist),
                player.y + 0.3,
                player.z + (Math.cos(rad) * dist)
        );

        try {
            if (isPlayerBot) {
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19_3)) {
                    UserProfile profile = new UserProfile(botUUID, "NPC_" + (currentBotId % 1000));
                    WrapperPlayServerPlayerInfoUpdate.PlayerInfo entry = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                            profile, false, 0, GameMode.SURVIVAL, null, null
                    );
                    player.user.sendPacket(new WrapperPlayServerPlayerInfoUpdate(
                            EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER),
                            Collections.singletonList(entry)
                    ));
                }
                player.user.sendPacket(new WrapperPlayServerSpawnEntity(
                        currentBotId, Optional.of(botUUID), EntityTypes.PLAYER, botPos, (float) baseAngle, 0f, 0f, 0, Optional.empty()
                ));
            } else {
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19)) {
                    player.user.sendPacket(new WrapperPlayServerSpawnEntity(
                            currentBotId, Optional.of(botUUID), botType, botPos, (float) baseAngle, 0f, 0f, 0, Optional.empty()
                    ));
                } else {
                    player.user.sendPacket(new WrapperPlayServerSpawnLivingEntity(
                            currentBotId, botUUID, botType, botPos, (float) baseAngle, 0f, 0f, new Vector3d(), Collections.emptyList()
                    ));
                }
            }

            // Set invisible metadata flag (0x20)
            player.user.sendPacket(new WrapperPlayServerEntityMetadata(
                    currentBotId,
                    Collections.singletonList(new EntityData(0, EntityDataTypes.BYTE, (byte) 0x20))
            ));
        } catch (Exception ignored) {
            botActive = false;
        }
    }

    public void despawnBot() {
        if (!botActive) return;
        botActive = false;
        try {
            player.user.sendPacket(new WrapperPlayServerDestroyEntities(currentBotId));
            if (isPlayerBot && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19_3) && botUUID != null) {
                player.user.sendPacket(new WrapperPlayServerPlayerInfoRemove(Collections.singletonList(botUUID)));
            }
        } catch (Exception ignored) {
        }
    }
}
