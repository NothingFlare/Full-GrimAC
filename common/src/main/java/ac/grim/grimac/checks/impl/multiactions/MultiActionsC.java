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

        // Relaxed movement checks:
        // 1. Shift-clicking in inventory is completely legitimate; never penalize sneaking alone!
        // 2. In-air movement (jumping, falling), swimming, or vehicle riding are normal vanilla momentum.
        boolean isActivelyMoving = (sprinting || input)
                && player.clientClaimsLastOnGround
                && !player.isSwimming
                && !player.isGliding;

        if (!isActivelyMoving) {
            buffer = Math.max(0.0, buffer - 1.0);
            return;
        }

        // 3. Buffer allowance: allow legitimate quick 1-3 clicks during residual momentum or quick swaps.
        // Only trigger on sustained, blatant inventory walking (4+ consecutive moving clicks).
        buffer += 1.0;
        if (buffer >= 4.0) {
            buffer = 2.0;
            if (flag(V.write(verbose()).bool(sprinting).bool(sneaking).bool(input)) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
                InventoryResync.resync(player);
            }
        }
    }
}
