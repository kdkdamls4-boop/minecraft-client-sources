package net.skycore.client.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Vector2f;

public final class FromFreeLookHelper {
   private float yaw;
   private float pitch;

   public static FromFreeLookHelper fromFreeLook() {
      return SnapBodyToFreeLookHelper.snapshot();
   }

   public FromFreeLookHelper(Entity entity) {
      this(entity.getYRot(), entity.getXRot());
   }

   public FromFreeLookHelper(Vector2f angles) {
      this(angles.x, angles.y);
   }

   public FromFreeLookHelper(float yaw2, float pitch2) {
      this.yaw = yaw2;
      this.pitch = pitch2;
   }

   public double angleDistance(FromFreeLookHelper fromFreeLookHelper) {
      float y = fromFreeLookHelper.yaw - this.yaw;
      double computedValue = Mth.wrapDegrees(y);
      double computedValue2 = Mth.wrapDegrees(fromFreeLookHelper.pitch - this.pitch);
      return Math.hypot(Math.abs(computedValue), Math.abs(computedValue2));
   }

   public float pitch() {
      return this.pitch;
   }

   public float yaw() {
      return this.yaw;
   }
}
