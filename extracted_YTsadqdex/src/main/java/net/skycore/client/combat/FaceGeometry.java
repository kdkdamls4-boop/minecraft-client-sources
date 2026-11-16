package net.skycore.client.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.util.MinecraftAccess;

public final class FaceGeometry implements MinecraftAccess {
   private static double tryHitPlaneZ(Vec3 position, Vec3 position2, double value, double value2, double value3, double value4, double value5) {
      if (position2.z == 0.0) {
         return -1.0;
      }

      double z = (value - position.z) / position2.z;
      if (z <= 0.0) {
         return -1.0;
      }

      double x = position.x + z * position2.x;
      double y = position.y + z * position2.y;
      return x >= value2 && x <= value3 && y >= value4 && y <= value5 ? z : -1.0;
   }

   private static double tryHitPlaneY(Vec3 position, Vec3 position2, double value, double value2, double value3, double value4, double value5) {
      if (position2.y == 0.0) {
         return -1.0;
      }

      double y = value - position.y;
      double y2 = y / position2.y;
      if (y2 <= 0.0) {
         return -1.0;
      }

      double x = position.x + y2 * position2.x;
      double z = position.z + y2 * position2.z;
      return x >= value2 && x <= value3 && z >= value4 && z <= value5 ? y2 : -1.0;
   }

   private FaceGeometry() {
   }

   private static Direction getFallbackDirection(Vec3 position, double value, double value2, double value3) {
      double x = position.x - value;
      double y = position.y - value2;
      double z = position.z - value3;
      double computedValue = Math.abs(x);
      double computedValue2 = Math.abs(y);
      double computedValue3 = Math.abs(z);
      if (computedValue2 >= computedValue && computedValue2 >= computedValue3) {
         return y > 0.0 ? Direction.UP : Direction.DOWN;
      } else if (computedValue >= computedValue3) {
         return x > 0.0 ? Direction.EAST : Direction.WEST;
      } else {
         return z > 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   public static Direction getFaceDirection(Rotation rotation, Player player, BlockPos blockPos) {
      float yRot = rotation.getYRot();
      float xRot = rotation.getXRot();
      return getFaceDirection(yRot, xRot, player, blockPos);
   }

   private static Direction getFaceDirection(float angle, float angle2, Player player, BlockPos blockPos) {
      Vec3 eyePosition = player.getEyePosition();
      Vec3 position = rotationVector(angle, angle2);
      double x = blockPos.getX() + 0.5;
      double y = blockPos.getY() + 0.5;
      double z = blockPos.getZ() + 0.5;
      double x2 = blockPos.getX();
      double y2 = blockPos.getY();
      double z2 = blockPos.getZ();
      double x3 = blockPos.getX() + 1.0;
      double y3 = blockPos.getY() + 1.0;
      double z3 = blockPos.getZ() + 1.0;
      Direction direction = null;
      double mAX_VALUE = Double.MAX_VALUE;
      double value = tryHitPlaneY(eyePosition, position, y3, x2, x3, z2, z3);
      if (value > 0.0 && value < mAX_VALUE) {
         mAX_VALUE = value;
         direction = Direction.UP;
      }

      value = tryHitPlaneY(eyePosition, position, y2, x2, x3, z2, z3);
      if (value > 0.0 && value < mAX_VALUE) {
         mAX_VALUE = value;
         direction = Direction.DOWN;
      }

      value = tryHitPlaneZ(eyePosition, position, z2, x2, x3, y2, y3);
      if (value > 0.0 && value < mAX_VALUE) {
         mAX_VALUE = value;
         direction = Direction.NORTH;
      }

      value = tryHitPlaneZ(eyePosition, position, z3, x2, x3, y2, y3);
      if (value > 0.0 && value < mAX_VALUE) {
         mAX_VALUE = value;
         direction = Direction.SOUTH;
      }

      value = tryHitPlaneX(eyePosition, position, x2, y2, y3, z2, z3);
      if (value > 0.0 && value < mAX_VALUE) {
         mAX_VALUE = value;
         direction = Direction.WEST;
      }

      value = tryHitPlaneX(eyePosition, position, x3, y2, y3, z2, z3);
      if (value > 0.0 && value < mAX_VALUE) {
         direction = Direction.EAST;
      }

      return direction != null ? direction : getFallbackDirection(eyePosition, x, y, z);
   }

   private static Vec3 rotationVector(float value, float value2) {
      double value3 = value2;
      double angleRadians = Math.toRadians(value3);
      double value4 = value;
      double angleRadians2 = Math.toRadians(value4);
      double computedValue = -angleRadians;
      double cosine = Math.cos(computedValue);
      double computedValue2 = -angleRadians2 - Math.PI;
      double computedValue3 = Math.sin(computedValue2) * cosine;
      double sine = Math.sin(-angleRadians);
      double computedValue4 = -angleRadians2 - Math.PI;
      double computedValue5 = Math.cos(computedValue4) * cosine;
      return new Vec3(computedValue3, sine, computedValue5);
   }

   private static double tryHitPlaneX(Vec3 position, Vec3 position2, double value, double value2, double value3, double value4, double value5) {
      if (position2.x == 0.0) {
         return -1.0;
      }

      double x = value - position.x;
      double x2 = x / position2.x;
      if (x2 <= 0.0) {
         return -1.0;
      }

      double y = position.y + x2 * position2.y;
      double z = position.z + x2 * position2.z;
      return y >= value2 && y <= value3 && z >= value4 && z <= value5 ? x2 : -1.0;
   }

   public static Direction getFaceDirection(Player player, BlockPos blockPos) {
      float yRot = player.getYRot();
      float xRot = player.getXRot();
      return getFaceDirection(yRot, xRot, player, blockPos);
   }
}
