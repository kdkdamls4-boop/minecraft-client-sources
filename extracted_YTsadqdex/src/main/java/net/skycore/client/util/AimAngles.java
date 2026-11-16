package net.skycore.client.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

public final class AimAngles {
   public static Vector2f aim(Vec3 position, Vec3 position2) {
      Vec3 position3 = position2.subtract(position);
      double x = Math.hypot(position3.x, position3.z);
      float x2 = (float)(Mth.atan2(position3.z, position3.x) * 180.0 / Math.PI) - 90.0F;
      float y = (float)(-(Mth.atan2(position3.y, x) * 180.0 / Math.PI));
      x2 = Mth.wrapDegrees(x2);
      y = (float)Mth.clamp(y, -90.0, 90.0);
      return new Vector2f(x2, y);
   }

   private AimAngles() {
   }
}
