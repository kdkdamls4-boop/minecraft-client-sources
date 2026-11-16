package deluxe.client.module.combat;

import deluxe.client.util.EntityUtils;

import deluxe.client.util.Easings;
import deluxe.client.util.MathUtils;
import deluxe.client.MinecraftAccess;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class AuraYawTracker implements MinecraftAccess {
   private float field01914;
   private float field01915 = 0.6F;
   private float field01916;
   private boolean field01917;
   private long field01918;

   public void method0937() {
      this.field01918 = System.currentTimeMillis() + (long)MathUtils.randomFloat(200.0F, 250.0F);
   }

   public static float method0938() {
      float attackCooldownProgress = mc.player.getAttackCooldownProgress(1.0F);
      double attributeValue = mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED);
      if (attributeValue > 1.6) {
         attackCooldownProgress *= (float)(1.6 / attributeValue);
      }

      return MathHelper.clamp(MathHelper.sin((float)(Easings.EASE_OUT_BACK.apply(attackCooldownProgress) * Math.PI)), 0.12F, 1.0F);
   }

   public float method0939(LivingEntity livingEntity, float local1270_2, float local1270_3, float local1270_4, boolean local1270_5) {
      if (livingEntity != null && mc.player != null) {
         Vec3d vec3d = new Vec3d(
            livingEntity.getX() - livingEntity.lastRenderX - mc.player.getX() + mc.player.lastRenderX,
            0.0,
            livingEntity.getZ() - livingEntity.lastRenderZ - mc.player.getZ() + mc.player.lastRenderZ
         );
         Vec3d cameraPosVec = mc.player.getCameraPosVec(local1270_2);
         Box box = livingEntity.getBoundingBox()
            .offset(livingEntity.getLerpedPos(local1270_2).subtract(livingEntity.getEntityPos()).add(vec3d.multiply(4.0)));
         box = box.contract(box.getLengthX() * 0.25, 0.0, box.getLengthZ() * 0.25);
         Vec3d vec3d2 = cameraPosVec.add(EntityUtils.method2810(local1270_3, local1270_4).multiply(cameraPosVec.distanceTo(box.getCenter())));
         double local1270_10 = MathHelper.clamp(vec3d2.x, box.minX, box.maxX) - cameraPosVec.x;
         double local1270_12 = MathHelper.clamp(vec3d2.z, box.minZ, box.maxZ) - cameraPosVec.z;
         float local1270_14 = MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(local1270_12, local1270_10)) - 90.0F - local1270_3)
            + MathHelper.sin(mc.player.age * 0.21F) * 0.35F;
         this.field01916 = Math.abs(local1270_14) < 1.5F ? Math.min(this.field01916 + 1.0F, 240.0F) : 0.0F;
         if (Math.abs(local1270_14) < this.field01915) {
            this.field01917 = true;
            this.field01914 = 0.0F;
            return 0.0F;
         }

         if (this.field01917) {
            this.field01917 = false;
            this.field01915 = MathUtils.randomFloat(0.4F, 0.9F);
         }

         float local1270_15 = local1270_5
            ? 1.0F
            : (1.0F - MathHelper.clamp((this.field01916 - 60.0F) / 72.0F, 0.0F, 1.0F))
               * (mc.player.handSwingTicks > MathUtils.randomFloat(0.2F, 0.4F) ? 0.5F : 1.0F)
               * (EntityUtils.method2798(livingEntity) ? 0.5F : 1.0F)
               * (System.currentTimeMillis() < this.field01918 ? 0.35F : 1.0F);
         float local1270_16 = local1270_5 ? 4.5F : 3.0F;
         float local1270_17 = local1270_5 ? 0.35F : 0.22F;
         this.field01914 = this.field01914
            + MathHelper.clamp(
               MathHelper.clamp(local1270_14 * (local1270_5 ? 0.2F : 0.14F * method0938()) * local1270_15, -local1270_16, local1270_16) - this.field01914,
               -local1270_17,
               local1270_17
            );
         return this.field01914;
      } else {
         return 0.0F;
      }
   }

   public void method0940() {
      this.field01914 = this.field01916 = 0.0F;
      this.field01917 = false;
      this.field01918 = 0L;
   }
}
