package net.skycore.client.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class RayTraceHelper {
   private static final double EPSILON = 1.0E-7;
   private static final float PARTIAL_TICK = 1.0F;

   public static boolean canSeeTarget(LocalPlayer player, LivingEntity entity, double value) {
      Vec3 position = entity.position();
      return findAimPoint(player, entity, position, value, false, Vec3.ZERO) != null;
   }

   private static List<Vec3> aimCandidates(AABB bounds, Vec3 position, double value) {
      ArrayList elements = new ArrayList(7);
      elements.add(bounds.getCenter());
      double intermediate = (bounds.minX + bounds.maxX) * 0.5;
      double clampedValue = Mth.clamp(value, bounds.minY, bounds.maxY);
      double intermediate2 = (bounds.minZ + bounds.maxZ) * 0.5;
      elements.add(new Vec3(intermediate, clampedValue, intermediate2));
      double computedValue = bounds.minY + (bounds.maxY - bounds.minY) * 0.75;
      elements.add(new Vec3(position.x, computedValue, position.z));
      double computedValue2 = (bounds.minY + bounds.maxY) * 0.5;
      elements.add(new Vec3(bounds.minX, computedValue2, bounds.minZ));
      double computedValue3 = (bounds.minY + bounds.maxY) * 0.5;
      elements.add(new Vec3(bounds.maxX, computedValue3, bounds.maxZ));
      elements.add(new Vec3(bounds.minX, (bounds.minY + bounds.maxY) * 0.5, bounds.maxZ));
      double computedValue4 = (bounds.minY + bounds.maxY) * 0.5;
      Vec3 position2 = new Vec3(bounds.maxX, computedValue4, bounds.minZ);
      elements.add(position2);
      return elements;
   }

   private RayTraceHelper() {
   }

   public static boolean rayTraceEntity(LocalPlayer player, Entity entity, float angle, float angle2, double angle3, boolean condition, boolean condition2) {
      if (player != null && entity != null && player.level() != null) {
         Vec3 eyePosition = player.getEyePosition(1.0F);
         Vec3 position = eyePosition.add(Vec3.directionFromRotation(angle2, angle).scale(angle3));
         AABB boundingBox = entity.getBoundingBox();
         var optional = boundingBox.clip(eyePosition, position);
         if (!boundingBox.contains(eyePosition) && optional.isEmpty()) {
            return false;
         }

         if (!condition) {
            return true;
         }

         Vec3 position2 = boundingBox.contains(eyePosition) ? eyePosition : optional.orElse(position);
         return isBlockPathClear(player, eyePosition, position2, condition2);
      } else {
         return false;
      }
   }

   public static double predictedHitboxDistanceSqr(LocalPlayer player, LivingEntity entity, Vec3 position) {
      Vec3 position2 = position.subtract(entity.position());
      AABB boundingBox = entity.getBoundingBox();
      AABB bounds = boundingBox.move(position2);
      double pickRadius = entity.getPickRadius();
      AABB bounds2 = bounds.inflate(pickRadius);
      return bounds2.distanceToSqr(player.getEyePosition(1.0F));
   }

   public static Vec3 findAimPoint(LocalPlayer player, LivingEntity entity, Vec3 position, double value, boolean condition, Vec3 position2) {
      Vec3 eyePosition = player.getEyePosition(1.0F);
      Vec3 position3 = position.subtract(entity.position());
      AABB boundingBox = entity.getBoundingBox().move(position3).inflate(entity.getPickRadius());
      double computedValue = value * value;
      if (boundingBox.distanceToSqr(eyePosition) <= 1.0E-7) {
         Vec3 center = boundingBox.getCenter();
         if (center.distanceToSqr(eyePosition) <= 1.0E-7) {
            center = eyePosition.add(Vec3.directionFromRotation(player.getXRot(), player.getYRot()).scale(0.01));
         }

         return center;
      } else {
         double eyeHeight = position.y + entity.getEyeHeight();
         double clampedValue = Mth.clamp(eyeHeight - 0.15, boundingBox.minY + 0.1, boundingBox.maxY - 0.1);
         Vec3 position4 = new Vec3((boundingBox.minX + boundingBox.maxX) * 0.5, clampedValue, (boundingBox.minZ + boundingBox.maxZ) * 0.5);
         Vec3 position5 = new Vec3(
            Mth.clamp(position4.x + position2.x * 0.25, boundingBox.minX, boundingBox.maxX),
            Mth.clamp(position4.y + position2.y * 0.25, boundingBox.minY, boundingBox.maxY),
            Mth.clamp(position4.z + position2.z * 0.25, boundingBox.minZ, boundingBox.maxZ)
         );
         if (!(position5.distanceToSqr(eyePosition) <= computedValue + 1.0E-7) || !condition && !isBlockPathClear(player, eyePosition, position5)) {
            var vec3s = aimCandidates(boundingBox, eyePosition, eyeHeight);
            vec3s.sort(Comparator.comparingDouble(position5::distanceToSqr));

            for (Vec3 position6 : vec3s) {
               if (!(position6.distanceToSqr(eyePosition) > computedValue + 1.0E-7) && (condition || isBlockPathClear(player, eyePosition, position6))) {
                  return position6;
               }
            }

            return null;
         } else {
            return position5;
         }
      }
   }

   private static boolean isBlockPathClear(LocalPlayer player, Vec3 position, Vec3 position2) {
      return isBlockPathClear(player, position, position2, false);
   }

   private static boolean isBlockPathClear(LocalPlayer player, Vec3 position, Vec3 position2, boolean condition) {
      Level level = player.level();
      ClipContext clipContext = new ClipContext(position, position2, Block.COLLIDER, Fluid.NONE, player);
      BlockHitResult blockHitResult = level.clip(clipContext);
      Type type = blockHitResult.getType();
      if (type != Type.MISS) {
         if (!(blockHitResult.getLocation().distanceToSqr(position) + 1.0E-7 >= position2.distanceToSqr(position))) {
            if (condition) {
               Level level2 = player.level();
               BlockPos blockPos = blockHitResult.getBlockPos();
               BlockState blockState = level2.getBlockState(blockPos);
               if (blockState.is(BlockTags.DOORS)) {
                  return true;
               }
            }

            return false;
         }
      }

      return true;
   }

   public static boolean rotationIntersectsTarget(
      LocalPlayer player, LivingEntity entity, float angle, float angle2, double angle3, double angle4, double angle5, boolean condition
   ) {
      return rotationIntersectsTarget(player, entity, angle, angle2, angle3, angle4, angle5, condition, false);
   }

   public static boolean rotationIntersectsTarget(
      LocalPlayer player, LivingEntity entity, float angle, float angle2, double value, double value2, double value3, boolean condition, boolean condition2
   ) {
      Vec3 eyePosition = player.getEyePosition(1.0F);
      Vec3 position = Vec3.directionFromRotation(angle2, angle);
      double computedValue = value + value3;
      Vec3 position2 = eyePosition.add(position.scale(computedValue));
      AABB boundingBox = entity.getBoundingBox().inflate(entity.getPickRadius());
      if (boundingBox.distanceToSqr(eyePosition) <= 1.0E-7) {
         return value2 - value3 <= 1.0E-7;
      } else {
         var optional = boundingBox.clip(eyePosition, position2);
         if (optional.isEmpty()) {
            return false;
         } else {
            Vec3 position3 = (Vec3)optional.get();
            double distance = position3.distanceTo(eyePosition);
            if (distance > computedValue + 1.0E-7 || distance < value2 - value3 - 1.0E-7) {
               return false;
            } else {
               return condition ? true : isBlockPathClear(player, eyePosition, position3, condition2);
            }
         }
      }
   }
}
