package net.skycore.client.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class TargetArcParticles {
   private final List<TargetParticleRenderer.Sprite> sprites;
   private LivingEntity lastTarget;
   private final ElapsedMillisHelper fade = new ElapsedMillisHelper(500L, ElapsedMillisHelper.Easing.LINEAR);
   private static final int ARM_COUNT = 4;
   private boolean animationTarget;
   private final TargetParticleRenderer renderer;
   private static final double ARC = Math.toRadians(60.0);
   private static final double VERTICAL_AMPLITUDE = 0.7;
   private static final int SEGMENT_COUNT = 14;

   public void reset() {
      this.lastTarget = null;
      this.animationTarget = false;
      this.sprites.clear();
      this.fade.animate(0.0F, 0.0F, 0L, ElapsedMillisHelper.Easing.LINEAR);
   }

   private static boolean valid(LivingEntity entity) {
      if (entity != null && entity.isAlive()) {
         if (!entity.isRemoved()) {
            return true;
         }
      }

      return false;
   }

   private void updateAnimation(boolean animationTarget2) {
      if (animationTarget2 != this.animationTarget) {
         this.animationTarget = animationTarget2;
         ElapsedMillisHelper fade2 = this.fade;
         float value = this.fade.getValue();
         float elapsed = animationTarget2 ? 1.0F : 0.0F;
         fade2.animate(value, elapsed, 500L, ElapsedMillisHelper.Easing.LINEAR);
      }
   }

   public TargetArcParticles() {
      this.sprites = new ArrayList<>(60);
      this.renderer = new TargetParticleRenderer();
   }

   public void render(LivingEntity lastTarget2, float count, int coordinate, int coordinate2) {
      boolean valid = valid(lastTarget2);
      if (valid) {
         this.lastTarget = lastTarget2;
      }

      this.updateAnimation(valid);
      float value = Mth.clamp(this.fade.getValue(), 0.0F, 1.0F);
      if (this.lastTarget != null && !(value <= 0.001F)) {
         Vec3 position = CameraViewPoseHelper.interpolatedPosition(this.lastTarget, count);
         double computedValue = (this.lastTarget.tickCount + count) / 20.0;
         double computedValue2 = computedValue * 3.0 % (Math.PI * 2);
         double computedValue3 = ARC / 14.0;
         double bbWidth = this.lastTarget.getBbWidth();
         double bbHeight = this.lastTarget.getBbHeight() / 2.0 + 0.2;
         this.sprites.clear();

         for (int value2 = 0; value2 < 4; value2++) {
            double index = computedValue + value2 * 15.0;
            double index2 = value2 * (Math.PI / 2);

            for (int value3 = 0; value3 <= 14; value3++) {
               double computedValue4 = value3 * computedValue3;
               double computedValue5 = computedValue4 + computedValue2 + index2;
               float computedValue6 = value3 / 14.0F;
               float computedValue7 = 0.2F * (0.5F + computedValue6);
               float computedValue8 = (float)(0.5 + 0.5 * Math.sin(computedValue * 1.5 + value3 * 14 * 0.035));
               int value4 = WriteHexByteHelper.multiplyAlpha(WriteHexByteHelper.lerp(coordinate, coordinate2, computedValue8), value);
               this.sprites
                  .add(
                     new TargetParticleRenderer.Sprite(
                        new Vec3(position.x + bbWidth * Math.cos(computedValue5), position.y + Math.sin(index + computedValue4 + value2) * 0.7 + bbHeight, position.z + bbWidth * Math.sin(computedValue5)),
                        computedValue7,
                        value4
                     )
                  );
            }
         }

         this.renderer.render(this.sprites, 1.0F, true, true);
      } else {
         if (!valid) {
            this.lastTarget = null;
            this.sprites.clear();
         }
      }
   }
}
