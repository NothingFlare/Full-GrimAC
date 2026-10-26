/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.checks.impl.world;

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

    private int rareOresMined = 0;
    private int totalMined = 0;

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

                String blockName = state.getType().getName().toLowerCase();
                boolean isRare = isRareOre(blockName);

                // 1. Occluded Ore Detection (Freecam / Xray Nuker)
                // When TPS is low, skip expensive 6-face block lookups to save server CPU
                if (isRare && !PerformanceProtectionManager.shouldSimplifyAntiXray()) {
                    if (isFullyOccluded(pos)) {
                        if (flag("occluded_ore=" + blockName + " at " + pos.getX() + "," + pos.getY() + "," + pos.getZ()) && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                            return;
                        }
                    }
                }

                if (isRare) {
                    rareOresMined++;
                    totalMined++;

                    // 2. Statistical Mining Ratio (Rolling Window)
                    if (totalMined >= 35) {
                        double ratio = (double) rareOresMined / totalMined;
                        // Vanilla branch mining diamond ratio is normally 1.5% - 3.5%. > 14% indicates straight Xray tunneling
                        if (ratio > 0.14) {
                            flag("xray_ratio=" + String.format("%.1f", ratio * 100) + "% (rare=" + rareOresMined + ", total=" + totalMined + ")");
                        }
                    }
                } else if (isStoneLike(blockName)) {
                    totalMined++;

                    // Exponential decay to keep window fresh
                    if (totalMined > 250) {
                        rareOresMined = (int) (rareOresMined * 0.7);
                        totalMined = (int) (totalMined * 0.7);
                    }
                }
            }
        }
    }

    private boolean isFullyOccluded(Vector3i pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        return isSolidOpaque(player.compensatedWorld.getBlock(x, y + 1, z)) &&
                isSolidOpaque(player.compensatedWorld.getBlock(x, y - 1, z)) &&
                isSolidOpaque(player.compensatedWorld.getBlock(x, y, z + 1)) &&
                isSolidOpaque(player.compensatedWorld.getBlock(x, y, z - 1)) &&
                isSolidOpaque(player.compensatedWorld.getBlock(x + 1, y, z)) &&
                isSolidOpaque(player.compensatedWorld.getBlock(x - 1, y, z));
    }

    private static boolean isSolidOpaque(WrappedBlockState state) {
        if (state == null || state.getType() == null) return false;
        return !state.getType().isAir() && state.getType().isSolid();
    }

    private static boolean isRareOre(String name) {
        return name.contains("diamond_ore") ||
                name.contains("ancient_debris") ||
                name.contains("emerald_ore");
    }

    private static boolean isStoneLike(String name) {
        return name.contains("stone") ||
                name.contains("deepslate") ||
                name.contains("netherrack") ||
                name.contains("tuff") ||
                name.contains("granite") ||
                name.contains("diorite") ||
                name.contains("andesite") ||
                name.contains("blackstone");
    }
}
