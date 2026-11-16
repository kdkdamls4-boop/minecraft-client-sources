package net.skycore.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class TargetOrbitParticles {
   private final SpawnFallbackHelper deathDissolve;
   private LivingEntity lastTarget;
   private float hurtPhase;
   private static final int PARTICLES_PER_ARM = 15;
   private static final float PERIOD_MILLIS = 1500.0F;
   private final ElapsedMillisHelper inner;
   private int dissolvedTargetId;
   private final TargetParticleRenderer renderer;
   private final ElapsedMillisHelper appear = new ElapsedMillisHelper(650L, ElapsedMillisHelper.Easing.EASE_OUT_EXPO);
   private static final int ARMS = 4;
   private long lastFrameNanos;
   private float clock;
   private boolean animationTarget;
   private final List<TargetParticleRenderer.Sprite> sprites;

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

   public void reset() {
      this.lastTarget = null;
      this.dissolvedTargetId = Integer.MIN_VALUE;
      this.animationTarget = false;
      this.lastFrameNanos = 0L;
      this.clock = 0.0F;
      this.hurtPhase = 0.0F;
      this.sprites.clear();
      this.deathDissolve.clear();
      this.appear.animate(0.0F, 0.0F, 0L, ElapsedMillisHelper.Easing.EASE_OUT_EXPO);
      this.inner.animate(0.0F, 0.0F, 0L, ElapsedMillisHelper.Easing.EASE_IN_OUT_QUAD);
   }

   private static boolean valid(LivingEntity entity) {
      return entity != null && entity.isAlive() && !entity.isRemoved();
   }

   public void render(LivingEntity lastTarget2, float elapsed, int coordinate) {
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
         this.deathDissolve.burst(this.sprites, this.lastTarget, coordinate, now);
         this.dissolvedTargetId = this.lastTarget.getId();
         valid = false;
      }

      this.updateAnimations((boolean)valid);
      float value = this.appear.getValue();
      float value2 = this.inner.getValue();
      if (this.lastTarget != null && !(value <= 0.01F) && !(value2 <= 0.001F)) {
         this.clock += computedValue * 1000.0F;
         float computedValue2 = this.lastTarget.hurtTime > 0 ? Math.max(0.0F, (this.lastTarget.hurtTime - elapsed) / 10.0F) : 0.0F;
         this.hurtPhase += computedValue2 * computedValue * 500.0F;
         Vec3 position = CameraViewPoseHelper.interpolatedPosition(this.lastTarget, elapsed);
         float bbHeight = this.lastTarget.getBbHeight();
         float computedValue3 = this.clock % 1500.0F / 1500.0F;
         float cosine = 0.5F + 0.5F * (float)Math.cos(computedValue3 * Math.PI * 2.0);
         double y = position.y + bbHeight * cosine;
         float computedValue4 = this.clock / 2.5F;
         float minimum = Math.min(0.8F, value2 * 1.2F);
         int alpha = EasedFactorHelper.blend(coordinate, this.lastTarget, 1.0F);
         this.sprites.clear();

         for (int value3 = 0; value3 < 4; value3++) {
            for (int value4 = 0; value4 < 15; value4++) {
               float computedValue5 = value4 / 14.0F;
               int computedValue6 = value3 * 15 + value4;
               float value5 = AssemblyHelper.assembly(value2, computedValue6, 60);
               float computedValue7 = 0.5F * value2 * (0.6F + value5 * 0.4F);
               float computedValue8 = 0.2F * (computedValue4 + this.hurtPhase - value4 * 3.5F) / 15.0F;
               float computedValue9 = computedValue5 < 0.5F ? computedValue5 * 2.0F : (1.0F - computedValue5) * 2.0F;
               double computedValue10 = Math.sin(computedValue9 * Math.PI) * 2.0;
               Random random = new Random(value4 * 12345L);
               double computedValue11 = (random.nextDouble() - 0.5) * computedValue10;
               double computedValue12 = (random.nextDouble() - 0.5) * computedValue10;
               double computedValue13 = (random.nextDouble() - 0.5) * computedValue10;
               double computedValue14 = computedValue11 * value5 - computedValue11;
               double computedValue15 = computedValue12 * value5 - computedValue12;
               double computedValue16 = computedValue13 * value5 - computedValue13;
               double value6 = 0.7;
               double value7;
               double value8;
               switch (value3) {
                  case 0:
                     value7 = Math.cos(computedValue8) * value6 + computedValue14;
                     value8 = Math.sin(computedValue8) * value6 + computedValue16;
                     break;
                  case 1:
                     value7 = -Math.sin(computedValue8) * value6 + computedValue14;
                     value8 = Math.cos(computedValue8) * value6 + computedValue16;
                     break;
                  case 2:
                     value7 = -Math.cos(computedValue8) * value6 + computedValue14;
                     value8 = -Math.sin(computedValue8) * value6 + computedValue16;
                     break;
                  default:
                     value7 = Math.sin(computedValue8) * value6 + computedValue14;
                     value8 = -Math.cos(computedValue8) * value6 + computedValue16;
               }

               float computedValue17 = (float)Math.pow(1.0F - computedValue5, 1.3);
               float clampedValue = Mth.clamp(minimum * computedValue17 * value, 0.0F, 1.0F);
               if (!(clampedValue <= 0.004F)) {
                  this.sprites
                     .add(
                        new TargetParticleRenderer.Sprite(
                           new Vec3(
                              position.x + value7 + AssemblyHelper.scatter(computedValue6, 0, 1.4, value5),
                              y + computedValue15 + AssemblyHelper.scatter(computedValue6, 1, 1.15, value5) + (1.0F - value5) * 0.25F,
                              position.z + value8 + AssemblyHelper.scatter(computedValue6, 2, 1.4, value5)
                           ),
                           computedValue7 * 0.5F,
                           WriteHexByteHelper.multiplyAlpha(alpha, clampedValue)
                        )
                     );
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

   public TargetOrbitParticles() {
      this.inner = new ElapsedMillisHelper(850L, ElapsedMillisHelper.Easing.EASE_IN_OUT_QUAD);
      this.sprites = new ArrayList<>(60);
      this.renderer = new TargetParticleRenderer();
      this.deathDissolve = new SpawnFallbackHelper();
      this.dissolvedTargetId = Integer.MIN_VALUE;
   }
}
