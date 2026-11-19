/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.platform.bukkit.antibot;

import ac.grim.grimac.utils.anticheat.LogUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;
import org.bukkit.plugin.Plugin;

import java.util.Random;
import java.util.UUID;

public class VerificationWorldManager {

    private static VerificationWorldManager instance;
    private final Plugin plugin;
    private World verificationWorld;
    private final String worldName;

    public static final double PLATFORM_X = 0.5;
    public static final double PLATFORM_Y = 100.0;
    public static final double PLATFORM_Z = 0.5;
    public static final double BLOCK_HEIGHT = 0.5; // Smooth Stone Slab height

    public VerificationWorldManager(Plugin plugin, String worldName) {
        this.plugin = plugin;
        this.worldName = worldName != null && !worldName.isEmpty() ? worldName : "grim_verification";
        instance = this;
    }

    public static VerificationWorldManager getInstance() {
        return instance;
    }

    public void initWorld() {
        try {
            World existing = Bukkit.getWorld(worldName);
            if (existing != null) {
                this.verificationWorld = existing;
            } else {
                WorldCreator creator = new WorldCreator(worldName);
                creator.generator(new VoidChunkGenerator());
                creator.environment(World.Environment.NORMAL);
                creator.type(WorldType.FLAT);
                this.verificationWorld = creator.createWorld();
            }

            if (verificationWorld != null) {
                verificationWorld.setAutoSave(false);
                verificationWorld.setKeepSpawnInMemory(true);
                verificationWorld.setTime(6000L);
                verificationWorld.setStorm(false);
                verificationWorld.setThundering(false);
                verificationWorld.setDifficulty(Difficulty.PEACEFUL);

                try {
                    verificationWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                    verificationWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                    verificationWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
                    verificationWorld.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
                    verificationWorld.setGameRule(GameRule.KEEP_INVENTORY, true);
                } catch (Throwable ignored) {}

                buildVerificationPlatform();
                LogUtil.info("[Antibot] Dedicated Sonar verification world '" + worldName + "' successfully initialized.");
            }
        } catch (Throwable t) {
            LogUtil.error("[Antibot] Failed to initialize dedicated verification world '" + worldName + "', fallback to main world limbo", t);
        }
    }

    private void buildVerificationPlatform() {
        if (verificationWorld == null) return;

        // Build a 5x5 platform around (0, 100, 0)
        Material slabMaterial = getSafeMaterial("SMOOTH_STONE_SLAB", "STEP", "STONE_SLAB");
        Material borderMaterial = getSafeMaterial("BARRIER", "GLASS");

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Block block = verificationWorld.getBlockAt(x, (int) PLATFORM_Y, z);
                if (x == 0 && z == 0) {
                    block.setType(slabMaterial); // Center slab for hitbox test (height 0.5)
                } else {
                    block.setType(borderMaterial);
                }
            }
        }
    }

    public Location getSpawnLocation(double heightOffset) {
        if (verificationWorld == null) {
            World fallback = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (fallback != null) {
                Location spawn = fallback.getSpawnLocation();
                return new Location(fallback, spawn.getX(), spawn.getY() + 30.0, spawn.getZ(), 0f, 0f);
            }
            return null;
        }
        return new Location(verificationWorld, PLATFORM_X, PLATFORM_Y + heightOffset, PLATFORM_Z, 0f, 0f);
    }

    public World getVerificationWorld() {
        return verificationWorld;
    }

    // --- Captcha Map Rendering ---

    public ItemStack createCaptchaMap(Player player, String code) {
        World world = player.getWorld();
        MapView view = Bukkit.createMap(world);
        for (MapRenderer r : view.getRenderers()) {
            view.removeRenderer(r);
        }
        view.addRenderer(new CaptchaMapRenderer(code));

        Material mapMat = getSafeMaterial("FILLED_MAP", "MAP");
        ItemStack mapItem = new ItemStack(mapMat);
        ItemMeta meta = mapItem.getItemMeta();
        if (meta instanceof MapMeta mapMeta) {
            try {
                mapMeta.setMapView(view);
            } catch (Throwable t) {
                try {
                    mapItem.setDurability((short) view.getId());
                } catch (Throwable ignored) {}
            }
            mapMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Код проверки");
            mapItem.setItemMeta(mapMeta);
        }
        return mapItem;
    }

    public static class CaptchaMapRenderer extends MapRenderer {
        private final String code;
        private boolean rendered = false;

        public CaptchaMapRenderer(String code) {
            super(true);
            this.code = code != null ? code : "12345";
        }

        @Override
        public void render(MapView map, MapCanvas canvas, Player player) {
            if (rendered) return;
            rendered = true;

            // Background & Border colors
            byte bg = MapPalette.matchColor(new java.awt.Color(230, 230, 230));
            byte border = MapPalette.matchColor(new java.awt.Color(30, 30, 30));

            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    if (x < 4 || x > 123 || y < 4 || y > 123 || y == 32 || y == 94) {
                        canvas.setPixel(x, y, border);
                    } else {
                        canvas.setPixel(x, y, bg);
                    }
                }
            }

            // Title & Instruction
            canvas.drawText(22, 14, MinecraftFont.Font, "GRIMAC ANTI-BOT");
            canvas.drawText(16, 44, MinecraftFont.Font, "ENTER IN CHAT:");

            // Bold code display (offset by 1 pixel)
            int startX = Math.max(12, 64 - (code.length() * 5));
            canvas.drawText(startX, 68, MinecraftFont.Font, code);
            canvas.drawText(startX + 1, 68, MinecraftFont.Font, code);

            // Subtitle
            canvas.drawText(20, 106, MinecraftFont.Font, "HUMAN VERIFY");
        }
    }

    // --- Entity Attack Spawning & Removal ---

    public Entity spawnTargetEntity(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        try {
            World world = loc.getWorld();
            ArmorStand stand = world.spawn(loc, ArmorStand.class);
            stand.setGravity(false);
            stand.setArms(true);
            stand.setBasePlate(true);
            stand.setVisible(true);
            stand.setCustomName(ChatColor.translateAlternateColorCodes('&', "&c&lУдарьте манекен (ЛКМ)"));
            stand.setCustomNameVisible(true);
            try {
                stand.setInvulnerable(true);
            } catch (Throwable ignored) {}
            return stand;
        } catch (Throwable t) {
            LogUtil.error("[Antibot] Failed to spawn verification armor stand", t);
            return null;
        }
    }

    public void removeTargetEntity(UUID id) {
        if (id == null) return;
        try {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        } catch (Throwable ignored) {}
    }

    public static class VoidChunkGenerator extends ChunkGenerator {
        @Override
        public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
            return createChunkData(world);
        }

        @Override
        public boolean canSpawn(World world, int x, int z) {
            return true;
        }

        @Override
        public Location getFixedSpawnLocation(World world, Random random) {
            return new Location(world, PLATFORM_X, PLATFORM_Y + 4.0, PLATFORM_Z);
        }
    }

    public static Material getSafeMaterial(String... names) {
        for (String name : names) {
            try {
                Material m = Material.matchMaterial(name);
                if (m != null) return m;
            } catch (Throwable ignored) {}
        }
        return Material.STONE;
    }
}
