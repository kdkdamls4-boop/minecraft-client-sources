package net.skycore.client.combat;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Rotation {
   private final float pitch;
   private final float yaw;
   public static final Rotation ZERO = new Rotation(0.0F, 0.0F);

   public static Rotation lookingAt(Vec3 position, Vec3 position2) {
      Vec3 position3 = position.subtract(position2);
      float angle = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(position3.z, position3.x)) - 90.0);
      double x = Math.atan2(position3.y, Math.hypot(position3.x, position3.z));
      float angle2 = (float)Mth.wrapDegrees(-Math.toDegrees(x));
      return new Rotation(angle, angle2);
   }

   public Rotation(float yaw2, float pitch2) {
      this.yaw = yaw2;
      this.pitch = pitch2;
   }

   public float getDelta(Rotation rotation) {
      double y = Mth.wrapDegrees(rotation.yaw - this.yaw);
      double intermediate = rotation.pitch - this.pitch;
      return (float)Math.hypot(y, intermediate);
   }

   public float getPitch() {
      return this.pitch;
   }

   public Vec3 toVector() {
      return Vec3.directionFromRotation(this.pitch, this.yaw);
   }

   public float getXRot() {
      return this.pitch;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getYRot() {
      return this.yaw;
   }
}
