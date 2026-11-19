/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.antibot;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

@Getter
public class AntiBotManager {

    private static AntiBotManager instance;

    private AntiBotConfig config;
    private AttackTracker attackTracker;
    private AntibotDatabase database;

    private final ConcurrentMap<String, Integer> onlinePerIp = new ConcurrentHashMap<>(256);
    private final ConcurrentMap<String, Long> lastConnectionTime = new ConcurrentHashMap<>(512);
    private final ConcurrentMap<String, Integer> violationsPerIp = new ConcurrentHashMap<>(256);

    private Pattern namePattern;
    private Pattern brandPattern;

    public record PreLoginResult(boolean allowed, String kickReason) {
        public static PreLoginResult allow() {
            return new PreLoginResult(true, null);
        }

        public static PreLoginResult disallow(String reason) {
            return new PreLoginResult(false, reason);
        }
    }

    public AntiBotManager() {
        instance = this;
    }

    public static AntiBotManager getInstance() {
        return instance;
    }

    public synchronized void init(File dataFolder) {
        config = new AntiBotConfig();
        config.load(dataFolder);

        attackTracker = new AttackTracker(config);
        database = new AntibotDatabase(dataFolder, config.getDbFilename());
        database.init();

        updatePatterns();
        LogUtil.info("[Antibot] Full GrimAC Antibot & Alts system activated.");
    }

    public synchronized void reload(File dataFolder) {
        if (config != null) {
            config.load(dataFolder);
            updatePatterns();
        }
    }

    private void updatePatterns() {
        if (config.isNameValidationEnabled() && config.getNameValidationRegex() != null) {
            try {
                namePattern = Pattern.compile(config.getNameValidationRegex());
            } catch (Throwable t) {
                namePattern = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
            }
        } else {
            namePattern = null;
        }

        if (config.isClientBrandEnabled() && config.getClientBrandRegex() != null) {
            try {
                brandPattern = Pattern.compile(config.getClientBrandRegex());
            } catch (Throwable t) {
                brandPattern = Pattern.compile("^[!-~ §]+$");
            }
        } else {
            brandPattern = null;
        }
    }

    public PreLoginResult evaluatePreLogin(String ip, String username, UUID uuid, int protocolVersion) {
        if (!config.isAntibotEnabled()) {
            return PreLoginResult.allow();
        }

        attackTracker.onConnection();

        // 1. Whitelist bypass
        if (database.isWhitelisted(ip) || database.isWhitelisted(username)) {
            attackTracker.onLogin();
            return PreLoginResult.allow();
        }

        // 2. Blacklist check
        if (database.isBlacklisted(ip)) {
            attackTracker.onBotBlocked();
            return PreLoginResult.disallow("&cВаш IP адрес временно заблокирован защитой антибота GrimAC.");
        }

        // 3. Username format validation
        if (namePattern != null && !namePattern.matcher(username).matches()) {
            recordViolation(ip, "Недопустимый формат никнейма");
            attackTracker.onBotBlocked();
            return PreLoginResult.disallow("&cНедопустимый формат никнейма.");
        }

        // 4. Protocol blacklist check
        if (config.getBlacklistedProtocols().contains(protocolVersion)) {
            attackTracker.onBotBlocked();
            return PreLoginResult.disallow("&cДанная версия клиента заблокирована на сервере.");
        }

        long now = System.currentTimeMillis();

        // 5. Rejoin delay throttling
        Long lastConn = lastConnectionTime.get(ip);
        if (lastConn != null && (now - lastConn) < config.getRejoinDelayMs()) {
            attackTracker.onBotBlocked();
            return PreLoginResult.disallow("&cСлишком частые подключения! Подождите пару секунд.");
        }
        lastConnectionTime.put(ip, now);

        // 6. Max online players per IP
        int online = onlinePerIp.getOrDefault(ip, 0);
        if (online >= config.getMaxOnlinePerIp()) {
            attackTracker.onBotBlocked();
            return PreLoginResult.disallow("&cПревышен лимит одновременных подключений с одного IP адреса (" + config.getMaxOnlinePerIp() + ").");
        }

        // 7. Max alts per IP check (if block-excess-alts is enabled)
        if (config.isAltsEnabled() && config.isBlockExcessAlts() && config.getMaxAltsPerIp() > 0) {
            List<AltRecord> existingAlts = database.getAltsByIp(ip);
            boolean isKnownAccount = existingAlts.stream().anyMatch(a -> a.username().equalsIgnoreCase(username));
            if (!isKnownAccount && existingAlts.size() >= config.getMaxAltsPerIp()) {
                attackTracker.onBotBlocked();
                return PreLoginResult.disallow(config.getAltsKickMessage());
            }
        }

        // 8. Attack mode strict filtering
        if (attackTracker.isUnderAttack()) {
            if (!database.isVerified(username, ip)) {
                // Connection during attack: must pass verification or rate check
                int violations = violationsPerIp.getOrDefault(ip, 0);
                if (violations >= 2) {
                    database.addBlacklist(ip, "Бот-атака", config.getBlacklistTimeMs());
                    attackTracker.onBotBlocked();
                    return PreLoginResult.disallow("&cСервер находится под защитой от бот-атаки. Попробуйте войти позже.");
                }
            }
        }

        attackTracker.onLogin();
        return PreLoginResult.allow();
    }

    public void onPlayerJoin(UUID uuid, String username, String ip) {
        if (config == null) return;

        // Increment online IP counter
        onlinePerIp.compute(ip, (k, v) -> (v == null ? 1 : v + 1));

        // Mark as verified
        database.addVerified(username, ip);
        attackTracker.onVerifiedLogin();

        // Record to DB async
        if (config.isAltsEnabled()) {
            List<AltRecord> priorAlts = database.getAltsByIp(ip);
            database.recordLoginAsync(uuid, username, ip);

            // Alts alert to staff
            if (config.isAlertOnAltJoin()) {
                List<String> altNames = new ArrayList<>();
                for (AltRecord r : priorAlts) {
                    if (!r.username().equalsIgnoreCase(username)) {
                        altNames.add(r.username());
                    }
                }

                if (altNames.size() >= config.getAlertMinAlts() - 1 && !altNames.isEmpty()) {
                    String namesStr = String.join(", ", altNames);
                    String alert = String.format("&e[GrimAC Alts] &7Игрок &b%s &7зашел с IP &f%s &7(Обнаружены твинки [&c%d&7]: &e%s&7).",
                            username, ip, altNames.size(), namesStr);

                    broadcastStaff(alert);
                }
            }
        }
    }

    public void onPlayerQuit(UUID uuid, String ip) {
        if (ip == null) return;
        onlinePerIp.compute(ip, (k, v) -> {
            if (v == null || v <= 1) return null;
            return v - 1;
        });
    }

    public void recordViolation(String ip, String reason) {
        int v = violationsPerIp.compute(ip, (k, count) -> (count == null ? 1 : count + 1));
        if (v >= config.getBlacklistThreshold()) {
            database.addBlacklist(ip, reason, config.getBlacklistTimeMs());
            violationsPerIp.remove(ip);
            LogUtil.warn("[Antibot] IP " + ip + " blacklisted for " + (config.getBlacklistTimeMs() / 1000) + "s (Reason: " + reason + ")");
        }
    }

    public boolean validateClientBrand(String brand) {
        if (brand == null || brand.isEmpty()) return false;
        if (brand.length() > config.getClientBrandMaxLength()) return false;
        return brandPattern == null || brandPattern.matcher(brand).matches();
    }

    public void tick() {
        if (attackTracker != null) {
            attackTracker.tick();
        }
    }

    private void broadcastStaff(String message) {
        try {
            Component comp = MessageUtil.miniMessage("%prefix% " + message);
            if (GrimAPI.INSTANCE != null && GrimAPI.INSTANCE.getPlayerDataManager() != null) {
                for (GrimPlayer p : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                    if (p.hasPermission("grim.alerts") || p.hasPermission("grim.alts") || p.hasPermission("grim.admin")) {
                        p.sendMessage(comp);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public void close() {
        if (database != null) {
            database.close();
        }
    }
}
