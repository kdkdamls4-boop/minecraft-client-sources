package net.skycore.client.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.skycore.client.util.MinecraftAccess;

public final class CrystalPlacement implements MinecraftAccess {
   private CrystalPlacement() {
   }

   public static BlockHitResult placeAgainstHit(BlockPos blockPos, Direction direction) {
      if (blockPos == null) {
         return null;
      }

      if (direction != null) {
         BlockHitResult opposite = visibleHitOnFace(blockPos.relative(direction.getOpposite()), direction);
         if (opposite != null) {
            return opposite;
         }
      }

      BlockHitResult blockHitResult = null;
      double mAX_VALUE = Double.MAX_VALUE;
      Vec3 eyePosition = mc.player == null ? Vec3.ZERO : mc.player.getEyePosition();

      for (Direction direction2 : Direction.values()) {
         BlockPos opposite2 = blockPos.relative(direction2.getOpposite());
         if (isSolid(opposite2)) {
            BlockHitResult blockHitResult2 = visibleHitOnFace(opposite2, direction2);
            if (blockHitResult2 != null) {
               double location = eyePosition.distanceToSqr(blockHitResult2.getLocation());
               if (location < mAX_VALUE) {
                  mAX_VALUE = location;
                  blockHitResult = blockHitResult2;
               }
            }
         }
      }

      return blockHitResult;
   }

   public static BlockHitResult interactPlace(BlockPos blockPos, Direction direction) {
      BlockHitResult blockHitResult = placeAgainstHit(blockPos, direction);
      if (blockHitResult != null) {
         return blockHitResult;
      }

      Direction[] directionValues = direction == null
         ? Direction.values()
         : new Direction[]{direction, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN};

      for (Direction direction2 : directionValues) {
         BlockPos opposite = blockPos.relative(direction2.getOpposite());
         if (isSolid(opposite)) {
            return new BlockHitResult(getFacePoint(opposite, direction2), direction2, opposite, false);
         }
      }

      return null;
   }

   public static boolean hasVisibleFace(BlockPos blockPos) {
      return visibleHit(blockPos) != null;
   }

   private static boolean isSolid(BlockPos blockPos) {
      if (mc.level != null && blockPos != null) {
         BlockState blockState = mc.level.getBlockState(blockPos);
         if (!blockState.isAir() && !blockState.canBeReplaced()) {
            if (blockState.getFluidState().isEmpty()) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static Vec3 getFacePoint(BlockPos blockPos, Direction direction) {
      double x = blockPos.getX() + 0.5 + direction.getStepX() * 0.5;
      double y = blockPos.getY() + 0.5 + direction.getStepY() * 0.5;
      double z = blockPos.getZ() + 0.5;
      z += direction.getStepZ() * 0.5;
      return new Vec3(x, y, z);
   }

   public static BlockHitResult visibleHit(BlockPos blockPos) {
      if (mc.player != null && mc.level != null && blockPos != null) {
         BlockHitResult blockHitResult = null;
         double mAX_VALUE = Double.MAX_VALUE;
         Vec3 eyePosition = mc.player.getEyePosition();

         for (Direction direction : Direction.values()) {
            BlockHitResult blockHitResult2 = visibleHitOnFace(blockPos, direction);
            if (blockHitResult2 != null) {
               double location = eyePosition.distanceToSqr(blockHitResult2.getLocation());
               if (location < mAX_VALUE) {
                  mAX_VALUE = location;
                  blockHitResult = blockHitResult2;
               }
            }
         }

         return blockHitResult;
      } else {
         return null;
      }
   }

   public static Direction getBestVisibleFaceToObsidian(BlockPos blockPos, Player player) {
      Direction direction = null;
      double index = 0.0;

      for (Direction direction2 : Direction.values()) {
         BlockPos blockPos2 = blockPos.relative(direction2);
         if (mc.level.getBlockState(blockPos2).isAir()) {
            Vec3 facePoint = getFacePoint(blockPos, direction2);
            Rotation rotation = LookHelper.calculateAngle(facePoint);
            if (CrystalRayTrace.raycastBlock(rotation, blockPos, 4.5) != null) {
               double damageOfGhostBlock = CrystalDamage.getDamageOfGhostBlock(Vec3.atCenterOf(blockPos2.above()), player, blockPos2);
               if (damageOfGhostBlock > index) {
                  index = damageOfGhostBlock;
                  direction = direction2;
               }
            }
         }
      }

      return direction;
   }

   public static Vec3 crystalClick(BlockPos blockPos) {
      double x = blockPos.getX() + 0.5;
      return new Vec3(x, blockPos.getY() + 1.0, blockPos.getZ() + 0.5);
   }

   public static Direction getBestVisibleFace(BlockPos blockPos, Player player) {
      BlockHitResult blockHitResult = visibleHit(blockPos);
      Direction direction;
      if (blockHitResult == null) {
         direction = null;
      } else {
         direction = blockHitResult.getDirection();
      }

      return direction;
   }

   public static BlockHitResult interactHit(BlockPos blockPos, Direction direction) {
      BlockHitResult blockHitResult = visibleHit(blockPos);
      if (blockHitResult != null) {
         return blockHitResult;
      }

      Direction direction2 = direction != null ? direction : Direction.UP;
      Vec3 facePoint = getFacePoint(blockPos, direction2);
      return new BlockHitResult(facePoint, direction2, blockPos, false);
   }

   public static BlockHitResult obstructionTo(Vec3 position) {
      if (mc.player != null && mc.level != null && position != null) {
         Vec3 eyePosition = mc.player.getEyePosition();
         BlockHitResult blockHitResult = mc.level.clip(new ClipContext(eyePosition, position, Block.OUTLINE, Fluid.NONE, mc.player));
         return blockHitResult.getType() != Type.BLOCK ? null : blockHitResult;
      } else {
         return null;
      }
   }

   public static BlockHitResult visibleHitOnFace(BlockPos blockPos, Direction direction) {
      if (mc.player != null && mc.level != null && blockPos != null && direction != null) {
         Vec3 eyePosition = mc.player.getEyePosition();
         Vec3 facePoint = getFacePoint(blockPos, direction);
         Vec3 stepX = facePoint.add(-direction.getStepX() * 0.02, -direction.getStepY() * 0.02, -direction.getStepZ() * 0.02);
         BlockHitResult blockHitResult = mc.level.clip(new ClipContext(eyePosition, stepX, Block.OUTLINE, Fluid.NONE, mc.player));
         return blockHitResult.getType() == Type.BLOCK && blockHitResult.getBlockPos().equals(blockPos)
            ? new BlockHitResult(blockHitResult.getLocation(), blockHitResult.getDirection(), blockPos, false)
            : null;
      } else {
         return null;
      }
   }

   public static Direction getBestFaceToObsidian(BlockPos blockPos, Player player) {
      Direction direction = null;
      double index = 0.0;

      for (Direction direction2 : Direction.values()) {
         BlockPos blockPos2 = blockPos.relative(direction2);
         if (mc.level.getBlockState(blockPos2).isAir()) {
            double damageOfGhostBlock = CrystalDamage.getDamageOfGhostBlock(Vec3.atCenterOf(blockPos2.above()), player, blockPos2);
            if (damageOfGhostBlock > index) {
               index = damageOfGhostBlock;
               direction = direction2;
            }
         }
      }

      return direction;
   }

   public static Direction getClosestFace(BlockPos blockPos, Player player) {
      Vec3 eyePosition = player.getEyePosition();
      Direction uP = Direction.UP;
      double mAX_VALUE = Double.MAX_VALUE;

      for (Direction direction : Direction.values()) {
         Vec3 facePoint = getFacePoint(blockPos, direction);
         double distanceSquared = eyePosition.distanceToSqr(facePoint);
         if (distanceSquared < mAX_VALUE) {
            mAX_VALUE = distanceSquared;
            uP = direction;
         }
      }

      return uP;
   }
}
