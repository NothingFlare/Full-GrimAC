/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.checks.impl.world;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.manager.performance.PerformanceProtectionManager;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

import java.util.Locale;

@CheckData(
        name = "AntiXray",
        stableKey = "grim.world.antixray",
        description = "Mined occluded ores or impossible ore-to-stone mining ratio"
)
public class AntiXray extends Check implements PacketReceiveListener {

    private int hiddenRareOresMined = 0;
    private int totalUndergroundMined = 0;
    private double ratioBuffer = 0.0;

    public AntiXray(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging digging = new WrapperPlayClientPlayerDigging(event);

            if (digging.getAction() == DiggingAction.START_DIGGING) {
                Vector3i pos = digging.getBlockPosition();
                WrappedBlockState state = player.compensatedWorld.getBlock(pos);
                if (state == null || state.getType() == null) return;

                String blockName = state.getType().getName().toLowerCase(Locale.ROOT);
                boolean isValuable = isValuableOrOre(blockName);
                boolean isRare = isRareOre(blockName);

                // 1. Occluded / Line-of-Sight Protection (Freecam, Wall-hack, Xray Nuker):
                // If a valuable block or ore is completely enclosed inside solid rock (no open neighbors),
                // OR if the player has NO unobstructed line of sight from their eye position to any exposed face,
                // the block is not visible to the player. The server cancels the dig and resyncs the block as solid.
                if (isValuable && !PerformanceProtectionManager.shouldSimplifyAntiXray()) {
                    boolean isExposed = isExposedToOpenSpace(pos);

                    if (!isExposed) {
                        if (flag("occluded_ore=" + blockName + " at " + pos.getX() + "," + pos.getY() + "," + pos.getZ()) && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                            player.resyncPosition(pos);
                            return;
                        }
                    } else {
                        Vector3d eyePos = new Vector3d(player.x, player.y + player.getEyeHeight(), player.z);
                        if (!hasLineOfSightToBlock(eyePos, pos)) {
                            if (flag("no_line_of_sight=" + blockName + " at " + pos.getX() + "," + pos.getY() + "," + pos.getZ()) && shouldModifyPackets()) {
                                event.setCancelled(true);
                                player.onPacketCancel();
                                player.resyncPosition(pos);
                                return;
                            }
                        }
                    }
                }

                // 2. Statistical Mining Ratio (Rolling Window)
                // Legitimate players finding exposed diamond veins in caves or mining out exposed clusters
                // must never count towards suspicious hidden tunneling ratios.
                if (isRare) {
                    totalUndergroundMined++;
                    if (!isExposedToOpenSpace(pos)) {
                        hiddenRareOresMined++;
                    }

                    if (totalUndergroundMined >= 60) {
                        double ratio = (double) hiddenRareOresMined / totalUndergroundMined;
                        // Legitimate blind branch mining produces 1.5% - 4.5% unexposed rare ores.
                        // Straight X-ray tunneling into hidden unexposed ores exceeds 16%.
                        if (ratio > 0.16) {
                            ratioBuffer += 1.0;
                            if (ratioBuffer >= 2.0) {
                                ratioBuffer = 1.0;
                                flag("xray_ratio=" + String.format("%.1f", ratio * 100) + "% (hidden=" + hiddenRareOresMined + ", total=" + totalUndergroundMined + ")");
                            }
                        } else {
                            ratioBuffer = Math.max(0.0, ratioBuffer - 0.2);
                        }
                    }
                } else if (isUndergroundBlock(blockName)) {
                    totalUndergroundMined++;
                    ratioBuffer = Math.max(0.0, ratioBuffer - 0.05);

                    // Exponential decay rolling window
                    if (totalUndergroundMined > 400) {
                        hiddenRareOresMined = (int) (hiddenRareOresMined * 0.7);
                        totalUndergroundMined = (int) (totalUndergroundMined * 0.7);
                    }
                }
            }
        }
    }

    private boolean isExposedToOpenSpace(Vector3i pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        // Check all 6 immediate neighbors
        return isNeighborOpen(x, y + 1, z) ||
                isNeighborOpen(x, y - 1, z) ||
                isNeighborOpen(x, y, z + 1) ||
                isNeighborOpen(x, y, z - 1) ||
                isNeighborOpen(x + 1, y, z) ||
                isNeighborOpen(x - 1, y, z);
    }

    private boolean isNeighborOpen(int x, int y, int z) {
        WrappedBlockState state = player.compensatedWorld.getBlock(x, y, z);
        if (state == null || state.getType() == null) return true;

        // Air, water, lava, or non-solid/transparent blocks (torches, slabs, glass, etc.)
        if (state.getType().isAir() || !state.getType().isSolid()) {
            return true;
        }

        // Neighbor was recently broken by the player in blockHistory
        int currentTick = GrimAPI.INSTANCE.getTickManager().currentTick;
        boolean wasRecentlyMined = player.blockHistory.getRecentModifications(mod ->
                Math.abs(currentTick - mod.tick()) <= 40 &&
                mod.location().getX() == x && mod.location().getY() == y && mod.location().getZ() == z &&
                (mod.newBlockContents() == null || mod.newBlockContents().getType().isAir())
        ).iterator().hasNext();

        if (wasRecentlyMined) {
            return true;
        }

        // Check if neighbor is part of the same ore vein that is already exposed
        String neighborName = state.getType().getName().toLowerCase(Locale.ROOT);
        if (isOre(neighborName)) {
            return touchesAirOrLiquid(x, y, z);
        }

        return false;
    }

    private boolean touchesAirOrLiquid(int x, int y, int z) {
        return isOpen(x, y + 1, z) || isOpen(x, y - 1, z) ||
               isOpen(x, y, z + 1) || isOpen(x, y, z - 1) ||
               isOpen(x + 1, y, z) || isOpen(x - 1, y, z);
    }

    private boolean isOpen(int x, int y, int z) {
        WrappedBlockState state = player.compensatedWorld.getBlock(x, y, z);
        if (state == null || state.getType() == null) return true;
        return state.getType().isAir() || !state.getType().isSolid();
    }

    private boolean hasLineOfSightToBlock(Vector3d eyePos, Vector3i blockPos) {
        double bx = blockPos.getX();
        double by = blockPos.getY();
        double bz = blockPos.getZ();

        // Distance to block center
        double dx = (bx + 0.5) - eyePos.getX();
        double dy = (by + 0.5) - eyePos.getY();
        double dz = (bz + 0.5) - eyePos.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;

        // If player is directly inside or directly touching the block (< 1.2 blocks), line of sight is clear
        if (distSq <= 1.44) return true;

        // 6 face centers and directional normal offsets:
        double[][] faces = new double[][] {
                {bx + 0.5, by + 1.0, bz + 0.5, 0, 1, 0},  // UP
                {bx + 0.5, by,       bz + 0.5, 0, -1, 0}, // DOWN
                {bx + 0.5, by + 0.5, bz + 1.0, 0, 0, 1},  // SOUTH
                {bx + 0.5, by + 0.5, bz,       0, 0, -1}, // NORTH
                {bx + 1.0, by + 0.5, bz + 0.5, 1, 0, 0},  // EAST
                {bx,       by + 0.5, bz + 0.5, -1, 0, 0}  // WEST
        };

        for (double[] f : faces) {
            int nx = (int) (bx + f[3]);
            int ny = (int) (by + f[4]);
            int nz = (int) (bz + f[5]);

            // Only test faces that touch an open / transparent space or recently mined block
            if (isNeighborOpen(nx, ny, nz)) {
                Vector3d targetFace = new Vector3d(f[0], f[1], f[2]);
                if (isRayClear(eyePos, targetFace, blockPos)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isRayClear(Vector3d start, Vector3d end, Vector3i targetBlock) {
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double dz = end.getZ() - start.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= 0.8) return true;

        int steps = (int) Math.ceil(distance / 0.35);
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            int sx = (int) Math.floor(start.getX() + t * dx);
            int sy = (int) Math.floor(start.getY() + t * dy);
            int sz = (int) Math.floor(start.getZ() + t * dz);

            if (sx == targetBlock.getX() && sy == targetBlock.getY() && sz == targetBlock.getZ()) {
                continue;
            }
            if (sx == (int) Math.floor(start.getX()) && sy == (int) Math.floor(start.getY()) && sz == (int) Math.floor(start.getZ())) {
                continue;
            }

            WrappedBlockState state = player.compensatedWorld.getBlock(sx, sy, sz);
            if (state != null && state.getType() != null && state.getType().isSolid()) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValuableOrOre(String name) {
        return name.contains("ore") ||
                name.contains("debris") ||
                name.contains("chest") ||
                name.contains("spawner") ||
                name.contains("raw_");
    }

    private static boolean isRareOre(String name) {
        return name.contains("diamond_ore") ||
                name.contains("ancient_debris") ||
                name.contains("emerald_ore");
    }

    private static boolean isOre(String name) {
        return name.contains("ore") || name.contains("debris");
    }

    private static boolean isUndergroundBlock(String name) {
        return name.contains("stone") ||
                name.contains("deepslate") ||
                name.contains("netherrack") ||
                name.contains("tuff") ||
                name.contains("granite") ||
                name.contains("diorite") ||
                name.contains("andesite") ||
                name.contains("blackstone") ||
                name.contains("dirt") ||
                name.contains("gravel") ||
                name.contains("sand") ||
                name.contains("calcite") ||
                name.contains("ore");
    }
}
