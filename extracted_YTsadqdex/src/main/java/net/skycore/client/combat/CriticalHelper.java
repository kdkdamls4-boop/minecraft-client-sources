package net.skycore.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class CriticalHelper {
   public static Vec3 getClosestVec(Vec3 position, Entity entity) {
      if (entity == null) {
         return position;
      }

      AABB boundingBox = entity.getBoundingBox();
      return getClosestPoint(position, boundingBox);
   }

   public static Vec3 getClosestPoint(Vec3 position, AABB bounds) {
      double clampedValue = Mth.clamp(position.x, bounds.minX, bounds.maxX);
      double clampedValue2 = Mth.clamp(position.y, bounds.minY, bounds.maxY);
      double clampedValue3 = Mth.clamp(position.z, bounds.minZ, bounds.maxZ);
      return new Vec3(clampedValue, clampedValue2, clampedValue3);
   }

   private CriticalHelper() {
   }

   public static double getStrictDistance(Entity entity) {
      return getClosestVec(entity).length();
   }

   public static Vec3 getClosestVec(Entity entity) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && entity != null) {
         Vec3 eyePosition = instance.getEyePosition(1.0F);
         AABB boundingBox = entity.getBoundingBox();
         return getClosestPoint(eyePosition, boundingBox).subtract(eyePosition);
      } else {
         return Vec3.ZERO;
      }
   }

   public static boolean canSeeEntity(Entity entity) {
      LocalPlayer instance = Minecraft.getInstance().player;
      Minecraft instance2 = Minecraft.getInstance();
      if (instance != null && entity != null && instance2.level != null) {
         Vec3 eyePosition = instance.getEyePosition(1.0F);
         Vec3 eyePosition2 = entity.getEyePosition(1.0F);
         BlockHitResult blockHitResult = instance2.level.clip(new ClipContext(eyePosition, eyePosition2, Block.COLLIDER, Fluid.NONE, instance));
         return blockHitResult.getType() == Type.MISS;
      } else {
         return false;
      }
   }

   public static Vec3 getClosestVec(Vec3 position, AABB bounds) {
      return getClosestPoint(position, bounds);
   }

   public static boolean isJumpBlockedByCeiling() {
      LocalPlayer instance = Minecraft.getInstance().player;
      Minecraft instance2 = Minecraft.getInstance();
      if (instance == null || instance2.level == null) {
         return false;
      } else if (!instance.isInWater() && !instance.onClimbable() && !instance.getAbilities().flying) {
         double value = !instance.onGround() ? 1.5 : 2.5;
         AABB bounds = new AABB(instance.getX() - 0.3, instance.getY() + instance.getBbHeight(), instance.getZ() - 0.3, instance.getX() + 0.3, instance.getY() + value, instance.getZ() + 0.3);
         int value2 = !instance2.level.noCollision(instance, bounds) ? 1 : 0;
         double bbHeight = instance.isCrouching() && instance2.options.keyJump.isDown() ? instance.getBbHeight() + 0.05 : instance.getBbHeight() + 0.2;
         AABB bounds2 = new AABB(instance.getX() - 0.3, instance.getY() + instance.getBbHeight(), instance.getZ() - 0.3, instance.getX() + 0.3, instance.getY() + bbHeight, instance.getZ() + 0.3);
         int value3 = !instance2.level.noCollision(instance, bounds2) ? 1 : 0;
         return instance.onGround() && value2 != 0 && value3 != 0;
      } else {
         return false;
      }
   }
}
