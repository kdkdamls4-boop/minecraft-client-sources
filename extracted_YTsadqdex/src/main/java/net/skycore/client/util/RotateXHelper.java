package net.skycore.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class RotateXHelper {
   private static Quaternionf rotationQuaternion;

   public static void shear(PoseStack poseStack, double value, double value2, double value3) {
      Matrix4f matrix = new Matrix4f(
         1.0F, (float)value, (float)value, 0.0F, (float)value2, 1.0F, (float)value2, 0.0F, (float)value3, (float)value3, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F
      );
      poseStack.last().pose().mul(matrix);
   }

   public static void rotateYAround(PoseStack poseStack, double value, double value2, double value3, double value4) {
      float angle = (float)value * (float) (Math.PI / 180.0);
      poseStack.rotateAround(rotationQuaternion.rotationY(angle), (float)value2, (float)value3, (float)value4);
   }

   public static void translate(PoseStack poseStack, double value, double value2, double value3) {
      poseStack.translate(value, value2, value3);
   }

   public static double ceil(double value) {
      return Math.ceil(value);
   }

   public static double round(double value) {
      return Math.round(value);
   }

   public static void rotateZAround(PoseStack poseStack, double value, double value2, double value3, double value4) {
      float angle = (float)value * (float) (Math.PI / 180.0);
      Quaternionf rotation = rotationQuaternion.rotationZ(angle);
      float value5 = (float)value2;
      float intermediate = (float)value3;
      float value6 = (float)value4;
      poseStack.rotateAround(rotation, value5, intermediate, value6);
   }

   public static void pushPose(PoseStack poseStack) {
      poseStack.pushPose();
   }

   public static double pow(double value, double value2) {
      return Math.pow(value, value2);
   }

   public static double cos(double value) {
      return Math.cos(value);
   }

   public static void rotateY(PoseStack poseStack, double value) {
      poseStack.mulPose(rotationQuaternion.rotationY((float)value * (float) (Math.PI / 180.0)));
   }

   public static double clamp(double value, double value2, double value3) {
      return Math.clamp(value, value2, value3);
   }

   public static void rotateZ(PoseStack poseStack, double value) {
      Quaternionf rotation = rotationQuaternion.rotationZ((float)value * (float) (Math.PI / 180.0));
      poseStack.mulPose(rotation);
   }

   private RotateXHelper() {
   }

   public static void scale(PoseStack poseStack, double value, double value2, double value3) {
      float value4 = (float)value;
      float intermediate = (float)value2;
      float intermediate2 = (float)value3;
      poseStack.scale(value4, intermediate, intermediate2);
   }

   public static void popPose(PoseStack poseStack) {
      poseStack.popPose();
   }

   public static void resetQuaternion() {
      rotationQuaternion = new Quaternionf();
   }

   public static void translateY(PoseStack poseStack, double value) {
      poseStack.translate(0.0, value, 0.0);
   }

   public static void translateX(PoseStack poseStack, double value) {
      poseStack.translate(value, 0.0, 0.0);
   }

   public static double abs(double value) {
      return Math.abs(value);
   }

   public static void rotateX(PoseStack poseStack, double value) {
      poseStack.mulPose(rotationQuaternion.rotationX((float)value * (float) (Math.PI / 180.0)));
   }

   public static double sin(double value) {
      return Math.sin(value);
   }

   public static double lerp(double value, double value2, double value3) {
      return Mth.lerp(value, value2, value3);
   }

   public static void rotateXAround(PoseStack poseStack, double value, double value2, double value3, double value4) {
      float angle = (float)value * (float) (Math.PI / 180.0);
      poseStack.rotateAround(rotationQuaternion.rotationX(angle), (float)value2, (float)value3, (float)value4);
   }

   public static void translateZ(PoseStack poseStack, double value) {
      poseStack.translate(0.0, 0.0, value);
   }

   public static double floor(double value) {
      return Math.floor(value);
   }

   static {
      resetQuaternion();
   }
}
