/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.antibot;

import java.util.UUID;

public record AltRecord(
        UUID uuid,
        String username,
        String ip,
        long firstLogin,
        long lastLogin
) {}
