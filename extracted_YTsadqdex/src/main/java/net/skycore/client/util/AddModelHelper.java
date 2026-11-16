package net.skycore.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.model.geom.ModelPart.Polygon;
import net.minecraft.client.model.geom.ModelPart.Vertex;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.mixin.LivingEntityRendererInvokeSetupRotationsAccessor;
import net.skycore.client.mixin.ModelPartGetCubesAccessor;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class AddModelHelper {
   public static void addModel(LivingEntity entity, Vec3 position, float value, int alpha, int value2, float value3) {
      if (entity != null && position != null) {
         Minecraft instance = Minecraft.getInstance();
         EntityRenderer renderer = instance.getEntityRenderDispatcher().getRenderer(entity);
         if (renderer instanceof LivingEntityRenderer livingEntityRenderer) {
            if (renderer.createRenderState(entity, value) instanceof LivingEntityRenderState livingEntityRenderState) {
               EntityModel model = livingEntityRenderer.getModel();
               model.setupAnim(livingEntityRenderState);
               PoseStack poseStack = new PoseStack();
               if (livingEntityRenderState.hasPose(Pose.SLEEPING) && livingEntityRenderState.bedOrientation != null) {
                  float computedValue = livingEntityRenderState.eyeHeight - 0.1F;
                  poseStack.translate(-livingEntityRenderState.bedOrientation.getStepX() * computedValue, 0.0F, -livingEntityRenderState.bedOrientation.getStepZ() * computedValue);
               }

               float scale = livingEntityRenderState.scale;
               poseStack.scale(scale, scale, scale);
               LivingEntityRendererInvokeSetupRotationsAccessor livingEntityRendererInvokeSetupRotationsAccessor = (LivingEntityRendererInvokeSetupRotationsAccessor)livingEntityRenderer;
               livingEntityRendererInvokeSetupRotationsAccessor.skycore$invokeSetupRotations(livingEntityRenderState, poseStack, livingEntityRenderState.bodyRot, scale);
               poseStack.scale(-1.0F, -1.0F, 1.0F);
               livingEntityRendererInvokeSetupRotationsAccessor.skycore$invokeScale(livingEntityRenderState, poseStack);
               poseStack.translate(0.0F, -1.501F, 0.0F);
               int value4 = WriteHexByteHelper.withAlpha(alpha, 34);
               int value5 = WriteHexByteHelper.withAlpha(alpha, 76);
               appendPart(model.root(), poseStack, position, value4, value4, value3, 1.085F);
               appendPart(model.root(), poseStack, position, value5, value5, value3, 1.042F);
               appendPart(model.root(), poseStack, position, alpha, value2, value3, 1.0F);
            }
         }
      }
   }

   private static void appendCube(Cube cube, com.mojang.blaze3d.vertex.PoseStack.Pose pose, Vec3 position, int color, int color2, float color3, float index) {
      for (Polygon polygon : cube.polygons) {
         Vertex[] vertexValues = polygon.vertices();
         if (vertexValues.length >= 3) {
            Vec3 position2 = transform(vertexValues[0], pose, position, index);
            int color4 = gradientColor(position2, position, color3, color, color2);

            for (int value = 1; value + 1 < vertexValues.length; value++) {
               Vec3 position3 = transform(vertexValues[value], pose, position, index);
               Vec3 position4 = transform(vertexValues[value + 1], pose, position, index);
               AddQuadHelper.addFacet(position2, position3, position4, color4, gradientColor(position3, position, color3, color, color2), gradientColor(position4, position, color3, color, color2));
            }
         }
      }
   }

   private static Vec3 transform(Vertex vertex, com.mojang.blaze3d.vertex.PoseStack.Pose pose, Vec3 position, float value) {
      Matrix4f matrix = pose.pose();
      float value2 = vertex.worldX();
      float value3 = vertex.worldY();
      float intermediate = vertex.worldZ();
      Vector3f vector = matrix.transformPosition(value2, value3, intermediate, new Vector3f());
      double x = vector.x * value;
      double y = vector.y * value;
      return position.add(x, y, vector.z * value);
   }

   private static void appendPart(ModelPart modelPart, PoseStack poseStack, Vec3 position, int value, int value2, float value3, float value4) {
      if (modelPart.visible) {
         ModelPartGetCubesAccessor modelPartGetCubesAccessor = (ModelPartGetCubesAccessor)(Object)modelPart;
         var cubes = modelPartGetCubesAccessor.skycore$getCubes();
         var lookup = modelPartGetCubesAccessor.skycore$getChildren();
         if (!cubes.isEmpty() || !lookup.isEmpty()) {
            poseStack.pushPose();
            modelPart.translateAndRotate(poseStack);
            if (!modelPart.skipDraw) {
               com.mojang.blaze3d.vertex.PoseStack.Pose pose = poseStack.last();

               for (Cube cube : cubes) {
                  appendCube(cube, pose, position, value, value2, value3, value4);
               }
            }

            for (ModelPart modelPart2 : lookup.values()) {
               appendPart(modelPart2, poseStack, position, value, value2, value3, value4);
            }

            poseStack.popPose();
         }
      }
   }

   private static int gradientColor(Vec3 position, Vec3 position2, float value, int value2, int value3) {
      double y = position.y - position2.y;
      double x = position.x - position2.x;
      double computedValue = value * 1.5 + y * 2.7 + x * 0.65;
      float computedValue2 = (float)(0.5 + 0.5 * Math.sin(computedValue));
      return WriteHexByteHelper.lerp(value2, value3, computedValue2);
   }

   private AddModelHelper() {
   }
}
