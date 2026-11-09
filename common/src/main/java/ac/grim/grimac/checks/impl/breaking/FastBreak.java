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

            startBreak = System.currentTimeMillis() - (targetBlockPosition == null ? 50 : 0);
            targetBlockPosition = blockBreak.position;

            maximumBlockDamage = BlockBreakSpeed.getBlockDamage(player, block);

            double breakDelay = System.currentTimeMillis() - lastFinishBreak;

            // In vanilla, breaking a non-instabreak block requires 5 ticks (250-300ms) delay before starting the next block
            if (breakDelay >= 225) {
                blockDelayBalance = Math.max(0.0, blockDelayBalance * 0.85);
            } else {
                blockDelayBalance += (250 - breakDelay);
            }

            double maxDelayAdvantage = Math.max(250.0, player.getTransactionPing());
            if (blockDelayBalance > maxDelayAdvantage) {
                int type = VerboseCodecs.block(blockBreak.block.getType(), player.getClientVersion());
                if (flag(V.write(verbose()).bool(true).ulong((long) breakDelay).f64(0).f64(blockDelayBalance).sint(type)) && shouldModifyPackets()) {
                    blockBreak.cancel();
                    player.resyncPosition(blockBreak.position);
                }
            }

            clampBalance();
        }

        if (blockBreak.action == DiggingAction.FINISHED_DIGGING && targetBlockPosition != null) {
            double predictedTime = Math.ceil(1.0 / maximumBlockDamage) * 50.0;
            double realTime = System.currentTimeMillis() - startBreak;
            double diff = predictedTime - realTime;

            clampBalance();

            // 1. Instant Break Exploit on non-instabreak blocks:
            // If the block requires multiple ticks (maximumBlockDamage < 1.0) and was broken in less than half the vanilla required time or < 75ms
            if (maximumBlockDamage < 1.0 && predictedTime >= 100.0 && (realTime < 75.0 || realTime < predictedTime * 0.45)) {
                int type = VerboseCodecs.block(blockBreak.block.getType(), player.getClientVersion());
                if (flag(V.write(verbose()).bool(false).ulong(0).f64(diff).f64(diff).sint(type)) && shouldModifyPackets()) {
                    blockBreak.cancel();
                    player.resyncPosition(blockBreak.position);
                    lastFinishBreak = startBreak = System.currentTimeMillis();
                    return;
                }
            }

            // 2. Sustained FastBreak balance check (tightened threshold from 1000ms down to 250ms)
            if (diff < 20) {
                blockBreakBalance = Math.max(0.0, blockBreakBalance * 0.85);
            } else {
                blockBreakBalance += diff;
            }

            double maxBreakAdvantage = Math.max(250.0, player.getTransactionPing());
            if (blockBreakBalance > maxBreakAdvantage) {
                int type = VerboseCodecs.block(blockBreak.block.getType(), player.getClientVersion());
                if (flag(V.write(verbose()).bool(false).ulong(0).f64(diff).f64(blockBreakBalance).sint(type)) && shouldModifyPackets()) {
                    blockBreak.cancel();
                    player.resyncPosition(blockBreak.position);
                }
            }

            lastFinishBreak = startBreak = System.currentTimeMillis();
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
        double balance = Math.max(300.0, player.getTransactionPing() * 1.5);
        blockBreakBalance = GrimMath.clamp(blockBreakBalance, -balance, balance);
        blockDelayBalance = GrimMath.clamp(blockDelayBalance, -balance, balance);
    }
}
