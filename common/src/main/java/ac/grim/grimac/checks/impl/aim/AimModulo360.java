/*
 * Copyright (C) 2020-2024 GrimAC Team
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */
package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.combat.KillAuraBot;
import ac.grim.grimac.checks.type.RotationListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

// Based on Kauri AimA,
// I also discovered this flaw before open source Kauri, but did not want to open source its detection.
// It works on clients who % 360 their rotation.
@CheckData(name = "AimModulo360", stableKey = "grim.aim.modulo_360", description = "Sent a large yaw snap", decay = 0.005)
public class AimModulo360 extends Check implements RotationListener {

    private float lastDeltaYaw;

    public AimModulo360(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        // Exempt for teleport, entering a vehicle due to rotation reset or
        // after forced, client-sided rotation change after interacting with a horse (not necessarily mounting it)
        if (player.packetStateData.lastPacketWasTeleport || player.vehicleData.wasVehicleSwitch
                || player.packetStateData.horseInteractCausedForcedRotation) {
            lastDeltaYaw = rotationUpdate.deltaYaw();
            return;
        }

        if (player.yaw < 360 && player.yaw > -360 && Math.abs(rotationUpdate.deltaYaw()) > 320 && Math.abs(lastDeltaYaw) < 30) {
            flag();
            KillAuraBot bot = player.checkManager.get(KillAuraBot.class);
            if (bot != null) {
                bot.addSuspicion(2);
            }
        } else {
            reward();
        }

        lastDeltaYaw = rotationUpdate.deltaYaw();
    }
}
