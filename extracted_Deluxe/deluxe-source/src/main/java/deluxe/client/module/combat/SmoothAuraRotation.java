package deluxe.client.module.combat;

import deluxe.client.util.EntityUtils;
import deluxe.client.util.MouseSensitivity;

import deluxe.client.util.MathUtils;
import deluxe.client.MinecraftAccess;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class SmoothAuraRotation implements MinecraftAccess {
   private float field11742;
   private float field11743;
   private float field11744;
   private float field11745;
   private long field11746;

   public void method3340() {
      this.field11742 = this.field11743 = 0.0F;
      this.field11744 = this.field11745 = 0.0F;
      this.field11746 = 0L;
   }

   public void method3341(LivingEntity livingEntity) {
      if (livingEntity != null && mc.player != null && mc.world != null) {
         float local5446_2 = Math.max(1.0F, Math.min((float)(System.currentTimeMillis() - this.field11746), 100.0F)) / 50.0F;
         this.field11746 = System.currentTimeMillis();
         Vec3d eyePos = mc.player.getEyePos();
         Box box = livingEntity.getBoundingBox().contract(livingEntity.getWidth() * 0.25, livingEntity.getHeight() * 0.2, livingEntity.getWidth() * 0.25);
         Vec3d vec3d = eyePos.add(
            EntityUtils.method2810(mc.player.getYaw(), mc.player.getPitch()).multiply(eyePos.distanceTo(box.getCenter()))
         );
         float[] floatArray = EntityUtils.method2814(
            new Vec3d(
               MathHelper.clamp(vec3d.x, box.minX, box.maxX),
               MathHelper.clamp(vec3d.y, box.minY, box.maxY),
               MathHelper.clamp(vec3d.z, box.minZ, box.maxZ)
            ),
            eyePos
         );
         float local5446_7 = MathHelper.wrapDegrees(floatArray[0] - mc.player.getYaw());
         float local5446_8 = floatArray[1] - mc.player.getPitch();
         float local5446_9 = MathUtils.clampUnit((100.0F - (float)Math.hypot(local5446_7, local5446_8)) / Math.max(1.0F, 25.0F));
         if (!(local5446_9 <= 0.0F)) {
            float local5446_10 = 0.15F;
            float local5446_11 = MathHelper.clamp(local5446_7 * local5446_10, -15.0F, 15.0F) * local5446_9;
            float local5446_12 = MathHelper.clamp(local5446_8 * local5446_10, -5.0F, 5.0F) * local5446_9;
            this.field11742 = this.field11742 + (local5446_11 - this.field11742) * MathUtils.clampUnit(0.4F * local5446_2);
            this.field11743 = this.field11743 + (local5446_12 - this.field11743) * MathUtils.clampUnit(0.4F * local5446_2);
            this.field11744 = this.field11744 + MathHelper.clamp(this.field11742 * local5446_2, -Math.abs(local5446_7), Math.abs(local5446_7));
            this.field11745 = this.field11745 + MathHelper.clamp(this.field11743 * local5446_2, -Math.abs(local5446_8), Math.abs(local5446_8));
            float local5446_13 = MouseSensitivity.method4410(this.field11744);
            float local5446_14 = MouseSensitivity.method4410(this.field11745);
            this.field11744 -= local5446_13;
            this.field11745 -= local5446_14;
            mc.player.setYaw(mc.player.getYaw() + local5446_13);
            mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + local5446_14, -90.0F, 90.0F));
            mc.player.lastYaw += local5446_13;
            mc.player.lastPitch = MathHelper.clamp(mc.player.lastPitch + local5446_14, -90.0F, 90.0F);
         }
      }
   }
}
