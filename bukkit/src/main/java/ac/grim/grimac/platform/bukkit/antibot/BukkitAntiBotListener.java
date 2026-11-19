/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.platform.bukkit.antibot;

import ac.grim.grimac.antibot.AntiBotManager;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

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
    public void onPlayerLogin(PlayerLoginEvent event) {
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager == null) return;

        Player player = event.getPlayer();
        if (player.hasMetadata("NPC")) return;

        InetAddress addr = event.getAddress();
        if (addr == null) return;
        String ip = addr.getHostAddress();

        // Secondary defense check for blacklisted IPs
        if (manager.getDatabase() != null && manager.getDatabase().isBlacklisted(ip)) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    ChatColor.translateAlternateColorCodes('&', "&cВаш IP адрес временно заблокирован защитой антибота GrimAC."));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager == null) return;

        Player player = event.getPlayer();
        if (player.hasMetadata("NPC")) return;

        String ip = null;
        if (player.getAddress() != null && player.getAddress().getAddress() != null) {
            ip = player.getAddress().getAddress().getHostAddress();
        }

        if (ip == null) return;

        manager.onPlayerJoin(player.getUniqueId(), player.getName(), ip);

        // Check if player requires Sonar-style In-Game Verification
        if (manager.shouldVerifyInGame(player.getName(), ip)) {
            int durationTicks = (int) Math.round(manager.getConfig().getInGameDurationSeconds() * 20.0);
            PlayerVerificationSession session = new PlayerVerificationSession(player, ip, durationTicks);
            activeSessions.put(player.getUniqueId(), session);

            // Send initial Sonar notification
            String welcome = manager.getConfig().getVerificationWelcomeMessage();
            if (welcome != null && !welcome.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', welcome));
            }

            sendTitleSafely(player, manager.getConfig().getVerificationTitle(), manager.getConfig().getVerificationSubtitle(), 5, 60, 5);
            playSoundSafely(player, SOUND_PLING, 1.0f, 1.0f);

            LogUtil.info("[Antibot] " + player.getName() + " (" + ip + ") начал внутриигровую верификацию Sonar...");
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

        activeSessions.remove(uuid);

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
        if (session == null || session.isCompleted()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to != null && (from.getX() != to.getX() || from.getZ() != to.getZ() || to.getY() > from.getY())) {
            event.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(), to.getPitch()));
        }
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

                sendTitleSafely(player, "§a§lПРОВЕРКА ПРОЙДЕНА!", "§7Приятной игры на сервере!", 5, 40, 10);
                playSoundSafely(player, SOUND_SUCCESS, 1.0f, 1.2f);

                String success = manager.getConfig().getVerificationSuccessMessage();
                if (success != null && !success.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', success));
                }

                LogUtil.info(String.format("[Antibot] %s прошел внутриигровую проверку на бота (%.1f сек).",
                        session.getUsername(), session.getTotalTicks() / 20.0));
            }
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
