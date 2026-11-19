/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.platform.bukkit.antibot;

import ac.grim.grimac.antibot.AntiBotManager;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.net.InetAddress;
import java.util.UUID;

public class BukkitAntiBotListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
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
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ChatColor.translateAlternateColorCodes('&', kickReason));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager == null) return;

        Player player = event.getPlayer();
        if (player.getAddress() != null && player.getAddress().getAddress() != null) {
            String ip = player.getAddress().getAddress().getHostAddress();
            manager.onPlayerJoin(player.getUniqueId(), player.getName(), ip);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager == null) return;

        Player player = event.getPlayer();
        String ip = null;
        if (player.getAddress() != null && player.getAddress().getAddress() != null) {
            ip = player.getAddress().getAddress().getHostAddress();
        }
        manager.onPlayerQuit(player.getUniqueId(), ip);
    }
}
