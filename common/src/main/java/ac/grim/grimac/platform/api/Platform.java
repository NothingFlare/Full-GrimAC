/*
 * Copyright (C) 2020-2024 GrimAC Team
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.platform.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public enum Platform {

    BUKKIT("bukkit"),
    FOLIA("folia");

    @Getter private final String name;

    public static @Nullable Platform getByName(String name) {
        for (Platform platform : values()) {
            if (platform.getName().equalsIgnoreCase(name)) return platform;
        }
        return null;
    }

}
