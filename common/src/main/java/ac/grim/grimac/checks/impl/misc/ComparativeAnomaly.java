/*
 * Copyright (C) 2026 NothingFlare
 * Part of Full GrimAC project.
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(
        name = "ComparativeAnomaly",
        stableKey = "grim.misc.comparative_anomaly",
        description = "Statistical pattern outlier compared to peer ping cohort on this server"
)
public class ComparativeAnomaly extends Check {

    public ComparativeAnomaly(GrimPlayer player) {
        super(player);
    }

    public void flagAnomaly(String details) {
        if (player.disableGrim) return;
        flag(details);
    }
}
