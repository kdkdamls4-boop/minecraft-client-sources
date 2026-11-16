package net.skycore.client.util;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import net.minecraft.util.Mth;

public final class ElapsedMillisHelper {
   private long startNanos;
   private float toValue;
   private ElapsedMillisHelper.Easing easing;
   private long durationMillis;
   private boolean reverse;
   private float fromValue;

   public float getValue() {
      float value = this.progress();
      if (this.reverse) {
         value = 1.0F - value;
      }

      float value2 = this.easing.ease(value);
      return Mth.lerp(value2, this.fromValue, this.toValue);
   }

   public ElapsedMillisHelper(long timestamp, ElapsedMillisHelper.Easing easing2) {
      this.setDuration(timestamp);
      this.setEasing(easing2);
      this.reset();
   }

   private void setDuration(long durationMillis2) {
      if (durationMillis2 < 0L) {
         throw new IllegalArgumentException("Animation duration cannot be negative");
      }

      this.durationMillis = durationMillis2;
   }

   private long elapsedMillis() {
      TimeUnit nANOSECONDS = TimeUnit.NANOSECONDS;
      long nowNanos = System.nanoTime();
      return nANOSECONDS.toMillis(Math.max(0L, nowNanos - this.startNanos));
   }

   public boolean isFinished() {
      long timestamp = this.elapsedMillis();
      return timestamp >= this.durationMillis;
   }

   public void reset() {
      this.startNanos = System.nanoTime();
   }

   private float progress() {
      if (this.durationMillis == 0L) {
         return 1.0F;
      }

      float computedValue = (float)this.elapsedMillis() / (float)this.durationMillis;
      return Mth.clamp(computedValue, 0.0F, 1.0F);
   }

   public void setReverse(boolean reverse2) {
      if (this.reverse != reverse2) {
         long timestamp = this.elapsedMillis();
         long minimum = Math.min(timestamp, this.durationMillis);
         this.reverse = reverse2;
         long nowNanos = System.nanoTime();
         long timestamp2 = this.durationMillis - minimum;
         this.startNanos = nowNanos - TimeUnit.MILLISECONDS.toNanos(timestamp2);
      }
   }

   public void animate(float fromValue2, float toValue2, long timestamp, ElapsedMillisHelper.Easing easing2) {
      this.fromValue = fromValue2;
      this.toValue = toValue2;
      this.setDuration(timestamp);
      this.setEasing(easing2);
      this.reverse = false;
      this.reset();
   }

   private void setEasing(ElapsedMillisHelper.Easing easing2) {
      ElapsedMillisHelper.Easing easing3 = Objects.requireNonNull(easing2);
      this.easing = easing3;
   }

   enum Easing {
      LINEAR {
         @Override
         public float ease(float progress) {
            return progress;
         }
      },
      EASE_IN_QUAD {
         @Override
         public float ease(float progress) {
            return progress * progress;
         }
      },
      EASE_OUT_QUAD {
         @Override
         public float ease(float progress) {
            return 1.0F - (1.0F - progress) * (1.0F - progress);
         }
      },
      EASE_OUT_CUBIC {
         @Override
         public float ease(float progress) {
            float computedValue = 1.0F - progress;
            return 1.0F - computedValue * computedValue * computedValue;
         }
      },
      EASE_OUT_EXPO {
         @Override
         public float ease(float progress) {
            return progress >= 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, -10.0F * progress);
         }
      },
      EASE_IN_OUT_QUAD {
         @Override
         public float ease(float progress) {
            return progress < 0.5F ? 2.0F * progress * progress : 1.0F - (float)Math.pow(-2.0F * progress + 2.0F, 2.0) / 2.0F;
         }
      };

      public abstract float ease(float progress);
   }
}
