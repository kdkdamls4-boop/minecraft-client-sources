package net.skycore.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class AddBackVertexHelper {
   private static final int PART_COUNT = 16;
   private static final Identifier CAPE_TEXTURE = Identifier.parse("skycore:textures/cape.png");

   private static double getAngle(CapeVector3 normalizeHelper3, CapeVector3 normalizeHelper32) {
      CapeVector3 normalizeHelper33 = normalizeHelper32.clone();
      CapeVector3 normalizeHelper34 = normalizeHelper33.subtract(normalizeHelper3);
      double x = normalizeHelper34.x;
      double angleRadians = Math.atan2(x, normalizeHelper34.y);
      return Math.toDegrees(angleRadians) + 180.0;
   }

   private static void addBackVertex(
      List<AddBackVertexHelper.Vert> verts, Matrix4f matrix, Matrix4f matrix2, float value, float value2, float value3, float value4, float value5, float value6, int value7
   ) {
      if (value < value4) {
         float value8 = value;
         value = value4;
         value4 = value8;
      }

      if (value2 < value5) {
         float value9 = value2;
         value2 = value5;
         value5 = value9;
         Matrix4f matrix3 = matrix;
         matrix = matrix2;
         matrix2 = matrix3;
      }

      float value10 = 0.015625F;
      float value11 = 0.171875F;
      float value12 = 0.03125F;
      float value13 = 0.53125F;
      float computedValue = value13 - value12;
      float computedValue2 = computedValue / 16.0F;
      value13 = value12 + computedValue2 * (value7 + 1);
      value12 += computedValue2 * value7;
      put(verts, matrix2, value, value5, value3, value11, value12, 1.0F, 0.0F, 0.0F);
      put(verts, matrix2, value4, value5, value3, value10, value12, 1.0F, 0.0F, 0.0F);
      put(verts, matrix, value4, value2, value6, value10, value13, 1.0F, 0.0F, 0.0F);
      put(verts, matrix, value, value2, value6, value11, value13, 1.0F, 0.0F, 0.0F);
   }

   private static void modifyPoseStackSimulation(PoseStack poseStack, AbstractClientPlayer player, float angle, int angle2) {
      PreventHardBendsHelper simulation = ((UpdateSimulationHelper)player).getSimulation();
      if (simulation == null || simulation.getPoints().isEmpty() || simulation.getPoints().size() <= angle2) {
         if (simulation != null && simulation.getPoints().isEmpty()) {
            simulation.init(16);
         }

         if (simulation == null || simulation.getPoints().isEmpty() || simulation.getPoints().size() <= angle2) {
            modifyPoseStackVanilla(poseStack, player, angle, angle2);
            return;
         }
      }

      poseStack.pushPose();
      double itemBySlot = player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() ? 0.125 : 0.15;
      poseStack.translate(0.0, 0.0, itemBySlot);
      PreventHardBendsHelper.Point points = simulation.getPoints().get(0);
      float lerpX = simulation.getPoints().get(angle2).getLerpX(angle) - points.getLerpX(angle);
      if (lerpX > 0.0F) {
         lerpX = 0.0F;
      }

      float lerpY = points.getLerpY(angle) - angle2 - simulation.getPoints().get(angle2).getLerpY(angle);
      float lerpZ = points.getLerpZ(angle) - simulation.getPoints().get(angle2).getLerpZ(angle);
      float rotation = getRotation(angle, angle2, simulation);
      float value = 0.0F;
      if (player.isCrouching()) {
         value += 25.0F;
         poseStack.translate(0.0, 0.15F, 0.0);
      }

      float value2 = getNaturalWindSwing(angle2, player.isUnderWater());
      poseStack.mulPose(Axis.XP.rotationDegrees(6.0F + value + value2));
      poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      poseStack.translate(-lerpZ / 16.0F, lerpY / 16.0F, lerpX / 16.0F);
      poseStack.translate(0.0, 0.03, -0.03);
      poseStack.translate(0.0, angle2 * 1.0F / 16.0F, 0.0);
      poseStack.mulPose(Axis.XP.rotationDegrees(-rotation));
      poseStack.translate(0.0, -angle2 * 1.0F / 16.0F, 0.0);
      poseStack.translate(0.0, -0.03, 0.03);
   }

   private static void addLeftVertex(
      List<AddBackVertexHelper.Vert> verts, Matrix4f matrix, Matrix4f matrix2, float value, float value2, float value3, float value4, float value5, float value6, int value7
   ) {
      if (value < value4) {
         value4 = value;
      }

      if (value2 < value5) {
         float value8 = value2;
         value2 = value5;
         value5 = value8;
      }

      float value9 = 0.0F;
      float value10 = 0.015625F;
      float value11 = 0.03125F;
      float value12 = 0.53125F;
      float computedValue = value12 - value11;
      float computedValue2 = computedValue / 16.0F;
      value12 = value11 + computedValue2 * (value7 + 1);
      value11 += computedValue2 * value7;
      put(verts, matrix, value4, value2, value3, value10, value12, 1.0F, 0.0F, 0.0F);
      put(verts, matrix, value4, value2, value6, value9, value12, 1.0F, 0.0F, 0.0F);
      put(verts, matrix2, value4, value5, value6, value9, value11, 1.0F, 0.0F, 0.0F);
      put(verts, matrix2, value4, value5, value3, value10, value11, 1.0F, 0.0F, 0.0F);
   }

   private static void modifyPoseStackVanilla(PoseStack poseStack, AbstractClientPlayer player, float value, int value2) {
      poseStack.pushPose();
      poseStack.translate(0.0, 0.0, 0.125);
      ClientAvatarState clientAvatarState = player.avatarState();
      double interpolatedCloakX = clientAvatarState.getInterpolatedCloakX(value) - Mth.lerp(value, player.xo, player.getX());
      double interpolatedCloakY = clientAvatarState.getInterpolatedCloakY(value) - Mth.lerp(value, player.yo, player.getY());
      double interpolatedCloakZ = clientAvatarState.getInterpolatedCloakZ(value) - Mth.lerp(value, player.zo, player.getZ());
      float y = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO);
      double computedValue = Mth.sin(y * (float) (Math.PI / 180.0));
      double computedValue2 = -Mth.cos(y * (float) (Math.PI / 180.0));
      float elapsed = (float)interpolatedCloakY * 10.0F;
      elapsed = Mth.clamp(elapsed, -6.0F, 32.0F);
      float computedValue3 = (float)(interpolatedCloakX * computedValue + interpolatedCloakZ * computedValue2) * easeOutSine(0.0625F * value2) * 100.0F;
      computedValue3 = Mth.clamp(computedValue3, 0.0F, 150.0F * easeOutSine(0.0625F * value2));
      float computedValue4 = (float)(interpolatedCloakX * computedValue2 - interpolatedCloakZ * computedValue) * 100.0F;
      computedValue4 = Mth.clamp(computedValue4, -20.0F, 20.0F);
      double deltaMovement = player.getDeltaMovement().horizontalDistance();
      float minimum = (float)Math.min(deltaMovement * 4.0, 1.0);
      elapsed += Mth.sin((float)(System.currentTimeMillis() / 100.0) * 6.0F) * 32.0F * minimum;
      if (player.isCrouching()) {
         elapsed += 25.0F;
         poseStack.translate(0.0, 0.15F, 0.0);
      }

      float value3 = getNaturalWindSwing(value2, player.isUnderWater());
      poseStack.mulPose(Axis.XP.rotationDegrees(6.0F + computedValue3 / 2.0F + elapsed + value3));
      poseStack.mulPose(Axis.ZP.rotationDegrees(computedValue4 / 2.0F));
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - computedValue4 / 2.0F));
   }

   private static float easeOutSine(float progress) {
      double computedValue = progress * Math.PI / 2.0;
      return (float)Math.sin(computedValue);
   }

   private static void addTopVertex(
      List<AddBackVertexHelper.Vert> verts, Matrix4f matrix, Matrix4f matrix2, float value, float value2, float value3, float value4, float value5, float value6, int value7
   ) {
      if (value < value4) {
         float value8 = value;
         value = value4;
         value4 = value8;
      }

      if (value2 < value5) {
         float value9 = value2;
         value2 = value5;
         value5 = value9;
      }

      float value10 = 0.015625F;
      float value11 = 0.171875F;
      float value12 = 0.0F;
      float value13 = 0.03125F;
      float computedValue = value13 - value12;
      float computedValue2 = computedValue / 16.0F;
      value13 = value12 + computedValue2 * (value7 + 1);
      value12 += computedValue2 * value7;
      put(verts, matrix2, value, value5, value3, value11, value13, 0.0F, 1.0F, 0.0F);
      put(verts, matrix2, value4, value5, value3, value10, value13, 0.0F, 1.0F, 0.0F);
      put(verts, matrix, value4, value2, value6, value10, value12, 0.0F, 1.0F, 0.0F);
      put(verts, matrix, value, value2, value6, value11, value12, 0.0F, 1.0F, 0.0F);
   }

   private static void put(
      List<AddBackVertexHelper.Vert> verts, Matrix4f matrix, float value, float value2, float value3, float value4, float value5, float value6, float value7, float value8
   ) {
      Vector3f vector = new Vector3f();
      Vector3f vector2 = matrix.transformPosition(value, value2, value3, vector);
      AddBackVertexHelper.Vert vert = new AddBackVertexHelper.Vert(vector2.x, vector2.y, vector2.z, value4, value5, value6, value7, value8);
      verts.add(vert);
   }

   private static void modifyPoseStack(PoseStack poseStack, AbstractClientPlayer player, float value, int value2) {
      if (CapeSimulationManager.config.capeMovement == CapeMovementMode.BASIC_SIMULATION) {
         modifyPoseStackSimulation(poseStack, player, value, value2);
      } else {
         modifyPoseStackVanilla(poseStack, player, value, value2);
      }
   }

   public static void render(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, AbstractClientPlayer player, float coordinate, int coordinate2) {
      Matrix4f matrix = new Matrix4f(poseStack.last().pose());
      Matrix4f matrix2 = new Matrix4f(matrix).invert();
      ArrayList elements = new ArrayList(384);
      Matrix4f matrix3 = null;

      for (int value = 0; value < 16; value++) {
         modifyPoseStack(poseStack, player, coordinate, value);
         Matrix4f matrix4 = new Matrix4f(poseStack.last().pose());
         if (matrix3 == null) {
            matrix3 = new Matrix4f(matrix4);
         }

         Matrix4f matrix5 = new Matrix4f(matrix2).mul(matrix4);
         Matrix4f matrix6 = new Matrix4f(matrix2).mul(matrix3);
         if (value == 0) {
            addTopVertex(elements, matrix5, matrix6, 0.3F, 0.0F, 0.0F, -0.3F, 0.0F, -0.06F, value);
         }

         if (value == 15) {
            addBottomVertex(elements, matrix5, matrix5, 0.3F, (value + 1) * 0.06F, 0.0F, -0.3F, (value + 1) * 0.06F, -0.06F, value);
         }

         addLeftVertex(elements, matrix5, matrix6, -0.3F, (value + 1) * 0.06F, 0.0F, -0.3F, value * 0.06F, -0.06F, value);
         addRightVertex(elements, matrix5, matrix6, 0.3F, (value + 1) * 0.06F, 0.0F, 0.3F, value * 0.06F, -0.06F, value);
         addBackVertex(elements, matrix5, matrix6, 0.3F, (value + 1) * 0.06F, -0.06F, -0.3F, value * 0.06F, -0.06F, value);
         addFrontVertex(elements, matrix6, matrix5, 0.3F, (value + 1) * 0.06F, 0.0F, -0.3F, value * 0.06F, 0.0F, value);
         matrix3 = matrix4;
         poseStack.popPose();
      }

      submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(CAPE_TEXTURE), (pose, vertexConsumer) -> {
         Iterator iterator = elements.iterator();

         while (true) {
            if (!iterator.hasNext()) {
               return;
            }

            AddBackVertexHelper.Vert element = (AddBackVertexHelper.Vert)iterator.next();
            write(vertexConsumer, pose, element, coordinate2);
         }
      });
   }

   private static void addRightVertex(
      List<AddBackVertexHelper.Vert> verts, Matrix4f matrix, Matrix4f matrix2, float value, float value2, float value3, float value4, float value5, float value6, int value7
   ) {
      if (value < value4) {
         value4 = value;
      }

      if (value2 < value5) {
         float value8 = value2;
         value2 = value5;
         value5 = value8;
      }

      float value9 = 0.171875F;
      float value10 = 0.1875F;
      float value11 = 0.03125F;
      float value12 = 0.53125F;
      float computedValue = value12 - value11;
      float computedValue2 = computedValue / 16.0F;
      value12 = value11 + computedValue2 * (value7 + 1);
      value11 += computedValue2 * value7;
      put(verts, matrix, value4, value2, value6, value9, value12, 1.0F, 0.0F, 0.0F);
      put(verts, matrix, value4, value2, value3, value10, value12, 1.0F, 0.0F, 0.0F);
      put(verts, matrix2, value4, value5, value3, value10, value11, 1.0F, 0.0F, 0.0F);
      put(verts, matrix2, value4, value5, value6, value9, value11, 1.0F, 0.0F, 0.0F);
   }

   private static void write(VertexConsumer vertexConsumer, Pose pose, AddBackVertexHelper.Vert vert, int value) {
      VertexConsumer vertexConsumer2 = vertexConsumer.addVertex(pose, vert.x, vert.y, vert.z).setColor(1.0F, 1.0F, 1.0F, 1.0F);
      VertexConsumer vertexConsumer3 = vertexConsumer2.setUv(vert.u, vert.v);
      VertexConsumer vertexConsumer4 = vertexConsumer3.setOverlay(OverlayTexture.NO_OVERLAY).setLight(value);
      vertexConsumer4.setNormal(pose, vert.nx, vert.ny, vert.nz);
   }

   private static float getNaturalWindSwing(int value, boolean condition) {
      long timestamp = System.currentTimeMillis() / (condition ? 9 : 3) % 360L;
      float computedValue = (value + 1) / 16.0F;
      float result;
      if (CapeSimulationManager.config.windMode == CapeWindMode.WAVES) {
         double computedValue2 = computedValue * 360.0F - (float)timestamp;
         double angleRadians = Math.toRadians(computedValue2);
         result = (float)(Math.sin(angleRadians) * 3.0);
      } else {
         result = 0.0F;
      }

      return result;
   }

   private AddBackVertexHelper() {
   }

   private static void addBottomVertex(
      List<AddBackVertexHelper.Vert> verts, Matrix4f matrix, Matrix4f matrix2, float value, float value2, float value3, float value4, float value5, float value6, int value7
   ) {
      if (value < value4) {
         float value8 = value;
         value = value4;
         value4 = value8;
      }

      if (value2 < value5) {
         float value9 = value2;
         value2 = value5;
         value5 = value9;
      }

      float value10 = 0.171875F;
      float value11 = 0.328125F;
      float value12 = 0.0F;
      float value13 = 0.03125F;
      float computedValue = value13 - value12;
      float computedValue2 = computedValue / 16.0F;
      value13 = value12 + computedValue2 * (value7 + 1);
      value12 += computedValue2 * value7;
      put(verts, matrix2, value, value5, value6, value11, value12, 1.0F, 0.0F, 0.0F);
      put(verts, matrix2, value4, value5, value6, value10, value12, 1.0F, 0.0F, 0.0F);
      put(verts, matrix, value4, value2, value3, value10, value13, 1.0F, 0.0F, 0.0F);
      put(verts, matrix, value, value2, value3, value11, value13, 1.0F, 0.0F, 0.0F);
   }

   private static void addFrontVertex(
      List<AddBackVertexHelper.Vert> verts, Matrix4f matrix, Matrix4f matrix2, float value, float value2, float value3, float value4, float value5, float value6, int value7
   ) {
      if (value < value4) {
         float value8 = value;
         value = value4;
         value4 = value8;
      }

      if (value2 < value5) {
         float value9 = value2;
         value2 = value5;
         value5 = value9;
         Matrix4f matrix3 = matrix;
         matrix = matrix2;
         matrix2 = matrix3;
      }

      float value10 = 0.1875F;
      float value11 = 0.34375F;
      float value12 = 0.03125F;
      float value13 = 0.53125F;
      float computedValue = value13 - value12;
      float computedValue2 = computedValue / 16.0F;
      value13 = value12 + computedValue2 * (value7 + 1);
      value12 += computedValue2 * value7;
      put(verts, matrix2, value, value2, value3, value11, value13, 1.0F, 0.0F, 0.0F);
      put(verts, matrix2, value4, value2, value3, value10, value13, 1.0F, 0.0F, 0.0F);
      put(verts, matrix, value4, value5, value6, value10, value12, 1.0F, 0.0F, 0.0F);
      put(verts, matrix, value, value5, value6, value11, value12, 1.0F, 0.0F, 0.0F);
   }

   private static float getRotation(float angle, int value, PreventHardBendsHelper preventHardBendsHelper) {
      if (preventHardBendsHelper == null || preventHardBendsHelper.points == null || preventHardBendsHelper.points.isEmpty()) {
         return 0.0F;
      } else if (value == 15) {
         return getRotation(angle, value - 1, preventHardBendsHelper);
      } else if (value >= 0 && value + 1 < preventHardBendsHelper.points.size()) {
         CapeVector3 lerpedPos = preventHardBendsHelper.points.get(value).getLerpedPos(angle);
         int intermediate = value + 1;
         PreventHardBendsHelper.Point point = preventHardBendsHelper.points.get(intermediate);
         CapeVector3 lerpedPos2 = point.getLerpedPos(angle);
         return (float)getAngle(lerpedPos, lerpedPos2);
      } else {
         return 0.0F;
      }
   }

   public record Vert(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
      public float u() {
         return this.u;
      }

      public float nz() {
         return this.nz;
      }

      public float ny() {
         return this.ny;
      }

      public float y() {
         return this.y;
      }

      public float nx() {
         return this.nx;
      }

      public float v() {
         return this.v;
      }

      public float x() {
         return this.x;
      }

      public float z() {
         return this.z;
      }
   }
}
