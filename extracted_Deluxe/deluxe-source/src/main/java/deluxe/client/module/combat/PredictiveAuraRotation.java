package deluxe.client.module.combat;

import deluxe.client.util.EntityUtils;

import deluxe.client.module.combat.AttackAuraModule;
import deluxe.client.util.Easings;
import deluxe.client.util.MathUtils;
import deluxe.client.MinecraftAccess;
import deluxe.client.RotationAngles;
import deluxe.client.RotationManager;
import deluxe.client.RotationSpeed;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class PredictiveAuraRotation implements MinecraftAccess {
   private float field13229;
   private float field13230;
   private float field13231;
   private float field13232;
   private float field13233;
   private float field13234;
   private float field13235;
   private long field13236;
   private long field13237;
   private long field13238;

   public void method3832(LivingEntity livingEntity, boolean local6308_2, float local6308_3) {
      if (livingEntity != null && mc.player != null && mc.world != null) {
         AttackAuraModule attackAuraModule = AttackAuraModule.method1029();
         long local6308_5 = System.currentTimeMillis();
         if (this.field13238 == 0L) {
            this.field13238 = local6308_5;
         }

         if (local6308_5 - this.field13236 > this.field13237) {
            this.field13236 = local6308_5;
            this.field13237 = (long)MathUtils.randomFloat(25.0F, 50.0F);
            this.field13232 = MathUtils.randomFloat(248.0F, 360.0F);
            this.field13233 = MathUtils.randomFloat(34.0F, 54.0F);
            this.field13234 = MathUtils.randomFloat(8.0F, 16.0F);
            this.field13235 = MathUtils.randomFloat(0.04F, 0.12F) * (ThreadLocalRandom.current().nextBoolean() ? 1.0F : -1.0F);
         }

         Vec3d vec3d = mc.player.getCameraPosVec(mc.getRenderTickCounter().getTickProgress(false));
         Vec3d vec3d2 = livingEntity.getEntityPos().add(0.0, Math.max(0.1, livingEntity.getHeight() - 0.1), 0.0).subtract(vec3d).normalize();
         float local6308_9 = (float)Math.toDegrees(Math.atan2(-vec3d2.x, vec3d2.z));
         float local6308_10 = (float)MathHelper.clamp(-Math.toDegrees(Math.atan2(vec3d2.y, Math.hypot(vec3d2.x, vec3d2.z))), -90.0, 90.0);
         boolean local6308_11 = false;
         if (local6308_2 && EntityUtils.method2816(livingEntity) < local6308_3) {
            this.field13229 = attackAuraModule.method1017(1.9F, 2.3F);
         }

         if (this.field13229 > attackAuraModule.method1017(0.43F, 0.75F)) {
            local6308_11 = true;
            this.field13229 = attackAuraModule.method1017(0.13F, 0.25F);
         }

         float local6308_12 = MathHelper.wrapDegrees(local6308_9 - mc.player.getYaw());
         float local6308_13 = local6308_10 - mc.player.getPitch();
         float local6308_14 = (float)Math.hypot(local6308_12, local6308_13);
         float local6308_15 = this.field13232
            * (float)Easings.LINEAR.apply(MathUtils.clampUnit(local6308_14 / this.field13233))
            * MathHelper.lerp(MathHelper.clamp((float)(local6308_5 - this.field13238) / 160.0F, 0.0F, 1.0F), 0.78F, 1.0F)
            * (local6308_11 ? 1.25F : 1.0F)
            * 3.6F;
         float local6308_16 = this.field13234 * (local6308_11 ? 4.0F : 1.0F) * 3.0F;
         this.field13230 = this.field13230
            + MathHelper.clamp(
               local6308_15 * (local6308_14 < 0.001F ? 0.0F : local6308_12 / local6308_14) * 1.25F / (EntityUtils.method2798(livingEntity) ? 2.0F : 1.0F)
                  - this.field13230,
               -local6308_16 * 1.2F,
               local6308_16 * 1.2F
            );
         this.field13231 = this.field13231
            + MathHelper.clamp(
               local6308_15 * (local6308_14 < 0.001F ? 0.0F : local6308_13 / local6308_14) + this.field13230 * this.field13235 - this.field13231,
               -local6308_16 * 0.68F,
               local6308_16 * 0.68F
            );
         this.field13230 = MathHelper.clamp(this.field13230, -Math.abs(local6308_12), Math.abs(local6308_12));
         this.field13231 = MathHelper.clamp(
            this.field13231,
            -Math.abs(local6308_13) - Math.abs(this.field13230 * this.field13235),
            Math.abs(local6308_13) + Math.abs(this.field13230 * this.field13235)
         );
         RotationManager.method0658()
            .method0660(
               new RotationAngles(local6308_9, local6308_10),
               new RotationSpeed(
                  Math.min(Math.abs(this.field13230), EntityUtils.method2797(new RotationAngles(local6308_9, local6308_10), 0.5F, 0.9F)) * 3.9F,
                  Math.min(Math.abs(this.field13231), EntityUtils.method2801(new RotationAngles(local6308_9, local6308_10), 0.9F, 1.6F)) * 3.9F,
                  25.0F,
                  25.0F
               ),
               RotationManager.ReturnMode.Empty
            );
      }
   }

   public void method3833() {
      this.field13229 = 0.0F;
      this.field13230 = this.field13231 = 0.0F;
      this.field13236 = this.field13237 = 0L;
      this.field13238 = 0L;
   }
}
