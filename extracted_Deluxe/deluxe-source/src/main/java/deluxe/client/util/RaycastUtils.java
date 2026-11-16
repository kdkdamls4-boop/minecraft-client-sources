package deluxe.client.util;

import deluxe.client.MinecraftAccess;
import deluxe.client.RotationAngles;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class RaycastUtils implements MinecraftAccess {
   public static Entity method3288(LivingEntity livingEntity, double local5373_1, double local5373_3, double local5373_5) {
      if (livingEntity != null && mc.player != null) {
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d vec3d = eyePos.add(method3291(local5373_3, local5373_1).multiply(local5373_5));
         EntityHitResult entityHitResult = method3295(
            mc.player,
            eyePos,
            vec3d,
            livingEntity.getBoundingBox().expand(local5373_5),
            entity -> !entity.isSpectator() && entity.canHit(),
            local5373_5
         );
         if (entityHitResult != null && entityHitResult.getEntity() == livingEntity) {
            return livingEntity;
         } else {
            return livingEntity.getBoundingBox().raycast(eyePos, vec3d).isPresent() ? livingEntity : null;
         }
      } else {
         return null;
      }
   }

   public static boolean method3289(Entity entity, Entity entity2) {
      return entity.getEntityWorld()
            .raycast(
               new RaycastContext(
                  new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ()),
                  new Vec3d(entity2.getX(), entity2.getEyeY(), entity2.getZ()),
                  ShapeType.COLLIDER,
                  FluidHandling.NONE,
                  entity
               )
            )
            .getType()
         == Type.MISS;
   }

   private RaycastUtils() {
   }

   public static Vec3d method3291(double local5377_0, double local5377_2) {
      double local5377_4 = -local5377_2 * (Math.PI / 180.0) - Math.PI;
      double local5377_6 = -local5377_0 * (Math.PI / 180.0);
      return new Vec3d(Math.sin(local5377_4) * -Math.cos(local5377_6), Math.sin(local5377_6), Math.cos(local5377_4) * -Math.cos(local5377_6));
   }

   public static EntityHitResult method3292(double local5378_0, RotationAngles rotationAngles, Predicate<Entity> local5378_3) {
      return mc.getCameraEntity() == null
         ? null
         : method3295(
            mc.getCameraEntity(),
            mc.getCameraEntity().getCameraPosVec(1.0F),
            mc.getCameraEntity().getCameraPosVec(1.0F).add(rotationAngles.method4363().multiply(local5378_0)),
            mc.getCameraEntity().getBoundingBox().stretch(rotationAngles.method4363().multiply(local5378_0)).expand(1.0, 1.0, 1.0),
            entity -> !entity.isSpectator() && local5378_3.test(entity),
            local5378_0
         );
   }

   public static EntityHitResult method3295(
      Entity entity, Vec3d vec3d, Vec3d vec3d2, Box box, Predicate<Entity> local5381_4, double local5381_5
   ) {
      double local5381_7 = local5381_5;
      Entity entity2 = null;
      Vec3d vec3d3 = null;

      for (Entity entity3 : entity.getEntityWorld().getOtherEntities(entity, box, local5381_4)) {
         Box boundingBox = entity3.getBoundingBox();
         Optional<Vec3d> local5381_14 = boundingBox.raycast(vec3d, vec3d2);
         if (boundingBox.contains(vec3d)) {
            if (local5381_7 >= 0.0) {
               entity2 = entity3;
               vec3d3 = vec3d;
               local5381_7 = 0.0;
            }
         } else if (local5381_14.isPresent()) {
            double local5381_15 = vec3d.distanceTo(local5381_14.get());
            if (local5381_15 < local5381_7 || local5381_7 == 0.0) {
               if (entity3.getRootVehicle() == entity.getRootVehicle()) {
                  if (local5381_7 == 0.0) {
                     entity2 = entity3;
                     vec3d3 = local5381_14.get();
                  }
               } else {
                  entity2 = entity3;
                  vec3d3 = local5381_14.get();
                  local5381_7 = local5381_15;
               }
            }
         }
      }

      return entity2 == null ? null : new EntityHitResult(entity2, vec3d3);
   }

   public static Entity method3296(LivingEntity livingEntity, double local5382_1, double local5382_3, double local5382_5) {
      if (livingEntity != null && mc.player != null) {
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d vec3d = eyePos.add(method3291(local5382_3, local5382_1).multiply(local5382_5));
         EntityHitResult entityHitResult = method3295(
            mc.player,
            eyePos,
            vec3d,
            livingEntity.getBoundingBox().expand(local5382_5),
            entity -> !entity.isSpectator(),
            local5382_5
         );
         return entityHitResult == null ? null : entityHitResult.getEntity();
      } else {
         return null;
      }
   }
}
