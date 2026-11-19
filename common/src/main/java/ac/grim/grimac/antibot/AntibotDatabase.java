/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.antibot;

import ac.grim.grimac.utils.anticheat.LogUtil;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class AntibotDatabase {

    private final File dbFile;
    private Connection connection;
    private final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "FullGrimAC-AntibotDB-Worker");
        thread.setDaemon(true);
        return thread;
    });

    // High-speed in-memory caches for zero-latency network checks
    private final ConcurrentMap<String, List<AltRecord>> ipToAltsCache = new ConcurrentHashMap<>(1024);
    private final ConcurrentMap<UUID, AltRecord> uuidToAltCache = new ConcurrentHashMap<>(1024);
    private final ConcurrentMap<String, Long> blacklistCache = new ConcurrentHashMap<>(256);
    private final Set<String> whitelistCache = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, Long> verifiedCache = new ConcurrentHashMap<>(1024);

    public AntibotDatabase(File dataFolder, String filename) {
        this.dbFile = new File(dataFolder, filename);
    }

    public synchronized void init() {
        try {
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }

            // Load driver safely
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException ignored) {}

            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            try (Statement st = connection.createStatement()) {
                // High-performance SQLite tuning
                st.execute("PRAGMA journal_mode = WAL;");
                st.execute("PRAGMA synchronous = NORMAL;");
                st.execute("PRAGMA temp_store = MEMORY;");
                st.execute("PRAGMA cache_size = -32000;"); // 32MB cache
                st.execute("PRAGMA mmap_size = 67108864;"); // 64MB memory-mapped IO

                // Tables
                st.execute("CREATE TABLE IF NOT EXISTS alt_accounts (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "username TEXT NOT NULL, " +
                        "ip TEXT NOT NULL, " +
                        "first_login INTEGER NOT NULL, " +
                        "last_login INTEGER NOT NULL);");

                st.execute("CREATE TABLE IF NOT EXISTS verified_players (" +
                        "fingerprint TEXT PRIMARY KEY, " +
                        "username TEXT NOT NULL, " +
                        "ip TEXT NOT NULL, " +
                        "verified_at INTEGER NOT NULL);");

                st.execute("CREATE TABLE IF NOT EXISTS ip_blacklist (" +
                        "ip TEXT PRIMARY KEY, " +
                        "reason TEXT NOT NULL, " +
                        "expires_at INTEGER NOT NULL);");

                st.execute("CREATE TABLE IF NOT EXISTS ip_whitelist (" +
                        "target TEXT PRIMARY KEY, " +
                        "added_at INTEGER NOT NULL);");

                // Indexes for instant lookup
                st.execute("CREATE INDEX IF NOT EXISTS idx_alt_ip ON alt_accounts(ip);");
                st.execute("CREATE INDEX IF NOT EXISTS idx_alt_name ON alt_accounts(username);");
                st.execute("CREATE INDEX IF NOT EXISTS idx_verified_time ON verified_players(verified_at);");
            }

            loadCaches();
            LogUtil.info("[Antibot] Database initialized with WAL mode and memory cache (" + ipToAltsCache.size() + " IP entries loaded).");
        } catch (Throwable t) {
            LogUtil.error("[Antibot] Failed to initialize SQLite database, fallback to in-memory mode", t);
        }
    }

    private synchronized void loadCaches() {
        if (connection == null) return;
        long now = System.currentTimeMillis();

        // 1. Blacklist
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT ip, expires_at FROM ip_blacklist;")) {
            while (rs.next()) {
                String ip = rs.getString("ip");
                long expires = rs.getLong("expires_at");
                if (expires > now || expires == -1) {
                    blacklistCache.put(ip, expires);
                }
            }
        } catch (Throwable ignored) {}

        // 2. Whitelist
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT target FROM ip_whitelist;")) {
            while (rs.next()) {
                whitelistCache.add(rs.getString("target").toLowerCase(Locale.ROOT));
            }
        } catch (Throwable ignored) {}

        // 3. Verified players
        long verificationCooldown = 3600L;
        AntiBotManager abm = AntiBotManager.getInstance();
        if (abm != null && abm.getConfig() != null) {
            verificationCooldown = abm.getConfig().getVerificationCooldownSeconds();
        }
        long maxAgeMs = verificationCooldown > 0 ? (verificationCooldown * 1000L) : -1L;

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT fingerprint, verified_at FROM verified_players;")) {
            while (rs.next()) {
                String fp = rs.getString("fingerprint");
                long verifiedAt = rs.getLong("verified_at");
                if (maxAgeMs <= 0 || (now - verifiedAt) <= maxAgeMs) {
                    verifiedCache.put(fp, verifiedAt);
                }
            }
        } catch (Throwable ignored) {}

        // 4. Preload active alt records
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT uuid, username, ip, first_login, last_login FROM alt_accounts;")) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("username");
                    String ip = rs.getString("ip");
                    long first = rs.getLong("first_login");
                    long last = rs.getLong("last_login");

                    AltRecord record = new AltRecord(uuid, name, ip, first, last);
                    uuidToAltCache.put(uuid, record);
                    ipToAltsCache.computeIfAbsent(ip, k -> new CopyOnWriteArrayList<>()).add(record);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    public void recordLoginAsync(UUID uuid, String username, String ip) {
        long now = System.currentTimeMillis();

        // Preserve original first_login timestamp if known
        AltRecord existing = uuidToAltCache.get(uuid);
        long first = existing != null ? existing.firstLogin() : now;

        // Immediate memory cache update
        AltRecord record = new AltRecord(uuid, username, ip, first, now);
        uuidToAltCache.put(uuid, record);

        List<AltRecord> list = ipToAltsCache.computeIfAbsent(ip, k -> new CopyOnWriteArrayList<>());
        list.removeIf(r -> r.uuid().equals(uuid));
        list.add(record);

        // Async DB flush
        asyncExecutor.submit(() -> {
            synchronized (this) {
                if (connection == null) return;
                String sql = "INSERT INTO alt_accounts (uuid, username, ip, first_login, last_login) VALUES (?, ?, ?, ?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET username=excluded.username, ip=excluded.ip, last_login=excluded.last_login;";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, username);
                    ps.setString(3, ip);
                    ps.setLong(4, first);
                    ps.setLong(5, now);
                    ps.executeUpdate();
                } catch (Throwable t) {
                    LogUtil.error("[Antibot] Failed to write login record for " + username, t);
                }
            }
        });
    }

    public List<AltRecord> getAltsByIp(String ip) {
        List<AltRecord> cached = ipToAltsCache.get(ip);
        if (cached != null) return Collections.unmodifiableList(cached);
        return Collections.emptyList();
    }

    public List<AltRecord> getAltsByPlayer(String username) {
        for (AltRecord record : uuidToAltCache.values()) {
            if (record.username().equalsIgnoreCase(username)) {
                return getAltsByIp(record.ip());
            }
        }
        return Collections.emptyList();
    }

    public boolean isBlacklisted(String ip) {
        Long expires = blacklistCache.get(ip);
        if (expires == null) return false;
        if (expires != -1 && expires < System.currentTimeMillis()) {
            blacklistCache.remove(ip);
            return false;
        }
        return true;
    }

    public void addBlacklist(String ip, String reason, long durationMs) {
        long expires = durationMs <= 0 ? -1 : (System.currentTimeMillis() + durationMs);
        blacklistCache.put(ip, expires);

        asyncExecutor.submit(() -> {
            synchronized (this) {
                if (connection == null) return;
                String sql = "INSERT OR REPLACE INTO ip_blacklist (ip, reason, expires_at) VALUES (?, ?, ?);";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, ip);
                    ps.setString(2, reason);
                    ps.setLong(3, expires);
                    ps.executeUpdate();
                } catch (Throwable ignored) {}
            }
        });
    }

    public void removeBlacklist(String ip) {
        blacklistCache.remove(ip);
        asyncExecutor.submit(() -> {
            synchronized (this) {
                if (connection == null) return;
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM ip_blacklist WHERE ip=?;")) {
                    ps.setString(1, ip);
                    ps.executeUpdate();
                } catch (Throwable ignored) {}
            }
        });
    }

    public void clearBlacklist() {
        blacklistCache.clear();
        asyncExecutor.submit(() -> {
            synchronized (this) {
                if (connection == null) return;
                try (Statement st = connection.createStatement()) {
                    st.execute("DELETE FROM ip_blacklist;");
                } catch (Throwable ignored) {}
            }
        });
    }

    public Map<String, Long> getBlacklist() {
        return Collections.unmodifiableMap(blacklistCache);
    }

    public boolean isWhitelisted(String target) {
        return whitelistCache.contains(target.toLowerCase(Locale.ROOT));
    }

    public void addWhitelist(String target) {
        String key = target.toLowerCase(Locale.ROOT);
        whitelistCache.add(key);
        long now = System.currentTimeMillis();
        asyncExecutor.submit(() -> {
            synchronized (this) {
                if (connection == null) return;
                try (PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO ip_whitelist (target, added_at) VALUES (?, ?);")) {
                    ps.setString(1, key);
                    ps.setLong(2, now);
                    ps.executeUpdate();
                } catch (Throwable ignored) {}
            }
        });
    }

    public void removeWhitelist(String target) {
        String key = target.toLowerCase(Locale.ROOT);
        whitelistCache.remove(key);
        asyncExecutor.submit(() -> {
            synchronized (this) {
                if (connection == null) return;
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM ip_whitelist WHERE target=?;")) {
                    ps.setString(1, key);
                    ps.executeUpdate();
                } catch (Throwable ignored) {}
            }
        });
    }

    public void clearWhitelist() {
        whitelistCache.clear();
        asyncExecutor.submit(() -> {
            synchronized (this) {
                if (connection == null) return;
                try (Statement st = connection.createStatement()) {
                    st.execute("DELETE FROM ip_whitelist;");
                } catch (Throwable ignored) {}
            }
        });
    }

    public Set<String> getWhitelist() {
        return Collections.unmodifiableSet(whitelistCache);
    }

    public boolean isVerified(String username, String ip, long cooldownSeconds) {
        String fingerprint = username.toLowerCase(Locale.ROOT) + ":" + ip;
        Long verifiedAt = verifiedCache.get(fingerprint);
        if (verifiedAt == null) return false;

        if (cooldownSeconds > 0) {
            long maxAgeMs = cooldownSeconds * 1000L;
            if (System.currentTimeMillis() - verifiedAt > maxAgeMs) {
                verifiedCache.remove(fingerprint);
                asyncExecutor.submit(() -> {
                    synchronized (this) {
                        if (connection == null) return;
                        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM verified_players WHERE fingerprint=?;")) {
                            ps.setString(1, fingerprint);
                            ps.executeUpdate();
                        } catch (Throwable ignored) {}
                    }
                });
                return false;
            }
        }
        return true;
    }

    public boolean isVerified(String username, String ip) {
        long cooldown = 3600L;
        AntiBotManager manager = AntiBotManager.getInstance();
        if (manager != null && manager.getConfig() != null) {
            cooldown = manager.getConfig().getVerificationCooldownSeconds();
        }
        return isVerified(username, ip, cooldown);
    }

    public void pruneExpiredVerified(long cooldownSeconds) {
        if (cooldownSeconds <= 0) return;
        long now = System.currentTimeMillis();
        long maxAgeMs = cooldownSeconds * 1000L;

        verifiedCache.entrySet().removeIf(entry -> (now - entry.getValue()) > maxAgeMs);

        asyncExecutor.submit(() -> {
            synchronized (this) {
                if (connection == null) return;
                long threshold = now - maxAgeMs;
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM verified_players WHERE verified_at < ?;")) {
                    ps.setLong(1, threshold);
                    ps.executeUpdate();
                } catch (Throwable ignored) {}
            }
        });
    }

    public void addVerified(String username, String ip) {
        String fingerprint = username.toLowerCase(Locale.ROOT) + ":" + ip;
        long now = System.currentTimeMillis();
        verifiedCache.put(fingerprint, now);

        asyncExecutor.submit(() -> {
            synchronized (this) {
                if (connection == null) return;
                try (PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO verified_players (fingerprint, username, ip, verified_at) VALUES (?, ?, ?, ?);")) {
                    ps.setString(1, fingerprint);
                    ps.setString(2, username);
                    ps.setString(3, ip);
                    ps.setLong(4, now);
                    ps.executeUpdate();
                } catch (Throwable ignored) {}
            }
        });
    }

    public void removeVerifiedByPlayer(String username) {
        String lower = username.toLowerCase(Locale.ROOT);
        verifiedCache.keySet().removeIf(k -> k.startsWith(lower + ":"));
        asyncExecutor.submit(() -> {
            synchronized (this) {
                if (connection == null) return;
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM verified_players WHERE LOWER(username)=?;")) {
                    ps.setString(1, lower);
                    ps.executeUpdate();
                } catch (Throwable ignored) {}
            }
        });
    }

    public void clearVerified() {
        verifiedCache.clear();
        asyncExecutor.submit(() -> {
            synchronized (this) {
                if (connection == null) return;
                try (Statement st = connection.createStatement()) {
                    st.execute("DELETE FROM verified_players;");
                } catch (Throwable ignored) {}
            }
        });
    }

    public int getVerifiedCount() {
        return verifiedCache.size();
    }

    public synchronized void close() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException ignored) {}

        if (connection != null) {
            try {
                connection.close();
            } catch (Throwable ignored) {}
        }
    }
}
