package net.skycore.client.util;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class HitsEntityHelper {
   public static boolean hitsEntity(float value, float value2, double value3, Entity entity) {
      Vec3 eyePosition = Minecraft.getInstance().player.getEyePosition();
      AABB boundingBox = entity.getBoundingBox();
      return hitsEntity(eyePosition, value, value2, value3, false, boundingBox);
   }

   public static boolean hitsEntity(Vec3 position, float value, float value2, double value3, boolean condition, AABB bounds) {
      ClientLevel instance = Minecraft.getInstance().level;
      if (instance == null) {
         return false;
      } else {
         Vec3 position2 = position.add(lookVector(value2, value).scale(value3));
         if (bounds.contains(position)) {
            return true;
         } else {
            var optional = bounds.clip(position, position2);
            if (optional.isEmpty()) {
               return false;
            } else if (!condition) {
               return true;
            } else {
               BlockHitResult instance2 = instance.clip(new ClipContext(position, (Vec3)optional.get(), Block.COLLIDER, Fluid.NONE, Minecraft.getInstance().player));
               if (instance2.getType() == Type.BLOCK) {
                  double location = position.distanceTo(instance2.getLocation());
                  double value4 = position.distanceTo((Vec3)optional.get());
                  return !(location < value4);
               } else {
                  return true;
               }
            }
         }
      }
   }

   public static boolean hitsBlock(Vec3 position, float value, float value2, double value3, BlockPos blockPos) {
      Vec3 position2 = position.add(lookVector(value2, value).scale(value3));
      AABB bounds = new AABB(blockPos);
      if (!bounds.contains(position)) {
         if (!bounds.clip(position, position2).isPresent()) {
            return false;
         }
      }

      return true;
   }

   public static Vec3 lookVector(float value, float value2) {
      float computedValue = -value2 * (float) (Math.PI / 180.0) - (float) Math.PI;
      float computedValue2 = -value * (float) (Math.PI / 180.0);
      float cosine = (float)Math.cos(computedValue);
      double value3 = computedValue;
      float sine = (float)Math.sin(value3);
      double value4 = computedValue2;
      float computedValue3 = -((float)Math.cos(value4));
      double value5 = computedValue2;
      float sine2 = (float)Math.sin(value5);
      double computedValue4 = sine * computedValue3;
      double intermediate = sine2;
      double intermediate2 = cosine * computedValue3;
      return new Vec3(computedValue4, intermediate, intermediate2);
   }

   private HitsEntityHelper() {
   }
}
