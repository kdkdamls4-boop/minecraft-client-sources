package deluxe.client.util;

import deluxe.client.util.MathUtils;
import deluxe.client.MinecraftAccess;
import deluxe.client.RotationAngles;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class AimPointUtils implements MinecraftAccess {
   private static Vec3d field15156 = Vec3d.ZERO;
   private static Vec3d field15157 = Vec3d.ZERO;
   private static float field15158;
   private static float field15159;
   private static long field15160;

   public static RotationAngles method4450(Vec3d vec3d) {
      double local7498_1 = Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
      float local7498_3 = (float)Math.toDegrees(Math.atan2(vec3d.z, vec3d.x)) - 90.0F;
      float local7498_4 = (float)(-Math.toDegrees(Math.atan2(vec3d.y, local7498_1)));
      return new RotationAngles(MathHelper.wrapDegrees(local7498_3), MathHelper.clamp(local7498_4, -90.0F, 90.0F));
   }

   private AimPointUtils() {
   }

   private static Vec3d method4451(LivingEntity livingEntity, Vec3d vec3d) {
      Vec3d vec3d2 = livingEntity.getBoundingBox()
         .raycast(
            vec3d,
            vec3d.add(
               new RotationAngles(mc.player.getYaw(), mc.player.getPitch())
                  .method4363()
                  .multiply(mc.player.distanceTo(livingEntity) + livingEntity.getWidth() + 1.0F)
            )
         )
         .orElse(
            new Vec3d(
               MathHelper.clamp(vec3d.x, livingEntity.getBoundingBox().minX, livingEntity.getBoundingBox().maxX),
               MathHelper.clamp(vec3d.y, livingEntity.getBoundingBox().minY, livingEntity.getBoundingBox().maxY),
               MathHelper.clamp(vec3d.z, livingEntity.getBoundingBox().minZ, livingEntity.getBoundingBox().maxZ)
            )
         );
      float local7500_3 = method4454(vec3d, vec3d2);

      for (float local7500_7 : new float[]{0.35F, 0.55F, 0.75F, 0.9F}) {
         Vec3d vec3d3 = new Vec3d(
            MathHelper.clamp(vec3d.x, livingEntity.getBoundingBox().minX, livingEntity.getBoundingBox().maxX),
            livingEntity.getBoundingBox().minY + (livingEntity.getBoundingBox().maxY - livingEntity.getBoundingBox().minY) * local7500_7,
            MathHelper.clamp(vec3d.z, livingEntity.getBoundingBox().minZ, livingEntity.getBoundingBox().maxZ)
         );
         float local7500_9 = method4454(vec3d, vec3d3);
         if (local7500_9 < local7500_3) {
            local7500_3 = local7500_9;
            vec3d2 = vec3d3;
         }
      }

      return vec3d2;
   }

   public static Vec3d method4452(LivingEntity livingEntity, boolean local7501_1, boolean local7501_2) {
      field15158 = MathHelper.lerp(MathUtils.method3825(0.65F, 0.85F), field15158, MathUtils.method3825(0.65F, 0.85F));
      field15159 = MathHelper.lerp(MathUtils.method3825(0.65F, 0.85F), field15159, MathUtils.method3825(0.65F, 0.85F));
      if (field15157 == Vec3d.ZERO) {
         field15157 = livingEntity.getLerpedPos(1.0F);
      }

      if (field15156 == Vec3d.ZERO) {
         field15156 = mc.player.getCameraPosVec(1.0F);
      }

      if (!local7501_2
         && System.currentTimeMillis() - field15160 > (local7501_1 ? MathUtils.method3822(25L, 67L) : MathUtils.method3822(87L, 185L))) {
         field15160 = System.currentTimeMillis();
         field15157 = new Vec3d(
            field15157.x,
            MathHelper.lerp(
               MathUtils.method3825(0.29F, Math.abs(livingEntity.getY() - mc.player.getY()) > 1.0 ? 0.45F : 0.69F),
               field15157.y,
               livingEntity.getY() + livingEntity.getHeight() / 2.0F
            ),
            field15157.z
         );
      }

      if (System.currentTimeMillis() - field15160 > (local7501_1 ? MathUtils.method3822(25L, 67L) : MathUtils.method3822(55L, 115L))) {
         field15160 = System.currentTimeMillis();
         field15156 = new Vec3d(
            field15156.x, MathHelper.lerp(MathUtils.method3825(0.15F, 0.35F), field15156.y, mc.player.getCameraPosVec(1.0F).y), field15156.z
         );
      }

      field15156 = new Vec3d(
         MathHelper.lerp(field15158, field15156.x, mc.player.getCameraPosVec(1.0F).x),
         field15156.y,
         MathHelper.lerp(field15159, field15156.z, mc.player.getCameraPosVec(1.0F).z)
      );
      if (local7501_2) {
         Vec3d vec3d = method4451(livingEntity, field15156);
         field15157 = new Vec3d(
            MathHelper.lerp(field15158, field15157.x, vec3d.x),
            MathHelper.lerp(local7501_1 ? 0.45F : 0.65F, field15157.y, vec3d.y),
            MathHelper.lerp(field15159, field15157.z, vec3d.z)
         );
         return field15157.subtract(field15156);
      } else {
         field15157 = new Vec3d(
            MathHelper.lerp(field15158, field15157.x, livingEntity.getX()), field15157.y, MathHelper.lerp(field15159, field15157.z, livingEntity.getZ())
         );
         return field15157.subtract(field15156);
      }
   }

   public static void method4453() {
      field15156 = Vec3d.ZERO;
      field15157 = Vec3d.ZERO;
      field15158 = MathUtils.method3825(0.38F, 0.55F);
      field15159 = MathUtils.method3825(0.38F, 0.55F);
      field15160 = 0L;
   }

   private static float method4454(Vec3d vec3d, Vec3d vec3d2) {
      return (float)Math.hypot(
         MathHelper.wrapDegrees(
            Math.toDegrees(Math.atan2(vec3d2.subtract(vec3d).z, vec3d2.subtract(vec3d).x)) - 90.0 - mc.player.getYaw()
         ),
         -Math.toDegrees(Math.atan2(vec3d2.subtract(vec3d).y, Math.hypot(vec3d2.subtract(vec3d).x, vec3d2.subtract(vec3d).z)))
            - mc.player.getPitch()
      );
   }

   public static Vec3d method4455(LivingEntity livingEntity, boolean local7505_1) {
      return method4452(livingEntity, local7505_1, false);
   }
}
