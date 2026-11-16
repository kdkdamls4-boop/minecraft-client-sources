package deluxe.client.module.combat;

import deluxe.client.MinecraftAccess;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class AuraPitchTracker implements MinecraftAccess {
   private float field01471;

   public void method0823() {
      this.field01471 = 0.0F;
   }

   public float method0824(LivingEntity livingEntity, float local1102_2, float local1102_3, boolean local1102_4) {
      if (livingEntity != null && mc.player != null) {
         Vec3d cameraPosVec = mc.player.getCameraPosVec(local1102_2);
         Box box = livingEntity.getBoundingBox().offset(livingEntity.getLerpedPos(local1102_2).subtract(livingEntity.getEntityPos()));
         double local1102_7 = Math.max(0.6, Math.hypot(box.getCenter().x - cameraPosVec.x, box.getCenter().z - cameraPosVec.z));
         float local1102_9 = method0825(box.maxY - box.getLengthY() * 0.3 - cameraPosVec.y, local1102_7);
         float local1102_10 = method0825(box.minY + box.getLengthY() * 0.2 - cameraPosVec.y, local1102_7);
         float local1102_11 = local1102_3 < local1102_9 ? local1102_9 - local1102_3 : (local1102_3 > local1102_10 ? local1102_10 - local1102_3 : 0.0F);
         float local1102_12 = MathHelper.sin(mc.player.age * 0.37F) * 0.55F + MathHelper.cos(mc.player.age * 0.11F) * 0.35F;
         float local1102_13 = local1102_4 ? 1.8F : 1.1F;
         float local1102_14 = local1102_4 ? 0.15F : 0.09F;
         this.field01471 = this.field01471
            + MathHelper.clamp(
               MathHelper.clamp((local1102_11 + local1102_12) * (local1102_4 ? 0.16F : 0.08F * AuraYawTracker.method0938()), -local1102_13, local1102_13)
                  - this.field01471,
               -local1102_14,
               local1102_14
            );
         return this.field01471;
      } else {
         return 0.0F;
      }
   }

   private static float method0825(double local1103_0, double local1103_2) {
      return (float)(-Math.toDegrees(Math.atan2(local1103_0, local1103_2)));
   }
}
