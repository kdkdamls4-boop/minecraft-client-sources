package net.skycore.client.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SpawnFallbackHelper {
   private static final int MAX_FRAGMENTS = 240;
   private final List<SpawnFallbackHelper.Fragment> fragments = new ArrayList<>();
   private final List<TargetParticleRenderer.Sprite> sprites = new ArrayList<>();
   private final Random random;
   private final TargetParticleRenderer renderer = new TargetParticleRenderer();

   public void burst(List<TargetParticleRenderer.Sprite> sprites2, LivingEntity entity, int value, long timestamp) {
      this.burst(sprites2, entity, value, timestamp, 240, 0L, 0.018F, 0.18F, 0.42F, 1.0F, 1200L, 1200, 1.0F);
   }

   private void spawnFallback(LivingEntity entity, int index, Vec3 position, long timestamp) {
      AABB boundingBox = entity.getBoundingBox();

      for (int value = 0; value < 90; value++) {
         double randomValue = this.random.nextDouble();
         double computedValue = Mth.lerp(randomValue, boundingBox.minX, boundingBox.maxX);
         double randomValue2 = this.random.nextDouble();
         randomValue2 = Mth.lerp(randomValue2, boundingBox.minY, boundingBox.maxY);
         double randomValue3 = this.random.nextDouble();
         double computedValue2 = Mth.lerp(randomValue3, boundingBox.minZ, boundingBox.maxZ);
         Vec3 position2 = new Vec3(computedValue, randomValue2, computedValue2);
         float value2 = this.randomRange(0.025F, 0.07F);
         this.spawnFragment(position2, position, value2, index, timestamp, 0L, 1.0F, 1200L, 1200, 1.0F);
      }
   }

   private void burst(
      List<TargetParticleRenderer.Sprite> sprites2,
      LivingEntity entity,
      int value,
      long timestamp,
      int value2,
      long timestamp2,
      float value3,
      float value4,
      float value5,
      float color,
      long timestamp3,
      int color2,
      float color3
   ) {
      this.fragments.clear();
      Vec3 bbHeight = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
      if (sprites2.isEmpty()) {
         this.spawnFallback(entity, value, bbHeight, timestamp);
      } else {
         int index = Math.max(1, (int)Math.ceil((double)sprites2.size() / value2));
         int index2 = (sprites2.size() + index - 1) / index;
         int index3 = Math.max(1, Math.min(3, value2 / index2));

         for (int value6 = 0; value6 < sprites2.size() && this.fragments.size() < value2; value6 += index) {
            TargetParticleRenderer.Sprite sprite = (TargetParticleRenderer.Sprite)sprites2.get(value6);

            for (int value7 = 0; value7 < index3 && this.fragments.size() < value2; value7++) {
               this.spawnFragment(
                  sprite.position(),
                  bbHeight,
                  Math.max(value3, sprite.halfSize() * this.randomRange(value4, value5)),
                  sprite.color(),
                  timestamp,
                  timestamp2,
                  color,
                  timestamp3,
                  color2,
                  color3
               );
            }
         }
      }
   }

   private void spawnFragment(Vec3 position, Vec3 position2, float value, int alpha, long timestamp, long timestamp2, float value2, long timestamp3, int value3, float alpha2) {
      Vec3 position3 = position.subtract(position2);
      double squareRoot = Math.sqrt(position3.x * position3.x + position3.z * position3.z);
      double x = squareRoot > 1.0E-5 ? position3.x / squareRoot : 0.0;
      double z = squareRoot > 1.0E-5 ? position3.z / squareRoot : 0.0;
      float computedValue = this.randomRange(0.35F, 1.15F) * value2;
      SpawnFallbackHelper.Fragment fragment = new SpawnFallbackHelper.Fragment();
      fragment.x = position.x + this.randomRange(-0.025F, 0.025F);
      fragment.y = position.y + this.randomRange(-0.025F, 0.025F);
      fragment.z = position.z + this.randomRange(-0.025F, 0.025F);
      fragment.vx = x * computedValue + this.randomRange(-0.24F, 0.24F);
      fragment.vy = this.randomRange(0.28F, 1.05F) + Math.max(0.0, position3.y) * 0.04;
      fragment.vz = z * computedValue + this.randomRange(-0.24F, 0.24F);
      fragment.halfSize = Math.min(0.18F, value);
      fragment.color = WriteHexByteHelper.withAlpha(alpha, 255);
      fragment.bornAt = timestamp;
      fragment.holdMillis = timestamp2;
      fragment.lifeMillis = timestamp2 + timestamp3 + this.random.nextInt(Math.max(1, value3));
      fragment.alphaScale = alpha2;
      fragment.phase = this.random.nextFloat() * (float) (Math.PI * 2);
      fragment.twinkleRate = this.randomRange(4.0F, 8.0F);
      this.fragments.add(fragment);
   }

   private float randomRange(float value, float value2) {
      float computedValue = value2 - value;
      return value + computedValue * this.random.nextFloat();
   }

   public void render(float coordinate, long timestamp) {
      this.sprites.clear();
      Iterator iterator = this.fragments.iterator();

      while (iterator.hasNext()) {
         SpawnFallbackHelper.Fragment element = (SpawnFallbackHelper.Fragment)iterator.next();
         long timestamp2 = timestamp - element.bornAt;
         if (timestamp2 >= element.lifeMillis) {
            iterator.remove();
         } else {
            element.phase = element.phase + element.twinkleRate * coordinate;
            if (timestamp2 >= element.holdMillis) {
               float computedValue = (float)Math.exp(-1.65F * coordinate);
               element.vx *= computedValue;
               element.vz *= computedValue;
               element.vy += 0.22F * coordinate;
               element.vx = element.vx + Math.cos(element.phase) * 0.045F * coordinate;
               element.vz = element.vz + Math.sin(element.phase * 0.87F) * 0.045F * coordinate;
               element.x = element.x + element.vx * coordinate;
               element.y = element.y + element.vy * coordinate;
               element.z = element.z + element.vz * coordinate;
            }

            long maximum = Math.max(0L, timestamp2 - element.holdMillis);
            long maximum2 = Math.max(1L, element.lifeMillis - element.holdMillis);
            float computedValue2 = Mth.clamp((float)maximum / (float)maximum2, 0.0F, 1.0F);
            float computedValue3 = Mth.clamp((float)timestamp2 / 90.0F, 0.0F, 1.0F);
            float computedValue4 = (1.0F - computedValue2) * (1.0F - computedValue2);
            float sine = 0.72F + 0.28F * (float)Math.sin(element.phase);
            float color = computedValue3 * computedValue4 * sine * element.alphaScale;
            float color2 = element.halfSize * (1.0F + computedValue2 * 0.65F);
            this.sprites.add(new TargetParticleRenderer.Sprite(new Vec3(element.x, element.y, element.z), color2, WriteHexByteHelper.multiplyAlpha(element.color, color)));
         }
      }

      this.renderer.render(this.sprites, 1.65F, true);
   }

   public void clear() {
      this.fragments.clear();
      this.sprites.clear();
   }

   public SpawnFallbackHelper() {
      this.random = new Random();
   }

   static final class Fragment {
      private float phase;
      private long bornAt;
      private float halfSize;
      private float alphaScale;
      private double y;
      private double x;
      private long lifeMillis;
      private float twinkleRate;
      private long holdMillis;
      private double vx;
      private double vy;
      private double z;
      private double vz;
      private int color;

      private Fragment() {
      }
   }
}
