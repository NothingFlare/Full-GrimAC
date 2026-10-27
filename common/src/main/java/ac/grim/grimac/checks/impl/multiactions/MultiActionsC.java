package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.inventory.InventoryResync;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "MultiActionsC", stableKey = "grim.multiactions.inventory_click_while_moving", description = "Clicked in inventory while moving")
public class MultiActionsC extends Check implements PacketReceiveListener {
    private static final Verbose V = Verbose.of("sprinting={bool}, sneaking={bool}, input={bool}");

    private double buffer = 0.0;

    public MultiActionsC(GrimPlayer player) {
        super(player);
    }

    @Contract(pure = true)
    public static boolean isVerboseSprinting(@NotNull GrimPlayer player) {
        return player.isSprinting && (!player.isSwimming || !player.clientClaimsLastOnGround);
    }

    @Contract(pure = true)
    public static boolean isVerboseSneaking(@NotNull GrimPlayer player) {
        return player.isSneaking && (player.getClientVersion().isOlderThan(ClientVersion.V_1_15) || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_9));
    }

    @Contract(pure = true)
    public static boolean isVerboseInput(@NotNull GrimPlayer player) {
        return player.supportsEndTickPreVia() && player.packetStateData.knownInput.moving();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;
        if (player.serverOpenedInventoryThisTick) return;

        boolean sprinting = isVerboseSprinting(player);
        boolean sneaking = isVerboseSneaking(player);
        boolean input = isVerboseInput(player);

        // Check physical movement displacement in the world (catches walking/jumping on 1.21.1 and older clients)
        double dx = player.x - player.lastX;
        double dz = player.z - player.lastZ;
        double hDistSq = dx * dx + dz * dz;

        // Ignore displacement caused by external forces (water flow, knockback, explosions) or vehicles
        boolean isPushed = player.wasTouchingWater
                || player.wasWasTouchingWater
                || player.firstBreadKB != null
                || player.likelyKB != null
                || player.firstBreadExplosion != null
                || player.likelyExplosions != null;
        boolean isRiding = player.inVehicle();

        // Player is actively walking, running, or jumping in the world
        boolean movingInWorld = hDistSq > 0.008 && !isPushed && !player.isGliding && !player.isSwimming && !isRiding;

        // Sneaking alone while stationary is legitimate shift-clicking in chests/crafting;
        // only treat sneaking as moving if the player is also actively moving across the world or sending inputs.
        boolean sneakMoving = sneaking && (movingInWorld || input);

        boolean isMoving = sprinting || input || movingInWorld || sneakMoving;

        if (!isMoving) {
            buffer = Math.max(0.0, buffer - 0.5);
            return;
        }

        // Active movement detection:
        // - Direct movement input (WASD/jump in GUI is impossible in vanilla) adds 1.5 (immediate flag).
        // - Sprinting or physical world displacement adds 1.0 (gives 1 grace click for network tick boundaries,
        //   then immediately flags and cancels on the 2nd moving click).
        double increment = input ? 1.5 : 1.0;
        buffer += increment;

        if (buffer >= 1.5) {
            buffer = 1.5;
            if (flag(V.write(verbose()).bool(sprinting).bool(sneaking).bool(input)) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
                InventoryResync.resync(player);
            }
        }
    }
}
