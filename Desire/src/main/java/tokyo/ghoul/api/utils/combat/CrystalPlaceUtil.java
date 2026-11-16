package tokyo.ghoul.api.utils.combat;

import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.player.InventoryUtil;
import tokyo.ghoul.api.utils.rotation.RotationUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.api.utils.rotation.manager.RotationStrategy;
import tokyo.ghoul.api.utils.rotation.rotations.InstantRotation;
import tokyo.ghoul.api.utils.task.TaskPriority;

@UtilityClass
public class CrystalPlaceUtil implements QuickImports {
    public final Direction[] PLACE_SIDES = {
            Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP
    };
    public final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    public boolean isReplaceable(BlockPos pos) {
        return isReplaceable(mc.level.getBlockState(pos));
    }

    public boolean isReplaceable(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }

    public boolean isSolid(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(mc.level, pos).isEmpty() && !state.is(Blocks.COBWEB);
    }

    public boolean isCrystalBase(BlockPos pos) {
        if (pos == null || mc.level == null) return false;
        BlockState state = mc.level.getBlockState(pos);
        return state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK)
                || state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.RESPAWN_ANCHOR);
    }

    public boolean isObsidianOrBedrock(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        return state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);
    }

    public Direction getPlaceSide(BlockPos pos) {
        if (pos == null || mc.level == null) return null;
        for (Direction direction : PLACE_SIDES) {
            if (isSolid(pos.relative(direction))) return direction;
        }
        return null;
    }

    public Vec3 getHitVec(BlockPos neighbor, Direction clickFace) {
        return new Vec3(
                neighbor.getX() + 0.5 + clickFace.getStepX() * 0.5,
                neighbor.getY() + 0.5 + clickFace.getStepY() * 0.5,
                neighbor.getZ() + 0.5 + clickFace.getStepZ() * 0.5
        );
    }

    public BlockPos feetPos(double x, double y, double z) {
        return BlockPos.containing(x, y, z);
    }

    public int findItemSlot(Item item, boolean fromInventory) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) return i;
        }
        if (fromInventory) {
            for (int i = 9; i < 36; i++) {
                if (mc.player.getInventory().getItem(i).is(item)) return i;
            }
        }
        return -1;
    }

    public int setOrSwapSlot(int slot) {
        if (slot < 0 || slot > 35 || mc.gameMode == null) return -1;
        int previous = mc.player.getInventory().getSelectedSlot();
        if (slot < 9) {
            if (previous != slot) mc.player.getInventory().setSelectedSlot(slot);
            return previous;
        }
        if (mc.player.containerMenu.containerId != mc.player.inventoryMenu.containerId) return -1;
        InventoryUtil.swapWithBypassGrim(() ->
                mc.gameMode.handleContainerInput(0, slot, previous, ContainerInput.SWAP, mc.player));
        return previous;
    }

    public void restoreSlot(int previous, int usedSlot) {
        if (mc.player == null || mc.gameMode == null || previous < 0) return;
        if (usedSlot >= 9) {
            int hotbar = mc.player.getInventory().getSelectedSlot();
            if (mc.player.containerMenu.containerId != mc.player.inventoryMenu.containerId) return;
            InventoryUtil.swapWithBypassGrim(() ->
                    mc.gameMode.handleContainerInput(0, usedSlot, hotbar, ContainerInput.SWAP, mc.player));
        } else if (previous < 9 && mc.player.getInventory().getSelectedSlot() != previous) {
            mc.player.getInventory().setSelectedSlot(previous);
        }
    }

    public void rotateTo(Vec3 point, int ticks, Module owner) {
        Rotation rotation = RotationUtil.rotationAt(point);
        RotationStrategy strategy = new RotationStrategy(new InstantRotation(), true);
        strategy.ticksUntilReset(ticks);
        RotationManager.getInstance().addRotation(rotation, strategy, TaskPriority.NORMAL, owner);
    }

    public void placeAt(BlockPos pos, Direction side, boolean rotate, Module owner) {
        BlockPos neighbor = pos.relative(side);
        Direction clickFace = side.getOpposite();
        Vec3 hit = getHitVec(neighbor, clickFace);
        if (rotate) rotateTo(hit, 10, owner);
        BlockHitResult hitResult = new BlockHitResult(hit, clickFace, neighbor, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    public boolean placeItemAt(Item item, BlockPos pos, boolean fromInventory, boolean rotate, Module owner) {
        Direction side = getPlaceSide(pos);
        if (side == null) return false;
        int slot = findItemSlot(item, fromInventory);
        if (slot == -1) return false;
        int previous = setOrSwapSlot(slot);
        if (previous == -1) return false;
        try {
            placeAt(pos, side, rotate, owner);
        } finally {
            restoreSlot(previous, slot);
        }
        return true;
    }
}
