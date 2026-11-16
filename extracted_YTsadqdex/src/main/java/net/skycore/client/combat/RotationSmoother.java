package net.skycore.client.combat;

import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.util.PerlinNoise;

public final class RotationSmoother implements AttackState {
   private float remainderPitch;
   private final LinkedList<Vec3> targetPositionHistory = new LinkedList<>();
   private float remainderYaw;

   @Override
   public void tick(LocalPlayer player, LivingEntity entity, Vec3 position, boolean condition) {
      if (entity != null && entity.isAlive()) {
         long now = System.currentTimeMillis();
         this.targetPositionHistory.addLast(entity.getDeltaMovement());

         while (this.targetPositionHistory.size() > 7) {
            this.targetPositionHistory.removeFirst();
         }

         Vec3 position2 = entity.position();
         if (!this.targetPositionHistory.isEmpty()) {
            int computedValue = Math.min(2, this.targetPositionHistory.size() - 1);
            Vec3 position3 = this.targetPositionHistory.get(computedValue);
            position2 = entity.position().subtract(position3.scale(0.35));
         }

         double computedValue2 = now / 450.0;
         float computedValue3 = PerlinNoise.perlin1D(computedValue2) * 0.1F;
         float computedValue4 = PerlinNoise.perlin2D(computedValue2, 10.0) * 0.06F;
         float computedValue5 = PerlinNoise.perlin2D(computedValue2, 20.0) * 0.06F;
         double bbHeight = position2.y + entity.getBbHeight() * (0.5F + computedValue3);
         Vec3 position4 = new Vec3(position2.x + computedValue4, bbHeight, position2.z + computedValue5).subtract(player.getEyePosition(1.0F));
         double x = Math.hypot(position4.x, position4.z);
         float x2 = (float)Math.toDegrees(Math.atan2(-position4.x, position4.z));
         float y = (float)Mth.clamp(-Math.toDegrees(Math.atan2(position4.y, x)), -90.0, 90.0);
         float yRot = RotationController.isActive() ? RotationController.getServerYaw() : player.getYRot();
         float xRot = RotationController.isActive() ? RotationController.getServerPitch() : player.getXRot();
         float computedValue6 = Mth.wrapDegrees(x2 - yRot);
         float computedValue7 = y - xRot;
         float computedValue8 = (float)Math.hypot(computedValue6, computedValue7);
         float value = 35.0F;
         if (computedValue8 < 5.0F) {
            value = randomLerp(12.0F, 18.0F);
         } else if (computedValue8 > 25.0F) {
            value = randomLerp(38.0F, 48.0F);
         }

         float yaw = Mth.clamp(computedValue6 * randomLerp(0.45F, 0.65F), -value, value);
         double computedValue9 = now / 180.0;
         float computedValue10 = 0.35F + PerlinNoise.perlin1D(computedValue9) * 0.35F;
         float computedValue11 = randomLerp(-0.12F, 0.12F) + PerlinNoise.perlin2D(now / 75.0, 5.0) * 0.2F;
         float computedValue12 = computedValue10 + computedValue11;
         float computedValue13 = (float)Math.sin(Math.toRadians(computedValue6)) * (yaw * computedValue12);
         float value2 = randomLerp(0.1F, 0.2F);
         float pitch = computedValue7 * value2 + computedValue13 * (0.15F + computedValue11 * 0.5F);
         double computedValue14 = now / 110.0;
         float computedValue15 = randomLerp(-0.15F, 0.15F);
         float computedValue16 = randomLerp(-0.08F, 0.08F);
         float computedValue17 = PerlinNoise.perlin2D(computedValue14, 1.0) * 0.4F + computedValue15;
         float computedValue18 = PerlinNoise.perlin2D(computedValue14, 2.0) * 0.15F + computedValue16;
         yaw += computedValue17;
         pitch += computedValue18;
         float gcdValue = getGcdValue();
         float computedValue19 = yaw + this.remainderYaw;
         float computedValue20 = pitch + this.remainderPitch;
         int roundedValue = Math.round(computedValue19 / gcdValue);
         int roundedValue2 = Math.round(computedValue20 / gcdValue);
         float computedValue21 = roundedValue * gcdValue;
         float computedValue22 = roundedValue2 * gcdValue;
         this.remainderYaw = computedValue19 - computedValue21;
         this.remainderPitch = computedValue20 - computedValue22;
         RotationController.setRotation(yRot + computedValue21, Mth.clamp(xRot + computedValue22, -90.0F, 90.0F));
      } else {
         this.reset();
      }
   }

   private static float randomLerp(float value, float value2) {
      return value + ThreadLocalRandom.current().nextFloat() * (value2 - value);
   }

   private static float getGcdValue() {
      Minecraft instance = Minecraft.getInstance();
      float computedValue = instance.options.sensitivity().get().floatValue() * 0.6F + 0.2F;
      return computedValue * computedValue * computedValue * 8.0F * 0.15F;
   }

   @Override
   public void reset() {
      this.targetPositionHistory.clear();
      this.remainderYaw = 0.0F;
      this.remainderPitch = 0.0F;
   }
}
