package net.skycore.client.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class LoadMeshHelper {
   private static final float[] ZERO3 = new float[]{0.0F, 0.0F, 0.0F};
   private static final float[] IDENTITY4 = new float[]{0.0F, 0.0F, 0.0F, 1.0F};
   private final Map<LegacyRigHelper, LoadMeshHelper.PetMesh> cache;
   private static final BindGroupLayout LAYOUT = BindGroupLayout.builder().withSampler("PetSampler").build();
   private static final RenderPipeline PIPELINE = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/pet_mesh"))
      .withVertexShader(Identifier.parse("skycore:core/pet_mesh"))
      .withFragmentShader(Identifier.parse("skycore:core/pet_mesh"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(LAYOUT)
      .withColorTargetState(ColorTargetState.DEFAULT)
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private final UploadHelper gpu;
   private static final RenderPipeline DRAGON_PIPELINE = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/world/pet_mesh_dragon"))
      .withVertexShader(Identifier.parse("skycore:core/pet_mesh"))
      .withFragmentShader(Identifier.parse("skycore:core/pet_mesh"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(LAYOUT)
      .withColorTargetState(ColorTargetState.DEFAULT)
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .build();
   private final Map<String, LoadMeshHelper.PetMesh> sceneMeshes = new HashMap<>();

   private LoadMeshHelper.PetMesh loadMesh(LegacyRigHelper legacyRigHelper) {
      Identifier texture = legacyRigHelper.mesh();
      return this.loadMesh(texture);
   }

   private static float[][] matrix(JsonArray jsonValues) {
      if (jsonValues == null) {
         return new float[0][];
      }

      float[][] floatArrayValues = new float[jsonValues.size()][];
      int value = 0;

      while (true) {
         if (value >= jsonValues.size()) {
            return floatArrayValues;
         }

         JsonElement jsonElement = jsonValues.get(value);
         floatArrayValues[value] = floats(jsonElement.getAsJsonArray());
         value++;
      }
   }

   private static int[] ints(JsonArray jsonValues) {
      if (jsonValues == null) {
         return new int[0];
      }

      int[] intValues = new int[jsonValues.size()];

      for (int value = 0; value < jsonValues.size(); value++) {
         JsonElement jsonElement = jsonValues.get(value);
         intValues[value] = jsonElement.getAsInt();
      }

      return intValues;
   }

   private static void applyDogAnimation(Matrix4f matrix, String text, float value, float value2) {
      float computedValue = (float)Math.sin(value2 * 8.5F) * 42.0F * value;
      float sine = (float)Math.sin(value2 * 1.35F);
      switch (text) {
         case "head":
            matrix.rotateY((float)Math.toRadians(sine * 4.0F));
            matrix.rotateX((float)Math.toRadians(sine * 1.8F));
            break;
         case "frontLeftLeg":
         case "rightBackLeg":
            matrix.rotateX((float)Math.toRadians(computedValue));
            break;
         case "frontRightLeg":
         case "leftBackLeg":
            matrix.rotateX((float)Math.toRadians(-computedValue));
            break;
         case "tail":
            matrix.rotateZ((float)Math.toRadians(Math.sin(value2 * 4.5F) * 18.0));
      }
   }

   private void applyAnimation(LoadMeshHelper.FrameContext frameContext, String text, Matrix4f matrix) {
      if (frameContext.type == LegacyRigHelper.DRAGON) {
         applyDragonAnimation(matrix, text, frameContext.movement, frameContext.seconds);
      } else if (frameContext.type == LegacyRigHelper.DOG) {
         applyDogAnimation(matrix, text, frameContext.movement, frameContext.seconds);
      } else {
         LoadMeshHelper.NodeTransform nodeTransform = frameContext.mesh.sample(text, frameContext.seconds, frameContext.movement);
         if (nodeTransform != null) {
            if (nodeTransform.translation != null) {
               matrix.translate(nodeTransform.translation[0], nodeTransform.translation[1], nodeTransform.translation[2]);
            }

            if (nodeTransform.rotation != null) {
               matrix.rotate(new Quaternionf(nodeTransform.rotation[0], nodeTransform.rotation[1], nodeTransform.rotation[2], nodeTransform.rotation[3]));
            }

            if (frameContext.type != LegacyRigHelper.PENGUIN) {
               return;
            }
         }

         applyFallbackRigAnimation(frameContext.type, matrix, text, frameContext.movement, frameContext.seconds);
      }
   }

   private static float smoothstep(float progress) {
      return progress * progress * (3.0F - 2.0F * progress);
   }

   public void render(LegacyRigHelper legacyRigHelper, NextIdleHopDelayHelper nextIdleHopDelayHelper, float yaw) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.level != null && instance.player != null && instance.gameRenderer != null) {
         Vec3 position = nextIdleHopDelayHelper.interpolatedPosition(yaw);
         Camera camera = instance.gameRenderer.mainCamera();
         if (position != null && camera != null && camera.isInitialized()) {
            LoadMeshHelper.PetMesh petMesh = this.cache.computeIfAbsent(legacyRigHelper, this::loadMesh);
            if (petMesh.vertexCount > 0) {
               float value = clamp(nextIdleHopDelayHelper.movementAmount(), 0.0F, 1.0F);
               double computedValue = System.nanoTime() / 1.0E9;
               double computedValue2 = legacyRigHelper.flying() ? Math.sin(computedValue * 2.25) * 0.055 : Math.abs(Math.sin(computedValue * 8.0)) * 0.018 * value - nextIdleHopDelayHelper.landingAmount() * 0.045;
               Vec3 position2 = position.add(0.0, computedValue2, 0.0);
               float value2 = nextIdleHopDelayHelper.interpolatedYaw(yaw);
               this.renderMesh(legacyRigHelper, petMesh, legacyRigHelper.texture(), position2, value2, value, (float)computedValue, LoadMeshHelper.ScenePose.NORMAL);
            }
         }
      }
   }

   private void emitVertex(LoadMeshHelper.FrameContext frameContext, Vector3f vector, LoadMeshHelper.PetVertex petVertex) {
      float value;
      float height;
      float value2;
      if (frameContext.type.legacyRig()) {
         value = vector.x;
         height = legacyBaseY(frameContext.type) - vector.y;
         value2 = -vector.z;
      } else {
         value = (vector.x - frameContext.mesh.centerX) * frameContext.type.modelScale();
         height = (vector.y - frameContext.mesh.minY) * frameContext.type.modelScale();
         value2 = (vector.z - frameContext.mesh.centerZ) * frameContext.type.modelScale();
      }

      value *= frameContext.pose.scale;
      height *= frameContext.pose.scale * frameContext.pose.height;
      value2 *= frameContext.pose.scale;
      float cosine = (float)Math.cos(frameContext.pose.roll);
      float sine = (float)Math.sin(frameContext.pose.roll);
      float computedValue = value * cosine - height * sine;
      height = value * sine + height * cosine;
      value = computedValue;
      float y = frameContext.yaw + (frameContext.type == LegacyRigHelper.DRAGON ? 180.0F : 0.0F);
      float angleRadians = (float)Math.toRadians(180.0F - y);
      float sine2 = (float)Math.sin(angleRadians);
      float cosine2 = (float)Math.cos(angleRadians);
      float computedValue2 = value * cosine2 + value2 * sine2;
      float computedValue3 = -value * sine2 + value2 * cosine2;
      Vec3 position = new Vec3(frameContext.position.x + computedValue2, frameContext.position.y + height, frameContext.position.z + computedValue3);
      Vector4f vector4f = CameraViewPoseHelper.toViewSpace(position, frameContext.cameraPosition, frameContext.view);
      frameContext.builder.addVertex(vector4f.x, vector4f.y, vector4f.z).setUv(petVertex.u, petVertex.v).setColor(frameContext.pose.color);
   }

   private static void applyFallbackRigAnimation(LegacyRigHelper legacyRigHelper, Matrix4f matrix, String text, float value, float value2) {
      float computedValue = (float)Math.sin(value2 * 8.0F) * 34.0F * value;
      float sine = (float)Math.sin(value2 * 1.6F);
      if (legacyRigHelper == LegacyRigHelper.MINUN) {
         switch (text) {
            case "Head":
               matrix.rotateY((float)Math.toRadians(sine * 4.0F));
               matrix.rotateX((float)Math.toRadians(sine * 1.5F));
               break;
            case "Leg0":
               matrix.rotateX((float)Math.toRadians(computedValue));
               break;
            case "Leg1":
               matrix.rotateX((float)Math.toRadians(-computedValue));
               break;
            case "Arm0":
               matrix.rotateZ((float)Math.toRadians(-8.0));
               matrix.rotateX((float)Math.toRadians(-computedValue * 0.55F));
               break;
            case "Arm1":
               matrix.rotateZ((float)Math.toRadians(8.0));
               matrix.rotateX((float)Math.toRadians(computedValue * 0.55F));
               break;
            case "Tail":
               matrix.rotateY((float)Math.toRadians(Math.sin(value2 * 2.6F) * 13.0));
               break;
            case "Ear0":
               matrix.rotateZ((float)Math.toRadians(sine * 2.0F));
               break;
            case "Ear":
               matrix.rotateZ((float)Math.toRadians(-sine * 2.0F));
         }
      } else {
         if (legacyRigHelper == LegacyRigHelper.PENGUIN) {
            switch (text) {
               case "head":
                  matrix.rotateY((float)Math.toRadians(sine * 3.5F));
                  break;
               case "left_leg":
                  matrix.rotateX((float)Math.toRadians(computedValue * 0.65F));
                  break;
               case "right_leg":
                  matrix.rotateX((float)Math.toRadians(-computedValue * 0.65F));
                  break;
               case "left_arm":
                  matrix.rotateX((float)Math.toRadians(computedValue * 0.2F));
                  break;
               case "right_arm":
                  matrix.rotateX((float)Math.toRadians(-computedValue * 0.2F));
                  break;
               case "tail":
               case "tail2":
                  matrix.rotateY((float)Math.toRadians(sine * 3.0F));
            }
         }
      }
   }

   public void renderProp(String text, Vec3 position, float coordinate, LoadMeshHelper.ScenePose scenePose) {
      LoadMeshHelper.PetMesh petMesh = this.sceneMeshes.computeIfAbsent(text, text2 -> {
         String text3 = "skycore:models/pet/duplishe_" + text2 + ".json";
         Identifier texture = Identifier.parse(text3);
         return this.loadMesh(texture);
      });
      LegacyRigHelper pENGUIN = LegacyRigHelper.PENGUIN;
      Identifier texture2 = Identifier.parse("skycore:textures/pet/duplishe_white.png");
      this.renderMesh(pENGUIN, petMesh, texture2, position, coordinate, 0.0F, 0.0F, scenePose);
   }

   public static List<RenderPipeline> pipelines() {
      return List.of(PIPELINE, DRAGON_PIPELINE);
   }

   private LoadMeshHelper.PetMesh loadMesh(Identifier texture) {
      Minecraft instance = Minecraft.getInstance();

      try (BufferedReader resourceManager = instance.getResourceManager().openAsReader(texture)) {
         JsonObject asJsonObject = JsonParser.parseReader(resourceManager).getAsJsonObject();
         JsonArray asJsonArray = asJsonObject.getAsJsonObject("bounds").getAsJsonArray("min");
         JsonArray asJsonArray2 = asJsonObject.getAsJsonObject("bounds").getAsJsonArray("max");
         float asFloat = asJsonArray.get(1).getAsFloat();
         float asFloat2 = (asJsonArray.get(0).getAsFloat() + asJsonArray2.get(0).getAsFloat()) * 0.5F;
         float asFloat3 = (asJsonArray.get(2).getAsFloat() + asJsonArray2.get(2).getAsFloat()) * 0.5F;
         JsonArray asJsonArray3 = asJsonObject.getAsJsonArray("nodes");
         ArrayList elements = new ArrayList(asJsonArray3.size());
         int value = 0;

         for (int value2 = 0; value2 < asJsonArray3.size(); value2++) {
            JsonObject asJsonObject2 = asJsonArray3.get(value2).getAsJsonObject();
            String asString = asJsonObject2.has("name") ? asJsonObject2.get("name").getAsString() : "";
            float[] asJsonArray4 = floats(asJsonObject2.getAsJsonArray("translation"), 3, new float[]{0.0F, 0.0F, 0.0F});
            float[] asJsonArray5 = floats(asJsonObject2.getAsJsonArray("rotation"), 4, new float[]{0.0F, 0.0F, 0.0F, 1.0F});
            float[] asJsonArray6 = floats(asJsonObject2.getAsJsonArray("scale"), 3, new float[]{1.0F, 1.0F, 1.0F});
            int[] asJsonArray7 = ints(asJsonObject2.getAsJsonArray("children"));
            ArrayList elements2 = new ArrayList();
            JsonArray asJsonArray8 = asJsonObject2.getAsJsonArray("triangles");
            if (asJsonArray8 != null) {
               for (int value3 = 0; value3 < asJsonArray8.size(); value3++) {
                  JsonArray asJsonArray9 = asJsonArray8.get(value3).getAsJsonArray();

                  for (int value4 = 0; value4 < asJsonArray9.size(); value4++) {
                     JsonArray asJsonArray10 = asJsonArray9.get(value4).getAsJsonArray();
                     elements2.add(
                        new LoadMeshHelper.PetVertex(
                           asJsonArray10.get(0).getAsFloat(),
                           asJsonArray10.get(1).getAsFloat(),
                           asJsonArray10.get(2).getAsFloat(),
                           asJsonArray10.get(3).getAsFloat(),
                           asJsonArray10.get(4).getAsFloat()
                        )
                     );
                  }
               }
            }

            value += elements2.size();
            elements.add(new LoadMeshHelper.PetNode(asString, asJsonArray4, asJsonArray5, asJsonArray6, asJsonArray7, List.copyOf(elements2)));
         }

         int[] asJsonArray11 = ints(asJsonObject.getAsJsonArray("rootNodes"));
         var asJsonArray12 = readAnimations(asJsonObject.getAsJsonArray("animations"));
         return new LoadMeshHelper.PetMesh(List.copyOf(elements), asJsonArray11, asFloat2, asFloat3, asFloat, value, asJsonArray12);
      } catch (Exception failure) {
         return LoadMeshHelper.PetMesh.empty();
      }
   }

   public void renderPhotoFace(String text, Vec3 position, float coordinate, LoadMeshHelper.ScenePose scenePose) {
      String text2 = "face_" + text;
      LoadMeshHelper.PetMesh petMesh = this.sceneMeshes.computeIfAbsent(text2, text3 -> {
         String text4 = "skycore:models/pet/duplishe_" + text3 + ".json";
         return this.loadMesh(Identifier.parse(text4));
      });
      this.renderMesh(LegacyRigHelper.PENGUIN, petMesh, Identifier.parse("skycore:textures/pet/duplishe_" + text + ".jpg"), position, coordinate, 0.0F, 0.0F, scenePose);
   }

   public static RenderPipeline pipeline() {
      return PIPELINE;
   }

   private static void applyDragonAnimation(Matrix4f matrix, String text, float value, float value2) {
      float computedValue = value2 * 1.0F;
      float computedValue2 = (float)Math.sin(value2 * (value > 0.08F ? 3.2F : 1.2F));
      float computedValue3 = (float)Math.sin(value2 * (value > 0.08F ? 5.5F : 1.15F));
      float sine = (float)Math.sin(value2 * 2.1F);
      switch (text) {
         case "head":
            matrix.rotateX((float)Math.toRadians(27.5 + Math.sin(computedValue * 0.6F) * 5.0));
            matrix.rotateY((float)Math.toRadians(Math.sin(computedValue * 0.8F) * 5.0));
            break;
         case "neck":
            matrix.rotateX((float)Math.toRadians(12.5));
            break;
         case "torso":
            matrix.rotateX((float)Math.toRadians(-30.0));
            matrix.rotateZ((float)Math.toRadians(Math.sin(computedValue * 0.4F) * 4.0));
            break;
         case "waist":
            matrix.rotateX((float)Math.toRadians(-20.0));
            break;
         case "segment_1":
            matrix.rotateX((float)Math.toRadians(-15.0));
            matrix.rotateY(computedValue2 * 0.6F);
            break;
         case "segment_2":
            matrix.rotateX((float)Math.toRadians(-10.0));
            matrix.rotateY(computedValue2 * 0.44F);
            break;
         case "segment_3":
            matrix.rotateX((float)Math.toRadians(-7.5));
            matrix.rotateY(computedValue2 * 0.3F);
            break;
         case "segment_4":
            matrix.rotateX((float)Math.toRadians(-5.0));
            matrix.rotateY(computedValue2 * 0.18F);
            break;
         case "left_wing":
            matrix.rotateX((float)Math.toRadians(5.0F + computedValue3 * 7.0F));
            matrix.rotateY((float)Math.toRadians(-15.0F + computedValue3 * 4.0F));
            matrix.rotateZ((float)Math.toRadians(45.0F + computedValue3 * (value > 0.08F ? 55.0F : 22.0F)));
            break;
         case "right_wing":
            matrix.rotateX((float)Math.toRadians(5.0F + computedValue3 * 7.0F));
            matrix.rotateY((float)Math.toRadians(15.0F - computedValue3 * 4.0F));
            matrix.rotateZ((float)Math.toRadians(-45.0F - computedValue3 * (value > 0.08F ? 55.0F : 22.0F)));
            break;
         case "left_rearleg":
            matrix.rotateX((float)Math.toRadians(25.0F + sine * 5.0F));
            break;
         case "right_rearleg":
            matrix.rotateX((float)Math.toRadians(25.0F - sine * 5.0F));
            break;
         case "left_forearm":
            matrix.rotateX((float)Math.toRadians(20.0F + sine * 7.0F));
            break;
         case "right_forearm":
            matrix.rotateX((float)Math.toRadians(20.0F - sine * 7.0F));
            break;
         case "jaw":
            matrix.rotateX((float)Math.toRadians(7.5 + Math.sin(value2 * 2.2F) * 2.0));
      }
   }

   public LoadMeshHelper() {
      this.cache = new EnumMap<>(LegacyRigHelper.class);
      this.gpu = new UploadHelper("Skycore Pet Mesh Vertices");
   }

   private static LoadMeshHelper.NodeTransform blend(LoadMeshHelper.NodeTransform nodeTransform, LoadMeshHelper.NodeTransform nodeTransform2, float value) {
      if (nodeTransform == null && nodeTransform2 == null) {
         return null;
      }

      LoadMeshHelper.NodeTransform nodeTransform3 = nodeTransform == null ? LoadMeshHelper.NodeTransform.IDENTITY : nodeTransform;
      LoadMeshHelper.NodeTransform nodeTransform4 = nodeTransform2 == null ? nodeTransform3 : nodeTransform2;
      float[] floatValues = null;
      if (nodeTransform3.translation != null || nodeTransform4.translation != null) {
         float[] floatValues2 = nodeTransform3.translation == null ? ZERO3 : nodeTransform3.translation;
         float[] floatValues3 = nodeTransform4.translation == null ? floatValues2 : nodeTransform4.translation;
         floatValues = new float[]{floatValues2[0] + (floatValues3[0] - floatValues2[0]) * value, floatValues2[1] + (floatValues3[1] - floatValues2[1]) * value, floatValues2[2] + (floatValues3[2] - floatValues2[2]) * value};
      }

      float[] floatValues4 = null;
      if (nodeTransform3.rotation != null || nodeTransform4.rotation != null) {
         float[] floatValues5 = nodeTransform3.rotation == null ? IDENTITY4 : nodeTransform3.rotation;
         float[] floatValues6 = nodeTransform4.rotation == null ? floatValues5 : nodeTransform4.rotation;
         Quaternionf rotation = new Quaternionf(floatValues5[0], floatValues5[1], floatValues5[2], floatValues5[3]).slerp(new Quaternionf(floatValues6[0], floatValues6[1], floatValues6[2], floatValues6[3]), value);
         floatValues4 = new float[]{rotation.x, rotation.y, rotation.z, rotation.w};
      }

      return new LoadMeshHelper.NodeTransform(floatValues, floatValues4);
   }

   private void emitNode(LoadMeshHelper.FrameContext frameContext, int value, Matrix4f matrix) {
      if (value >= 0 && value < frameContext.mesh.nodes.size()) {
         LoadMeshHelper.PetNode petNode = frameContext.mesh.nodes.get(value);
         Matrix4f matrix2 = new Matrix4f(matrix)
            .translate(petNode.translation[0], petNode.translation[1], petNode.translation[2])
            .rotate(new Quaternionf(petNode.rotation[0], petNode.rotation[1], petNode.rotation[2], petNode.rotation[3]))
            .scale(petNode.scale[0], petNode.scale[1], petNode.scale[2]);
         this.applyAnimation(frameContext, petNode.name, matrix2);

         for (LoadMeshHelper.PetVertex petVertex : petNode.vertices) {
            Vector3f vector = new Vector3f(petVertex.x, petVertex.y, petVertex.z);
            matrix2.transformPosition(vector);
            this.emitVertex(frameContext, vector, petVertex);
         }

         for (int index : petNode.children) {
            this.emitNode(frameContext, index, matrix2);
         }
      }
   }

   private static List<LoadMeshHelper.ModelAnimation> readAnimations(JsonArray jsonValues) {
      if (jsonValues == null) {
         return List.of();
      }

      ArrayList elements = new ArrayList();

      for (int value = 0; value < jsonValues.size(); value++) {
         JsonObject asJsonObject = jsonValues.get(value).getAsJsonObject();
         String asString = asJsonObject.has("name") ? asJsonObject.get("name").getAsString() : "";
         float asFloat = asJsonObject.has("duration") ? asJsonObject.get("duration").getAsFloat() : 0.0F;
         ArrayList elements2 = new ArrayList();
         JsonArray asJsonArray = asJsonObject.getAsJsonArray("channels");
         if (asJsonArray != null) {
            for (int value2 = 0; value2 < asJsonArray.size(); value2++) {
               JsonObject asJsonObject2 = asJsonArray.get(value2).getAsJsonObject();
               String asString2 = asJsonObject2.has("node") ? asJsonObject2.get("node").getAsString() : "";
               String asString3 = asJsonObject2.has("path") ? asJsonObject2.get("path").getAsString() : "";
               float[] asJsonArray2 = floats(asJsonObject2.getAsJsonArray("times"));
               float[][] asJsonArray3 = matrix(asJsonObject2.getAsJsonArray("values"));
               elements2.add(new LoadMeshHelper.AnimationChannel(asString2, asString3, asJsonArray2, asJsonArray3));
            }
         }

         elements.add(new LoadMeshHelper.ModelAnimation(asString, asFloat, List.copyOf(elements2)));
      }

      return List.copyOf(elements);
   }

   private static float legacyBaseY(LegacyRigHelper legacyRigHelper) {
      return legacyRigHelper == LegacyRigHelper.DRAGON ? 0.8F : 1.2F;
   }

   private static float clamp(float value, float value2, float value3) {
      float minimum = Math.min(value3, value);
      return Math.max(value2, minimum);
   }

   private static float[] floats(JsonArray jsonValues) {
      if (jsonValues == null) {
         return new float[0];
      }

      float[] floatValues = new float[jsonValues.size()];
      int value = 0;

      while (true) {
         if (value >= jsonValues.size()) {
            return floatValues;
         }

         JsonElement jsonElement = jsonValues.get(value);
         floatValues[value] = jsonElement.getAsFloat();
         value++;
      }
   }

   private void renderMesh(
      LegacyRigHelper legacyRigHelper, LoadMeshHelper.PetMesh petMesh, Identifier texture, Vec3 position, float coordinate, float coordinate2, float coordinate3, LoadMeshHelper.ScenePose scenePose
   ) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.level != null && instance.player != null && petMesh.vertexCount > 0) {
         Camera camera = instance.gameRenderer.mainCamera();
         if (camera != null && camera.isInitialized()) {
            try (ByteBufferBuilder vertexSize = ByteBufferBuilder.exactlySized(petMesh.vertexCount * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
               BufferBuilder bufferBuilder = new BufferBuilder(vertexSize, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
               LoadMeshHelper.FrameContext frameContext = new LoadMeshHelper.FrameContext(legacyRigHelper, petMesh, bufferBuilder, position, camera, coordinate, coordinate2, coordinate3, scenePose);
               Matrix4f matrix = new Matrix4f();

               for (int index : petMesh.roots) {
                  this.emitNode(frameContext, index, matrix);
               }

               try (MeshData meshData = bufferBuilder.buildOrThrow()) {
                  RenderTarget renderTarget = instance.gameRenderer.mainRenderTarget();
                  GpuTextureView colorTextureView = renderTarget != null ? renderTarget.getColorTextureView() : null;
                  if (colorTextureView == null) {
                     return;
                  }

                  AbstractTexture texture2 = instance.getTextureManager().getTexture(texture);
                  GpuTextureView textureView = texture2 == null ? null : texture2.getTextureView();
                  if (textureView == null) {
                     return;
                  }

                  int value = meshData.vertexBuffer().remaining();
                  GpuBuffer gpuBuffer = this.gpu.upload(meshData);
                  GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
                  GpuTextureView depthTextureView = renderTarget.getDepthTextureView();
                  RenderPass device = depthTextureView != null ? RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                     return "Skycore Pet Mesh Pass";
                  }, colorTextureView, Optional.empty(), depthTextureView, OptionalDouble.empty()) : RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                     return "Skycore Pet Mesh Pass";
                  }, colorTextureView, Optional.empty());

                  try {
                     device.setPipeline(legacyRigHelper == LegacyRigHelper.DRAGON ? DRAGON_PIPELINE : PIPELINE);
                     device.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                     device.bindTexture("PetSampler", textureView, clampToEdge);
                     device.setVertexBuffer(0, gpuBuffer.slice(0L, value));
                     device.draw(petMesh.vertexCount, 1, 0, 0);
                  } finally {
                     device.close();
                  }
               }
            }
         }
      }
   }

   public void renderScene(LegacyRigHelper legacyRigHelper, Vec3 position, float coordinate, float coordinate2, LoadMeshHelper.ScenePose scenePose) {
      var cache2 = this.cache;
      Function<net.skycore.client.util.LegacyRigHelper, net.skycore.client.util.LoadMeshHelper.PetMesh> function = this::loadMesh;
      LoadMeshHelper.PetMesh petMesh = (LoadMeshHelper.PetMesh)cache2.computeIfAbsent(legacyRigHelper, function);
      this.renderMesh(legacyRigHelper, petMesh, legacyRigHelper.texture(), position, coordinate, 0.0F, coordinate2, scenePose);
   }

   private static float[] floats(JsonArray jsonValues, int index, float[] floatValues) {
      if (jsonValues != null && jsonValues.size() >= index) {
         float[] floatValues2 = new float[index];

         for (int value = 0; value < index; value++) {
            floatValues2[value] = jsonValues.get(value).getAsFloat();
         }

         return floatValues2;
      } else {
         return floatValues;
      }
   }

   public record AnimationChannel(String node, String path, float[] times, float[][] values) {
      public float[] times() {
         return this.times;
      }

      public String node() {
         return this.node;
      }

      private float[] sample(float elapsed) {
         if (this.values.length == 0) {
            return null;
         }

         if (this.values.length != 1 && this.times.length >= 2) {
            int value = 1;

            while (value < this.times.length && elapsed > this.times[value]) {
               value++;
            }

            if (value >= this.times.length) {
               value = this.times.length - 1;
            }

            int maximum = Math.max(0, value - 1);
            float elapsed2 = this.times[value] - this.times[maximum];
            float computedValue = elapsed2 <= 1.0E-5F ? 0.0F : LoadMeshHelper.clamp((elapsed - this.times[maximum]) / elapsed2, 0.0F, 1.0F);
            float[] floatValues = this.values[Math.min(maximum, this.values.length - 1)];
            float[] floatValues2 = this.values[Math.min(value, this.values.length - 1)];
            if ("rotation".equalsIgnoreCase(this.path) && floatValues.length >= 4 && floatValues2.length >= 4) {
               Quaternionf rotation = new Quaternionf(floatValues[0], floatValues[1], floatValues[2], floatValues[3]).slerp(new Quaternionf(floatValues2[0], floatValues2[1], floatValues2[2], floatValues2[3]), computedValue);
               return new float[]{rotation.x, rotation.y, rotation.z, rotation.w};
            }

            int minimum = Math.min(floatValues.length, floatValues2.length);
            float[] floatValues3 = new float[minimum];

            for (int value2 = 0; value2 < minimum; value2++) {
               floatValues3[value2] = floatValues[value2] + (floatValues2[value2] - floatValues[value2]) * computedValue;
            }

            return floatValues3;
         } else {
            return (float[])this.values[0].clone();
         }
      }

      public float[][] values() {
         return this.values;
      }

      public String path() {
         return this.path;
      }
   }

   public record FrameContext(
      LegacyRigHelper type,
      LoadMeshHelper.PetMesh mesh,
      BufferBuilder builder,
      Vec3 position,
      Camera camera,
      float yaw,
      float movement,
      float seconds,
      LoadMeshHelper.ScenePose pose,
      Vec3 cameraPosition,
      Matrix4f view
   ) {
      public Camera camera() {
         return this.camera;
      }

      private FrameContext(
         LegacyRigHelper legacyRigHelper,
         LoadMeshHelper.PetMesh petMesh,
         BufferBuilder bufferBuilder,
         Vec3 position2,
         Camera camera2,
         float value,
         float value2,
         float value3,
         LoadMeshHelper.ScenePose scenePose
      ) {
         this(legacyRigHelper, petMesh, bufferBuilder, position2, camera2, value, value2, value3, scenePose, camera2.position(), CameraViewPoseHelper.cameraViewPose(camera2));
      }

      public BufferBuilder builder() {
         return this.builder;
      }

      public Vec3 cameraPosition() {
         return this.cameraPosition;
      }

      public LoadMeshHelper.PetMesh mesh() {
         return this.mesh;
      }

      public Matrix4f view() {
         return this.view;
      }

      public LoadMeshHelper.ScenePose pose() {
         return this.pose;
      }

      public float yaw() {
         return this.yaw;
      }

      public Vec3 position() {
         return this.position;
      }

      public float movement() {
         return this.movement;
      }
   }

   public record ModelAnimation(String name, float duration, List<LoadMeshHelper.AnimationChannel> channels) {
      public String name() {
         return this.name;
      }

      private LoadMeshHelper.NodeTransform sample(String text, float index) {
         float index2 = this.duration > 1.0E-4F ? index % this.duration : 0.0F;
         float[] floatValues = null;
         float[] floatValues2 = null;

         for (LoadMeshHelper.AnimationChannel animationChannel : this.channels) {
            if (animationChannel.node.equals(text)) {
               float[] floatValues3 = animationChannel.sample(index2);
               if ("translation".equalsIgnoreCase(animationChannel.path)) {
                  floatValues = floatValues3;
               } else if ("rotation".equalsIgnoreCase(animationChannel.path)) {
                  floatValues2 = floatValues3;
               }
            }
         }

         return floatValues == null && floatValues2 == null ? null : new LoadMeshHelper.NodeTransform(floatValues, floatValues2);
      }

      public List<LoadMeshHelper.AnimationChannel> channels() {
         return this.channels;
      }

      public float duration() {
         return this.duration;
      }
   }

   public record NodeTransform(float[] translation, float[] rotation) {
      private static final LoadMeshHelper.NodeTransform IDENTITY = new LoadMeshHelper.NodeTransform(null, null);

      public float[] translation() {
         return this.translation;
      }

      public float[] rotation() {
         return this.rotation;
      }
   }

   static final class PetMesh {
      private final LoadMeshHelper.ModelAnimation idle;
      private final float minY;
      private final LoadMeshHelper.ModelAnimation walk;
      private final int[] roots;
      private final LoadMeshHelper.ModelAnimation pose;
      private final int vertexCount;
      private final List<LoadMeshHelper.PetNode> nodes;
      private final float centerZ;
      private final float centerX;

      private PetMesh(List<LoadMeshHelper.PetNode> nodes2, int[] roots2, float centerX2, float centerZ2, float minY2, int vertexCount2, List<LoadMeshHelper.ModelAnimation> modelAnimations) {
         this.nodes = nodes2;
         this.roots = roots2;
         this.centerX = centerX2;
         this.centerZ = centerZ2;
         this.minY = minY2;
         this.vertexCount = vertexCount2;
         LoadMeshHelper.ModelAnimation modelAnimation = null;
         LoadMeshHelper.ModelAnimation modelAnimation2 = null;
         LoadMeshHelper.ModelAnimation modelAnimation3 = null;

         for (LoadMeshHelper.ModelAnimation modelAnimation4 : modelAnimations) {
            if ("idle".equalsIgnoreCase(modelAnimation4.name)) {
               modelAnimation = modelAnimation4;
            } else if (!"walk".equalsIgnoreCase(modelAnimation4.name) && !"walking".equalsIgnoreCase(modelAnimation4.name) && !"running".equalsIgnoreCase(modelAnimation4.name)) {
               if (modelAnimation4.name.toLowerCase().contains("pose")) {
                  modelAnimation3 = modelAnimation4;
               }
            } else {
               modelAnimation2 = modelAnimation4;
            }
         }

         this.idle = modelAnimation;
         this.walk = modelAnimation2;
         this.pose = modelAnimation3;
      }

      private static LoadMeshHelper.PetMesh empty() {
         return new LoadMeshHelper.PetMesh(List.of(), new int[0], 0.0F, 0.0F, 0.0F, 0, List.of());
      }

      private LoadMeshHelper.NodeTransform sample(String text, float value, float value2) {
         LoadMeshHelper.NodeTransform nodeTransform = this.idle == null ? null : this.idle.sample(text, value);
         LoadMeshHelper.NodeTransform nodeTransform2;
         if (this.walk == null) {
            nodeTransform2 = null;
         } else {
            nodeTransform2 = this.walk.sample(text, value);
         }

         LoadMeshHelper.NodeTransform nodeTransform3 = nodeTransform2;
         float computedValue = value2 * 1.35F;
         float value3 = LoadMeshHelper.smoothstep(LoadMeshHelper.clamp(computedValue, 0.0F, 1.0F));
         LoadMeshHelper.NodeTransform nodeTransform4 = LoadMeshHelper.blend(nodeTransform, nodeTransform3, value3);
         if (nodeTransform4 == null) {
            if (this.pose != null) {
               return this.pose.sample(text, 0.0F);
            }
         }

         return nodeTransform4;
      }
   }

   public record PetNode(String name, float[] translation, float[] rotation, float[] scale, int[] children, List<LoadMeshHelper.PetVertex> vertices) {
      public float[] rotation() {
         return this.rotation;
      }

      public float[] translation() {
         return this.translation;
      }

      public int[] children() {
         return this.children;
      }

      public String name() {
         return this.name;
      }

      public List<LoadMeshHelper.PetVertex> vertices() {
         return this.vertices;
      }

      public float[] scale() {
         return this.scale;
      }
   }

   public record PetVertex(float x, float y, float z, float u, float v) {
      public float v() {
         return this.v;
      }

      public float y() {
         return this.y;
      }

      public float x() {
         return this.x;
      }

      public float z() {
         return this.z;
      }

      public float u() {
         return this.u;
      }
   }

   public record ScenePose(float scale, float height, float roll, int color) {
      public static final LoadMeshHelper.ScenePose NORMAL = new LoadMeshHelper.ScenePose(1.0F, 1.0F, 0.0F, -1);

      public float height() {
         return this.height;
      }

      public int color() {
         return this.color;
      }

      public float scale() {
         return this.scale;
      }

      public float roll() {
         return this.roll;
      }
   }
}
