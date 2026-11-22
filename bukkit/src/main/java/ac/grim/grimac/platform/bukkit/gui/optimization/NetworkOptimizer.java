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
 * - Anti-Coalescing: Enforces TCP_NODELAY (Nagle's algorithm disabled) to eliminate packet clumping.
 * - Fast Flush: Injects outbound handler that immediately flushes packets upon generation by the server,
 *   bypassing tick-boundary batching and flush consolidation delays.
 * - Low Latency Routing: Sets IP_TOS (0x10 / IPTOS_LOWDELAY) for minimal network transfer jitter.
 * - Anti-Bufferbloat: Tunes Netty channel WriteBufferWaterMark (16KB / 64KB) to prevent bloated queues.
 * - SO_KEEPALIVE: Prevents dead TCP hangs and connection drops.
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
     * Outbound Netty handler that immediately flushes packets to the OS socket.
     * Prevents Netty and Paper from buffering/coalescing packets across server ticks.
     */
    @ChannelHandler.Sharable
    public static class FastFlushOutboundHandler extends ChannelOutboundHandlerAdapter {
        public static final FastFlushOutboundHandler INSTANCE = new FastFlushOutboundHandler();

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            // Write and flush directly: pushes packet into TCP socket without waiting for tick end
            ctx.write(msg, promise);
            ctx.flush();
        }
    }

    /**
     * Applies TCP socket options and Netty immediate-flush handler to a connection channel.
     */
    public static void optimizeChannel(Channel channel) {
        if (channel == null || !channel.isOpen()) return;

        channel.eventLoop().execute(() -> {
            try {
                // 1. Socket Options: Disable Nagle's algorithm and enable low-delay TOS
                ChannelConfig config = channel.config();
                if (config instanceof SocketChannelConfig socketConfig) {
                    socketConfig.setTcpNoDelay(true);
                    socketConfig.setKeepAlive(true);
                    try {
                        socketConfig.setTrafficClass(0x10); // IPTOS_LOWDELAY
                    } catch (Throwable ignored) {}
                    socketConfig.setWriteBufferWaterMark(new WriteBufferWaterMark(16 * 1024, 64 * 1024));
                } else {
                    config.setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
                    config.setOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
                    try {
                        config.setOption(ChannelOption.IP_TOS, 0x10);
                    } catch (Throwable ignored) {}
                    config.setOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(16 * 1024, 64 * 1024));
                }

                // 2. Remove flush consolidation handlers that bundle/delay packets
                ChannelPipeline pipeline = channel.pipeline();
                for (String name : pipeline.names()) {
                    ChannelHandler handler = pipeline.get(name);
                    if (handler != null) {
                        String cls = handler.getClass().getName().toLowerCase();
                        if (cls.contains("flushconsolidation") || cls.contains("coalesc")) {
                            try {
                                pipeline.remove(name);
                            } catch (Throwable ignored) {}
                        }
                    }
                }

                // 3. Inject immediate outbound flush handler right at the start of outbound flow
                if (pipeline.get(HANDLER_NAME) == null) {
                    pipeline.addFirst(HANDLER_NAME, FastFlushOutboundHandler.INSTANCE);
                }
            } catch (Throwable ignored) {}
        });
    }

    /**
     * Removes the network optimization handler from a channel.
     */
    public static void removeOptimization(Channel channel) {
        if (channel == null || !channel.isOpen()) return;

        channel.eventLoop().execute(() -> {
            try {
                ChannelPipeline pipeline = channel.pipeline();
                if (pipeline.get(HANDLER_NAME) != null) {
                    pipeline.remove(HANDLER_NAME);
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
