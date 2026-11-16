package deluxe.client.cosmetic;

import deluxe.client.cosmetic.CosmeticAnimationLoader;
import deluxe.client.cosmetic.CosmeticAnimationState;
import deluxe.client.cosmetic.CosmeticEntry;

import java.util.Map.Entry;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BedrockModelRenderer {
   private static final float[] field13007 = new float[]{0.0F, 0.0F, 0.0F};
   private static final float[] field13008 = new float[]{1.0F, 1.0F, 1.0F};
   private static final float[] field13009 = new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F};
   private static final float[] field13010 = new float[3];
   private static final MatrixStack field13011 = new MatrixStack();
   private static final Vector3f field13012 = new Vector3f();
   private static final Matrix4f field13013 = new Matrix4f();
   private static final Matrix4f field13014 = new Matrix4f();
   private static final Matrix3f field13015 = new Matrix3f();
   private static final Matrix3f field13016 = new Matrix3f();
   private static int field13017;

   private static void method3752(CosmeticAnimationState cosmeticAnimationState, MatrixStack matrixStack, boolean local6204_2) {
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
      if (local6204_2) {
         matrixStack.translate(cosmeticAnimationState.field15529, cosmeticAnimationState.field15530, cosmeticAnimationState.field15531);
      }

      if (cosmeticAnimationState.field15532 != 0.0F) {
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cosmeticAnimationState.field15532));
      }

      if (cosmeticAnimationState.field15533 != 0.0F) {
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cosmeticAnimationState.field15533));
      }

      if (cosmeticAnimationState.field15534 != 0.0F) {
         matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(cosmeticAnimationState.field15534));
      }

      matrixStack.scale(cosmeticAnimationState.field15528, cosmeticAnimationState.field15528, cosmeticAnimationState.field15528);
   }

   private static void method3753(
      BedrockCube[] recoveredClass0169Array,
      float[] floatArray,
      int local6205_2,
      Matrix4f matrix4f,
      Matrix3f matrix3f,
      VertexConsumer vertexConsumer,
      int local6205_6
   ) {
      for (int index = 0; index < local6205_2; index++) {
         int local6205_8 = index * 25;
         field13013.set(matrix4f).mul(field13014.set(floatArray, local6205_8));
         field13015.set(matrix3f).mul(field13016.set(floatArray, local6205_8 + 16));
         method3757(recoveredClass0169Array[index], field13013, field13015, vertexConsumer, local6205_6);
      }
   }

   private static CosmeticAnimationState method3754(
      CosmeticEntry cosmeticEntry, boolean local6206_1, boolean local6206_2, boolean local6206_3, boolean local6206_4, float local6206_5
   ) {
      CosmeticAnimationState cosmeticAnimationState = CosmeticAnimationLoader.method3955(cosmeticEntry);
      if (cosmeticAnimationState != null && cosmeticEntry.textureId() != null) {
         cosmeticAnimationState.method4577();
         String local6206_7 = cosmeticAnimationState.method4574(local6206_1, local6206_2, local6206_3, local6206_4);
         if (local6206_7 != null) {
            CosmeticAnimationState.Nested0534 local6206_8 = cosmeticAnimationState.field15536.get(local6206_7);
            if (local6206_8 != null) {
               method3760(cosmeticAnimationState, local6206_8, local6206_5);
            }
         }

         return cosmeticAnimationState;
      } else {
         return null;
      }
   }

   private BedrockModelRenderer() {
   }

   private static void method3755(BedrockBone bedrockBone, MatrixStack matrixStack, VertexConsumer vertexConsumer, int local6208_3) {
      if (!bedrockBone.field01590) {
         matrixStack.push();
         method3759(bedrockBone, matrixStack);

         for (BedrockCube bedrockCube : bedrockBone.field01588) {
            matrixStack.push();
            method3761(bedrockCube, matrixStack);
            method3757(bedrockCube, matrixStack.peek().getPositionMatrix(), matrixStack.peek().getNormalMatrix(), vertexConsumer, local6208_3);
            matrixStack.pop();
         }

         for (BedrockBone bedrockBone2 : bedrockBone.field01587) {
            method3755(bedrockBone2, matrixStack, vertexConsumer, local6208_3);
         }

         matrixStack.pop();
      }
   }

   private static void method3757(BedrockCube bedrockCube, Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer vertexConsumer, int local6210_4) {
      for (BedrockQuad bedrockQuad : bedrockCube.recoveredClass0278Array) {
         if (bedrockQuad != null) {
            Vector3f vector3f = field13012.set(bedrockQuad.modelVec3.method4368(), bedrockQuad.modelVec3.method4374(), bedrockQuad.modelVec3.method4372());
            matrix3f.transform(vector3f);
            float local6210_10 = vector3f.x();
            float local6210_11 = vector3f.y();
            float local6210_12 = vector3f.z();
            if ((bedrockCube.field09831.method4374() == 0.0F || bedrockCube.field09831.method4372() == 0.0F) && local6210_10 < 0.0F) {
               local6210_10 = -local6210_10;
            }

            if ((bedrockCube.field09831.method4368() == 0.0F || bedrockCube.field09831.method4372() == 0.0F) && local6210_11 < 0.0F) {
               local6210_11 = -local6210_11;
            }

            if ((bedrockCube.field09831.method4368() == 0.0F || bedrockCube.field09831.method4374() == 0.0F) && local6210_12 < 0.0F) {
               local6210_12 = -local6210_12;
            }

            for (BedrockVertex bedrockVertex : bedrockQuad.recoveredClass0057Array) {
               vertexConsumer.vertex(matrix4f, bedrockVertex.modelVec3.method4368(), bedrockVertex.modelVec3.method4374(), bedrockVertex.modelVec3.method4372())
                  .color(255, 255, 255, 255)
                  .texture(bedrockVertex.field03510, bedrockVertex.field03511)
                  .overlay(OverlayTexture.DEFAULT_UV)
                  .light(local6210_4)
                  .normal(local6210_10, local6210_11, local6210_12);
            }
         }
      }
   }

   private static void method3758(
      BedrockBone bedrockBone, MatrixStack matrixStack, BedrockCube[] recoveredClass0169Array, float[] floatArray, Matrix4f matrix4f
   ) {
      if (!bedrockBone.field01590) {
         matrixStack.push();
         method3759(bedrockBone, matrixStack);

         for (BedrockCube bedrockCube : bedrockBone.field01588) {
            matrixStack.push();
            method3761(bedrockCube, matrixStack);
            int local6212_7 = field13017++;
            recoveredClass0169Array[local6212_7] = bedrockCube;
            matrixStack.peek().getPositionMatrix().get(floatArray, local6212_7 * 25);
            matrixStack.peek().getNormalMatrix().get(floatArray, local6212_7 * 25 + 16);
            if (matrix4f != null) {
               BedrockVertexCollector.method2288(bedrockCube, field13013.set(matrix4f).mul(matrixStack.peek().getPositionMatrix()));
            }

            matrixStack.pop();
         }

         for (BedrockBone bedrockBone2 : bedrockBone.field01587) {
            method3758(bedrockBone2, matrixStack, recoveredClass0169Array, floatArray, matrix4f);
         }

         matrixStack.pop();
      }
   }

   private static void method3759(BedrockBone bedrockBone, MatrixStack matrixStack) {
      BedrockTransforms.method4425(bedrockBone, matrixStack);
      BedrockTransforms.method4432(bedrockBone, matrixStack);
      BedrockTransforms.method4430(bedrockBone, matrixStack);
      BedrockTransforms.method4428(bedrockBone, matrixStack);
      BedrockTransforms.method4426(bedrockBone, matrixStack);
   }

   static void method3760(CosmeticAnimationState cosmeticAnimationState, CosmeticAnimationState.Nested0534 local6214_1, float local6214_2) {
      float local6214_3 = local6214_1.field15564 ? local6214_2 % local6214_1.field15565 : Math.min(local6214_2, local6214_1.field15565);

      for (Entry<String, CosmeticAnimationState.Nested0535> local6214_5 : local6214_1.field15566.entrySet()) {
         BedrockBone bedrockBone = cosmeticAnimationState.method4578(local6214_5.getKey());
         if (bedrockBone != null) {
            float[] floatArray = cosmeticAnimationState.field15537.get(local6214_5.getKey());
            if (floatArray == null) {
               floatArray = field13009;
            }

            CosmeticAnimationState.Nested0535 value = local6214_5.getValue();
            if (!value.field15568.method4579()) {
               float[] floatArray2 = value.field15568.method4581(local6214_3, field13007, field13010);
               bedrockBone.method0856(floatArray[0] + (float)Math.toRadians(-floatArray2[0]));
               bedrockBone.method0862(floatArray[1] + (float)Math.toRadians(-floatArray2[1]));
               bedrockBone.method0863(floatArray[2] + (float)Math.toRadians(floatArray2[2]));
            }

            if (!value.field15569.method4579()) {
               float[] floatArray3 = value.field15569.method4581(local6214_3, field13007, field13010);
               bedrockBone.method0850(floatArray[3] + floatArray3[0]);
               bedrockBone.method0874(floatArray[4] + floatArray3[1]);
               bedrockBone.method0855(floatArray[5] + floatArray3[2]);
            }

            if (!value.field15570.method4579()) {
               float[] floatArray4 = value.field15570.method4581(local6214_3, field13008, field13010);
               bedrockBone.method0864(floatArray[6] * floatArray4[0]);
               bedrockBone.method0873(floatArray[7] * floatArray4[1]);
               bedrockBone.method0861(floatArray[8] * floatArray4[2]);
            }
         }
      }
   }

   private static void method3761(BedrockCube bedrockCube, MatrixStack matrixStack) {
      BedrockTransforms.method4431(bedrockCube, matrixStack);
      BedrockTransforms.method4427(bedrockCube, matrixStack);
      BedrockTransforms.method4429(bedrockCube, matrixStack);
   }

   public static boolean method3762(
      CosmeticEntry cosmeticEntry,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int local6216_3,
      boolean local6216_4,
      boolean local6216_5,
      boolean local6216_6,
      boolean local6216_7,
      float local6216_8
   ) {
      CosmeticAnimationState cosmeticAnimationState = method3754(cosmeticEntry, local6216_4, local6216_5, local6216_6, local6216_7, local6216_8);
      if (cosmeticAnimationState == null) {
         return false;
      }

      matrixStack.push();
      method3752(cosmeticAnimationState, matrixStack, true);
      RenderLayer renderLayer = RenderLayers.entityCutoutNoCull(cosmeticEntry.textureId());
      boolean local6216_11 = BedrockVertexCollector.method2294();
      if (local6216_11) {
         BedrockVertexCollector.method2291(cosmeticEntry.textureId());
      }

      BedrockCube[] recoveredClass0169Array = new BedrockCube[cosmeticAnimationState.field15541];
      float[] floatArray = new float[cosmeticAnimationState.field15541 * 25];
      field13011.peek().getPositionMatrix().identity();
      field13011.peek().getNormalMatrix().identity();
      field13017 = 0;
      Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();

      for (BedrockBone bedrockBone : cosmeticAnimationState.bedrockModel.field03648) {
         method3758(bedrockBone, field13011, recoveredClass0169Array, floatArray, local6216_11 ? positionMatrix : null);
      }

      int local6216_15 = field13017;
      if (local6216_15 > 0) {
         orderedRenderCommandQueue.submitCustom(
            matrixStack,
            renderLayer,
            (entry, vertexConsumer) -> method3753(
               recoveredClass0169Array, floatArray, local6216_15, entry.getPositionMatrix(), entry.getNormalMatrix(), vertexConsumer, local6216_3
            )
         );
      }

      matrixStack.pop();
      return true;
   }

   public static boolean method3763(
      CosmeticEntry cosmeticEntry,
      MatrixStack matrixStack,
      VertexConsumerProvider vertexConsumerProvider,
      int local6217_3,
      boolean local6217_4,
      boolean local6217_5,
      boolean local6217_6,
      boolean local6217_7,
      float local6217_8
   ) {
      CosmeticAnimationState cosmeticAnimationState = method3754(cosmeticEntry, local6217_4, local6217_5, local6217_6, local6217_7, local6217_8);
      if (cosmeticAnimationState == null) {
         return false;
      }

      matrixStack.push();
      method3752(cosmeticAnimationState, matrixStack, false);
      if (!cosmeticAnimationState.bedrockModel.field03648.isEmpty()) {
         BedrockBone bedrockBone = cosmeticAnimationState.bedrockModel.field03648.get(0);
         float local6217_11 = bedrockBone.method0853() / 16.0F;
         float local6217_12 = bedrockBone.method0860() / 16.0F;
         float local6217_13 = bedrockBone.method0875() / 16.0F;
         matrixStack.translate(local6217_11, local6217_12, local6217_13);
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
         matrixStack.scale(1.0F, -1.0F, 1.0F);
         matrixStack.translate(-local6217_11, -local6217_12, -local6217_13);
      } else {
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
         matrixStack.scale(1.0F, -1.0F, 1.0F);
      }

      VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayers.entityCutoutNoCull(cosmeticEntry.textureId()));

      try {
         for (BedrockBone bedrockBone2 : cosmeticAnimationState.bedrockModel.field03648) {
            method3755(bedrockBone2, matrixStack, vertexConsumer, local6217_3);
         }
      } finally {
         matrixStack.pop();
      }

      return true;
   }
}
