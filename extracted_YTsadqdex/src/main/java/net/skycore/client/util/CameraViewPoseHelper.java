package net.skycore.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public final class CameraViewPoseHelper {
   public static Matrix4f cameraViewPose(Camera camera) {
      return camera.getViewRotationMatrix(new Matrix4f());
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

   public static Matrix4f buildBillboardPose(Camera camera, Vec3 position, double value, float angle) {
      Vec3 position2 = camera.position();
      PoseStack poseStack = new PoseStack();
      Axis xP = Axis.XP;
      float angle2 = camera.xRot();
      poseStack.mulPose(xP.rotationDegrees(angle2));
      Axis yP = Axis.YP;
      float angle3 = camera.yRot() + 180.0F;
      Quaternionf rotation = yP.rotationDegrees(angle3);
      poseStack.mulPose(rotation);
      double x = position.x - position2.x;
      double y = position.y - position2.y + value;
      double z = position.z - position2.z;
      poseStack.translate(x, y, z);
      Quaternionf rotation2 = Axis.YP.rotationDegrees(-camera.yRot());
      poseStack.mulPose(rotation2);
      Axis xP2 = Axis.XP;
      float angle4 = camera.xRot();
      Quaternionf rotation3 = xP2.rotationDegrees(angle4);
      poseStack.mulPose(rotation3);
      if (angle != 0.0F) {
         Quaternionf rotation4 = Axis.ZP.rotationDegrees(angle);
         poseStack.mulPose(rotation4);
      }

      Pose pose = poseStack.last();
      return new Matrix4f(pose.pose());
   }

   private CameraViewPoseHelper() {
   }

   public static Vector4f toViewSpace(Vec3 position, Vec3 position2, Matrix4f matrix) {
      float x = (float)(position.x - position2.x);
      float y = (float)(position.y - position2.y);
      float z = (float)(position.z - position2.z);
      return new Vector4f(x, y, z, 1.0F).mul(matrix);
   }
}
