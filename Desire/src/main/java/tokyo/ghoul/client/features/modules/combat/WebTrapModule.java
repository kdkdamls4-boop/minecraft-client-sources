package tokyo.ghoul.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.system.configs.FriendManager;
import tokyo.ghoul.api.utils.combat.PredictUtils;
import tokyo.ghoul.api.utils.math.MathUtil;
import tokyo.ghoul.api.utils.player.DirectionUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.api.utils.rotation.manager.RotationStrategy;
import tokyo.ghoul.api.utils.rotation.rotations.InstantRotation;
import tokyo.ghoul.api.utils.task.TaskPriority;

@ModuleRegister(name = "AutoWeb", description = "Автоматически ставит паутину на цель", category = Category.COMBAT)
public final class WebTrapModule extends Module {
    @Getter private static final WebTrapModule instance = new WebTrapModule();

    private int slotToRestore = -1;
    private int ticksToReturnSlot = -1;

    private boolean pendingUpperWeb = false;
    private BlockPos pendingUpperPos = null;

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        addEvents(update);
    }

    @Override
    public void onDisable() {
        if (slotToRestore >= 0 && slotToRestore < 9 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(slotToRestore);
        }
        slotToRestore = -1;
        ticksToReturnSlot = -1;
        pendingUpperWeb = false;
        pendingUpperPos = null;
        super.onDisable();
    }

    private void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        if (ticksToReturnSlot == 0 && slotToRestore >= 0 && slotToRestore < 9) {
            mc.player.getInventory().setSelectedSlot(slotToRestore);
            slotToRestore = -1;
            ticksToReturnSlot = -1;
        }

        int webSlot = getWebSlot();
        if (webSlot == -1) {
            tickReturnSlot();
            return;
        }

        if (pendingUpperWeb && pendingUpperPos != null) {
            tryPlaceWebAt(pendingUpperPos, webSlot);

            pendingUpperWeb = false;
            pendingUpperPos = null;

            if (slotToRestore != -1) {
                ticksToReturnSlot = 1;
            }

            tickReturnSlot();
            return;
        }

        Player target = findTarget();
        if (target == null) {
            tickReturnSlot();
            return;
        }

        Vec3 predictVec = PredictUtils.predictGround(target, 2);
        BlockPos predictPos = BlockPos.containing(predictVec);

        int prevSlot = mc.player.getInventory().getSelectedSlot();

        boolean placedLower = tryPlaceWebAt(predictPos, webSlot);
        if (placedLower) {
            pendingUpperWeb = true;
            pendingUpperPos = predictPos.above();

            slotToRestore = prevSlot;
            ticksToReturnSlot = 2;
        }

        tickReturnSlot();
    }

    private void tickReturnSlot() {
        if (ticksToReturnSlot > 0) {
            ticksToReturnSlot--;
        }
    }

    private boolean tryPlaceWebAt(BlockPos placePos, int webSlot) {
        if (!canPlaceAt(placePos)) return false;
        if (intersectsPlayer(placePos)) return false;

        PlaceData placeData = findPlaceData(placePos);
        if (placeData == null) return false;

        float[] rawRot = getRotations(placeData.hitVec);
        float yaw = rawRot[0] + MathUtil.randomInRange(-2f, 2f);
        float pitch = rawRot[1] + MathUtil.randomInRange(-2f, 2f);
        Rotation rotation = new Rotation(yaw, pitch);

        if (DirectionUtil.raycastBlock(rotation, mc.player, placeData.neighbourPos, 4.5) == null) return false;

        Direction lookedFace = DirectionUtil.getFaceDirection(rotation, mc.player, placeData.neighbourPos);
        if (lookedFace != placeData.side) return false;

        applyRotation(rotation);

        mc.player.getInventory().setSelectedSlot(webSlot);

        BlockHitResult hitResult = new BlockHitResult(
                placeData.hitVec,
                placeData.side,
                placeData.neighbourPos,
                false
        );

        InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);

        if (result.consumesAction()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
            return true;
        }

        return false;
    }

    private void applyRotation(Rotation rotation) {
        RotationStrategy strategy = new RotationStrategy(new InstantRotation(), true);
        strategy.ticksUntilReset(2);
        RotationManager.getInstance().addRotation(rotation, strategy, TaskPriority.HIGH, this);
    }

    private int getWebSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.COBWEB)) {
                return i;
            }
        }
        return -1;
    }

    private boolean canPlaceAt(BlockPos pos) {
        if (!mc.level.getBlockState(pos).canBeReplaced()) return false;

        ItemStack stack = new ItemStack(Items.COBWEB);
        BlockHitResult dummyHit = new BlockHitResult(
                Vec3.atCenterOf(pos),
                Direction.UP,
                pos.below(),
                false
        );

        BlockPlaceContext context = new BlockPlaceContext(mc.player, InteractionHand.MAIN_HAND, stack, dummyHit);
        return mc.level.getBlockState(pos).canBeReplaced(context);
    }

    private boolean intersectsPlayer(BlockPos pos) {
        AABB placeBox = new AABB(pos);
        return mc.player.getBoundingBox().intersects(placeBox);
    }

    private Vec3 resolveHitVecForFace(BlockPos neighbour, Direction side) {
        Vec3 center = Vec3.atCenterOf(neighbour);
        Vec3 normal = new Vec3(side.getStepX(), side.getStepY(), side.getStepZ());

        Vec3 base = center.add(normal.scale(0.5 - 0.01));

        Vec3[] offsets = new Vec3[]{
                Vec3.ZERO,
                new Vec3(0.1, 0.0, 0.1),
                new Vec3(-0.1, 0.0, 0.1),
                new Vec3(0.1, 0.0, -0.1),
                new Vec3(-0.1, 0.0, -0.1),
                new Vec3(0.0, 0.1, 0.0),
                new Vec3(0.0, -0.1, 0.0)
        };

        for (Vec3 offset : offsets) {
            Vec3 candidate = adjustOffsetForFace(base, offset, side);

            float[] rotations = getRotations(candidate);

            Direction lookedFace = DirectionUtil.getFaceDirection(new Rotation(rotations[0], rotations[1]), mc.player, neighbour);

            if (lookedFace == side) {
                return candidate;
            }
        }

        return base;
    }

    private Vec3 adjustOffsetForFace(Vec3 base, Vec3 offset, Direction side) {
        return switch (side) {
            case UP, DOWN -> base.add(offset.x, 0.0, offset.z);
            case NORTH, SOUTH -> base.add(offset.x, offset.y, 0.0);
            case EAST, WEST -> base.add(0.0, offset.y, offset.z);
        };
    }

    private float[] getRotations(Vec3 vec) {
        Vec3 eyes = mc.player.getEyePosition();

        double diffX = vec.x - eyes.x;
        double diffY = vec.y - eyes.y;
        double diffZ = vec.z - eyes.z;

        double distXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, distXZ));

        return new float[]{yaw, pitch};
    }

    private PlaceData findPlaceData(BlockPos placePos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbour = placePos.relative(side);
            Direction hitSide = side.getOpposite();

            BlockState neighbourState = mc.level.getBlockState(neighbour);
            if (!isValidSupportBlock(neighbourState, neighbour)) continue;

            Vec3 hitVec = resolveHitVecForFace(neighbour, hitSide);
            return new PlaceData(neighbour, hitSide, hitVec);
        }
        return null;
    }

    private boolean isValidSupportBlock(BlockState state, BlockPos pos) {
        if (state.isAir()) return false;
        if (state.getFluidState() != null && !state.getFluidState().isEmpty()) return false;

        Block block = state.getBlock();

        if (block == Blocks.COBWEB) return true;

        if (!state.isRedstoneConductor(mc.level, pos)) return false;

        if (block instanceof DoublePlantBlock) return false;
        if (block instanceof BushBlock) return false;
        if (block instanceof ButtonBlock) return false;
        if (block instanceof LeverBlock) return false;
        if (block instanceof TorchBlock) return false;
        if (block instanceof WallTorchBlock) return false;
        if (block instanceof RedstoneTorchBlock) return false;
        if (block instanceof WallBlock) return false;
        if (block instanceof FenceBlock) return false;
        if (block instanceof FenceGateBlock) return false;
        if (block instanceof DoorBlock) return false;
        if (block instanceof TrapDoorBlock) return false;
        if (block instanceof CarpetBlock) return false;
        if (block instanceof VineBlock) return false;
        if (block instanceof LadderBlock) return false;

        return true;
    }

    private Player findTarget() {
        if (mc.player == null || mc.level == null) return null;
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player player : mc.level.players()) {
            if (player == mc.player || !player.isAlive()) continue;
            if (player.isInvulnerable() || player.isCreative() || player.isSpectator()) continue;
            if (FriendManager.getInstance().contains(player.getName().getString())) continue;
            if (AntiBotModule.isBot(player)) continue;
            double dist = mc.player.distanceTo(player);
            if (dist > 4.5) continue;
            if (dist < bestDist) {
                bestDist = dist;
                best = player;
            }
        }
        return best;
    }

    public boolean isPlacing() {
        return isEnabled() && (pendingUpperWeb || ticksToReturnSlot > 0);
    }

    public float getDistance() {
        return 4.5F;
    }

    public boolean isTargetTrapped(Player player) {
        if (player == null || mc.level == null) return false;
        BlockPos pos = player.blockPosition();
        return mc.level.getBlockState(pos).is(Blocks.COBWEB)
                || mc.level.getBlockState(pos.above()).is(Blocks.COBWEB);
    }

    private record PlaceData(BlockPos neighbourPos, Direction side, Vec3 hitVec) {}
}
