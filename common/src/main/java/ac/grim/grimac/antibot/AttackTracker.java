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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class AttackTracker {

    private final AtomicInteger connectionsThisSecond = new AtomicInteger();
    private final AtomicInteger loginsThisSecond = new AtomicInteger();

    private long lastJoinsPerSecond = 0;
    private long lastConnectionsPerSecond = 0;

    private long peakJoinsPerSecond = 0;
    private long peakConnectionsPerSecond = 0;

    private final AtomicLong totalBlockedBots = new AtomicLong();
    private final AtomicLong totalVerifiedLogins = new AtomicLong();

    private volatile boolean underAttack = false;
    private long attackStartTime = 0;
    private long lastAttackPeakTime = 0;
    private int attackIncidentCount = 0;

    private final AntiBotConfig config;

    public AttackTracker(AntiBotConfig config) {
        this.config = config;
    }

    public void onConnection() {
        connectionsThisSecond.incrementAndGet();
    }

    public void onLogin() {
        loginsThisSecond.incrementAndGet();
    }

    public void onBotBlocked() {
        totalBlockedBots.incrementAndGet();
    }

    public void onVerifiedLogin() {
        totalVerifiedLogins.incrementAndGet();
    }

    public void tick() {
        lastConnectionsPerSecond = connectionsThisSecond.getAndSet(0);
        lastJoinsPerSecond = loginsThisSecond.getAndSet(0);

        long now = System.currentTimeMillis();

        if (lastJoinsPerSecond >= config.getMinPlayersForAttack() || lastConnectionsPerSecond >= config.getMinPlayersForAttack() * 2) {
            if (lastJoinsPerSecond > peakJoinsPerSecond) {
                peakJoinsPerSecond = lastJoinsPerSecond;
            }
            if (lastConnectionsPerSecond > peakConnectionsPerSecond) {
                peakConnectionsPerSecond = lastConnectionsPerSecond;
            }

            attackIncidentCount++;
            lastAttackPeakTime = now;

            if (!underAttack && attackIncidentCount >= config.getMinAttackThreshold()) {
                underAttack = true;
                attackStartTime = now;
                String alert = String.format("[GrimAC Antibot] ВНИМАНИЕ: Обнаружена бот-атака! (Поток: %d вх/сек). Активирован защитный режим фильтрации.", lastJoinsPerSecond);
                LogUtil.warn(alert);
                if (config.isNotifyStaff()) {
                    broadcastStaff("&e[GrimAC Antibot] &c&lВНИМАНИЕ: &7Обнаружена бот-атака (&c" + lastJoinsPerSecond + " &7вх/сек). Режим защиты &aАКТИВИРОВАН&7.");
                }
            }
        } else {
            if (underAttack) {
                long duration = now - attackStartTime;
                long sincePeak = now - lastAttackPeakTime;

                if (duration >= config.getMinAttackDurationMs() && sincePeak >= config.getAttackCooldownDelayMs()) {
                    underAttack = false;
                    attackIncidentCount = 0;
                    long durationSec = Math.max(1, duration / 1000);
                    String info = String.format("[GrimAC Antibot] Бот-атака успешно отражена. Длительность: %d сек, пик: %d вх/сек, заблокировано: %d ботов.",
                            durationSec, peakJoinsPerSecond, totalBlockedBots.get());
                    LogUtil.info(info);
                    if (config.isNotifyStaff()) {
                        broadcastStaff("&e[GrimAC Antibot] &a&lАТАКА ОТРАЖЕНА! &7Длительность: &e" + durationSec + " сек&7, пик: &c" + peakJoinsPerSecond + " вх/сек&7, заблокировано: &c" + totalBlockedBots.get() + " &7ботов.");
                    }
                    peakJoinsPerSecond = 0;
                    peakConnectionsPerSecond = 0;
                }
            } else if (attackIncidentCount > 0) {
                attackIncidentCount--;
            }
        }
    }

    public void setUnderAttackManual(boolean status) {
        this.underAttack = status;
        if (status) {
            attackStartTime = System.currentTimeMillis();
            lastAttackPeakTime = attackStartTime;
        }
    }

    private void broadcastStaff(String message) {
        try {
            Component comp = MessageUtil.miniMessage("%prefix% " + message);
            if (GrimAPI.INSTANCE != null && GrimAPI.INSTANCE.getPlayerDataManager() != null) {
                for (GrimPlayer p : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                    if (p.hasPermission("grim.alerts") || p.hasPermission("grim.antibot") || p.hasPermission("grim.admin")) {
                        p.sendMessage(comp);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
