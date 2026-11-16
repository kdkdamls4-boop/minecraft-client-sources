package deluxe.client;

import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class RotationAngles implements MinecraftAccess {
   private final float field14921;
   private final float field14922;

   public float method4362() {
      return this.field14921;
   }

   public RotationAngles(float local7367_1, float local7367_2) {
      this.field14921 = local7367_1;
      this.field14922 = local7367_2;
   }

   public Vec3d method4363() {
      float local7368_1 = this.field14922 * (float) (Math.PI / 180.0);
      float local7368_2 = -this.field14921 * (float) (Math.PI / 180.0);
      float local7368_3 = MathHelper.cos(local7368_2);
      float local7368_4 = MathHelper.sin(local7368_2);
      float local7368_5 = MathHelper.cos(local7368_1);
      float local7368_6 = MathHelper.sin(local7368_1);
      return new Vec3d(local7368_4 * local7368_5, -local7368_6, local7368_3 * local7368_5);
   }

   public static float method4364() {
      return (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT ? -1 : 1) * mc.gameRenderer.getCamera().getPitch();
   }

   public float method4365() {
      return this.field14922;
   }

   public static float method4366() {
      return MathHelper.wrapDegrees(
         mc.gameRenderer.getCamera().getYaw() + (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT ? 180 : 0)
      );
   }
}
