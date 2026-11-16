package deluxe.client.module.combat;

import deluxe.client.util.EntityUtils;

import deluxe.client.module.combat.AttackAuraModule;
import deluxe.client.util.Easings;
import deluxe.client.module.combat.MaceHelperModule;
import deluxe.client.util.MathUtils;
import deluxe.client.MinecraftAccess;
import deluxe.client.RotationAngles;
import deluxe.client.RotationManager;
import deluxe.client.RotationSpeed;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class TimedAuraRotation implements MinecraftAccess {
   private float field03562;
   private float field03563;
   private float field03564;
   private long field03565;
   private long field03566;
   private float field03567;
   private float field03568;
   private float field03569;
   private float field03570;
   private float field03571;
   private float field03572;
   private float field03573;
   private long field03574;
   private long field03575;
   private long field03576;
   private long field03577;
   private float field03578;
   private float field03579;
   private float field03580;
   private float field03581;

   public void method1304(LivingEntity livingEntity, boolean local1812_2, float local1812_3) {
      if (livingEntity != null && mc.player != null && mc.world != null) {
         AttackAuraModule attackAuraModule = AttackAuraModule.method1029();
         long local1812_5 = System.currentTimeMillis();
         boolean local1812_7 = attackAuraModule.method1038() || MaceHelperModule.method1776().method1784() && !mc.player.isOnGround();
         if (this.field03576 == 0L) {
            this.field03576 = local1812_5;
         }

         if (this.field03579 == 0.0F) {
            this.field03579 = MathUtils.randomFloat(0.24F, 0.72F);
         }

         if (local1812_5 - this.field03574 > this.field03575) {
            this.field03574 = local1812_5;
            this.field03575 = (long)MathUtils.randomFloat(25.0F, 50.0F);
            this.field03569 = MathUtils.randomFloat(142.0F, 200.0F);
            this.field03570 = MathUtils.randomFloat(34.0F, 54.0F);
            this.field03571 = MathUtils.randomFloat(7.0F, 14.0F);
            this.field03572 = MathUtils.randomFloat(0.04F, 0.12F) * (ThreadLocalRandom.current().nextBoolean() ? 1.0F : -1.0F);
            this.field03573 = MathUtils.randomFloat(0.4F, 0.9F);
         }

         Vec3d vec3d = mc.player.getCameraPosVec(mc.getRenderTickCounter().getTickProgress(false));
         boolean local1812_9 = AttackAuraModule.method1029().field02100.isSelected("Неполноценные")
            && !EntityUtils.method2811(vec3d, livingEntity.getBoundingBox().getCenter())
            && EntityUtils.method2793(vec3d, livingEntity.getBoundingBox().getCenter());
         Vec3d vec3d2 = this.method1306(
               livingEntity,
               local1812_7 ? 1.0 : this.field03579,
               !local1812_7 && !local1812_9 ? this.field03580 : 0.0,
               0.0,
               !local1812_7 && !local1812_9 ? this.field03581 : 0.0
            )
            .subtract(vec3d)
            .normalize();
         float local1812_11 = (float)Math.toDegrees(Math.atan2(-vec3d2.x, vec3d2.z));
         float local1812_12 = (float)MathHelper.clamp(-Math.toDegrees(Math.atan2(vec3d2.y, Math.hypot(vec3d2.x, vec3d2.z))), -90.0, 90.0);
         float yaw = mc.player.getYaw();
         float pitch = mc.player.getPitch();
         boolean local1812_15 = false;
         if (local1812_2 && EntityUtils.method2816(livingEntity) < local1812_3) {
            this.field03562 = attackAuraModule.method1017(1.9F, 2.3F);
            this.field03565 = local1812_5;
            this.field03566 = (long)ThreadLocalRandom.current().nextDouble(200.0, 250.0);
            this.field03579 = MathUtils.randomFloat(0.54F, 0.72F);
            this.field03580 = MathUtils.randomFloat(-0.12F, 0.12F);
            this.field03581 = MathUtils.randomFloat(-0.12F, 0.12F);
         }

         if (this.field03562 > attackAuraModule.method1017(0.43F, 0.75F)) {
            local1812_15 = true;
            this.field03562 = attackAuraModule.method1017(0.13F, 0.25F);
         }

         if (local1812_15) {
            this.field03563 = local1812_11;
            this.field03564 = local1812_12;
         } else if (local1812_5 - this.field03565 < this.field03566) {
            local1812_11 = this.field03563;
            local1812_12 = this.field03564;
         }

         if (EntityUtils.method2799(
            mc.player.getEyePos(), new RotationAngles(yaw, pitch).method4363(), livingEntity.getBoundingBox(), local1812_3 + 1.0
         )) {
            if (this.field03577 == 0L) {
               this.field03577 = local1812_5;
            }
         } else {
            this.field03577 = 0L;
         }

         float local1812_16 = MathHelper.clamp(((float)(this.field03577 == 0L ? 0L : local1812_5 - this.field03577) - 250.0F) / 300.0F, 0.0F, 1.0F);
         if (local1812_16 >= this.field03578) {
            this.field03578 = local1812_16;
         } else {
            this.field03578 = this.field03578 + (local1812_16 - this.field03578) * 4.0F;
         }

         float local1812_17 = MathHelper.wrapDegrees(local1812_11 - yaw);
         float local1812_18 = local1812_12 - pitch;
         float local1812_19 = (float)Math.hypot(local1812_17, local1812_18);
         float local1812_20 = this.field03569
            * (float)Easings.LINEAR.apply(MathUtils.clampUnit(local1812_19 / this.field03570))
            * MathHelper.lerp(MathHelper.clamp((float)(local1812_5 - this.field03576) / 220.0F, 0.0F, 1.0F), 0.78F, 1.0F)
            * (1.0F - MathUtils.clampUnit(this.field03578) * (local1812_7 ? 0.35F : 1.0F))
            * (local1812_15 ? 1.25F : 1.0F)
            * (local1812_7 ? 3.2F : 1.0F)
            * (mc.player.handSwingTicks > attackAuraModule.method1017(0.2F, 0.4F) && !local1812_7 ? 0.5F : 1.0F);
         float local1812_21 = this.field03571 * (local1812_15 ? 4.0F : 1.0F) * (local1812_7 ? 2.6F : 1.0F);
         this.field03567 = this.field03567
            + MathHelper.clamp(
               local1812_20 * (local1812_19 < 0.001F ? 0.0F : local1812_17 / local1812_19) * 1.25F / (EntityUtils.method2798(livingEntity) ? 2.0F : 1.0F)
                  - this.field03567,
               -local1812_21 * (local1812_7 ? 0.6F : 1.2F),
               local1812_21 * (local1812_7 ? 0.6F : 1.2F)
            );
         this.field03568 = this.field03568
            + MathHelper.clamp(
               local1812_20 * (local1812_19 < 0.001F ? 0.0F : local1812_18 / local1812_19) + this.field03567 * this.field03572 - this.field03568,
               -local1812_21 * 0.68F,
               local1812_21 * 0.68F
            );
         this.field03567 = MathHelper.clamp(this.field03567, -Math.abs(local1812_17), Math.abs(local1812_17));
         this.field03568 = MathHelper.clamp(
            this.field03568,
            -Math.abs(local1812_18) - Math.abs(this.field03567 * this.field03572),
            Math.abs(local1812_18) + Math.abs(this.field03567 * this.field03572)
         );
         if (local1812_19 < this.field03573) {
            this.field03567 = this.field03568 = 0.0F;
         }

         RotationManager.method0658()
            .method0660(
               new RotationAngles(local1812_11, local1812_12),
               new RotationSpeed(
                  Math.min(
                        Math.abs(this.field03567),
                        EntityUtils.method2797(new RotationAngles(local1812_11, local1812_12), local1812_7 ? 0.6F : 1.0F, local1812_7 ? 1.0F : 1.5F)
                     )
                     * (local1812_7 ? 3.4F : 1.0F),
                  Math.min(
                        Math.abs(this.field03568),
                        EntityUtils.method2801(new RotationAngles(local1812_11, local1812_12), local1812_7 ? 1.0F : 2.5F, local1812_7 ? 1.8F : 4.0F)
                     )
                     * (local1812_7 ? 3.4F : 1.0F),
                  25.0F,
                  25.0F
               ),
               RotationManager.ReturnMode.Empty
            );
      }
   }

   public void method1305() {
      this.field03562 = 0.0F;
      this.field03563 = this.field03564 = 0.0F;
      this.field03565 = this.field03566 = 0L;
      this.field03567 = this.field03568 = 0.0F;
      this.field03574 = this.field03575 = 0L;
      this.field03576 = 0L;
      this.field03577 = 0L;
      this.field03578 = 0.0F;
      this.field03579 = 0.0F;
      this.field03580 = this.field03581 = 0.0F;
   }

   private Vec3d method1306(LivingEntity livingEntity, double local1815_2, double local1815_4, double local1815_6, double local1815_8) {
      return livingEntity.getEntityPos()
         .add(
            local1815_4, MathHelper.clamp(livingEntity.getHeight() * local1815_2 + local1815_6, 0.1, Math.max(0.1, livingEntity.getHeight() - 0.1)), local1815_8
         );
   }
}
