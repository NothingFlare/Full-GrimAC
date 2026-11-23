/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.platform.bukkit.gui.optimization;

import ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin;
import com.github.retrooper.packetevents.PacketEvents;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannelConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.jonesdev.sonar.bukkit.antibot.BukkitInjector;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced Netty and TCP Network Optimizer.
 *
 * Features:
 * - Anti-Coalescing: Enforces kernel-level TCP_NODELAY (Nagle's algorithm disabled) to eliminate
 *   packet clumping and segment buffering delay across TCP stacks.
 * - Optimized Buffer Watermarks: Configures Netty channel WriteBufferWaterMark (256KB low / 1024KB high)
 *   to eliminate false channel write-throttling during chunk bursts and high entity counts.
 * - Tuned Socket Buffers: Allocates 256KB SO_SNDBUF and SO_RCVBUF to prevent packet drops and bufferbloat.
 * - SO_KEEPALIVE: Prevents dead TCP hangs and connection drops.
 * - Zero EventLoop Contention: Avoids per-packet syscall flushing to maintain ultra-low ping.
 */
public class NetworkOptimizer implements Listener {

    public static volatile boolean enabled = false;
    public static final String HANDLER_NAME = "fullgrim_network_optimizer";

    private static final Set<UUID> optimizedPlayers = ConcurrentHashMap.newKeySet();

    // Reflection cache for CraftPlayer -> Channel fallback
    private static MethodHandle craftPlayerGetHandle;
    private static MethodHandle playerConnectionGetter;
    private static MethodHandle networkManagerGetter;
    private static MethodHandle channelGetter;
    private static boolean reflectionInitialized = false;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean state) {
        if (enabled == state) return;
        enabled = state;
        if (enabled) {
            applyToAllOnlinePlayers();
        } else {
            removeFromAllOnlinePlayers();
        }
    }

    public static void onServerShutdown() {
        setEnabled(false);
    }

    /**
     * Applies TCP socket options and tuned Netty buffer watermarks to a connection channel.
     * Disables Nagle's algorithm (TCP_NODELAY) at the OS kernel level without choking
     * Netty's EventLoop with per-packet syscall flushes.
     */
    public static void optimizeChannel(Channel channel) {
        if (channel == null || !channel.isOpen()) return;

        channel.eventLoop().execute(() -> {
            try {
                // 1. Socket Options: Disable Nagle's algorithm and tune buffers
                ChannelConfig config = channel.config();
                if (config instanceof SocketChannelConfig socketConfig) {
                    socketConfig.setTcpNoDelay(true);
                    socketConfig.setKeepAlive(true);
                    try {
                        socketConfig.setSendBufferSize(256 * 1024);
                        socketConfig.setReceiveBufferSize(256 * 1024);
                    } catch (Throwable ignored) {}
                    socketConfig.setWriteBufferWaterMark(new WriteBufferWaterMark(256 * 1024, 1024 * 1024));
                } else {
                    config.setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
                    config.setOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
                    try {
                        config.setOption(ChannelOption.SO_SNDBUF, 256 * 1024);
                        config.setOption(ChannelOption.SO_RCVBUF, 256 * 1024);
                    } catch (Throwable ignored) {}
                    config.setOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(256 * 1024, 1024 * 1024));
                }

                // 2. Remove legacy per-packet flush handler if present to prevent EventLoop starvation
                ChannelPipeline pipeline = channel.pipeline();
                if (pipeline.get(HANDLER_NAME) != null) {
                    pipeline.remove(HANDLER_NAME);
                }
            } catch (Throwable ignored) {}
        });
    }

    /**
     * Removes the network optimization handler and restores default Netty watermarks.
     */
    public static void removeOptimization(Channel channel) {
        if (channel == null || !channel.isOpen()) return;

        channel.eventLoop().execute(() -> {
            try {
                ChannelPipeline pipeline = channel.pipeline();
                if (pipeline.get(HANDLER_NAME) != null) {
                    pipeline.remove(HANDLER_NAME);
                }

                // Restore default buffer watermarks
                ChannelConfig config = channel.config();
                if (config instanceof SocketChannelConfig socketConfig) {
                    socketConfig.setWriteBufferWaterMark(WriteBufferWaterMark.DEFAULT);
                } else {
                    config.setOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT);
                }
            } catch (Throwable ignored) {}
        });
    }

    /**
     * Retrieves Netty Channel for a player via PacketEvents or reflection fallback.
     */
    public static Channel getChannel(Player player) {
        if (player == null) return null;

        // Try PacketEvents protocol manager
        try {
            Object ch = PacketEvents.getAPI().getProtocolManager().getChannel(player.getUniqueId());
            if (ch instanceof Channel) {
                return (Channel) ch;
            }
        } catch (Throwable ignored) {}

        // Fallback to reflection
        initReflection();
        if (channelGetter != null) {
            try {
                Object handle = craftPlayerGetHandle.invoke(player);
                Object listener = playerConnectionGetter.invoke(handle);
                Object networkManager = networkManagerGetter.invoke(listener);
                return (Channel) channelGetter.invoke(networkManager);
            } catch (Throwable ignored) {}
        }

        return null;
    }

    private static synchronized void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;
        try {
            final Method handleMethod = BukkitInjector.getOBCClass("entity.CraftPlayer").getDeclaredMethod("getHandle");
            final Field listenerField = BukkitInjector.findField(false, handleMethod.getReturnType(),
                "PlayerConnection", "ServerGamePacketListenerImpl");
            final Field networkManagerField = BukkitInjector.findField(true, listenerField.getType(),
                "NetworkManager", "Connection");
            final Field channelField = BukkitInjector.getFieldAt(networkManagerField.getType(), Channel.class, 0);

            craftPlayerGetHandle = MethodHandles.lookup().unreflect(handleMethod);
            playerConnectionGetter = MethodHandles.lookup().unreflectGetter(listenerField);
            networkManagerGetter = MethodHandles.lookup().unreflectGetter(networkManagerField);
            channelGetter = MethodHandles.lookup().unreflectGetter(channelField);
        } catch (Throwable ignored) {}
    }

    public static void applyToAllOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            optimizedPlayers.add(player.getUniqueId());
            Channel ch = getChannel(player);
            if (ch != null) {
                optimizeChannel(ch);
            }
        }
    }

    public static void removeFromAllOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Channel ch = getChannel(player);
            if (ch != null) {
                removeOptimization(ch);
            }
        }
        optimizedPlayers.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        optimizedPlayers.add(player.getUniqueId());

        Channel ch = getChannel(player);
        if (ch != null) {
            optimizeChannel(ch);
        }

        // Re-verify after 1 tick to ensure late-registered handlers do not overwrite settings
        Bukkit.getScheduler().runTaskLater(GrimACBukkitLoaderPlugin.LOADER, () -> {
            if (enabled && player.isOnline()) {
                Channel channel = getChannel(player);
                if (channel != null) {
                    optimizeChannel(channel);
                }
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        optimizedPlayers.remove(event.getPlayer().getUniqueId());
    }
}
