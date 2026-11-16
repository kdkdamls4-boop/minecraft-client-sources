package net.skycore.client.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class TargetSpiralParticles {
   private static final int PARTICLES_PER_SPIRAL = 20;
   private final ElapsedMillisHelper inner;
   private final ElapsedMillisHelper appear = new ElapsedMillisHelper(650L, ElapsedMillisHelper.Easing.EASE_OUT_EXPO);
   private LivingEntity lastTarget;
   private final List<TargetParticleRenderer.Sprite> sprites;
   private int dissolvedTargetId;
   private boolean animationTarget;
   private float clock;
   private static final int SPIRAL_STEP = 3;
   private final SpawnFallbackHelper deathDissolve;
   private final TargetParticleRenderer renderer;
   private static final int SPIRAL_LIMIT = 9;
   private long lastFrameNanos;

   public void reset() {
      this.lastTarget = null;
      this.animationTarget = false;
      this.lastFrameNanos = 0L;
      this.clock = 0.0F;
      this.dissolvedTargetId = Integer.MIN_VALUE;
      this.sprites.clear();
      this.deathDissolve.clear();
      this.appear.animate(0.0F, 0.0F, 0L, ElapsedMillisHelper.Easing.EASE_OUT_EXPO);
      this.inner.animate(0.0F, 0.0F, 0L, ElapsedMillisHelper.Easing.EASE_IN_OUT_QUAD);
   }

   private void updateAnimations(boolean animationTarget2) {
      if (animationTarget2 != this.animationTarget) {
         this.animationTarget = animationTarget2;
         float value = animationTarget2 ? 1.0F : 0.0F;
         ElapsedMillisHelper appear2 = this.appear;
         float value2 = this.appear.getValue();
         long timestamp = animationTarget2 ? 650L : 450L;
         ElapsedMillisHelper.Easing easing;
         if (animationTarget2) {
            easing = ElapsedMillisHelper.Easing.EASE_OUT_EXPO;
         } else {
            easing = ElapsedMillisHelper.Easing.EASE_OUT_QUAD;
         }

         appear2.animate(value2, value, timestamp, easing);
         ElapsedMillisHelper inner2 = this.inner;
         float value3 = this.inner.getValue();
         long timestamp2 = animationTarget2 ? 850L : 500L;
         if (animationTarget2) {
            easing = ElapsedMillisHelper.Easing.EASE_IN_OUT_QUAD;
         } else {
            easing = ElapsedMillisHelper.Easing.EASE_OUT_QUAD;
         }

         inner2.animate(value3, value, timestamp2, easing);
      }
   }

   public void render(LivingEntity lastTarget2, float coordinate, int coordinate2) {
      long nowNanos = System.nanoTime();
      long now = System.currentTimeMillis();
      float computedValue = this.lastFrameNanos == 0L ? 0.016F : Math.min(0.1F, (float)(nowNanos - this.lastFrameNanos) / 1.0E9F);
      this.lastFrameNanos = nowNanos;
      boolean valid = valid(lastTarget2);
      if (valid) {
         this.lastTarget = lastTarget2;
         if (lastTarget2.getId() == this.dissolvedTargetId) {
            this.dissolvedTargetId = Integer.MIN_VALUE;
         }
      }

      if (dead(this.lastTarget) && this.lastTarget.getId() != this.dissolvedTargetId) {
         this.deathDissolve.burst(this.sprites, this.lastTarget, coordinate2, now);
         this.dissolvedTargetId = this.lastTarget.getId();
         valid = false;
      }

      this.updateAnimations((boolean)valid);
      float value = this.appear.getValue();
      float value2 = this.inner.getValue();
      if (this.lastTarget != null && !(value <= 0.01F) && !(value2 <= 0.001F)) {
         this.clock += computedValue * 1000.0F;
         Vec3 position = CameraViewPoseHelper.interpolatedPosition(this.lastTarget, coordinate);
         float computedValue2 = this.clock / 120.0F;
         float minimum = Math.min(0.85F, value2 * 1.6F);
         int alpha = EasedFactorHelper.blend(coordinate2, this.lastTarget, 1.0F);
         this.sprites.clear();

         for (byte flag = 0; flag < 9; flag += 3) {
            int index = (int)Math.pow(flag, 2.0);

            for (int value3 = 0; value3 < 20; value3++) {
               float computedValue3 = computedValue2 + value3 * 0.1F;
               int computedValue4 = flag / 3 * 20 + value3;
               float value4 = AssemblyHelper.assembly(value2, computedValue4, 60);
               double x = position.x + 0.8F * Math.sin(computedValue3 + index) + AssemblyHelper.scatter(computedValue4, 0, 1.75, value4);
               double y = position.y
                  + 0.5
                  + 0.3F * Math.sin(computedValue2 + value3 * 0.2F)
                  + 0.2F * flag
                  + AssemblyHelper.scatter(computedValue4, 1, 1.35, value4)
                  + (1.0F - value4) * 0.35F;
               double z = position.z + 0.8F * Math.cos(computedValue3 - index) + AssemblyHelper.scatter(computedValue4, 2, 1.75, value4);
               float computedValue5 = value2 * (0.005F + value3 / 2000.0F);
               float computedValue6 = (float)Math.pow(value3 / 11.0F, 1.3);
               float clampedValue = Mth.clamp(minimum * computedValue6 * value, 0.0F, 1.0F);
               float alpha2 = 25.0F * computedValue5 * (0.55F + value4 * 0.45F);
               if (!(clampedValue <= 0.004F) && !(alpha2 <= 0.001F)) {
                  this.sprites.add(new TargetParticleRenderer.Sprite(new Vec3(x, y, z), alpha2, WriteHexByteHelper.multiplyAlpha(alpha, clampedValue)));
               }
            }
         }

         this.renderer.render(this.sprites, 1.0F, true);
         this.deathDissolve.render(computedValue, now);
      } else {
         if (!valid && value <= 0.01F) {
            this.lastTarget = null;
            this.sprites.clear();
         }

         this.deathDissolve.render(computedValue, now);
      }
   }

   private static boolean dead(LivingEntity entity) {
      if (entity != null) {
         if (entity.isDeadOrDying()) {
            return true;
         }

         if (entity.deathTime > 0 || entity.getHealth() <= 0.0F) {
            return true;
         }
      }

      return false;
   }

   private static boolean valid(LivingEntity entity) {
      if (entity != null && entity.isAlive()) {
         if (!entity.isRemoved()) {
            return true;
         }
      }

      return false;
   }

   public TargetSpiralParticles() {
      this.inner = new ElapsedMillisHelper(850L, ElapsedMillisHelper.Easing.EASE_IN_OUT_QUAD);
      this.sprites = new ArrayList<>(60);
      this.renderer = new TargetParticleRenderer();
      this.deathDissolve = new SpawnFallbackHelper();
      this.dissolvedTargetId = Integer.MIN_VALUE;
   }
}
