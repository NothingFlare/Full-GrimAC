/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.checks;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.player.GrimPlayer;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CheckToggleRegistry {

    private static final Set<String> disabledChecks = ConcurrentHashMap.newKeySet();

    private CheckToggleRegistry() {
    }

    public static boolean isCheckDisabled(String checkName) {
        if (checkName == null) return false;
        return disabledChecks.contains(checkName.toLowerCase(Locale.ROOT));
    }

    public static void setCheckDisabled(String checkName, boolean disabled) {
        if (checkName == null) return;
        String key = checkName.toLowerCase(Locale.ROOT);
        if (disabled) {
            disabledChecks.add(key);
        } else {
            disabledChecks.remove(key);
        }

        // Apply immediately to all online players
        try {
            if (GrimAPI.INSTANCE != null && GrimAPI.INSTANCE.getPlayerDataManager() != null) {
                for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                    for (AbstractCheck check : player.getChecks()) {
                        if (matches(check, key)) {
                            check.setEnabled(!disabled);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean matches(AbstractCheck check, String key) {
        if (key.equalsIgnoreCase("all_checks")) return true;
        if (check.getCheckName() != null && check.getCheckName().toLowerCase(Locale.ROOT).contains(key)) return true;
        if (check.getAlternativeName() != null && check.getAlternativeName().toLowerCase(Locale.ROOT).contains(key)) return true;
        if (check.getConfigName() != null && check.getConfigName().toLowerCase(Locale.ROOT).contains(key)) return true;
        return false;
    }

    public static Set<String> getDisabledChecks() {
        return Collections.unmodifiableSet(disabledChecks);
    }

    public static void clear() {
        disabledChecks.clear();
        try {
            if (GrimAPI.INSTANCE != null && GrimAPI.INSTANCE.getPlayerDataManager() != null) {
                for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                    for (AbstractCheck check : player.getChecks()) {
                        check.setEnabled(true);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
