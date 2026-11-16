package net.skycore.client.combat;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class AimNoise {
   public static float jitterYaw(Vec3 position) {
      float value = randomRange(0.0F, 3.0F);
      float value2 = calcYaw(position);
      float computedValue = -value;
      return value2 + randomRange(computedValue, value);
   }

   public static float jitterPitch(Vec3 position) {
      float value = randomRange(0.0F, 2.0F);
      return calcPitch(position) + randomRange(-value, value);
   }

   private AimNoise() {
   }

   public static float calcYaw(Vec3 position) {
      return (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(position.z, position.x)) - 90.0);
   }

   public static void update(Vec3 position) {
      if (position != null) {
         float angle = jitterYaw(position);
         float angle2 = jitterPitch(position);
         RotationManager.update(new Rotation(angle, angle2), 960.0F, 1.0F, 6, 1);
      }
   }

   public static float calcPitch(Vec3 position) {
      return (float)(-Math.toDegrees(Math.atan2(position.y, Math.hypot(position.x, position.z))));
   }

   private static float randomRange(float value, float value2) {
      return value >= value2 ? value : value + ThreadLocalRandom.current().nextFloat() * (value2 - value);
   }
}
