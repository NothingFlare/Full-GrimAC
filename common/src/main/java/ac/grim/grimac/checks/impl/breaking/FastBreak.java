package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.verbose.VerboseCodecs;
import ac.grim.grimac.checks.type.BlockBreakListener;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsutil.BlockBreakSpeed;
import ac.grim.grimac.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import java.util.Set;

// Based loosely off of Hawk BlockBreakSpeedSurvival
// Also based loosely off of NoCheatPlus FastBreak
// Also based off minecraft wiki: https://minecraft.wiki/w/Breaking#Instant_breaking
@CheckData(name = "FastBreak", stableKey = "grim.breaking.fast_break", description = "Breaking blocks too quickly")
public class FastBreak extends Check implements BlockBreakListener, PreViaPacketReceiveListener {
    private static final Verbose V =
            Verbose.of("[delay={ulong}ms|diff={f64:%.1f}ms, balance={f64:%.1f}ms], type={block}");

    private static final Set<StateType> EXEMPT_STATES = Set.of();
    private final boolean clientOlderThanServer = PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion() > player.getClientVersion().getProtocolVersion();

    public FastBreak(GrimPlayer player) {
        super(player);
    }

    // The block the player is currently breaking
    Vector3i targetBlockPosition = null;
    // The maximum amount of damage the player deals to the block
    double maximumBlockDamage = 0;
    // The last time a finish digging packet was sent, to enforce delay after non-instabreak
    long lastFinishBreak = 0;
    // The time the player started to break the block
    long startBreak = 0;

    // The balance buffers
    double blockBreakBalance = 0;
    double blockDelayBalance = 0;

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.START_DIGGING) {
            if (!ViaVersionUtil.isAvailable) {
                final WrappedBlockState defaultState = WrappedBlockState.getDefaultState(player.getClientVersion(), blockBreak.block.getType());
                if (defaultState.getType() == StateTypes.AIR || EXEMPT_STATES.contains(defaultState.getType())) {
                    return;
                }
            }
            WrappedBlockState block = clientOlderThanServer ? WrappedBlockState.getByGlobalId(player.getClientVersion(), player.getViaTranslatedClientBlockID(blockBreak.block.getGlobalId())) : blockBreak.block;

            startBreak = System.currentTimeMillis();
            targetBlockPosition = blockBreak.position;
            maximumBlockDamage = BlockBreakSpeed.getBlockDamage(player, block);

            // In vanilla survival, breaking a non-instabreak block enforces a 5-tick (250ms) delay before starting the next block.
            // Instant-break blocks (damage >= 1.0) have no delay.
            if (maximumBlockDamage < 1.0 && lastFinishBreak > 0) {
                double breakDelay = (double) (System.currentTimeMillis() - lastFinishBreak);

                // Allow for network jitter: vanilla client waits 250ms, allow >= 175ms without penalty
                if (breakDelay >= 175.0) {
                    blockDelayBalance = Math.max(0.0, blockDelayBalance * 0.8 - 50.0);
                } else {
                    blockDelayBalance += (250.0 - breakDelay);
                }

                double maxDelayAdvantage = Math.max(800.0, player.getTransactionPing() * 2.5);
                if (blockDelayBalance > maxDelayAdvantage) {
                    int type = VerboseCodecs.block(blockBreak.block.getType(), player.getClientVersion());
                    if (flag(V.write(verbose()).bool(true).ulong((long) breakDelay).f64(0).f64(blockDelayBalance).sint(type)) && shouldModifyPackets()) {
                        blockBreak.cancel();
                    }
                }
            } else {
                blockDelayBalance = Math.max(0.0, blockDelayBalance * 0.5);
            }

            clampBalance();
        }

        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            targetBlockPosition = null;
            maximumBlockDamage = 0;
        }

        if (blockBreak.action == DiggingAction.FINISHED_DIGGING && targetBlockPosition != null) {
            // In vanilla Minecraft, the client sends START_DIGGING at tick 0 and FINISHED_DIGGING at tick (ceil(1.0/damage) - 1).
            // So the minimum elapsed time between START and FINISH packets is (ceil(1.0/damage) - 1) * 50ms.
            double minAllowedTime = Math.max(0.0, (Math.ceil(1.0 / maximumBlockDamage) - 1.0) * 50.0);
            double realTime = (double) (System.currentTimeMillis() - startBreak);
            double diff = minAllowedTime - realTime;

            clampBalance();

            // 1. Blatant Instant Break Exploit on multi-tick blocks (Nuker, FastBreak 100%):
            // If the block requires at least 4 ticks (minAllowedTime >= 150ms) and was broken in tick 0 (< 35ms)
            if (maximumBlockDamage < 1.0 && minAllowedTime >= 150.0 && realTime < 35.0) {
                int type = VerboseCodecs.block(blockBreak.block.getType(), player.getClientVersion());
                if (flag(V.write(verbose()).bool(false).ulong(0).f64(diff).f64(diff).sint(type)) && shouldModifyPackets()) {
                    blockBreak.cancel();
                    lastFinishBreak = startBreak = System.currentTimeMillis();
                    targetBlockPosition = null;
                    maximumBlockDamage = 0;
                    return;
                }
            }

            // 2. Sustained FastBreak balance buffer:
            if (diff <= 25.0) {
                // Legitimate mining: rapidly decay the advantage balance
                blockBreakBalance = Math.max(0.0, blockBreakBalance * 0.85 - 25.0);
            } else {
                // Cheating or faster than allowed: accumulate advantage
                blockBreakBalance += diff;
            }

            double maxBreakAdvantage = Math.max(750.0, player.getTransactionPing() * 2.0);
            if (blockBreakBalance > maxBreakAdvantage) {
                int type = VerboseCodecs.block(blockBreak.block.getType(), player.getClientVersion());
                if (flag(V.write(verbose()).bool(false).ulong(0).f64(diff).f64(blockBreakBalance).sint(type)) && shouldModifyPackets()) {
                    blockBreak.cancel();
                }
            }

            lastFinishBreak = startBreak = System.currentTimeMillis();
            targetBlockPosition = null;
            maximumBlockDamage = 0;
        }
    }

    @Override
    public void onPreViaPacketReceive(PacketReceiveEvent event) {
        boolean flying = WrapperPlayClientPlayerFlying.isFlying(event.getPacketType());
        if ((flying || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) && isAnimation(event.getPacketType())) && targetBlockPosition != null) {
            maximumBlockDamage = Math.max(maximumBlockDamage, BlockBreakSpeed.getBlockDamage(player, player.compensatedWorld.getBlock(targetBlockPosition)));
        }
    }

    private void clampBalance() {
        double balance = Math.max(1000.0, player.getTransactionPing() * 2.5);
        blockBreakBalance = GrimMath.clamp(blockBreakBalance, -balance, balance);
        blockDelayBalance = GrimMath.clamp(blockDelayBalance, -balance, balance);
    }
}
