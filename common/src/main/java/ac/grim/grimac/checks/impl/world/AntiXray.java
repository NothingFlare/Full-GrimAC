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
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

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

                String blockName = state.getType().getName().toLowerCase(java.util.Locale.ROOT);
                boolean isRare = isRareOre(blockName);

                boolean isExposed = isExposedToOpenSpace(pos);

                // 1. Occluded Ore Detection (Freecam / Xray Nuker):
                // If a rare ore has NO exposed faces, NO adjacent air/water, NO recently broken neighbors in blockHistory,
                // and NO adjacent exposed vein block, it is completely occluded inside solid rock.
                if (isRare && !PerformanceProtectionManager.shouldSimplifyAntiXray()) {
                    if (!isExposed) {
                        if (flag("occluded_ore=" + blockName + " at " + pos.getX() + "," + pos.getY() + "," + pos.getZ()) && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                            return;
                        }
                    }
                }

                // 2. Statistical Mining Ratio (Rolling Window)
                // Legitimate players finding exposed diamond veins in caves or mining out exposed clusters
                // must NEVER count towards suspicious hidden tunneling ratios!
                if (isRare) {
                    totalUndergroundMined++;
                    if (!isExposed) {
                        hiddenRareOresMined++;
                    }

                    if (totalUndergroundMined >= 120) {
                        double ratio = (double) hiddenRareOresMined / totalUndergroundMined;
                        // Legitimate blind branch mining produces 1.5% - 4.5% unexposed rare ores.
                        // Straight X-ray tunneling into hidden unexposed ores exceeds 22%.
                        if (ratio > 0.22) {
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
        String neighborName = state.getType().getName().toLowerCase(java.util.Locale.ROOT);
        if (isOre(neighborName)) {
            // If the adjacent ore itself touches air or was mined, the vein is exposed
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
