/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.manager.cohort;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.impl.misc.ComparativeAnomaly;
import ac.grim.grimac.player.GrimPlayer;

import java.util.ArrayList;
import java.util.List;

public final class PingCohortManager {

    private static boolean enabled = true;
    private static double zScoreThreshold = 3.0;

    private PingCohortManager() {
    }

    public static void reload() {
        try {
            var config = GrimAPI.INSTANCE.getConfigManager().getConfig();
            enabled = config.getBooleanElse("cohort-comparison.enabled", true);
            zScoreThreshold = config.getDoubleElse("cohort-comparison.z-score-threshold", 3.0);
        } catch (Throwable ignored) {
        }
    }

    public static int getBracket(GrimPlayer player) {
        int ping = player.getTransactionPing();
        if (ping <= 0) ping = player.getKeepAlivePing();
        if (ping <= 0) ping = 50;

        if (ping < 60) return 0;
        if (ping < 120) return 1;
        if (ping < 200) return 2;
        return 3;
    }

    public static void update(int currentTick) {
        if (!enabled) return;

        // Run comparative analysis every 100 ticks (5 seconds)
        if (currentTick % 100 != 0) return;

        List<GrimPlayer> players = new ArrayList<>();
        for (GrimPlayer p : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (p != null && !p.disableGrim) {
                players.add(p);
            }
        }

        if (players.size() < 2) return;

        // Group players into 4 ping cohorts
        List<List<GrimPlayer>> brackets = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            brackets.add(new ArrayList<>());
        }

        for (GrimPlayer player : players) {
            brackets.get(getBracket(player)).add(player);
        }

        for (int b = 0; b < brackets.size(); b++) {
            List<GrimPlayer> cohort = brackets.get(b);
            if (cohort.size() < 2) continue; // Need at least 2 peers for baseline

            // 1. Evaluate Click Standard Deviation (robotic consistency vs peer human jitter)
            List<Double> validClickDevs = new ArrayList<>();
            for (GrimPlayer p : cohort) {
                if (p.packetStateData.currentClickStdDev > 0 && p.packetStateData.currentClickStdDev < 150.0) {
                    validClickDevs.add(p.packetStateData.currentClickStdDev);
                }
            }

            if (validClickDevs.size() >= 2) {
                double meanDev = calculateMean(validClickDevs);
                double sdDev = calculateSD(validClickDevs, meanDev);

                // If cohort shows realistic human variance (mean > 12ms and SD > 2ms)
                if (meanDev > 12.0 && sdDev > 2.0) {
                    for (GrimPlayer p : cohort) {
                        double playerDev = p.packetStateData.currentClickStdDev;
                        if (playerDev > 0 && playerDev < 4.0) { // Suspiciously low variance
                            double z = (meanDev - playerDev) / sdDev;
                            if (z >= zScoreThreshold) {
                                ComparativeAnomaly check = p.checkManager.get(ComparativeAnomaly.class);
                                if (check != null) {
                                    check.flagAnomaly("robotic_cohort: Z=" + String.format("%.2f", z) +
                                            ", dev=" + String.format("%.1f", playerDev) + "ms vs cohort_mean=" + String.format("%.1f", meanDev) + "ms (ping_bracket=" + b + ")");
                                }
                            }
                        }
                    }
                }
            }

            // 2. Evaluate Reaction Time to Totem Pop / Damage
            List<Long> validReactions = new ArrayList<>();
            for (GrimPlayer p : cohort) {
                if (p.packetStateData.lastReactionTime >= 50 && p.packetStateData.lastReactionTime < 1500) {
                    validReactions.add(p.packetStateData.lastReactionTime);
                }
            }

            if (validReactions.size() >= 2) {
                double meanReact = 0;
                for (long r : validReactions) meanReact += r;
                meanReact /= validReactions.size();

                double sumSq = 0;
                for (long r : validReactions) sumSq += Math.pow(r - meanReact, 2);
                double sdReact = Math.sqrt(sumSq / validReactions.size());

                if (meanReact > 180.0 && sdReact > 20.0) {
                    for (GrimPlayer p : cohort) {
                        long playerReact = p.packetStateData.lastReactionTime;
                        if (playerReact > 0 && playerReact < 130) {
                            double z = (meanReact - playerReact) / sdReact;
                            if (z >= zScoreThreshold) {
                                ComparativeAnomaly check = p.checkManager.get(ComparativeAnomaly.class);
                                if (check != null) {
                                    check.flagAnomaly("reaction_cohort: Z=" + String.format("%.2f", z) +
                                            ", reaction=" + playerReact + "ms vs cohort_mean=" + String.format("%.1f", meanReact) + "ms (ping_bracket=" + b + ")");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static double calculateMean(List<Double> values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.size();
    }

    private static double calculateSD(List<Double> values, double mean) {
        double sum = 0;
        for (double v : values) sum += Math.pow(v - mean, 2);
        return Math.sqrt(sum / values.size());
    }
}
