package net.skycore.client.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.util.MinecraftAccess;

public final class BlockFaces implements MinecraftAccess {
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

   private BlockFaces() {
   }

   public static Direction getBestFaceToObsidian(BlockPos blockPos, Player player) {
      Direction direction = null;
      double index = 0.0;

      for (Direction direction2 : Direction.values()) {
         BlockPos blockPos2 = blockPos.relative(direction2);
         if (mc.level.getBlockState(blockPos2).isAir()) {
            double damageOfGhostBlock = VirtualExplosionDamage.getDamageOfGhostBlock(Vec3.atCenterOf(blockPos2.above()), player, blockPos2);
            if (damageOfGhostBlock > index) {
               index = damageOfGhostBlock;
               direction = direction2;
            }
         }
      }

      return direction;
   }

   public static Direction getBestVisibleFace(BlockPos blockPos, Player player) {
      Vec3 eyePosition = player.getEyePosition();
      Direction direction = null;
      double mAX_VALUE = Double.MAX_VALUE;

      for (Direction direction2 : Direction.values()) {
         Vec3 facePoint = getFacePoint(blockPos, direction2);
         Rotation rotation = LookHelper.calculateAngle(facePoint);
         if (BlockRayTrace.raycastBlock(rotation, blockPos, 4.5) != null) {
            double distanceSquared = eyePosition.distanceToSqr(facePoint);
            if (distanceSquared < mAX_VALUE) {
               mAX_VALUE = distanceSquared;
               direction = direction2;
            }
         }
      }

      return direction;
   }

   public static Vec3 getFacePoint(BlockPos blockPos, Direction direction) {
      double x = blockPos.getX() + 0.5;
      x += direction.getStepX() * 0.5001;
      double y = blockPos.getY() + 0.5 + direction.getStepY() * 0.5001;
      double z = blockPos.getZ() + 0.5;
      return new Vec3(x, y, z + direction.getStepZ() * 0.5001);
   }

   public static Direction getBestVisibleFaceToObsidian(BlockPos blockPos, Player player) {
      Direction direction = null;
      double index = 0.0;

      for (Direction direction2 : Direction.values()) {
         BlockPos blockPos2 = blockPos.relative(direction2);
         if (mc.level.getBlockState(blockPos2).isAir()) {
            Vec3 facePoint = getFacePoint(blockPos, direction2);
            Rotation rotation = LookHelper.calculateAngle(facePoint);
            if (BlockRayTrace.raycastBlock(rotation, blockPos, 4.5) != null) {
               double damageOfGhostBlock = VirtualExplosionDamage.getDamageOfGhostBlock(Vec3.atCenterOf(blockPos2.above()), player, blockPos2);
               if (damageOfGhostBlock > index) {
                  index = damageOfGhostBlock;
                  direction = direction2;
               }
            }
         }
      }

      return direction;
   }
}
