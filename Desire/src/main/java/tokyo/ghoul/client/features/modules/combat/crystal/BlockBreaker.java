package tokyo.ghoul.client.features.modules.combat.crystal;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector2f;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.system.interfaces.QuickImports;

public final class BlockBreaker implements QuickImports {
    @Getter private static final BlockBreaker instance = new BlockBreaker();

    private Module owner;
    @Getter private BlockPos pos;
    @Getter private float progress;
    @Getter private State state = State.IDLE;
    private int lastRequestTick = -100;
    private int lastProgressTick = -100;
    private boolean aimedThisTick;

    private int clientTicks;
    private int lastActionPacketTick = -1;

    private BlockBreaker() {}


    public void onClientTick() {
        clientTicks++;
    }

    public void onPacketSend(Object packet) {
        if (packet instanceof ServerboundPlayerActionPacket action) {
            switch (action.getAction()) {
                case START_DESTROY_BLOCK, ABORT_DESTROY_BLOCK, STOP_DESTROY_BLOCK -> lastActionPacketTick = clientTicks;
                default -> {}
            }
        }
    }

    public boolean sentDestroyPacketThisTick() {
        return lastActionPacketTick == clientTicks;
    }

    public void updateState() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            reset();
            return;
        }
        if (pos == null) return;

        if (mc.options.keyAttack.isDown() || ticksSince(lastRequestTick) > 1) {
            reset();
            return;
        }
        if (!isMinable(mc.level.getBlockState(pos))) {
            state = State.BROKEN;
            return;
        }

        syncProgress();
        boolean stuck = ticksSince(lastProgressTick) > 20;
        state = stuck ? State.STUCK : aimedThisTick ? State.MINING : State.AIMING;
        aimedThisTick = false;
    }


    public boolean mine(Module requester, BlockPos target) {
        if (mc.player == null || mc.level == null || target == null) return false;
        if (owner != null && owner != requester) return false;
        if (mc.options.keyAttack.isDown()) return false;

        if (!target.equals(pos)) {
            pos = target.immutable();
            progress = 0.0F;
            lastProgressTick = mc.player.tickCount;
            state = State.IDLE;
        }

        owner = requester;
        lastRequestTick = mc.player.tickCount;
        step();
        return true;
    }

    public void keepAlive(Module requester) {
        if (owner == requester && pos != null && mc.player != null) {
            lastRequestTick = mc.player.tickCount;
            step();
        }
    }

    public void release(Module requester) {
        if (owner == null || owner == requester) reset();
    }

    public boolean isOwnedBy(Module requester) {
        return owner == requester && pos != null;
    }

    public boolean isMining() {
        return pos != null && state == State.MINING;
    }

    public boolean willFinish() {
        if (!isMining() || mc.player == null || mc.level == null) return false;
        return progress + destroyDelta(pos) >= 1.0F;
    }


    private void step() {
        boolean finishing = willFinish() || isMining() && !isVanillaMining(pos);
        int priority = finishing ? CrystalRotations.FINISH_MINING : CrystalRotations.ACTION;
        Vec3 aim = MatrixPlacement.isActive() ? MatrixPlacement.jitteredCenter(pos) : Vec3.atCenterOf(pos);
        Vector2f rotation = CrystalUtil.rotationTo(mc.player.getEyePosition(), aim);
        if (MatrixPlacement.isActive()) rotation = MatrixPlacement.snapToGcd(rotation);
        if (!CrystalRotations.request(owner, rotation, priority)) return;

        BlockHitResult hit = aimedHit();
        if (hit == null || mc.gameMode == null) return;
        aimedThisTick = true;

        if (mc.level.getBlockState(pos).isAir()) return;
        boolean progressed = isVanillaMining(pos)
                ? mc.gameMode.continueDestroyBlock(pos, hit.getDirection())
                : mc.gameMode.startDestroyBlock(pos, hit.getDirection());
        if (progressed) {
            mc.level.addBreakingBlockEffect(pos, hit.getDirection());
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private BlockHitResult aimedHit() {
        Vec3 eye = mc.player.getEyePosition();
        double reach = mc.player.blockInteractionRange();
        Vec3 end = eye.add(CrystalUtil.direction(CrystalRotations.aimYaw(), CrystalRotations.aimPitch()).scale(reach));
        VoxelShape shape = mc.level.getBlockState(pos).getShape(mc.level, pos);
        if (shape.isEmpty()) return null;
        return shape.clip(eye, end, pos);
    }

    private boolean isVanillaMining(BlockPos target) {
        return mc.gameMode.isDestroying && target.equals(mc.gameMode.destroyBlockPos);
    }

    private void syncProgress() {
        float current = pos.equals(mc.gameMode.destroyBlockPos) ? Mth.clamp(mc.gameMode.destroyProgress, 0.0F, 1.0F) : 0.0F;
        if (current > progress + 1.0E-6F) lastProgressTick = mc.player.tickCount;
        progress = current;
    }

    private float destroyDelta(BlockPos target) {
        return mc.level.getBlockState(target).getDestroyProgress(mc.player, mc.level, target);
    }

    private static boolean isMinable(BlockState blockState) {
        return !blockState.isAir() && !(blockState.getBlock() instanceof LiquidBlock);
    }

    private int ticksSince(int tick) {
        int delta = mc.player.tickCount - tick;
        return delta < 0 ? Integer.MAX_VALUE : delta;
    }

    private void reset() {
        owner = null;
        pos = null;
        lastRequestTick = -100;
        lastProgressTick = -100;
        progress = 0.0F;
        state = State.IDLE;
        aimedThisTick = false;
    }

    public enum State {IDLE, AIMING, MINING, BROKEN, STUCK}
}
