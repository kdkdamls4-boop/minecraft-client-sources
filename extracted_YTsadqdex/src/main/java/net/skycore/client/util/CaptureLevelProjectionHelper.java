package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public final class CaptureLevelProjectionHelper {
   private static boolean levelProjectionCaptured;
   private static final Matrix4f LEVEL_PROJECTION = new Matrix4f();

   public static void captureLevelProjection(Matrix4fc matrix4fc) {
      LEVEL_PROJECTION.set(matrix4fc);
      levelProjectionCaptured = true;
   }

   private CaptureLevelProjectionHelper() {
   }

   public static Matrix4f levelProjectionCopy() {
      Matrix4f matrix;
      if (levelProjectionCaptured) {
         matrix = new Matrix4f(LEVEL_PROJECTION);
      } else {
         matrix = null;
      }

      return matrix;
   }

   public static Vec3 interpolatedPosition(Entity entity, float value) {
      double intermediate = value;
      intermediate = Mth.lerp(intermediate, entity.xOld, entity.getX());
      double intermediate2 = value;
      intermediate2 = Mth.lerp(intermediate2, entity.yOld, entity.getY());
      double intermediate3 = value;
      double zOld = entity.zOld;
      double z = entity.getZ();
      return new Vec3(intermediate, intermediate2, Mth.lerp(intermediate3, zOld, z));
   }

   public static CaptureLevelProjectionHelper.ScreenPoint projectToScreen(Minecraft minecraft, Vec3 position) {
      if (position != null && minecraft.getWindow() != null && minecraft.gameRenderer != null && levelProjectionCaptured) {
         CameraRenderState cameraRenderState = minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
         if (!cameraRenderState.initialized) {
            return null;
         }

         int guiScaledWidth = minecraft.getWindow().getGuiScaledWidth();
         int guiScaledHeight = minecraft.getWindow().getGuiScaledHeight();
         if (guiScaledWidth > 0 && guiScaledHeight > 0) {
            Vector4f vector4f = new Vector4f((float)(position.x - cameraRenderState.pos.x()), (float)(position.y - cameraRenderState.pos.y()), (float)(position.z - cameraRenderState.pos.z()), 1.0F);
            cameraRenderState.viewRotationMatrix.transform(vector4f);
            LEVEL_PROJECTION.transform(vector4f);
            if (vector4f.w <= 1.0E-4F) {
               return null;
            } else {
               float x = vector4f.x / vector4f.w;
               float y = vector4f.y / vector4f.w;
               if (!(Math.abs(x) > 2.0F) && !(Math.abs(y) > 2.0F)) {
                  float computedValue = (x + 1.0F) * 0.5F * guiScaledWidth;
                  float computedValue2 = (1.0F - y) * 0.5F * guiScaledHeight;
                  return new CaptureLevelProjectionHelper.ScreenPoint(computedValue, computedValue2);
               } else {
                  return null;
               }
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public record ScreenPoint(float x, float y) {
      public float y() {
         return this.y;
      }

      public float x() {
         return this.x;
      }
   }
}
