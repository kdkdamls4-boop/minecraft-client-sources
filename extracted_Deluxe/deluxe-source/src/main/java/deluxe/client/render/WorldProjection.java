package deluxe.client.render;

import deluxe.client.MinecraftAccess;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class WorldProjection implements MinecraftAccess {
   private static final Matrix4f field03080 = new Matrix4f();
   private static final Matrix4f field03081 = new Matrix4f();
   private static final Matrix4f field03082 = new Matrix4f();
   private static final FrustumIntersection field03083 = new FrustumIntersection();

   public static Vector4f method1119(Box box, float local1540_1, float local1540_2) {
      if (mc.gameRenderer == null) {
         return null;
      }

      float local1540_3 = Float.MAX_VALUE;
      float local1540_4 = Float.MAX_VALUE;
      float local1540_5 = -Float.MAX_VALUE;
      float local1540_6 = -Float.MAX_VALUE;
      boolean local1540_7 = false;
      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      Vector4f vector4f = new Vector4f();

      for (int index = 0; index < 8; index++) {
         vector4f.set(
            (float)((index & 1) == 0 ? box.minX : box.maxX) - (float)cameraPos.x,
            (float)((index & 2) == 0 ? box.minY : box.maxY) - (float)cameraPos.y,
            (float)((index & 4) == 0 ? box.minZ : box.maxZ) - (float)cameraPos.z,
            1.0F
         );
         field03082.transform(vector4f);
         if (!(vector4f.w <= 0.05F)) {
            local1540_7 = true;
            local1540_3 = Math.min(local1540_3, (vector4f.x / vector4f.w * 0.5F + 0.5F) * local1540_1);
            local1540_5 = Math.max(local1540_5, (vector4f.x / vector4f.w * 0.5F + 0.5F) * local1540_1);
            local1540_4 = Math.min(local1540_4, (0.5F - vector4f.y / vector4f.w * 0.5F) * local1540_2);
            local1540_6 = Math.max(local1540_6, (0.5F - vector4f.y / vector4f.w * 0.5F) * local1540_2);
         }
      }

      return local1540_7 ? new Vector4f(local1540_3, local1540_4, local1540_5 - local1540_3, local1540_6 - local1540_4) : null;
   }

   public static Matrix4f method1120() {
      return field03081;
   }

   public static boolean method1121(Box box) {
      if (mc.gameRenderer == null) {
         return true;
      }

      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      return field03083.testAab(
         (float)(box.minX - cameraPos.x),
         (float)(box.minY - cameraPos.y),
         (float)(box.minZ - cameraPos.z),
         (float)(box.maxX - cameraPos.x),
         (float)(box.maxY - cameraPos.y),
         (float)(box.maxZ - cameraPos.z)
      );
   }

   public static Vec3d method1122(Vec3d vec3d, float local1543_1, float local1543_2) {
      if (mc.gameRenderer == null) {
         return null;
      }

      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      Vector4f vector4f = field03082.transform(
         new Vector4f((float)(vec3d.x - cameraPos.x), (float)(vec3d.y - cameraPos.y), (float)(vec3d.z - cameraPos.z), 1.0F)
      );
      return vector4f.w <= 0.05F
         ? null
         : new Vec3d((vector4f.x / vector4f.w * 0.5F + 0.5F) * local1543_1, (0.5F - vector4f.y / vector4f.w * 0.5F) * local1543_2, vector4f.w);
   }

   public static void method1123(Matrix4f matrix4f, Matrix4f matrix4f2) {
      field03080.set(matrix4f);
      field03081.set(matrix4f2);
      field03082.set(matrix4f2).mul(matrix4f);
      field03083.set(field03082);
   }

   public static boolean method1124(double local1545_0, double local1545_2, double local1545_4, float local1545_6) {
      if (mc.gameRenderer == null) {
         return true;
      }

      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      return field03083.testSphere(
         (float)(local1545_0 - cameraPos.x), (float)(local1545_2 - cameraPos.y), (float)(local1545_4 - cameraPos.z), local1545_6
      );
   }

   public static Vec3d method1125(Vec3d vec3d, float local1546_1, float local1546_2) {
      Vec3d vec3d2 = method1122(vec3d, local1546_1, local1546_2);
      return vec3d2 != null
            && !(vec3d2.x < -local1546_1 * 0.25F)
            && !(vec3d2.x > local1546_1 * 1.25F)
            && !(vec3d2.y < -local1546_2 * 0.25F)
            && !(vec3d2.y > local1546_2 * 1.25F)
         ? vec3d2
         : null;
   }

   public static Matrix4f method1126() {
      return field03080;
   }
}
