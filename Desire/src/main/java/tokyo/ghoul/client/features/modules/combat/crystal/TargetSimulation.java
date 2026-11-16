package tokyo.ghoul.client.features.modules.combat.crystal;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import tokyo.ghoul.api.system.interfaces.QuickImports;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class TargetSimulation implements QuickImports {
    public final Player player;
    public final GuessedInput input;
    public Vec3 pos;
    public Vec3 velocity;
    public AABB box;
    private EntityDimensions dimensions;
    private float yaw, pitch;
    private boolean sprinting;
    private float fallDistance;
    private int jumpDelay;
    private boolean jumping;
    private boolean fallFlying;
    public boolean onGround;
    public boolean horizontalCollision;
    private boolean verticalCollision;
    private boolean inWater;
    private boolean swimming;
    private boolean eyeInWater;
    private final Object2DoubleMap<TagKey<Fluid>> fluidHeights;
    private final HashSet<TagKey<Fluid>> eyeFluids;
    private int ticks;

    private TargetSimulation(Player player, GuessedInput input, Vec3 pos, Vec3 velocity, AABB box, float yaw, float pitch,
                             boolean sprinting, float fallDistance, int jumpDelay, boolean jumping, boolean fallFlying,
                             boolean onGround, boolean horizontalCollision, boolean verticalCollision, boolean inWater,
                             boolean swimming, boolean eyeInWater, Object2DoubleMap<TagKey<Fluid>> fluidHeights,
                             HashSet<TagKey<Fluid>> eyeFluids) {
        this.player = player;
        this.input = input;
        this.pos = pos;
        this.velocity = velocity;
        this.box = box;
        this.yaw = yaw;
        this.pitch = pitch;
        this.sprinting = sprinting;
        this.fallDistance = fallDistance;
        this.jumpDelay = jumpDelay;
        this.jumping = jumping;
        this.fallFlying = fallFlying;
        this.onGround = onGround;
        this.horizontalCollision = horizontalCollision;
        this.verticalCollision = verticalCollision;
        this.inWater = inWater;
        this.swimming = swimming;
        this.eyeInWater = eyeInWater;
        this.fluidHeights = fluidHeights;
        this.eyeFluids = eyeFluids;
        this.dimensions = player.getDimensions(player.getPose());
    }

    public static TargetSimulation of(Player player, GuessedInput input) {
        return new TargetSimulation(player, input,
                player.position(),
                player.position().subtract(player.xo, player.yo, player.zo),
                player.getBoundingBox(),
                player.getYRot(), player.getXRot(),
                player.isSprinting(),
                (float) player.fallDistance,
                player.noJumpDelay,
                player.jumping,
                player.isFallFlying(),
                player.onGround(),
                player.horizontalCollision,
                player.verticalCollision,
                player.isInWater(),
                player.isSwimming(),
                player.isUnderWater(),
                fluidHeights(player),
                eyeFluids(player));
    }

    public static TargetSimulation predict(Player player, int ticks) {
        TargetSimulation simulation = of(player, GuessedInput.guess(player));
        for (int i = 0; i < ticks; i++) simulation.tick();
        return simulation;
    }

    public TargetSimulation copy() {
        TargetSimulation copy = new TargetSimulation(player, input, pos, velocity, box, yaw, pitch, sprinting, fallDistance,
                jumpDelay, jumping, fallFlying, onGround, horizontalCollision, verticalCollision, inWater, swimming,
                eyeInWater, new Object2DoubleArrayMap<>(fluidHeights), new HashSet<>(eyeFluids));
        copy.dimensions = dimensions;
        copy.ticks = ticks;
        return copy;
    }


    public void tick() {
        ticks++;
        if (pos.y <= -70.0) return;

        input.update();
        updateWaterState();
        updateEyeFluids();
        updateSprinting();
        updateSwimming();
        if (jumpDelay > 0) jumpDelay--;

        jumping = input.keys.jump();
        double vx = Math.abs(velocity.x) < 0.003 ? 0.0 : velocity.x;
        double vy = Math.abs(velocity.y) < 0.003 ? 0.0 : velocity.y;
        double vz = Math.abs(velocity.z) < 0.003 ? 0.0 : velocity.z;
        if (onGround) fallFlying = false;
        velocity = new Vec3(vx, vy, vz);

        if (jumping) {
            double fluidHeight = inLava() ? fluidHeight(FluidTags.LAVA) : fluidHeight(FluidTags.WATER);
            boolean inFluid = isTouchingWater() && fluidHeight > 0.0;
            double swimThreshold = fluidJumpThreshold();
            if (inFluid && (!onGround || fluidHeight > swimThreshold)) {
                swimUp();
            } else if (inLava() && (!onGround || fluidHeight > swimThreshold)) {
                swimUp();
            } else if ((onGround || inFluid && fluidHeight <= swimThreshold) && jumpDelay == 0) {
                jump();
                jumpDelay = 10;
            }
        }

        float strafe = input.strafe * 0.98F;
        float forward = input.forward * 0.98F;
        if (hasEffect(MobEffects.SLOW_FALLING) || hasEffect(MobEffects.LEVITATION)) resetFall();
        travel(new Vec3(strafe, 0.0F, forward));
    }

    private void travel(Vec3 movement) {
        if (swimming && !player.isPassenger()) {
            double lookY = lookVector().y;
            double factor = lookY < -0.2 ? 0.085 : 0.06;
            BlockPos above = new BlockPos(Mth.floor(pos.x), Mth.floor(pos.y + 1.0 - 0.1), Mth.floor(pos.z));
            if (lookY <= 0.0 || input.keys.jump() || !player.level().getBlockState(above).getFluidState().isEmpty()) {
                velocity = velocity.add(0.0, (lookY - velocity.y) * factor, 0.0);
            }
        }

        double startVelocityY = velocity.y;
        double gravity = 0.08;
        boolean falling = velocity.y <= 0.0;
        if (velocity.y <= 0.0 && hasEffect(MobEffects.SLOW_FALLING)) {
            gravity = 0.01;
            resetFall();
        }

        if (isTouchingWater() && player.isAffectedByFluids()) {
            double startY = pos.y;
            float drag = sprinting ? 0.9F : 0.8F;
            float speed = 0.02F;
            float efficiency = (float) attribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
            if (!onGround) efficiency *= 0.5F;
            if (efficiency > 0.0F) {
                drag += (0.54600006F - drag) * efficiency / 3.0F;
                speed += (movementSpeed() - speed) * efficiency / 3.0F;
            }
            if (hasEffect(MobEffects.DOLPHINS_GRACE)) drag = 0.96F;

            accelerate(speed, movement);
            move(velocity);
            Vec3 moved = velocity;
            if (horizontalCollision && onClimbable()) moved = new Vec3(moved.x, 0.2, moved.z);

            velocity = moved.multiply(drag, 0.8, drag);
            Vec3 adjusted = player.getFluidFallingAdjustedMovement(gravity, falling, velocity);
            velocity = adjusted;
            if (horizontalCollision && isFree(adjusted.x, adjusted.y + 0.6 - pos.y + startY, adjusted.z)) {
                velocity = new Vec3(adjusted.x, 0.3, adjusted.z);
            }
        } else if (inLava() && player.isAffectedByFluids()) {
            double startY = pos.y;
            accelerate(0.02F, movement);
            move(velocity);
            if (fluidHeight(FluidTags.LAVA) <= fluidJumpThreshold()) {
                velocity = velocity.multiply(0.5, 0.8, 0.5);
                velocity = player.getFluidFallingAdjustedMovement(gravity, falling, velocity);
            } else {
                velocity = velocity.scale(0.5);
            }
            if (!player.isNoGravity()) velocity = velocity.add(0.0, -gravity / 4.0, 0.0);
            if (horizontalCollision && isFree(velocity.x, velocity.y + 0.6 - pos.y + startY, velocity.z)) {
                velocity = new Vec3(velocity.x, 0.3, velocity.z);
            }
        } else if (fallFlying) {
            Vec3 look = lookVector();
            float pitchRad = pitch * Mth.DEG_TO_RAD;
            double lookHorizontal = Math.sqrt(look.x * look.x + look.z * look.z);
            double speedHorizontal = velocity.horizontalDistance();
            double lookLength = look.length();
            float lift = Mth.cos(pitchRad);
            lift = (float) (lift * (lift * Math.min(1.0, lookLength / 0.4)));

            Vec3 next = velocity.add(0.0, gravity * (-1.0 + lift * 0.75), 0.0);
            if (next.y < 0.0 && lookHorizontal > 0.0) {
                double convert = next.y * -0.1 * lift;
                next = next.add(look.x * convert / lookHorizontal, convert, look.z * convert / lookHorizontal);
            }
            if (pitchRad < 0.0F && lookHorizontal > 0.0) {
                double climb = speedHorizontal * -Mth.sin(pitchRad) * 0.04;
                next = next.add(-look.x * climb / lookHorizontal, climb * 3.2, -look.z * climb / lookHorizontal);
            }
            if (lookHorizontal > 0.0) {
                next = next.add((look.x / lookHorizontal * speedHorizontal - next.x) * 0.1, 0.0,
                        (look.z / lookHorizontal * speedHorizontal - next.z) * 0.1);
            }
            velocity = next.multiply(0.99, 0.98, 0.99);
            move(velocity);
        } else {
            BlockPos below = blockBelow();
            float slipperiness = player.level().getBlockState(below).getBlock().getFriction();
            float friction = onGround ? slipperiness * 0.91F : 0.91F;
            Vec3 moved = applyMovementInput(movement, slipperiness);
            double y = moved.y;

            if (hasEffect(MobEffects.LEVITATION)) {
                MobEffectInstance levitation = effect(MobEffects.LEVITATION);
                if (levitation != null) y += (0.05 * (levitation.getAmplifier() + 1) - moved.y) * 0.2;
            } else if (player.level().isClientSide() && !player.level().isLoaded(below)) {
                y = pos.y > player.level().getMaxY() ? -0.1 : 0.0;
            } else if (!player.isNoGravity()) {
                y -= gravity;
            }

            velocity = player.shouldDiscardFriction()
                    ? new Vec3(moved.x, y, moved.z)
                    : new Vec3(moved.x * friction, y * 0.98F, moved.z * friction);
        }

        if (player.getAbilities().flying && !player.isPassenger()) {
            velocity = new Vec3(velocity.x, startVelocityY * 0.6, velocity.z);
            resetFall();
        }
    }

    private Vec3 applyMovementInput(Vec3 movement, float slipperiness) {
        accelerate(movementSpeed(slipperiness), movement);
        velocity = applyClimbing(velocity);
        move(velocity);

        Vec3 result = velocity;
        BlockState state = blockState(blockPos(pos));
        if ((horizontalCollision || jumping) && (onClimbable()
                || state != null && state.is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(player))) {
            result = new Vec3(result.x, 0.2, result.z);
        }
        return result;
    }

    private void move(Vec3 movement) {
        Vec3 adjusted = maybeBackOffFromEdge(movement);
        Vec3 collided = collide(adjusted);
        if (collided.lengthSqr() > 1.0E-7) {
            pos = pos.add(collided);
            box = dimensions.makeBoundingBox(pos);
        }

        boolean blockedX = !Mth.equal(movement.x, collided.x);
        boolean blockedZ = !Mth.equal(movement.z, collided.z);
        horizontalCollision = blockedX || blockedZ;
        verticalCollision = movement.y != collided.y;
        onGround = verticalCollision && movement.y < 0.0;

        if (!isTouchingWater()) updateWaterState();

        if (onGround) {
            resetFall();
        } else if (movement.y < 0.0) {
            fallDistance -= (float) movement.y;
        }

        if (horizontalCollision || verticalCollision) {
            Vec3 current = velocity;
            velocity = new Vec3(blockedX ? 0.0 : current.x, verticalCollision ? 0.0 : current.y, blockedZ ? 0.0 : current.z);
        }
    }

    private Vec3 collide(Vec3 movement) {
        AABB current = dimensions.makeBoundingBox(pos);
        List<VoxelShape> extra = Collections.emptyList();
        Vec3 collided = movement.lengthSqr() == 0.0 ? movement
                : Entity.collideBoundingBox(player, movement, current, player.level(), extra);

        boolean blockedX = movement.x != collided.x;
        boolean blockedY = movement.y != collided.y;
        boolean blockedZ = movement.z != collided.z;
        boolean grounded = onGround || blockedY && movement.y < 0.0;

        float step = player.maxUpStep();
        if (step > 0.0F && grounded && (blockedX || blockedZ)) {
            Vec3 stepped = Entity.collideBoundingBox(player, new Vec3(movement.x, step, movement.z), current, player.level(), extra);
            Vec3 lift = Entity.collideBoundingBox(player, new Vec3(0.0, step, 0.0), current.expandTowards(movement.x, 0.0, movement.z), player.level(), extra);
            Vec3 liftedMove = Entity.collideBoundingBox(player, new Vec3(movement.x, 0.0, movement.z), current.move(lift), player.level(), extra).add(lift);
            if (lift.y < step && liftedMove.horizontalDistanceSqr() > stepped.horizontalDistanceSqr()) {
                stepped = liftedMove;
            }
            if (stepped.horizontalDistanceSqr() > collided.horizontalDistanceSqr()) {
                return stepped.add(Entity.collideBoundingBox(player, new Vec3(0.0, -stepped.y + movement.y, 0.0), current.move(stepped), player.level(), extra));
            }
        }
        return collided;
    }

    private Vec3 maybeBackOffFromEdge(Vec3 movement) {
        if (movement.y > 0.0 || !isAboveGround()) return movement;

        double x = movement.x;
        double z = movement.z;
        double step = 0.05;

        while (x != 0.0 && player.level().noCollision(player, box.move(x, -0.5, 0.0))) {
            if (x < step && x >= -step) {
                x = 0.0;
                break;
            }
            x += x > 0.0 ? -step : step;
        }
        while (z != 0.0 && player.level().noCollision(player, box.move(0.0, -0.5, z))) {
            if (z < step && z >= -step) {
                z = 0.0;
                break;
            }
            z += z > 0.0 ? -step : step;
        }
        while (x != 0.0 && z != 0.0 && player.level().noCollision(player, box.move(x, -0.5, z))) {
            x = x < step && x >= -step ? 0.0 : (x > 0.0 ? x - step : x + step);
            if (z < step && z >= -step) {
                z = 0.0;
                break;
            }
            z += z > 0.0 ? -step : step;
        }

        return isSneaking() ? new Vec3(x, movement.y, z) : movement;
    }

    private boolean isAboveGround() {
        return onGround || fallDistance < 0.5F && !player.level().noCollision(player, box.move(0.0, fallDistance - 0.5, 0.0));
    }

    private Vec3 applyClimbing(Vec3 motion) {
        if (!onClimbable()) return motion;
        resetFall();
        double x = Mth.clamp(motion.x, -0.15F, 0.15F);
        double z = Mth.clamp(motion.z, -0.15F, 0.15F);
        double y = Math.max(motion.y, -0.15F);
        if (y < 0.0 && !blockState(blockPos(pos)).is(Blocks.SCAFFOLDING) && player.isSuppressingSlidingDownLadder()) {
            y = 0.0;
        }
        return new Vec3(x, y, z);
    }

    private void accelerate(float speed, Vec3 movement) {
        velocity = velocity.add(inputToVelocity(movement, speed, yaw));
    }

    private static Vec3 inputToVelocity(Vec3 movement, float speed, float yaw) {
        double length = movement.lengthSqr();
        if (length < 1.0E-7) return Vec3.ZERO;
        Vec3 scaled = (length > 1.0 ? movement.normalize() : movement).scale(speed);
        float sin = Mth.sin(yaw * Mth.DEG_TO_RAD);
        float cos = Mth.cos(yaw * Mth.DEG_TO_RAD);
        return new Vec3(scaled.x * cos - scaled.z * sin, scaled.y, scaled.z * cos + scaled.x * sin);
    }

    private float movementSpeed(float slipperiness) {
        return onGround ? movementSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness)) : airSpeed();
    }

    private float movementSpeed() {
        return 0.1F;
    }

    private float airSpeed() {
        return input.keys.sprint() ? 0.02F + 0.006F : 0.02F;
    }

    public void jump() {
        velocity = velocity.add(0.0, jumpVelocity() - velocity.y, 0.0);
        if (sprinting) {
            float yawRad = (float) Math.toRadians(yaw);
            velocity = velocity.add(-Mth.sin(yawRad) * 0.2, 0.0, Mth.cos(yawRad) * 0.2);
        }
    }

    private void swimUp() {
        velocity = velocity.add(0.0, 0.04F, 0.0);
    }

    private float jumpVelocity() {
        return 0.42F * jumpFactor() + jumpBoost();
    }

    private float jumpBoost() {
        MobEffectInstance boost = effect(MobEffects.JUMP_BOOST);
        return boost == null ? 0.0F : 0.1F * (boost.getAmplifier() + 1);
    }

    private float jumpFactor() {
        Block at = blockState(blockPos(pos)).getBlock();
        float factor = at.getJumpFactor();
        float belowFactor = blockState(blockBelow()).getBlock().getJumpFactor();
        return factor == 1.0F ? belowFactor : factor;
    }


    private void updateSprinting() {
        if (sprinting && shouldStopSprinting()) sprinting = false;
        if (!sprinting && canStartSprinting()) sprinting = true;
    }

    private boolean shouldStopSprinting() {
        return input.forward <= 0.0F || input.keys.shift() || horizontalCollision || isTouchingWater() && !isSubmerged();
    }

    private boolean canStartSprinting() {
        return input.keys.sprint() && input.forward > 0.8F && !input.keys.shift() && !horizontalCollision
                && !player.isUsingItem() && !player.isPassenger() && (!isTouchingWater() || isSubmerged());
    }

    private void updateSwimming() {
        if (swimming) {
            swimming = sprinting && isTouchingWater() && !player.isPassenger();
        } else {
            swimming = sprinting && isSubmerged() && !player.isPassenger()
                    && player.level().getFluidState(blockPos(pos)).is(FluidTags.WATER);
        }
    }

    private void updateWaterState() {
        if (player.getVehicle() instanceof Boat boat && !boat.isUnderWater()) {
            inWater = false;
            return;
        }
        if (updateFluidMovement(FluidTags.WATER, 0.014)) {
            resetFall();
            inWater = true;
        } else {
            inWater = false;
        }
    }

    private void updateEyeFluids() {
        eyeInWater = eyeFluids.contains(FluidTags.WATER);
        eyeFluids.clear();
        double eyeY = pos.y + player.getEyeHeight() - 0.11111111F;
        if (player.getVehicle() instanceof Boat boat && !boat.isUnderWater()
                && boat.getBoundingBox().maxY >= eyeY && boat.getBoundingBox().minY <= eyeY) {
            return;
        }

        BlockPos eyePos = BlockPos.containing(pos.x, eyeY, pos.z);
        FluidState fluid = player.level().getFluidState(eyePos);
        if (eyePos.getY() + fluid.getHeight(player.level(), eyePos) > eyeY) {
            eyeFluids.addAll(fluid.typeHolder().tags().toList());
        }
    }

    private boolean updateFluidMovement(TagKey<Fluid> tag, double speed) {
        if (isRegionUnloaded()) return false;

        AABB area = box.deflate(0.001);
        int minX = Mth.floor(area.minX), maxX = Mth.ceil(area.maxX);
        int minY = Mth.floor(area.minY), maxY = Mth.ceil(area.maxY);
        int minZ = Mth.floor(area.minZ), maxZ = Mth.ceil(area.maxZ);
        double height = 0.0;
        boolean touching = false;
        Vec3 flow = Vec3.ZERO;
        int samples = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    cursor.set(x, y, z);
                    FluidState fluid = player.level().getFluidState(cursor);
                    if (!fluid.is(tag)) continue;

                    double top = y + fluid.getHeight(player.level(), cursor);
                    if (top < area.minY) continue;

                    touching = true;
                    height = Math.max(top - area.minY, height);
                    Vec3 push = fluid.getFlow(player.level(), cursor);
                    if (height < 0.4) push = push.scale(height);
                    flow = flow.add(push);
                    samples++;
                }
            }
        }

        if (flow.length() > 0.0) {
            if (samples > 0) flow = flow.scale(1.0 / samples);
            flow = flow.scale(speed);
            if (Math.abs(velocity.x) < 0.003 && Math.abs(velocity.z) < 0.003 && flow.length() < 0.0045) {
                flow = flow.normalize().scale(0.0045);
            }
            velocity = velocity.add(flow);
        }

        fluidHeights.put(tag, height);
        return touching;
    }

    private boolean isRegionUnloaded() {
        AABB area = box.inflate(1.0);
        return !player.level().hasChunksAt(Mth.floor(area.minX), Mth.floor(area.minZ), Mth.ceil(area.maxX), Mth.ceil(area.maxZ));
    }

    private boolean onClimbable() {
        BlockPos at = blockPos(pos);
        BlockState state = blockState(at);
        if (state.is(BlockTags.CLIMBABLE)) return true;
        return state.getBlock() instanceof TrapDoorBlock && isTrapdoorLadder(at, state);
    }

    private boolean isTrapdoorLadder(BlockPos at, BlockState state) {
        if (!state.getValue(TrapDoorBlock.OPEN)) return false;
        BlockState below = player.level().getBlockState(at.below());
        return below.is(Blocks.LADDER) && below.getValue(LadderBlock.FACING).equals(state.getValue(TrapDoorBlock.FACING));
    }

    private boolean isFree(double x, double y, double z) {
        AABB moved = box.move(x, y, z);
        return player.level().noCollision(player, moved) && !player.level().containsAnyLiquid(moved);
    }

    private boolean isSneaking() {
        return input.keys.shift() || input.sneaking;
    }

    private boolean isTouchingWater() {
        return inWater;
    }

    private boolean isSubmerged() {
        return eyeInWater && isTouchingWater();
    }

    private boolean inLava() {
        return fluidHeights.getDouble(FluidTags.LAVA) > 0.0;
    }

    private double fluidHeight(TagKey<Fluid> tag) {
        return fluidHeights.getDouble(tag);
    }

    private double fluidJumpThreshold() {
        return player.getEyeHeight() < 0.4 ? 0.0 : 0.4;
    }

    private void resetFall() {
        fallDistance = 0.0F;
    }

    private Vec3 lookVector() {
        float pitchRad = (float) (pitch * Math.PI / 180.0);
        float yawRad = (float) (-yaw * Math.PI / 180.0);
        float cosYaw = Mth.cos(yawRad);
        float sinYaw = Mth.sin(yawRad);
        float cosPitch = Mth.cos(pitchRad);
        float sinPitch = Mth.sin(pitchRad);
        return new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
    }

    private BlockPos blockBelow() {
        return BlockPos.containing(pos.x, box.minY - 0.5000001, pos.z);
    }

    private static BlockPos blockPos(Vec3 vec) {
        return new BlockPos(Mth.floor(vec.x), Mth.floor(vec.y), Mth.floor(vec.z));
    }

    private BlockState blockState(BlockPos at) {
        return player.level().getBlockState(at);
    }

    private boolean hasEffect(Holder<MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        return instance != null && instance.getDuration() >= ticks;
    }

    private MobEffectInstance effect(Holder<MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        return instance != null && instance.getDuration() >= ticks ? instance : null;
    }

    private double attribute(Holder<Attribute> attribute) {
        return player.getAttributes().getValue(attribute);
    }

    private static Object2DoubleMap<TagKey<Fluid>> fluidHeights(Player player) {
        Object2DoubleArrayMap<TagKey<Fluid>> heights = new Object2DoubleArrayMap<>();
        double water = player.getFluidHeight(FluidTags.WATER);
        if (water > 0.0) heights.put(FluidTags.WATER, water);
        double lava = player.getFluidHeight(FluidTags.LAVA);
        if (lava > 0.0) heights.put(FluidTags.LAVA, lava);
        return heights;
    }

    private static HashSet<TagKey<Fluid>> eyeFluids(Player player) {
        HashSet<TagKey<Fluid>> fluids = new HashSet<>();
        if (player.isEyeInFluid(FluidTags.WATER)) fluids.add(FluidTags.WATER);
        if (player.isEyeInFluid(FluidTags.LAVA)) fluids.add(FluidTags.LAVA);
        return fluids;
    }


    public static final class GuessedInput {
        public Input keys;
        public boolean sneaking;
        public float forward;
        public float strafe;

        public GuessedInput(Input keys) {
            this.keys = keys;
        }

        public static GuessedInput guess(Player player) {
            Vec3 delta = player.position().subtract(player.xo, player.yo, player.zo);
            double speedSq = delta.horizontalDistanceSqr();
            Input keys = new Input(false, false, false, false, !player.onGround(), player.isShiftKeyDown(), speedSq >= 0.121 * 0.121);
            if (speedSq > 0.05 * 0.05) {
                double angle = Mth.wrapDegrees(movementAngle(delta, player.getYRot()));
                keys = keysForAngle(keys, angle, 20.0F);
            }
            return new GuessedInput(keys);
        }

        private static double movementAngle(Vec3 delta, float yaw) {
            float moveAngle = (float) Math.atan2(-delta.x, delta.z);
            double yawRad = Math.toRadians(Mth.wrapDegrees(yaw));
            return Math.toDegrees(Mth.wrapDegrees(moveAngle - yawRad));
        }

        private static Input keysForAngle(Input keys, double angle, float margin) {
            boolean forward = keys.forward();
            boolean backward = keys.backward();
            boolean left = keys.left();
            boolean right = keys.right();

            if (angle >= -90.0F + margin && angle <= 90.0F - margin) {
                forward = true;
            } else if (angle < -90.0F - margin || angle > 90.0F + margin) {
                backward = true;
            }
            if (angle >= 0.0F + margin && angle <= 180.0F - margin) {
                right = true;
            } else if (angle >= -180.0F + margin && angle <= 0.0F - margin) {
                left = true;
            }
            return new Input(forward, backward, left, right, keys.jump(), keys.shift(), keys.sprint());
        }

        public void update() {
            forward = keys.forward() == keys.backward() ? 0.0F : keys.forward() ? 1.0F : -1.0F;
            strafe = keys.left() == keys.right() ? 0.0F : keys.left() ? 1.0F : -1.0F;
            if (keys.shift()) {
                strafe *= 0.3F;
                forward *= 0.3F;
            }
        }
    }
}
