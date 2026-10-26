/*
 * Copyright (C) 2020-2024 GrimAC Team
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.utils.reflection;

import lombok.experimental.UtilityClass;
import org.geysermc.api.Geyser;

import java.lang.reflect.Method;
import java.util.UUID;

@UtilityClass
public class GeyserUtil {
    // Floodgate is the authentication system for Geyser on servers that use Geyser as a proxy instead of installing it as a plugin directly on the server
    private static final boolean floodgate = ReflectionUtils.hasClass("org.geysermc.floodgate.api.FloodgateApi");
    private static final boolean geyser = ReflectionUtils.hasClass("org.geysermc.api.Geyser");

    private static Object floodgateInstance;
    private static Method isFloodgatePlayerMethod;

    static {
        if (floodgate) {
            try {
                Class<?> clazz = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                floodgateInstance = clazz.getMethod("getInstance").invoke(null);
                isFloodgatePlayerMethod = clazz.getMethod("isFloodgatePlayer", UUID.class);
            } catch (Throwable ignored) {
            }
        }
    }

    public static boolean isBedrockPlayer(UUID uuid) {
        if (floodgate && floodgateInstance != null && isFloodgatePlayerMethod != null) {
            try {
                return (boolean) isFloodgatePlayerMethod.invoke(floodgateInstance, uuid);
            } catch (Throwable ignored) {
            }
        }
        return geyser && Geyser.api().isBedrockPlayer(uuid);
    }
}
