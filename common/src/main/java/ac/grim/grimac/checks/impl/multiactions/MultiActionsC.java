package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.GrimAPI;
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
    private double lastHDistSq = 0.0;
    private int movingClicks = 0;

    public MultiActionsC(GrimPlayer player) {
        super(player);
    }

    @Contract(pure = true)
    public static boolean isVerboseSprinting(@NotNull GrimPlayer player) {
        // In vanilla Minecraft, in-air sprint status is retained from the jump;
        // only active ground sprinting is an actual movement state.
        return player.isSprinting && player.clientClaimsLastOnGround && !player.isSwimming;
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
        // Reset tracking state whenever the player closes their inventory
        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            buffer = 0.0;
            movingClicks = 0;
            lastHDistSq = 0.0;
            return;
        }

        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;
        if (player.serverOpenedInventoryThisTick) return;

        boolean input = isVerboseInput(player);
        boolean sprinting = isVerboseSprinting(player);
        boolean sneaking = isVerboseSneaking(player);

        // 1. External forces (knockback, explosions, water currents, vehicles, riptide, pistons)
        int currentTick = GrimAPI.INSTANCE.getTickManager().currentTick;
        boolean recentExternalForces = (Math.abs(currentTick - player.lastRiptideTick) <= 45)
                || (Math.abs(currentTick - player.lastCollidedWithRiptideTick) <= 30)
                || player.checkManager.getKnockbackHandler().hasActiveOrPendingKnockback()
                || player.checkManager.getExplosionHandler().hasActiveOrPendingExplosion();

        boolean isPushed = player.wasTouchingWater
                || player.wasWasTouchingWater
                || player.firstBreadKB != null
                || player.likelyKB != null
                || player.firstBreadExplosion != null
                || player.likelyExplosions != null
                || recentExternalForces
                || player.uncertaintyHandler.pistonX.stream().anyMatch(d -> d != 0.0)
                || player.uncertaintyHandler.pistonZ.stream().anyMatch(d -> d != 0.0);
        boolean isRiding = player.inVehicle();

        if (isPushed || isRiding || player.isGliding || player.isSwimming) {
            buffer = Math.max(0.0, buffer - 1.0);
            movingClicks = 0;
            return;
        }

        // 2. In-air momentum / jump inertia:
        // In vanilla Minecraft, jumping and opening inventory in mid-air is completely legitimate.
        // Once airborne, horizontal displacement is ballistic jump trajectory (momentum), NOT active walking.
        // Only direct movement key input (input=true) indicates a cheat while in the air.
        boolean airborne = !player.clientClaimsLastOnGround;
        if (airborne && !input) {
            buffer = Math.max(0.0, buffer - 0.5);
            movingClicks = 0;
            return;
        }

        // 3. Low-friction surfaces (Ice, Slime, Blue Ice):
        // Sliding across ice or slime is vanilla physics inertia.
        boolean isSlippery = player.uncertaintyHandler.isSteppingOnIce
                || player.uncertaintyHandler.isSteppingOnSlime
                || player.friction > 0.8f;
        if (isSlippery && !input) {
            buffer = Math.max(0.0, buffer - 0.5);
            movingClicks = 0;
            return;
        }

        // 4. Physical horizontal displacement in the world
        double dx = player.x - player.lastX;
        double dz = player.z - player.lastZ;
        double hDistSq = dx * dx + dz * dz;

        // Player is stationary or nearly stopped (< 0.07 blocks/tick)
        if (hDistSq <= 0.005) {
            buffer = Math.max(0.0, buffer - 1.0);
            movingClicks = 0;
            lastHDistSq = hDistSq;
            return;
        }

        // 5. Ground Deceleration / Entry Inertia:
        // When entering an inventory while running, the player slides to a stop over 2-3 ticks.
        // Under natural inertia, speed is strictly decelerating (hDistSq <= lastHDistSq + 0.003).
        // Cheats with InvMove maintain or increase speed (walking at ~0.22 b/t, hDistSq ~ 0.048).
        boolean isDecelerating = (lastHDistSq > 0.005 && hDistSq <= lastHDistSq + 0.003);
        lastHDistSq = hDistSq;

        movingClicks++;

        // Legitimate ground inertia & entry transition:
        // Natural deceleration or early entry clicks are allowed as transition regardless of stale knownInput.
        if (movingClicks <= 2 || isDecelerating) {
            buffer = Math.max(0.0, buffer - 0.5);
            return;
        }

        // If player is moving below standard walking threshold (~0.187 b/t), decay buffer
        if (hDistSq < 0.035) {
            buffer = Math.max(0.0, buffer - 0.5);
            return;
        }

        // Active illegal inventory movement detection:
        // Cheats with InvMove maintain active walking/sprinting speed over sustained clicks (>= 4 clicks)
        boolean isAccelerating = (hDistSq > lastHDistSq + 0.005);
        boolean sustainedWalking = (movingClicks >= 4 || isAccelerating || sprinting);

        if (!sustainedWalking) {
            buffer = Math.max(0.0, buffer - 0.5);
            return;
        }

        double increment = sprinting ? 1.5 : 1.0;
        buffer += increment;

        // Require buffer >= 3.5:
        // Gives proper grace period for legitimate players who entered GUI with momentum,
        // while firmly catching cheaters who actively walk or sprint through inventory screens.
        if (buffer >= 3.5) {
            buffer = 2.0;
            if (flag(V.write(verbose()).bool(sprinting).bool(sneaking).bool(input)) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
                InventoryResync.resync(player);
            }
        }
    }
}
