package deluxe.client.module.combat;

import deluxe.client.util.EntityUtils;
import deluxe.client.util.RotationQuantizer;

import deluxe.client.MinecraftAccess;
import deluxe.client.RotationAngles;
import deluxe.client.RotationManager;
import deluxe.client.RotationSpeed;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class AdaptiveAuraRotation implements MinecraftAccess, AuraRotationStrategy {
   private final AuraYawTracker auraYawTracker = new AuraYawTracker();
   private final AuraPitchTracker auraPitchTracker = new AuraPitchTracker();
   private final RotationQuantizer field01003 = new RotationQuantizer();
   private final RotationQuantizer field01004 = new RotationQuantizer();
   private int field01005;
   private boolean field01006;

   @Override
   public void l_() {
      this.auraYawTracker.method0940();
      this.auraPitchTracker.method0823();
      this.field01003.reset();
      this.field01004.reset();
   }

   @Override
   public void method0676(LivingEntity entity, boolean local901_2, float local901_3) {
      if (entity != null && mc.player != null) {
         this.field01005 = mc.player.age;
         this.field01006 = local901_2 || AttackController.method1169(-150L);
         if (local901_2 && EntityUtils.method2816(entity) < local901_3) {
            this.auraYawTracker.method0937();
         }

         RotationManager.method0658()
            .method0660(
               new RotationAngles(mc.player.getYaw(), mc.player.getPitch()),
               new RotationSpeed(0.0F, 0.0F, 60.0F, 24.0F),
               RotationManager.ReturnMode.Empty
            );
      }
   }

   @Override
   public void method0677(LivingEntity livingEntity, float local902_2) {
      if (livingEntity != null && mc.player != null && RotationManager.method0658().method0667() && mc.player.age - this.field01005 <= 1) {
         float yaw = mc.player.getYaw();
         float pitch = mc.player.getPitch();
         Vec3d cameraPosVec = mc.player.getCameraPosVec(local902_2);
         boolean local902_6 = this.field01006
            && !livingEntity.getBoundingBox()
               .offset(livingEntity.getLerpedPos(local902_2).subtract(livingEntity.getEntityPos()))
               .expand(livingEntity.getTargetingMargin())
               .raycast(cameraPosVec, cameraPosVec.add(EntityUtils.method2810(yaw, pitch).multiply(8.0)))
               .isPresent();
         mc.player
            .setYaw(
               mc.player.headYaw = yaw
                  + this.field01003.quantize(this.auraYawTracker.method0939(livingEntity, local902_2, yaw, pitch, local902_6))
            );
         mc.player
            .setPitch(
               MathHelper.clamp(
                  pitch + this.field01004.quantize(this.auraPitchTracker.method0824(livingEntity, local902_2, pitch, local902_6)), -90.0F, 90.0F
               )
            );
      }
   }
}
