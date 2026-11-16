package tokyo.ghoul.api.utils.combat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.client.features.modules.combat.PacketCriticalsModule;

public final class CriticalHitHandler implements QuickImports {
    private static final long STARTUP_HOLD_MS = 450L;
    private long activationTime;
    private int lastObservedTick = Integer.MIN_VALUE;
    private float simulatedFallDistance;
    private int descendingTicks;

    public void onEnable() {
        activationTime = System.currentTimeMillis();
        lastObservedTick = Integer.MIN_VALUE;
        simulatedFallDistance = 0.0F;
        descendingTicks = 0;
    }

    public void onDisable() {
        activationTime = 0L;
        lastObservedTick = Integer.MIN_VALUE;
        simulatedFallDistance = 0.0F;
        descendingTicks = 0;
    }

    public boolean canAttack(int predictionTicks, boolean smartCrits) {
        if (mc.player == null || mc.level == null || mc.options == null) return false;
        if (mc.player.isFallFlying() || mc.player.getAbilities().flying) return true;
        if (PacketCriticalsModule.getInstance().canBypassCriticalRequirement()) {
            return true;
        }

        SimulatedState state = simulate(Math.max(0, predictionTicks));
        if (hasMovementRestrictions(state)) return true;

        boolean jumpIntent = mc.options.keyJump.isDown() || !state.onGround && state.velocityY > 0.08D;
        if (shouldHoldOnStartup(state, smartCrits, jumpIntent)) return false;
        if (hasDebuffThatBreaksCrit()) return true;

        if (smartCrits && !mc.options.keyJump.isDown() && mc.player.tickCount > 6) return true;

        if (predictionTicks <= 0) return isStrictCriticalWindow(state);
        if (isInsideTightSpace(state)) return isTightSpaceCriticalWindow(state);
        return isCriticalWindow(state);
    }

    private SimulatedState simulate(int ticks) {
        LocalPlayer player = mc.player;
        boolean onGround = player.onGround();
        float fallDistance = observedFallDistance(player);
        double velocityY = player.getDeltaMovement().y;
        double gravity = Math.max(0.0D, player.getAttributeValue(Attributes.GRAVITY));
        AABB box = player.getBoundingBox();

        for (int i = 0; i < ticks; i++) {
            if (onGround && mc.options.keyJump.isDown()) {
                onGround = false;
                velocityY = 0.42D;
            } else if (!onGround) {
                velocityY = (velocityY - gravity) * 0.98D;
                if (velocityY < 0.0D) fallDistance += (float) -velocityY;
            }
            box = box.move(0.0D, velocityY, 0.0D);
        }

        return new SimulatedState(onGround, fallDistance, velocityY, box, player.horizontalCollision);
    }

    private float observedFallDistance(LocalPlayer player) {
        if (lastObservedTick != player.tickCount) {
            lastObservedTick = player.tickCount;
            double velocityY = player.getDeltaMovement().y;
            if (player.onGround() || velocityY >= 0.0D) {
                simulatedFallDistance = 0.0F;
                descendingTicks = 0;
            } else {
                simulatedFallDistance += (float) -velocityY;
                descendingTicks++;
            }
        }
        return Math.max((float) player.fallDistance, simulatedFallDistance);
    }

    public float localFallDistance() {
        return simulatedFallDistance;
    }

    private boolean hasMovementRestrictions(SimulatedState state) {
        return mc.player.hasEffect(MobEffects.BLINDNESS)
                || mc.player.hasEffect(MobEffects.LEVITATION)
                || isInsideCobweb(state.box)
                || mc.player.isEyeInFluid(FluidTags.WATER)
                || mc.player.isInLava()
                || mc.player.onClimbable()
                || mc.player.getAbilities().flying;
    }

    private boolean hasCriticalRestrictions() {
        return mc.player.hasEffect(MobEffects.BLINDNESS)
                || mc.player.hasEffect(MobEffects.LEVITATION)
                || mc.player.isEyeInFluid(FluidTags.WATER)
                || mc.player.isInLava()
                || mc.player.onClimbable()
                || mc.player.isSwimming()
                || mc.player.isFallFlying()
                || mc.player.getAbilities().flying;
    }

    private boolean hasDebuffThatBreaksCrit() {
        return mc.player.hasEffect(MobEffects.BLINDNESS) || mc.player.hasEffect(MobEffects.LEVITATION);
    }

    private boolean shouldHoldOnStartup(SimulatedState state, boolean smartCrits, boolean jumpIntent) {
        if (activationTime <= 0L || System.currentTimeMillis() - activationTime > STARTUP_HOLD_MS) return false;
        if (smartCrits && !jumpIntent) return false;
        return !state.onGround && state.fallDistance <= 0.0F && state.velocityY > -0.03D;
    }

    private boolean isCriticalWindow(SimulatedState state) {
        if (state.onGround || hasCriticalRestrictions()) return false;
        if (isPvpTradeWindow(state)) return true;
        return state.fallDistance > requiredFallDistance(state, false)
                && state.velocityY < requiredVelocity(state, false);
    }

    private boolean isStrictCriticalWindow(SimulatedState state) {
        return !mc.player.onGround()
                && !state.onGround
                && !hasCriticalRestrictions()
                && state.fallDistance > 0.0F
                && descendingTicks >= 2
                && mc.player.getDeltaMovement().y < 0.0D;
    }

    private boolean isTightSpaceCriticalWindow(SimulatedState state) {
        if (isPvpTradeWindow(state)) return true;
        float fall = requiredFallDistance(state, true);
        double velocity = requiredVelocity(state, true);
        if (state.horizontalCollision || hasHorizontalPressure(state)) {
            fall = Math.min(fall, 0.004F);
            velocity = Math.max(velocity, -0.010D);
        }
        return !state.onGround && state.fallDistance > fall && state.velocityY < velocity;
    }

    private boolean isInsideTightSpace(SimulatedState state) {
        return state.horizontalCollision || hasHorizontalPressure(state) || countBlockingSides(state) >= 2;
    }

    private boolean isPvpTradeWindow(SimulatedState state) {
        return (mc.player.hurtTime > 0 || mc.player.hasEffect(MobEffects.SLOWNESS))
                && !state.onGround && state.fallDistance > 0.0F && state.velocityY < -0.01D;
    }

    private float requiredFallDistance(SimulatedState state, boolean tightSpace) {
        float required = tightSpace ? 0.01F : 0.03F;
        if (mc.player.hurtTime > 0 || mc.player.hasEffect(MobEffects.SLOWNESS)) {
            required = Math.min(required, tightSpace ? 0.008F : 0.012F);
        }
        if (state.horizontalCollision) required = Math.min(required, 0.012F);
        return required;
    }

    private double requiredVelocity(SimulatedState state, boolean tightSpace) {
        double required = tightSpace ? -0.02D : -0.03D;
        if (mc.player.hurtTime > 0 || mc.player.hasEffect(MobEffects.SLOWNESS)) {
            required = Math.max(required, tightSpace ? -0.012D : -0.018D);
        }
        if (state.horizontalCollision) required = Math.max(required, -0.015D);
        return required;
    }

    private boolean hasHorizontalPressure(SimulatedState state) {
        return !mc.level.noCollision(mc.player, state.box.inflate(0.22D, 0.0D, 0.22D).deflate(1.0E-7D));
    }

    private int countBlockingSides(SimulatedState state) {
        Vec3 center = state.box.getCenter();
        double footY = state.box.minY + 0.10D;
        double bodyY = Math.min(state.box.maxY - 0.10D, state.box.minY + 0.95D);
        int count = 0;
        count += isBlocked(center.x + 0.72D, center.z, footY, bodyY) ? 1 : 0;
        count += isBlocked(center.x - 0.72D, center.z, footY, bodyY) ? 1 : 0;
        count += isBlocked(center.x, center.z + 0.72D, footY, bodyY) ? 1 : 0;
        count += isBlocked(center.x, center.z - 0.72D, footY, bodyY) ? 1 : 0;
        return count;
    }

    private boolean isBlocked(double x, double z, double footY, double bodyY) {
        return isSolid(BlockPos.containing(x, footY, z)) || isSolid(BlockPos.containing(x, bodyY, z));
    }

    private boolean isSolid(BlockPos pos) {
        return !mc.level.getBlockState(pos).isAir()
                && !mc.level.getBlockState(pos).getCollisionShape(mc.level, pos).isEmpty();
    }

    private boolean isInsideCobweb(AABB box) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = (int) Math.floor(box.minX); x <= (int) Math.floor(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y <= (int) Math.floor(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z <= (int) Math.floor(box.maxZ); z++) {
                    pos.set(x, y, z);
                    if (mc.level.getBlockState(pos).is(Blocks.COBWEB)) return true;
                }
            }
        }
        return false;
    }

    private record SimulatedState(boolean onGround, float fallDistance, double velocityY, AABB box,
                                  boolean horizontalCollision) {
    }
}
