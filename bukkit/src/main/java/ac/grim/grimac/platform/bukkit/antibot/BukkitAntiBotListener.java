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
import ac.grim.grimac.player.GrimPlayer;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.net.InetAddress;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitAntiBotListener implements Listener {

    private final Map<UUID, PlayerVerificationSession> activeSessions = new ConcurrentHashMap<>();

    private static final Sound SOUND_PLING = getSafeSound("BLOCK_NOTE_BLOCK_PLING", "NOTE_PLING");
    private static final Sound SOUND_SUCCESS = getSafeSound("ENTITY_PLAYER_LEVELUP", "LEVEL_UP");
    private static final String CAPTCHA_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

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
            AntiBotConfig config = manager.getConfig();

            int durationTicks = (int) Math.round(config.getInGameDurationSeconds() * 20.0);
            if (config.isCaptchaEnabled()) {
                durationTicks = Math.max(durationTicks, (int) Math.round(config.getCaptchaMaxDurationSeconds() * 20.0));
            }
            if (config.isCheckEntityAttack()) {
                durationTicks = Math.max(durationTicks, 300); // at least 15s for interactive checks
            }

            Location originalLoc = player.getLocation().clone();
            PlayerVerificationSession session = new PlayerVerificationSession(player, ip, durationTicks, originalLoc);
            activeSessions.put(player.getUniqueId(), session);

            // 1. Stash and clear inventory so player has completely empty inventory
            session.stashInventory();

            // 2. Teleport to dedicated Sonar verification world if enabled
            Location targetPlatformLoc = null;
            if (config.isDedicatedWorldEnabled() && VerificationWorldManager.getInstance() != null) {
                Location spawn = VerificationWorldManager.getInstance().getSpawnLocation(config.getDedicatedWorldSpawnOffset());
                if (spawn != null) {
                    session.setVerificationLocation(spawn);
                    session.setPlatformY(VerificationWorldManager.PLATFORM_Y);
                    session.setBlockHeight(VerificationWorldManager.BLOCK_HEIGHT);
                    targetPlatformLoc = spawn;
                    teleportSafely(player, spawn);
                }
            } else {
                session.setVerificationLocation(originalLoc);
                targetPlatformLoc = originalLoc;
            }

            // 3. Hide from all online players if configured
            if (config.isHidePlayersDuringVerification()) {
                hidePlayerFromAll(player);
            }

            // 4. Send Sonar welcome notification in chat
            String welcome = config.getVerificationWelcomeMessage();
            if (welcome != null && !welcome.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', welcome));
            }

            // 5. Map Captcha check setup
            if (config.isCaptchaEnabled() && VerificationWorldManager.getInstance() != null) {
                session.setCaptchaRequired(true);
                session.setCaptchaTriesLeft(config.getCaptchaMaxTries());
                String code = generateRandomCode(config.getCaptchaCodeLength());
                session.setCaptchaCode(code);

                ItemStack mapItem = VerificationWorldManager.getInstance().createCaptchaMap(player, code);
                try {
                    player.getInventory().setItemInHand(mapItem);
                } catch (Throwable t) {
                    try {
                        player.getInventory().setItem(0, mapItem);
                    } catch (Throwable ignored) {}
                }

                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&e&lGrimAC &8» &fВведите код с карты в чат. &7(Осталось попыток: " + session.getCaptchaTriesLeft() + ")"));
            }

            // 6. Entity Attack / Arm Swing check setup
            if (config.isCheckEntityAttack() && VerificationWorldManager.getInstance() != null && targetPlatformLoc != null) {
                session.setEntityHitRequired(true);

                Location standLoc = new Location(
                        targetPlatformLoc.getWorld(),
                        VerificationWorldManager.PLATFORM_X,
                        VerificationWorldManager.PLATFORM_Y + VerificationWorldManager.BLOCK_HEIGHT,
                        VerificationWorldManager.PLATFORM_Z + 1.8,
                        180f,
                        0f
                );
                Entity stand = VerificationWorldManager.getInstance().spawnTargetEntity(standLoc);
                if (stand != null) {
                    session.setTargetEntityId(stand.getUniqueId());
                }

                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&e&lGrimAC &8» &fУдарьте манекен перед собой &e(ЛКМ)&f для подтверждения."));
            }

            LogUtil.info("[Antibot] " + player.getName() + " (" + ip + ") начал верификацию Sonar (мир: " +
                    (config.isDedicatedWorldEnabled() ? config.getDedicatedWorldName() : "limbo") +
                    ", капча: " + config.isCaptchaEnabled() +
                    ", удар цели: " + config.isCheckEntityAttack() + ")...");
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
            // Remove spawned target entity if present
            if (session.getTargetEntityId() != null && VerificationWorldManager.getInstance() != null) {
                VerificationWorldManager.getInstance().removeTargetEntity(session.getTargetEntityId());
            }

            // Safely restore player inventory so items are saved to player.dat
            session.restoreInventory();

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

        Location to = event.getTo();
        if (to == null) return;

        // If player has not yet transitioned to verification world, ignore movement in main world
        if (session.getVerificationLocation() != null &&
                !to.getWorld().equals(session.getVerificationLocation().getWorld())) {
            return;
        }

        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager == null) return;
        AntiBotConfig config = manager.getConfig();

        // 1. Sonar Horizontal movement lock (platform boundary check)
        if (config.isCheckMovementLock()) {
            Location center = session.getVerificationLocation();
            if (center != null) {
                double dx = to.getX() - center.getX();
                double dz = to.getZ() - center.getZ();
                if (Math.abs(dx) > 3.2 || Math.abs(dz) > 3.2) {
                    event.setTo(new Location(to.getWorld(), center.getX(), to.getY(), center.getZ(), to.getYaw(), to.getPitch()));
                    return;
                }
            }
        }

        double y = to.getY();
        double deltaY = y - event.getFrom().getY();

        // 2. Sonar falling below platform check
        if (y < session.getPlatformY() - 0.5) {
            failOrShowCaptcha(session, "Игрок провалился сквозь блоки платформы (y=" + String.format(Locale.ROOT, "%.2f", y) + ")");
            event.setCancelled(true);
            return;
        }

        // 3. Sonar Gravity & Hitbox Collision checks
        if (!player.isOnGround()) {
            if (deltaY < 0) {
                session.incrementFallTicks();
                if (session.hasMoved() && config.isCheckGravity() && !session.isGravityValidated()) {
                    double lastDeltaY = session.getLastDeltaY();
                    double predicted = (lastDeltaY - 0.08) * 0.98;
                    double diff = Math.abs(deltaY - predicted);
                    // Check against Sonar's gravity equation tolerance
                    if (diff > 0.05 && session.getFallTicks() > 1) {
                        failOrShowCaptcha(session, "Нарушение физики гравитации Sonar (diff=" + String.format(Locale.ROOT, "%.4f", diff) + ")");
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
            // Player touches ground: verify block hitbox surface ONCE upon landing
            if (config.isCheckHitboxCollision() && !session.isHitboxValidated()) {
                double platformSurface = session.getPlatformY() + session.getBlockHeight();
                double diffFromHitbox = Math.abs(y - platformSurface);
                if (diffFromHitbox > 0.05) {
                    failOrShowCaptcha(session, "Несовпадение хитбокса коллизии (y=" + String.format(Locale.ROOT, "%.3f", y) + ", surface=" + platformSurface + ")");
                    event.setCancelled(true);
                    return;
                }
                session.setHitboxValidated(true);
            }
        }

        session.setLastY(y);
    }

    private void failOrShowCaptcha(PlayerVerificationSession session, String reason) {
        AntiBotManager manager = AntiBotManager.getInstance();
        AntiBotConfig config = manager != null ? manager.getConfig() : null;

        // Sonar failOrShowCaptcha fallback logic
        if (config != null && (config.isCaptchaEnabled() || config.isCaptchaOnFail())) {
            if (!session.isCaptchaRequired()) {
                session.setCaptchaRequired(true);
                session.setGravityValidated(true);
                session.setHitboxValidated(true);
                session.setCaptchaTriesLeft(config.getCaptchaMaxTries());
                String code = generateRandomCode(config.getCaptchaCodeLength());
                session.setCaptchaCode(code);

                Player player = session.getPlayer();
                if (player != null && player.isOnline() && VerificationWorldManager.getInstance() != null) {
                    ItemStack mapItem = VerificationWorldManager.getInstance().createCaptchaMap(player, code);
                    try {
                        player.getInventory().setItemInHand(mapItem);
                    } catch (Throwable t) {
                        try {
                            player.getInventory().setItem(0, mapItem);
                        } catch (Throwable ignored) {}
                    }
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&e&lGrimAC &8» &fВведите код с карты в чат: &7(Осталось попыток: " + session.getCaptchaTriesLeft() + ")"));
                }
                return;
            }
        }

        failSession(session, reason);
    }

    private void failSession(PlayerVerificationSession session, String reason) {
        if (session.isFailed()) return;
        session.setFailed(true, reason);
        activeSessions.remove(session.getUuid());

        // Remove target entity
        if (session.getTargetEntityId() != null && VerificationWorldManager.getInstance() != null) {
            VerificationWorldManager.getInstance().removeTargetEntity(session.getTargetEntityId());
        }

        // Restore inventory before kicking so data is intact
        session.restoreInventory();

        Player player = session.getPlayer();
        if (player == null || !player.isOnline()) return;

        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager != null) {
            manager.recordViolation(session.getIp(), reason);
            manager.getAttackTracker().onBotBlocked();
        }

        LogUtil.warn(String.format("[Antibot] %s (%s) провалил проверку: %s",
                session.getUsername(), session.getIp(), reason));

        Runnable kickAction = () -> {
            showPlayerToAll(player);
            if (session.getOriginalLocation() != null) {
                teleportSafely(player, session.getOriginalLocation());
            }
            String failedMsg = manager != null ? manager.getConfig().getVerificationFailedMessage() : "&cВы не прошли проверку на бота.";
            player.kickPlayer(ChatColor.translateAlternateColorCodes('&', failedMsg));
        };

        if (Bukkit.isPrimaryThread()) {
            kickAction.run();
        } else {
            Plugin plugin = GrimAPI.INSTANCE != null && GrimAPI.INSTANCE.getGrimPlugin() instanceof Plugin
                    ? (Plugin) GrimAPI.INSTANCE.getGrimPlugin() : null;
            if (plugin != null) {
                Bukkit.getScheduler().runTask(plugin, kickAction);
            } else {
                kickAction.run();
            }
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
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (activeSessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
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

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager) {
            PlayerVerificationSession session = activeSessions.get(damager.getUniqueId());
            if (session != null) {
                event.setCancelled(true);

                // Handle entity attack check
                if (session.isEntityHitRequired() && !session.isEntityHitValidated()) {
                    if (session.getTargetEntityId() != null &&
                            session.getTargetEntityId().equals(event.getEntity().getUniqueId())) {
                        session.setEntityHitValidated(true);
                        event.getEntity().remove();
                        session.setTargetEntityId(null);
                        damager.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aУдар засчитан!"));
                        playSoundSafely(damager, SOUND_SUCCESS, 1.0f, 1.2f);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmAnimation(PlayerAnimationEvent event) {
        if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
            PlayerVerificationSession session = activeSessions.get(event.getPlayer().getUniqueId());
            if (session != null) {
                session.setArmSwung(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PlayerVerificationSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        event.setCancelled(true);

        if (session.isCaptchaRequired() && !session.isCaptchaValidated()) {
            String msg = event.getMessage().trim();
            String code = session.getCaptchaCode();
            if (code != null && msg.equalsIgnoreCase(code)) {
                session.setCaptchaValidated(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aКод подтвержден!"));
                playSoundSafely(player, SOUND_SUCCESS, 1.0f, 1.2f);

                // Clear map from hand
                Plugin plugin = GrimAPI.INSTANCE != null && GrimAPI.INSTANCE.getGrimPlugin() instanceof Plugin
                        ? (Plugin) GrimAPI.INSTANCE.getGrimPlugin() : null;
                if (plugin != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> player.getInventory().clear());
                } else {
                    player.getInventory().clear();
                }
            } else {
                session.decrementCaptchaTries();
                if (session.getCaptchaTriesLeft() <= 0) {
                    failSession(session, "Превышено количество попыток ввода капчи");
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&cНеверный код! Осталось попыток: " + session.getCaptchaTriesLeft()));
                    playSoundSafely(player, SOUND_PLING, 1.0f, 0.5f);
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Пожалуйста, дождитесь завершения проверки перед отправкой сообщений.");
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
        AntiBotConfig config = manager.getConfig();

        for (PlayerVerificationSession session : activeSessions.values()) {
            Player player = session.getPlayer();
            if (player == null || !player.isOnline()) {
                if (session.getTargetEntityId() != null && VerificationWorldManager.getInstance() != null) {
                    VerificationWorldManager.getInstance().removeTargetEntity(session.getTargetEntityId());
                }
                session.restoreInventory();
                activeSessions.remove(session.getUuid());
                continue;
            }

            session.incrementTick();

            // At tick 10: validate client settings (gives client time to send ClientInformation packet)
            if (session.getTicksElapsed() == 10 && config.isCheckClientSettings() && !session.isClientSettingsValidated()) {
                if (!validateClientSettings(player, config, session)) {
                    continue;
                }
            }

            // Check if all active verification conditions are satisfied
            boolean gravityOk = !config.isCheckGravity() || session.isGravityValidated();
            boolean hitboxOk = !config.isCheckHitboxCollision() || session.isHitboxValidated();
            boolean captchaOk = !session.isCaptchaRequired() || session.isCaptchaValidated();
            boolean entityOk = !session.isEntityHitRequired() || session.isEntityHitValidated();
            boolean settingsOk = !config.isCheckClientSettings() || session.isClientSettingsValidated();
            boolean minTimeOk = session.getTicksElapsed() >= 20;

            if (gravityOk && hitboxOk && captchaOk && entityOk && settingsOk && minTimeOk) {
                session.setCompleted(true);
                activeSessions.remove(session.getUuid());

                // Remove target entity if present
                if (session.getTargetEntityId() != null && VerificationWorldManager.getInstance() != null) {
                    VerificationWorldManager.getInstance().removeTargetEntity(session.getTargetEntityId());
                }

                // Restore original inventory completely
                session.restoreInventory();

                manager.getDatabase().addVerified(session.getUsername(), session.getIp());
                manager.getAttackTracker().onVerifiedLogin();

                // Teleport safely back to original location in main world
                if (session.getOriginalLocation() != null) {
                    teleportSafely(player, session.getOriginalLocation());
                }

                // Restore visibility to other players
                showPlayerToAll(player);

                // Send success message only if configured (Sonar default is silent transition)
                String success = config.getVerificationSuccessMessage();
                if (success != null && !success.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', success));
                }

                LogUtil.info(String.format("[Antibot] %s успешно прошел проверки (гравитация=%s, хитбокс=%s, капча=%s, манекен=%s, настройки=%s, %.1f сек).",
                        session.getUsername(),
                        session.isGravityValidated(),
                        session.isHitboxValidated(),
                        session.isCaptchaValidated(),
                        session.isEntityHitValidated(),
                        session.isClientSettingsValidated(),
                        session.getTicksElapsed() / 20.0));
                continue;
            }

            // Check timeout
            if (session.getTicksElapsed() >= session.getTotalTicks()) {
                if (session.isCaptchaRequired() && !session.isCaptchaValidated()) {
                    failSession(session, "Истекло время на ввод капчи с карты");
                } else if (session.isEntityHitRequired() && !session.isEntityHitValidated()) {
                    failSession(session, "Истекло время на выполнение удара по цели");
                } else if (config.isCheckGravity() && !session.isGravityValidated()) {
                    failSession(session, "Истекло время на проверку физики гравитации Sonar");
                } else if (config.isCheckHitboxCollision() && !session.isHitboxValidated()) {
                    failSession(session, "Истекло время на проверку хитбокса Sonar");
                } else {
                    failSession(session, "Истекло время прохождения проверки");
                }
            }
        }
    }

    private boolean validateClientSettings(Player player, AntiBotConfig config, PlayerVerificationSession session) {
        if (!config.isCheckClientSettings()) {
            session.setClientSettingsValidated(true);
            return true;
        }

        // 1. View distance check
        int viewDist = -1;
        try {
            viewDist = player.getClientViewDistance();
        } catch (Throwable ignored) {}

        if (viewDist > 0 && viewDist < config.getMinViewDistance()) {
            failSession(session, "Некорректная дальность прорисовки (viewDistance=" + viewDist + " < " + config.getMinViewDistance() + ")");
            return false;
        }

        // 2. Client Locale / Language check
        String locale = null;
        try {
            locale = player.getLocale();
        } catch (Throwable ignored) {}

        if (locale != null && !locale.isEmpty()) {
            String clean = locale.trim().toLowerCase(Locale.ROOT);
            if (clean.length() < 2 || clean.length() > 16 || !clean.matches("^[a-z]{2,4}([_-][a-z0-9]{2,4})?$")) {
                failSession(session, "Некорректная локаль клиента (locale=" + locale + ")");
                return false;
            }
        }

        // 3. Client Brand check
        if (config.isClientBrandEnabled()) {
            GrimPlayer grimPlayer = GrimAPI.INSTANCE != null && GrimAPI.INSTANCE.getPlayerDataManager() != null
                    ? GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(player.getUniqueId()) : null;
            if (grimPlayer != null) {
                String brand = grimPlayer.getBrand();
                if (brand != null && !brand.isEmpty()) {
                    if (brand.length() > config.getClientBrandMaxLength() || !brand.matches(config.getClientBrandRegex())) {
                        failSession(session, "Недопустимый клиентский бренд (brand=" + brand + ")");
                        return false;
                    }
                }
            }
        }

        session.setClientSettingsValidated(true);
        return true;
    }

    private static String generateRandomCode(int length) {
        int len = Math.max(3, Math.min(10, length));
        Random random = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CAPTCHA_CHARS.charAt(random.nextInt(CAPTCHA_CHARS.length())));
        }
        return sb.toString();
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
