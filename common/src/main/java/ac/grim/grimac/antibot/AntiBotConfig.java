/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.antibot;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.LogUtil;
import lombok.Getter;
import lombok.Setter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.*;

@Getter
@Setter
public class AntiBotConfig {

    private boolean antibotEnabled = true;
    private boolean logPlayerAddresses = true;
    private int maxOnlinePerIp = 3;
    private long rejoinDelayMs = 2500;

    // Attack tracker
    private int minPlayersForAttack = 8;
    private long minAttackDurationMs = 30000;
    private int minAttackThreshold = 2;
    private long attackCooldownDelayMs = 3000;
    private boolean notifyStaff = true;

    // Verification & Blacklist
    private String verificationTiming = "ALWAYS"; // ALWAYS, DURING_ATTACK, OFF
    private long reconnectMinDelayMs = 2000;
    private long reconnectWindowMs = 60000;
    private String verificationKickMessage = "&e[GrimAC AntiBot]\n&aПроверка на бота!\n&7Пожалуйста, перезайдите на сервер через 2 секунды для подтверждения.";
    private String verificationSuccessMessage = "&eGrimAC &8» &aПроверка на бота успешно пройдена! Приятной игры.";
    private long blacklistTimeMs = 600000;
    private int blacklistThreshold = 3;
    private boolean nameValidationEnabled = true;
    private String nameValidationRegex = "^[a-zA-Z0-9_]{3,16}$";
    private boolean clientBrandEnabled = true;
    private String clientBrandRegex = "^[!-~ §]+$";
    private int clientBrandMaxLength = 64;
    private Set<Integer> blacklistedProtocols = new HashSet<>();

    // Alts detection
    private boolean altsEnabled = true;
    private int maxAltsPerIp = 5;
    private boolean blockExcessAlts = false;
    private boolean alertOnAltJoin = true;
    private int alertMinAlts = 2;
    private String altsKickMessage = "&cПревышен лимит зарегистрированных аккаунтов для вашего IP адреса.";

    // Database
    private String dbType = "SQLITE";
    private String dbFilename = "antibot.db";
    private int cacheDays = 7;

    public void load(File dataFolder) {
        File configFile = new File(dataFolder, "antibot.yml");
        if (!configFile.exists()) {
            saveDefault(configFile);
            return;
        }

        try (InputStream in = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(in);
            if (data == null) return;

            parseConfig(data);
        } catch (Throwable t) {
            LogUtil.error("Failed to load antibot.yml, using default settings", t);
        }
    }

    @SuppressWarnings("unchecked")
    private void parseConfig(Map<String, Object> data) {
        Map<String, Object> antibot = getSection(data, "antibot");
        if (antibot != null) {
            antibotEnabled = getBoolean(antibot, "enabled", antibotEnabled);
            logPlayerAddresses = getBoolean(antibot, "log-player-addresses", logPlayerAddresses);
            maxOnlinePerIp = getInt(antibot, "max-online-per-ip", maxOnlinePerIp);
            rejoinDelayMs = getLong(antibot, "rejoin-delay-ms", rejoinDelayMs);
        }

        Map<String, Object> tracker = getSection(data, "attack-tracker");
        if (tracker != null) {
            minPlayersForAttack = getInt(tracker, "min-players-for-attack", minPlayersForAttack);
            minAttackDurationMs = getLong(tracker, "min-attack-duration-ms", minAttackDurationMs);
            minAttackThreshold = getInt(tracker, "min-attack-threshold", minAttackThreshold);
            attackCooldownDelayMs = getLong(tracker, "attack-cooldown-delay-ms", attackCooldownDelayMs);
            notifyStaff = getBoolean(tracker, "notify-staff", notifyStaff);
        }

        Map<String, Object> verification = getSection(data, "verification");
        if (verification != null) {
            verificationTiming = getString(verification, "timing", verificationTiming).toUpperCase(Locale.ROOT);
            reconnectMinDelayMs = getLong(verification, "reconnect-min-delay-ms", reconnectMinDelayMs);
            reconnectWindowMs = getLong(verification, "reconnect-window-ms", reconnectWindowMs);
            verificationKickMessage = getString(verification, "kick-message", verificationKickMessage);
            verificationSuccessMessage = getString(verification, "success-message", verificationSuccessMessage);
            blacklistTimeMs = getLong(verification, "blacklist-time-ms", blacklistTimeMs);
            blacklistThreshold = getInt(verification, "blacklist-threshold", blacklistThreshold);

            Map<String, Object> nameVal = getSection(verification, "name-validation");
            if (nameVal != null) {
                nameValidationEnabled = getBoolean(nameVal, "enabled", nameValidationEnabled);
                nameValidationRegex = getString(nameVal, "regex", nameValidationRegex);
            }

            Map<String, Object> brand = getSection(verification, "client-brand");
            if (brand != null) {
                clientBrandEnabled = getBoolean(brand, "enabled", clientBrandEnabled);
                clientBrandRegex = getString(brand, "regex", clientBrandRegex);
                clientBrandMaxLength = getInt(brand, "max-length", clientBrandMaxLength);
            }

            Object protocols = verification.get("blacklisted-protocols");
            if (protocols instanceof List<?> list) {
                blacklistedProtocols.clear();
                for (Object o : list) {
                    if (o instanceof Number num) {
                        blacklistedProtocols.add(num.intValue());
                    }
                }
            }
        }

        Map<String, Object> alts = getSection(data, "alts");
        if (alts != null) {
            altsEnabled = getBoolean(alts, "enabled", altsEnabled);
            maxAltsPerIp = getInt(alts, "max-alts-per-ip", maxAltsPerIp);
            blockExcessAlts = getBoolean(alts, "block-excess-alts", blockExcessAlts);
            alertOnAltJoin = getBoolean(alts, "alert-on-alt-join", alertOnAltJoin);
            alertMinAlts = getInt(alts, "alert-min-alts", alertMinAlts);
            altsKickMessage = getString(alts, "kick-message", altsKickMessage);
        }

        Map<String, Object> db = getSection(data, "database");
        if (db != null) {
            dbType = getString(db, "type", dbType);
            dbFilename = getString(db, "filename", dbFilename);
            cacheDays = getInt(db, "cache-days", cacheDays);
        }
    }

    public void saveDefault(File configFile) {
        try {
            configFile.getParentFile().mkdirs();
            String defaultContent =
                    "# ==============================================================================\n" +
                    "#                    Full GrimAC — AntiBot & Alts Configuration\n" +
                    "# ==============================================================================\n" +
                    "\n" +
                    "# Общие настройки антибота\n" +
                    "antibot:\n" +
                    "  enabled: true\n" +
                    "  log-player-addresses: true\n" +
                    "  max-online-per-ip: 3\n" +
                    "  rejoin-delay-ms: 2500\n" +
                    "\n" +
                    "# Настройки трекера атак (Attack Tracker)\n" +
                    "attack-tracker:\n" +
                    "  min-players-for-attack: 8\n" +
                    "  min-attack-duration-ms: 30000\n" +
                    "  min-attack-threshold: 2\n" +
                    "  attack-cooldown-delay-ms: 3000\n" +
                    "  notify-staff: true\n" +
                    "\n" +
                    "# Проверка и фильтрация бот-подключений (Verification)\n" +
                    "verification:\n" +
                    "  timing: ALWAYS # Режим проверки: ALWAYS (всегда для новых), DURING_ATTACK (только при атаке), OFF (выкл)\n" +
                    "  reconnect-min-delay-ms: 2000 # Минимальная задержка перед переподключением\n" +
                    "  reconnect-window-ms: 60000 # Окно ожидания переподключения\n" +
                    "  kick-message: \"&e[GrimAC AntiBot]\\n&aПроверка на бота!\\n&7Пожалуйста, перезайдите на сервер через 2 секунды для подтверждения.\"\n" +
                    "  success-message: \"&eGrimAC &8» &aПроверка на бота успешно пройдена! Приятной игры.\"\n" +
                    "  blacklist-time-ms: 600000\n" +
                    "  blacklist-threshold: 3\n" +
                    "  name-validation:\n" +
                    "    enabled: true\n" +
                    "    regex: \"^[a-zA-Z0-9_]{3,16}$\"\n" +
                    "  client-brand:\n" +
                    "    enabled: true\n" +
                    "    regex: \"^[!-~ §]+$\"\n" +
                    "    max-length: 64\n" +
                    "  blacklisted-protocols: []\n" +
                    "\n" +
                    "# Система детекта и контроля твинков (Alt Accounts)\n" +
                    "alts:\n" +
                    "  enabled: true\n" +
                    "  max-alts-per-ip: 5\n" +
                    "  block-excess-alts: false\n" +
                    "  alert-on-alt-join: true\n" +
                    "  alert-min-alts: 2\n" +
                    "  kick-message: \"&cПревышен лимит зарегистрированных аккаунтов для вашего IP адреса.\"\n" +
                    "\n" +
                    "# Локальная база данных антибота и твинков\n" +
                    "database:\n" +
                    "  type: \"SQLITE\"\n" +
                    "  filename: \"antibot.db\"\n" +
                    "  cache-days: 7\n";

            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(defaultContent);
            }
        } catch (Throwable t) {
            LogUtil.error("Failed to save default antibot.yml", t);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getSection(Map<String, Object> parent, String key) {
        Object val = parent.get(key);
        return val instanceof Map ? (Map<String, Object>) val : null;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean def) {
        Object val = map.get(key);
        return val instanceof Boolean ? (Boolean) val : def;
    }

    private int getInt(Map<String, Object> map, String key, int def) {
        Object val = map.get(key);
        return val instanceof Number ? ((Number) val).intValue() : def;
    }

    private long getLong(Map<String, Object> map, String key, long def) {
        Object val = map.get(key);
        return val instanceof Number ? ((Number) val).longValue() : def;
    }

    private String getString(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? val.toString() : def;
    }
}
