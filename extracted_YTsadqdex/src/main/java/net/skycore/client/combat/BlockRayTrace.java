package net.skycore.client.combat;

import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.skycore.client.util.MinecraftAccess;

public final class BlockRayTrace implements MinecraftAccess {
   public static BlockHitResult raycastBlock(Player player, BlockPos blockPos, double value) {
      Vec3 eyePosition = player.getEyePosition();
      Vec3 viewVector = player.getViewVector(1.0F);
      Vec3 position = viewVector.scale(value);
      Vec3 position2 = eyePosition.add(position);
      return raycastBlockBox(eyePosition, position2, blockPos);
   }

   private static BlockHitResult raycastBlockBox(Vec3 position, Vec3 position2, BlockPos blockPos) {
      AABB bounds = new AABB(blockPos);
      var optional = bounds.clip(position, position2);
      if (optional.isEmpty()) {
         return null;
      }

      Vec3 position3 = (Vec3)optional.get();
      return new BlockHitResult(position3, getHitSide(position3, blockPos), blockPos, false);
   }

   private static Vec3 getClosestPointOnBox(Vec3 position, AABB bounds) {
      double clampedValue = clamp(position.x, bounds.minX, bounds.maxX);
      double clampedValue2 = clamp(position.y, bounds.minY, bounds.maxY);
      double clampedValue3 = clamp(position.z, bounds.minZ, bounds.maxZ);
      return new Vec3(clampedValue, clampedValue2, clampedValue3);
   }

   public static BlockHitResult raycastBlock(Rotation rotation, Player player, BlockPos blockPos, double value) {
      Vec3 eyePosition = player.getEyePosition();
      Vec3 position = rotation.toVector();
      Vec3 position2 = position.scale(value);
      Vec3 position3 = eyePosition.add(position2);
      return raycastBlockBox(eyePosition, position3, blockPos);
   }

   public static boolean isLookingAtEntity(Entity entity, double value) {
      if (mc.player != null && entity != null) {
         Vec3 eyePosition = mc.player.getEyePosition();
         Vec3 viewVector = eyePosition.add(mc.player.getViewVector(1.0F).scale(value));
         AABB boundingBox = entity.getBoundingBox();
         if (!boundingBox.clip(eyePosition, viewVector).isPresent()) {
            if (!boundingBox.contains(eyePosition)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static BlockHitResult raycastBlock(BlockPos blockPos, double value) {
      BlockHitResult blockHitResult;
      if (mc.player == null) {
         blockHitResult = null;
      } else {
         blockHitResult = raycastBlock(mc.player, blockPos, value);
      }

      return blockHitResult;
   }

   public static boolean isLookingAtEntity(Rotation rotation, Entity entity, double value) {
      if (mc.player != null && entity != null && rotation != null) {
         Vec3 eyePosition = mc.player.getEyePosition();
         Vec3 position = rotation.toVector();
         Vec3 position2 = position.scale(value);
         Vec3 position3 = eyePosition.add(position2);
         AABB boundingBox = entity.getBoundingBox();
         if (!boundingBox.clip(eyePosition, position3).isPresent()) {
            if (!boundingBox.contains(eyePosition)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean rayTrace(Rotation rotation, double value, AABB bounds) {
      if (mc.player != null && rotation != null && bounds != null) {
         Vec3 eyePosition = mc.player.getEyePosition();
         if (!bounds.contains(eyePosition)) {
            Vec3 position = rotation.toVector();
            position = position.scale(value);
            if (!bounds.clip(eyePosition, eyePosition.add(position)).isPresent()) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static double clamp(double value, double value2, double value3) {
      return Math.max(value2, Math.min(value3, value));
   }

   public static boolean canReachBox(AABB bounds, double value) {
      if (mc.player != null && bounds != null) {
         Vec3 eyePosition = mc.player.getEyePosition();
         Vec3 closestPointOnBox = getClosestPointOnBox(eyePosition, bounds);
         return eyePosition.distanceToSqr(closestPointOnBox) <= value * value;
      } else {
         return false;
      }
   }

   public static BlockHitResult findObstruction(BlockPos blockPos, Direction direction) {
      if (mc.player != null) {
         if (mc.level != null && blockPos != null && direction != null) {
            Vec3 eyePosition = mc.player.getEyePosition();
            Vec3 facePoint = BlockFaces.getFacePoint(blockPos, direction);
            ClientLevel level = mc.level;
            ClipContext clipContext = new ClipContext(eyePosition, facePoint, Block.OUTLINE, Fluid.NONE, mc.player);
            BlockHitResult blockHitResult = level.clip(clipContext);
            Type type = blockHitResult.getType();
            if (type != Type.BLOCK) {
               return null;
            }

            BlockPos blockPos2 = blockHitResult.getBlockPos();
            return blockPos2.equals(blockPos) ? null : blockHitResult;
         }
      }

      return null;
   }

   private BlockRayTrace() {
   }

   private static Direction getHitSide(Vec3 position, BlockPos blockPos) {
      double x = position.x - (blockPos.getX() + 0.5);
      double y = position.y - (blockPos.getY() + 0.5);
      double z = position.z - (blockPos.getZ() + 0.5);
      double computedValue = Math.abs(x);
      double computedValue2 = Math.abs(y);
      double computedValue3 = Math.abs(z);
      if (computedValue > computedValue2 && computedValue > computedValue3) {
         return x > 0.0 ? Direction.EAST : Direction.WEST;
      } else if (computedValue2 > computedValue3) {
         return y > 0.0 ? Direction.UP : Direction.DOWN;
      } else {
         return z > 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   public static BlockHitResult raycastBlock(Rotation rotation, BlockPos blockPos, double value) {
      BlockHitResult blockHitResult;
      if (mc.player == null) {
         blockHitResult = null;
      } else {
         blockHitResult = raycastBlock(rotation, mc.player, blockPos, value);
      }

      return blockHitResult;
   }
}
