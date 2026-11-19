/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.platform.bukkit.antibot;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.antibot.AntiBotConfig;
import ac.grim.grimac.antibot.AntiBotManager;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;

import java.net.InetAddress;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitAntiBotListener implements Listener {

    private final Map<UUID, PlayerVerificationSession> activeSessions = new ConcurrentHashMap<>();

    private static final Sound SOUND_PLING = getSafeSound("BLOCK_NOTE_BLOCK_PLING", "NOTE_PLING");
    private static final Sound SOUND_SUCCESS = getSafeSound("ENTITY_PLAYER_LEVELUP", "LEVEL_UP");

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager == null) return;

        InetAddress addr = event.getAddress();
        if (addr == null) return;
        String ip = addr.getHostAddress();
        String username = event.getName();
        UUID uuid = event.getUniqueId();

        int protocolVersion = 0;
        try {
            if (PacketEvents.getAPI() != null && PacketEvents.getAPI().getPlayerManager() != null) {
                User user = PacketEvents.getAPI().getPlayerManager().getUser(uuid);
                if (user != null && user.getClientVersion() != null) {
                    protocolVersion = user.getClientVersion().getProtocolVersion();
                }
            }
        } catch (Throwable ignored) {}

        AntiBotManager.PreLoginResult result = manager.evaluatePreLogin(ip, username, uuid, protocolVersion);
        if (!result.allowed()) {
            String kickReason = result.kickReason() != null ? result.kickReason() : "&cОтключен защитой антибота.";
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.translateAlternateColorCodes('&', kickReason));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager == null) return;

        Player player = event.getPlayer();
        if (player.hasMetadata("NPC")) return;

        InetAddress addr = event.getAddress();
        if (addr == null) return;
        String ip = addr.getHostAddress();

        // Secondary defense check for blacklisted IPs
        if (manager.getDatabase() != null && manager.getDatabase().isBlacklisted(ip)) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, ChatColor.RED + "Ваш IP адрес временно заблокирован защитой антибота GrimAC.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager == null) return;

        Player player = event.getPlayer();
        if (player.hasMetadata("NPC")) return;

        // If player was saved in dedicated world upon restart, safely return to main world
        if (manager.getConfig().isDedicatedWorldEnabled() &&
                player.getWorld().getName().equalsIgnoreCase(manager.getConfig().getDedicatedWorldName())) {
            World mainWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (mainWorld != null) {
                player.teleport(mainWorld.getSpawnLocation());
            }
        }

        String ip = null;
        if (player.getAddress() != null && player.getAddress().getAddress() != null) {
            ip = player.getAddress().getAddress().getHostAddress();
        }

        if (ip == null) return;

        manager.onPlayerJoin(player.getUniqueId(), player.getName(), ip);

        // Check if player requires Sonar In-Game Verification
        if (manager.shouldVerifyInGame(player.getName(), ip)) {
            int durationTicks = (int) Math.round(manager.getConfig().getInGameDurationSeconds() * 20.0);
            Location originalLoc = player.getLocation().clone();

            PlayerVerificationSession session = new PlayerVerificationSession(player, ip, durationTicks, originalLoc);
            activeSessions.put(player.getUniqueId(), session);

            // Teleport to dedicated Sonar verification world if enabled
            if (manager.getConfig().isDedicatedWorldEnabled() && VerificationWorldManager.getInstance() != null) {
                Location spawn = VerificationWorldManager.getInstance().getSpawnLocation(manager.getConfig().getDedicatedWorldSpawnOffset());
                if (spawn != null) {
                    session.setVerificationLocation(spawn);
                    session.setPlatformY(VerificationWorldManager.PLATFORM_Y);
                    session.setBlockHeight(VerificationWorldManager.BLOCK_HEIGHT);
                    teleportSafely(player, spawn);
                }
            } else {
                session.setVerificationLocation(originalLoc);
            }

            // Hide from all online players if configured
            if (manager.getConfig().isHidePlayersDuringVerification()) {
                hidePlayerFromAll(player);
            }

            // Send initial Sonar notification
            String welcome = manager.getConfig().getVerificationWelcomeMessage();
            if (welcome != null && !welcome.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', welcome));
            }

            sendTitleSafely(player, manager.getConfig().getVerificationTitle(), manager.getConfig().getVerificationSubtitle(), 5, 60, 5);
            playSoundSafely(player, SOUND_PLING, 1.0f, 1.0f);

            LogUtil.info("[Antibot] " + player.getName() + " (" + ip + ") начал внутриигровую верификацию Sonar (выделенный мир: " +
                    (manager.getConfig().isDedicatedWorldEnabled() ? manager.getConfig().getDedicatedWorldName() : "limbo") + ")...");
        } else if (manager.checkAndClearWelcomePending(player.getUniqueId())) {
            // Reconnect verification passed earlier
            String successMsg = manager.getConfig().getVerificationSuccessMessage();
            if (successMsg != null && !successMsg.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', successMsg));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        AntiBotManager manager = AntiBotManager.getInstance();
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PlayerVerificationSession session = activeSessions.remove(uuid);
        if (session != null) {
            showPlayerToAll(player);
            if (session.getOriginalLocation() != null) {
                try {
                    player.teleport(session.getOriginalLocation());
                } catch (Throwable ignored) {}
            }
        }

        if (manager == null) return;

        String ip = null;
        if (player.getAddress() != null && player.getAddress().getAddress() != null) {
            ip = player.getAddress().getAddress().getHostAddress();
        }
        manager.onPlayerQuit(uuid, ip);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerVerificationSession session = activeSessions.get(player.getUniqueId());
        if (session == null || session.isCompleted() || session.isFailed()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager == null) return;
        AntiBotConfig config = manager.getConfig();

        // 1. Sonar Horizontal movement lock
        if (config.isCheckMovementLock()) {
            Location center = session.getVerificationLocation();
            if (center != null) {
                double dx = to.getX() - center.getX();
                double dz = to.getZ() - center.getZ();
                if (Math.abs(dx) > 0.8 || Math.abs(dz) > 0.8) {
                    event.setTo(new Location(to.getWorld(), center.getX(), to.getY(), center.getZ(), to.getYaw(), to.getPitch()));
                    return;
                }
            }
        }

        double y = to.getY();
        double deltaY = y - from.getY();

        // 2. Sonar falling below platform check
        if (y < session.getPlatformY() - 0.5) {
            failSession(session, "Игрок провалился сквозь блоки платформы (y=" + String.format(Locale.ROOT, "%.2f", y) + ")");
            event.setCancelled(true);
            return;
        }

        // 3. Sonar Gravity & Hitbox Collision checks
        if (!player.isOnGround()) {
            if (deltaY < 0) {
                session.incrementFallTicks();
                if (session.hasMoved() && config.isCheckGravity()) {
                    double lastDeltaY = session.getLastDeltaY();
                    double predicted = (lastDeltaY - 0.08) * 0.98;
                    double diff = Math.abs(deltaY - predicted);
                    // Check against Sonar's gravity equation tolerance
                    if (diff > 0.05 && session.getFallTicks() > 1) {
                        failSession(session, "Нарушение физики гравитации Sonar (diff=" + String.format(Locale.ROOT, "%.4f", diff) + ")");
                        event.setCancelled(true);
                        return;
                    } else if (diff < 0.01) {
                        session.setGravityValidated(true);
                    }
                }
                session.setHasMoved(true);
                session.setLastDeltaY(deltaY);
            }
        } else {
            // Player touches ground: verify block hitbox surface
            if (config.isCheckHitboxCollision()) {
                double platformSurface = session.getPlatformY() + session.getBlockHeight();
                double diffFromHitbox = Math.abs(y - platformSurface);
                if (diffFromHitbox > 0.05) {
                    failSession(session, "Несовпадение хитбокса коллизии (y=" + String.format(Locale.ROOT, "%.3f", y) + ", surface=" + platformSurface + ")");
                    event.setCancelled(true);
                    return;
                }
                session.setHitboxValidated(true);
            }
        }

        session.setLastY(y);
    }

    private void failSession(PlayerVerificationSession session, String reason) {
        if (session.isFailed()) return;
        session.setFailed(true, reason);
        activeSessions.remove(session.getUuid());

        Player player = session.getPlayer();
        if (player == null || !player.isOnline()) return;

        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager != null) {
            manager.recordViolation(session.getIp(), reason);
            manager.getAttackTracker().onBotBlocked();
        }

        LogUtil.warn(String.format("[Antibot] %s (%s) провалил проверку Sonar: %s",
                session.getUsername(), session.getIp(), reason));

        showPlayerToAll(player);
        if (session.getOriginalLocation() != null) {
            teleportSafely(player, session.getOriginalLocation());
        }

        String failedMsg = manager != null ? manager.getConfig().getVerificationFailedMessage() : "&cВы не прошли проверку на бота.";
        player.kickPlayer(ChatColor.translateAlternateColorCodes('&', failedMsg));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (activeSessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (activeSessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (activeSessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (activeSessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (activeSessions.containsKey(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager && activeSessions.containsKey(damager.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (activeSessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Пожалуйста, дождитесь завершения проверки перед отправкой сообщений.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (activeSessions.containsKey(event.getPlayer().getUniqueId())) {
            String msg = event.getMessage().toLowerCase(Locale.ROOT);
            if (!msg.startsWith("/grim") && !msg.startsWith("/grimac")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "Вы не можете использовать команды во время проверки на бота.");
            }
        }
    }

    public void tickVerificationSessions() {
        if (activeSessions.isEmpty()) return;

        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager == null) return;

        for (PlayerVerificationSession session : activeSessions.values()) {
            Player player = session.getPlayer();
            if (player == null || !player.isOnline()) {
                activeSessions.remove(session.getUuid());
                continue;
            }

            session.incrementTick();

            // Update action bar countdown
            double remaining = session.getSecondsRemaining();
            String barFormat = manager.getConfig().getVerificationActionbarMessage();
            if (barFormat != null && !barFormat.isEmpty()) {
                String bar = barFormat.replace("{time}", String.format(Locale.ROOT, "%.1f", remaining));
                sendActionBarSafely(player, bar);
            }

            // Keep title active
            if (session.getTicksElapsed() % 20 == 0) {
                sendTitleSafely(player, manager.getConfig().getVerificationTitle(), manager.getConfig().getVerificationSubtitle(), 0, 30, 5);
            }

            // Check completion
            if (session.getTicksElapsed() >= session.getTotalTicks()) {
                session.setCompleted(true);
                activeSessions.remove(session.getUuid());

                manager.getDatabase().addVerified(session.getUsername(), session.getIp());
                manager.getAttackTracker().onVerifiedLogin();

                // Teleport safely back to original location in main world
                if (session.getOriginalLocation() != null) {
                    teleportSafely(player, session.getOriginalLocation());
                }

                // Restore visibility to other players
                showPlayerToAll(player);

                sendTitleSafely(player, "§a§lПРОВЕРКА ПРОЙДЕНА!", "§7Приятной игры на сервере!", 5, 40, 10);
                playSoundSafely(player, SOUND_SUCCESS, 1.0f, 1.2f);

                String success = manager.getConfig().getVerificationSuccessMessage();
                if (success != null && !success.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', success));
                }

                LogUtil.info(String.format("[Antibot] %s успешно прошел проверки Sonar (гравитация, хитбокс, таймер %.1f сек).",
                        session.getUsername(), session.getTotalTicks() / 20.0));
            }
        }
    }

    private static void teleportSafely(Player player, Location target) {
        if (player == null || target == null) return;
        try {
            player.teleportAsync(target);
        } catch (Throwable t) {
            try {
                player.teleport(target);
            } catch (Throwable ignored) {}
        }
    }

    private void hidePlayerFromAll(Player player) {
        Plugin plugin = GrimAPI.INSTANCE != null && GrimAPI.INSTANCE.getGrimPlugin() instanceof Plugin
                ? (Plugin) GrimAPI.INSTANCE.getGrimPlugin() : null;
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            try {
                if (plugin != null) {
                    other.hidePlayer(plugin, player);
                } else {
                    other.hidePlayer(player);
                }
            } catch (Throwable ignored) {}
        }
    }

    private void showPlayerToAll(Player player) {
        Plugin plugin = GrimAPI.INSTANCE != null && GrimAPI.INSTANCE.getGrimPlugin() instanceof Plugin
                ? (Plugin) GrimAPI.INSTANCE.getGrimPlugin() : null;
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            try {
                if (plugin != null) {
                    other.showPlayer(plugin, player);
                } else {
                    other.showPlayer(player);
                }
            } catch (Throwable ignored) {}
        }
    }

    private static void sendTitleSafely(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            String t = title != null ? ChatColor.translateAlternateColorCodes('&', title) : "";
            String s = subtitle != null ? ChatColor.translateAlternateColorCodes('&', subtitle) : "";
            player.sendTitle(t, s, fadeIn, stay, fadeOut);
        } catch (Throwable ignored) {}
    }

    private static void sendActionBarSafely(Player player, String message) {
        try {
            String formatted = ChatColor.translateAlternateColorCodes('&', message);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(formatted));
        } catch (Throwable ignored) {}
    }

    private static void playSoundSafely(Player player, Sound sound, float volume, float pitch) {
        if (sound == null) return;
        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Throwable ignored) {}
    }

    private static Sound getSafeSound(String name, String fallback) {
        try {
            return Sound.valueOf(name);
        } catch (Throwable t1) {
            try {
                return Sound.valueOf(fallback);
            } catch (Throwable ignored) {}
        }
        return null;
    }
}
