package net.skycore.client.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.mixin.GuiGraphicsExtractorAccessor;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class EnsureIndexHelper {
   private final UploadHelper gpu;
   private final Map<String, EnsureIndexHelper.Mesh> meshes;
   private final Map<String, EnsureIndexHelper.AnimatedMesh> animatedMeshes;
   private final Minecraft mc = Minecraft.getInstance();
   private final Map<String, JsonObject> animationJson;
   private final Map<String, EnsureIndexHelper.Asset> assets = new HashMap<>();
   private static final float PX = 0.0625F;
   private boolean indexed;

   private void ensureIndex() {
      if (!this.indexed) {
         this.indexed = true;
         Identifier texture = Identifier.parse("skycore:cosmetics_port/catalog.json");

         try {
            Resource resourceManager = this.mc.getResourceManager().getResource(texture).orElseThrow();

            try (InputStreamReader inputStreamReader = new InputStreamReader(resourceManager.open(), StandardCharsets.UTF_8)) {
               JsonObject asJsonObject = JsonParser.parseReader(inputStreamReader).getAsJsonObject();

               for (JsonElement jsonElement : asJsonObject.getAsJsonArray("items")) {
                  JsonObject asJsonObject2 = jsonElement.getAsJsonObject();
                  String text = str(asJsonObject2, "id", "");
                  String text2 = str(asJsonObject2, "category", "");
                  String text3 = str(asJsonObject2, "path", text2 + "/" + text);
                  if (!text.isBlank() && !text3.isBlank()) {
                     this.assets
                        .put(
                           text.toLowerCase(Locale.ROOT),
                           new EnsureIndexHelper.Asset(
                              text,
                              text2,
                              Identifier.parse("skycore:cosmetics_port/" + text3 + "/geometry.json"),
                              Identifier.parse("skycore:cosmetics_port/" + text3 + "/animation.json"),
                              Identifier.parse("skycore:cosmetics_port/" + text3 + "/texture.png"),
                              this.loadMetadata(Identifier.parse("skycore:cosmetics_port/" + text3 + "/meta.json"))
                           )
                        );
                  }
               }
            }
         } catch (Exception failure) {
         }
      }
   }

   private static Vector3f sampleAnimationTrack(JsonElement jsonElement, double value, Vector3f vector, boolean condition, double value2) {
      if (jsonElement == null || jsonElement.isJsonNull()) {
         return new Vector3f(vector);
      }

      if (!jsonElement.isJsonObject()) {
         return keyframeVector(jsonElement, vector);
      }

      JsonObject asJsonObject = jsonElement.getAsJsonObject();
      if (!asJsonObject.has("vector") && !asJsonObject.has("pre") && !asJsonObject.has("post")) {
         ArrayList elements = new ArrayList();

         for (Entry entry : asJsonObject.entrySet()) {
            try {
               elements.add(Map.entry(Double.parseDouble((String)entry.getKey()), (JsonElement)entry.getValue()));
            } catch (NumberFormatException failure) {
            }
         }

         if (elements.isEmpty()) {
            return new Vector3f(vector);
         }

         elements.sort(Comparator.<Entry<Double, JsonElement>>comparingDouble(Entry::getKey));
         if (elements.size() == 1) {
            return keyframeVector((JsonElement)((Entry)elements.get(0)).getValue(), vector);
         }

         if (condition && value2 > 0.0 && value >= 0.0) {
            double minimum = Math.min(0.12, value2 * 0.1);
            double computedValue = value2 - minimum;
            if (minimum > 0.0 && value > computedValue) {
               Vector3f vector2 = sampleSortedFrames(elements, computedValue, vector);
               Vector3f vector3 = sampleSortedFrames(elements, 0.0, vector);
               float computedValue2 = smoothstep((float)((value - computedValue) / minimum));
               return vector2.lerp(vector3, computedValue2, new Vector3f());
            }
         }

         return sampleSortedFrames(elements, value, vector);
      } else {
         return keyframeVector(asJsonObject, vector);
      }
   }

   private static EnsureIndexHelper.Face[] cubeUvs(JsonObject json, float value, float value2, float value3, float value4, float value5) {
      EnsureIndexHelper.Face[] faceValues = new EnsureIndexHelper.Face[6];
      if (json.has("uv") && json.get("uv").isJsonObject()) {
         JsonObject asJsonObject = json.getAsJsonObject("uv");
         faceValues[0] = faceUv(asJsonObject, "north", value, value2);
         faceValues[1] = faceUv(asJsonObject, "south", value, value2);
         faceValues[2] = faceUv(asJsonObject, "west", value, value2);
         faceValues[3] = faceUv(asJsonObject, "east", value, value2);
         faceValues[4] = faceUv(asJsonObject, "up", value, value2);
         faceValues[5] = faceUv(asJsonObject, "down", value, value2);
         return faceValues;
      }

      float value6 = 0.0F;
      float value7 = 0.0F;
      if (json.has("uv") && json.get("uv").isJsonArray()) {
         JsonArray asJsonArray = json.getAsJsonArray("uv");
         if (asJsonArray.size() >= 2) {
            value6 = asJsonArray.get(0).getAsFloat();
            value7 = asJsonArray.get(1).getAsFloat();
         }
      }

      faceValues[2] = rect(value6 + value5 + value3, value7 + value5, value5, value4, value, value2);
      faceValues[3] = rect(value6, value7 + value5, value5, value4, value, value2);
      faceValues[0] = rect(value6 + value5, value7 + value5, value3, value4, value, value2);
      faceValues[1] = rect(value6 + value5 + value3 + value5, value7 + value5, value3, value4, value, value2);
      faceValues[4] = rect(value6 + value5, value7, value3, value5, value, value2);
      faceValues[5] = rect(value6 + value5 + value3, value7 + value5, value3, -value5, value, value2);
      return faceValues;
   }

   private EnsureIndexHelper.Mesh animatedMesh(EnsureIndexHelper.Asset asset) {
      double computedValue = System.nanoTime() * 1.0E-9;
      long roundedDown = (long)Math.floor(computedValue * 120.0);
      EnsureIndexHelper.AnimatedMesh animatedMesh = this.animatedMeshes.get(asset.id);
      if (animatedMesh != null && animatedMesh.frame == roundedDown) {
         return animatedMesh.mesh;
      }

      var lookup = this.loadAnimationPose(asset.animation, computedValue);
      EnsureIndexHelper.Mesh mesh;
      if (lookup.isEmpty()) {
         var meshes2 = this.meshes;
         String id = asset.id;
         Function<java.lang.String, net.skycore.client.util.EnsureIndexHelper.Mesh> function = text -> {
            return this.loadMesh(asset);
         };
         mesh = (EnsureIndexHelper.Mesh)meshes2.computeIfAbsent(id, function);
      } else {
         mesh = this.loadMesh(asset, lookup);
      }

      EnsureIndexHelper.Mesh mesh2 = mesh;
      var animatedMeshes2 = this.animatedMeshes;
      String id2 = asset.id;
      EnsureIndexHelper.AnimatedMesh animatedMesh2 = new EnsureIndexHelper.AnimatedMesh(roundedDown, mesh2);
      animatedMeshes2.put(id2, animatedMesh2);
      return mesh2;
   }

   private static Vector3f keyframeVectorForStart(JsonElement jsonElement, Vector3f vector) {
      if (jsonElement != null) {
         if (jsonElement.isJsonObject()) {
            JsonObject asJsonObject = jsonElement.getAsJsonObject();
            if (asJsonObject.has("post")) {
               return keyframeVector(asJsonObject.get("post"), vector);
            }
         }
      }

      return keyframeVector(jsonElement, vector);
   }

   private void submitAttachedId(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, PlayerModel playerModel, int value, String text, String text2) {
      if (text != null && !text.isBlank()) {
         EnsureIndexHelper.Asset asset = this.assets.get(text.toLowerCase(Locale.ROOT));
         if (asset != null) {
            EnsureIndexHelper.Mesh mesh = this.animatedMesh(asset);
            if (!mesh.vertices.isEmpty()) {
               poseStack.pushPose();
               if (!"hats".equals(text2) && !"masks".equals(text2)) {
                  playerModel.body.translateAndRotate(poseStack);
               } else {
                  playerModel.head.translateAndRotate(poseStack);
               }

               poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
               if ("hats".equals(text2)) {
                  poseStack.translate(0.0F, -0.125F, 0.0F);
               }

               EnsureIndexHelper.Metadata metadata = asset.metadata;
               float scale = metadata.scale;
               poseStack.scale(scale, scale, scale);
               float x = metadata.x;
               float y = metadata.y;
               float z = metadata.z;
               if (!metadata.rawOffset) {
                  x += 0.5F;
                  y += 0.51F;
                  z += 0.5F;
               }

               boolean playerSpaceMesh = mesh.playerSpaceMesh;
               if (metadata.rawOffset && playerSpaceMesh && ("hats".equals(text2) || "pets".equals(text2))) {
                  x -= mesh.rawCx;
                  y -= mesh.rawCy;
                  z -= mesh.rawCz;
               }

               poseStack.translate(x, y, z);
               submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(asset.texture), (pose, vertexConsumer) -> {
                  for (int vertexIndex = 0; vertexIndex + 5 < mesh.vertices.size(); vertexIndex += 6) {
                     EnsureIndexHelper.V v = mesh.vertices.get(vertexIndex);
                     EnsureIndexHelper.V v2 = mesh.vertices.get(vertexIndex + 1);
                     EnsureIndexHelper.V v3 = mesh.vertices.get(vertexIndex + 2);
                     EnsureIndexHelper.V v4 = mesh.vertices.get(vertexIndex + 5);
                     float x2 = v2.x - v.x;
                     float y2 = v2.y - v.y;
                     float z2 = v2.z - v.z;
                     float x3 = v3.x - v.x;
                     float y3 = v3.y - v.y;
                     float z3 = v3.z - v.z;
                     float computedValue = y2 * z3 - z2 * y3;
                     float computedValue2 = z2 * x3 - x2 * z3;
                     float computedValue3 = x2 * y3 - y2 * x3;
                     float squareRoot = (float)Math.sqrt(computedValue * computedValue + computedValue2 * computedValue2 + computedValue3 * computedValue3);
                     if (squareRoot > 1.0E-6F) {
                        computedValue /= squareRoot;
                        computedValue2 /= squareRoot;
                        computedValue3 /= squareRoot;
                     } else {
                        computedValue = 0.0F;
                        computedValue2 = 1.0F;
                        computedValue3 = 0.0F;
                     }

                     submitQuadVertex(vertexConsumer, pose, v, value, computedValue, computedValue2, computedValue3);
                     submitQuadVertex(vertexConsumer, pose, v2, value, computedValue, computedValue2, computedValue3);
                     submitQuadVertex(vertexConsumer, pose, v3, value, computedValue, computedValue2, computedValue3);
                     submitQuadVertex(vertexConsumer, pose, v4, value, computedValue, computedValue2, computedValue3);
                  }
               });
               poseStack.popPose();
            }
         }
      }
   }

   private static void submitQuadVertex(VertexConsumer vertexConsumer, Pose pose, EnsureIndexHelper.V v, int value, float value2, float value3, float value4) {
      VertexConsumer vertexConsumer2 = vertexConsumer.addVertex(pose, v.x, v.y, v.z).setColor(1.0F, 1.0F, 1.0F, 1.0F);
      vertexConsumer2 = vertexConsumer2.setUv(v.u, v.v);
      vertexConsumer2 = vertexConsumer2.setOverlay(OverlayTexture.NO_OVERLAY);
      vertexConsumer2 = vertexConsumer2.setLight(value);
      vertexConsumer2.setNormal(pose, value2, value3, value4);
   }

   private static EnsureIndexHelper.PreviewVertex previewVertex(
      EnsureIndexHelper.V v, EnsureIndexHelper.Mesh mesh, float value, float value2, float value3, float value4, float value5, float value6, float value7
   ) {
      float x = v.x - mesh.cx;
      float computedValue = x / mesh.span;
      float y = (v.y - mesh.cy) / mesh.span;
      float z = v.z - mesh.cz;
      float computedValue2 = z / mesh.span;
      float computedValue3 = computedValue * value4 + computedValue2 * value5;
      float computedValue4 = -computedValue * value5 + computedValue2 * value4;
      float computedValue5 = y * value6 - computedValue4 * value7;
      float computedValue6 = y * value7 + computedValue4 * value6;
      float computedValue7 = value + computedValue3 * value3;
      float computedValue8 = value2 - computedValue5 * value3;
      return new EnsureIndexHelper.PreviewVertex(computedValue7, computedValue8, v.u, v.v, computedValue6);
   }

   private void renderId(String text, Vec3 position, float coordinate, String text2) {
      if (text != null && !text.isBlank()) {
         EnsureIndexHelper.Asset asset = this.assets.get(text.toLowerCase(Locale.ROOT));
         if (asset != null) {
            EnsureIndexHelper.Mesh mesh = this.meshes.computeIfAbsent(asset.id, text3 -> {
               return this.loadMesh(asset);
            });
            if (!mesh.vertices.isEmpty()) {
               Camera camera = this.mc.gameRenderer.mainCamera();
               if (camera != null && camera.isInitialized()) {
                  Vec3 position2 = anchor(position, coordinate, text2);

                  float computedValue = switch (text2) {
                     case "wings" -> 1.75F;
                     case "backpacks" -> 0.95F;
                     case "hats" -> 0.72F;
                     case "masks" -> 0.58F;
                     case "pets" -> 0.9F;
                     default -> 0.8F;
                  };
                  float maximum = computedValue / Math.max(0.05F, mesh.span);
                  float angleRadians = (float)Math.toRadians(180.0F - coordinate);
                  float sine = (float)Math.sin(angleRadians);
                  float cosine = (float)Math.cos(angleRadians);
                  Vec3 position3 = camera.position();
                  Matrix4f matrix = CameraViewPoseHelper.cameraViewPose(camera);
                  int size = mesh.vertices.size();

                  try (ByteBufferBuilder vertexSize = ByteBufferBuilder.exactlySized(size * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
                     BufferBuilder bufferBuilder = new BufferBuilder(vertexSize, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);

                     for (EnsureIndexHelper.V v : mesh.vertices) {
                        float x = (v.x - mesh.cx) * maximum;
                        float y = (v.y - mesh.cy) * maximum;
                        float z = (v.z - mesh.cz) * maximum;
                        float computedValue2 = x * cosine + z * sine;
                        float computedValue3 = -x * sine + z * cosine;
                        Vec3 position4 = new Vec3(position2.x + computedValue2, position2.y + y, position2.z + computedValue3);
                        Vector4f vector4f = CameraViewPoseHelper.toViewSpace(position4, position3, matrix);
                        bufferBuilder.addVertex(vector4f.x, vector4f.y, vector4f.z).setUv(v.u, v.v).setColor(-1);
                     }

                     try (MeshData meshData = bufferBuilder.buildOrThrow()) {
                        RenderTarget renderTarget = this.mc.gameRenderer.mainRenderTarget();
                        if (renderTarget != null) {
                           GpuTextureView colorTextureView = renderTarget.getColorTextureView();
                           if (colorTextureView == null) {
                              return;
                           }

                           AbstractTexture texture = this.mc.getTextureManager().getTexture(asset.texture);
                           GpuTextureView textureView = texture == null ? null : texture.getTextureView();
                           if (textureView == null) {
                              return;
                           }

                           int value = meshData.vertexBuffer().remaining();
                           GpuBuffer gpuBuffer = this.gpu.upload(meshData);
                           GpuSampler clampToEdge = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
                           GpuTextureView depthTextureView = renderTarget.getDepthTextureView();
                           RenderPass device = depthTextureView != null ? RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
                              return "Skycore Source Cosmetic";
                           }, colorTextureView, Optional.empty(), depthTextureView, OptionalDouble.empty()) : RenderSystem.getDevice()
                              .createCommandEncoder()
                              .createRenderPass(() -> {
                                 return "Skycore Source Cosmetic";
                              }, colorTextureView, Optional.empty());

                           try {
                              device.setPipeline(LoadMeshHelper.pipeline());
                              device.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                              device.bindTexture("PetSampler", textureView, clampToEdge);
                              device.setVertexBuffer(0, gpuBuffer.slice(0L, value));
                              device.draw(size, 1, 0, 0);
                              return;
                           } finally {
                              device.close();
                           }
                        }
                     }
                  } catch (Throwable failure) {
                  }
               }
            }
         }
      }
   }

   private static boolean bool(JsonObject json, String text, boolean condition) {

      try {
         boolean condition2;
         if (json.has(text) && !json.get(text).isJsonNull()) {
            JsonElement jsonElement = json.get(text);
            condition2 = jsonElement.getAsBoolean();
         } else {
            condition2 = condition;
         }

         return condition2;
      } catch (Exception failure) {
         return condition;
      }
   }

   public void renderPetWorld(String text) {
      if (this.mc.player != null) {
         if (this.mc.level != null) {
            if (this.mc.gameRenderer != null && text != null) {
               if (!text.isBlank()) {
                  this.ensureIndex();
                  EnsureIndexHelper.Asset asset = this.assets.get(text.toLowerCase(Locale.ROOT));
                  if (asset != null) {
                     if ("orbit".equalsIgnoreCase(asset.metadata.placement)) {
                        DeltaTracker deltaTracker = this.mc.getDeltaTracker();
                        float gameTimeDeltaPartialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
                        Vec3 position = CameraViewPoseHelper.interpolatedPosition(this.mc.player, gameTimeDeltaPartialTick);
                        float yRot = this.mc.player.getYRot();
                        this.renderId(text, position, yRot, "pets");
                        return;
                     }
                  }

                  return;
               }
            }
         }
      }
   }

   private static float smoothstep(float progress) {
      float minimum = Math.min(1.0F, progress);
      progress = Math.max(0.0F, minimum);
      return progress * progress * (3.0F - 2.0F * progress);
   }

   public EnsureIndexHelper() {
      this.meshes = new HashMap<>();
      this.animatedMeshes = new HashMap<>();
      this.animationJson = new HashMap<>();
      this.gpu = new UploadHelper("Skycore Source Cosmetics");
   }

   private static float applyEasing(JsonElement jsonElement, float result) {
      if (jsonElement != null && jsonElement.isJsonObject()) {
         JsonObject asJsonObject = jsonElement.getAsJsonObject();
         if (asJsonObject.has("easing") && !asJsonObject.get("easing").isJsonNull()) {
            String asString = asJsonObject.get("easing").getAsString().toLowerCase(Locale.ROOT);

            return switch (asString) {
               case "easeinoutquad" -> result < 0.5F ? 2.0F * result * result : 1.0F - (float)Math.pow(-2.0F * result + 2.0F, 2.0) * 0.5F;
               case "easeinquad" -> result * result;
               case "easeoutquad" -> 1.0F - (1.0F - result) * (1.0F - result);
               default -> result;
            };
         } else {
            return result;
         }
      } else {
         return result;
      }
   }

   public void renderGuiPreview(String text, float coordinate, float coordinate2, float coordinate3, float coordinate4) {
      if (text != null && !text.isBlank() && !(coordinate3 <= 0.0F) && !(coordinate4 <= 0.0F)) {
         this.ensureIndex();
         EnsureIndexHelper.Asset asset = this.assets.get(text.toLowerCase(Locale.ROOT));
         if (asset != null) {
            EnsureIndexHelper.Mesh mesh = this.animatedMesh(asset);
            if (!mesh.vertices.isEmpty()) {
               GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
               if (guiGraphicsExtractor != null) {
                  float angleRadians = (float)Math.toRadians(asset.metadata.previewYaw);
                  float angleRadians2 = (float)Math.toRadians(asset.metadata.previewPitch);
                  float cosine = (float)Math.cos(angleRadians);
                  float sine = (float)Math.sin(angleRadians);
                  float cosine2 = (float)Math.cos(angleRadians2);
                  float sine2 = (float)Math.sin(angleRadians2);
                  float computedValue = Math.max(0.78F, Math.min(1.18F, asset.metadata.previewScale));
                  float computedValue2 = coordinate3 * 0.78F * computedValue;
                  float computedValue3 = coordinate + coordinate3 * 0.5F;
                  float computedValue4 = coordinate2 + coordinate3 * (0.52F - asset.metadata.previewY);
                  ArrayList<EnsureIndexHelper.PreviewFace> previewFaces = new ArrayList(mesh.vertices.size() / 6);

                  for (int vertexIndex = 0; vertexIndex + 5 < mesh.vertices.size(); vertexIndex += 6) {
                     EnsureIndexHelper.PreviewVertex previewVertex = previewVertex(mesh.vertices.get(vertexIndex), mesh, computedValue3, computedValue4, computedValue2, cosine, sine, cosine2, sine2);
                     EnsureIndexHelper.PreviewVertex previewVertex2 = previewVertex(mesh.vertices.get(vertexIndex + 1), mesh, computedValue3, computedValue4, computedValue2, cosine, sine, cosine2, sine2);
                     EnsureIndexHelper.PreviewVertex previewVertex3 = previewVertex(mesh.vertices.get(vertexIndex + 2), mesh, computedValue3, computedValue4, computedValue2, cosine, sine, cosine2, sine2);
                     EnsureIndexHelper.PreviewVertex previewVertex4 = previewVertex(mesh.vertices.get(vertexIndex + 5), mesh, computedValue3, computedValue4, computedValue2, cosine, sine, cosine2, sine2);
                     previewFaces.add(new EnsureIndexHelper.PreviewFace(previewVertex, previewVertex2, previewVertex3, previewVertex4, (previewVertex.depth + previewVertex2.depth + previewVertex3.depth + previewVertex4.depth) * 0.25F));
                  }

                  previewFaces.sort(Comparator.comparingDouble(EnsureIndexHelper.PreviewFace::depth));
                  float[] floatValues = new float[previewFaces.size() * 24];
                  int value = 0;

                  for (EnsureIndexHelper.PreviewFace previewFace : previewFaces) {
                     EnsureIndexHelper.PreviewVertex[] previewVertexValues = new EnsureIndexHelper.PreviewVertex[]{previewFace.a, previewFace.b, previewFace.c, previewFace.a, previewFace.c, previewFace.d};

                     for (EnsureIndexHelper.PreviewVertex previewVertex5 : previewVertexValues) {
                        floatValues[value++] = previewVertex5.x;
                        floatValues[value++] = previewVertex5.y;
                        floatValues[value++] = previewVertex5.u;
                        floatValues[value++] = previewVertex5.v;
                     }
                  }

                  int computedValue5 = Math.max(0, Math.min(255, Math.round(coordinate4 * 255.0F)));
                  int value2 = computedValue5 << 24 | 16777215;
                  BeginFrameHelper.flush();
                  ((GuiGraphicsExtractorAccessor)guiGraphicsExtractor)
                     .getGuiRenderState()
                     .addGuiElement(new TexturedTriangleRenderState(guiGraphicsExtractor.pose(), asset.texture, floatValues, value2, coordinate, coordinate2, coordinate3, coordinate3, BeginFrameHelper.activeScissor()));
               }
            }
         }
      }
   }

   private static String str(JsonObject json, String text, String text2) {
      if (json.has(text)) {
         JsonElement jsonElement = json.get(text);
         if (!jsonElement.isJsonNull()) {
            return json.get(text).getAsString();
         }
      }

      return text2;
   }

   private static Vector3f vec(JsonArray jsonValues) {
      if (jsonValues == null) {
         return new Vector3f();
      }

      float asFloat = jsonValues.size() > 0 ? jsonValues.get(0).getAsFloat() : 0.0F;
      float asFloat2 = jsonValues.size() > 1 ? jsonValues.get(1).getAsFloat() : 0.0F;
      float value;
      if (jsonValues.size() > 2) {
         JsonElement jsonElement = jsonValues.get(2);
         value = jsonElement.getAsFloat();
      } else {
         value = 0.0F;
      }

      float value2 = value;
      return new Vector3f(asFloat, asFloat2, value2);
   }

   private EnsureIndexHelper.Mesh loadMesh(EnsureIndexHelper.Asset asset) {
      return this.loadMesh(asset, Map.of());
   }

   private static Vector3f keyframeVectorForEnd(JsonElement jsonElement, Vector3f vector) {
      if (jsonElement != null && jsonElement.isJsonObject()) {
         JsonObject asJsonObject = jsonElement.getAsJsonObject();
         if (asJsonObject.has("pre")) {
            JsonElement jsonElement2 = asJsonObject.get("pre");
            return keyframeVector(jsonElement2, vector);
         }
      }

      return keyframeVector(jsonElement, vector);
   }

   private EnsureIndexHelper.Mesh loadMesh(EnsureIndexHelper.Asset asset, Map<String, EnsureIndexHelper.AnimationPose> lookup) {
      try {
         Resource resourceManager = this.mc.getResourceManager().getResource(asset.geometry).orElseThrow();

         try (InputStreamReader inputStreamReader = new InputStreamReader(resourceManager.open(), StandardCharsets.UTF_8)) {
            JsonObject asJsonObject = JsonParser.parseReader(inputStreamReader).getAsJsonObject();
            JsonArray asJsonArray = asJsonObject.getAsJsonArray("minecraft:geometry");
            if (asJsonArray == null || asJsonArray.isEmpty()) {
               return EnsureIndexHelper.Mesh.empty();
            }

            JsonObject asJsonObject2 = asJsonArray.get(0).getAsJsonObject();
            JsonObject asJsonObject3 = asJsonObject2.getAsJsonObject("description");
            float asFloat = asJsonObject3 != null && asJsonObject3.has("texture_width") ? asJsonObject3.get("texture_width").getAsFloat() : 64.0F;
            float asFloat2 = asJsonObject3 != null && asJsonObject3.has("texture_height") ? asJsonObject3.get("texture_height").getAsFloat() : 64.0F;
            LinkedHashMap<String, EnsureIndexHelper.Bone> lookup2 = new LinkedHashMap<>();
            JsonArray asJsonArray2 = asJsonObject2.getAsJsonArray("bones");
            if (asJsonArray2 == null) {
               return EnsureIndexHelper.Mesh.empty();
            }

            for (JsonElement jsonElement : asJsonArray2) {
               JsonObject asJsonObject4 = jsonElement.getAsJsonObject();
               String text = str(asJsonObject4, "name", UUID.randomUUID().toString());
               String text2 = str(asJsonObject4, "parent", "");
               Vector3f asJsonArray3 = vec(asJsonObject4.getAsJsonArray("pivot"));
               Vector3f asJsonArray4 = vec(asJsonObject4.getAsJsonArray("rotation"));
               lookup2.put(text, new EnsureIndexHelper.Bone(text, text2, asJsonArray3, asJsonArray4, asJsonObject4.getAsJsonArray("cubes")));
            }

            float[] floatValues = rawMeshBounds(asJsonArray2);
            ArrayList elements = new ArrayList();
            HashMap lookup3 = new HashMap();

            for (EnsureIndexHelper.Bone bone : lookup2.values()) {
               Matrix4f matrix = boneTransform(bone, lookup2, lookup3, lookup);
               if (bone.cubes != null) {
                  for (JsonElement jsonElement2 : bone.cubes) {
                     bakeCube(jsonElement2.getAsJsonObject(), matrix, asFloat, asFloat2, elements);
                  }
               }
            }

            return normalize(elements, floatValues);
         }
      } catch (Exception failure) {
         return EnsureIndexHelper.Mesh.empty();
      }
   }

   private static void face(
      List<EnsureIndexHelper.V> vs,
      Matrix4f matrix,
      float value,
      float value2,
      float value3,
      float value4,
      float value5,
      float value6,
      float value7,
      float value8,
      float value9,
      float value10,
      float value11,
      float value12,
      EnsureIndexHelper.Face face
   ) {
      if (face != null) {
         Vector3f vector = transform(matrix, value, value2, value3);
         Vector3f vector2 = transform(matrix, value4, value5, value6);
         Vector3f vector3 = transform(matrix, value7, value8, value9);
         Vector3f vector4 = transform(matrix, value10, value11, value12);
         vs.add(new EnsureIndexHelper.V(vector.x, vector.y, vector.z, face.u1, face.v2));
         vs.add(new EnsureIndexHelper.V(vector2.x, vector2.y, vector2.z, face.u2, face.v2));
         vs.add(new EnsureIndexHelper.V(vector3.x, vector3.y, vector3.z, face.u2, face.v1));
         vs.add(new EnsureIndexHelper.V(vector.x, vector.y, vector.z, face.u1, face.v2));
         vs.add(new EnsureIndexHelper.V(vector3.x, vector3.y, vector3.z, face.u2, face.v1));
         vs.add(new EnsureIndexHelper.V(vector4.x, vector4.y, vector4.z, face.u1, face.v1));
      }
   }

   private static Vector3f keyframeVector(JsonElement jsonElement, Vector3f vector) {
      if (jsonElement == null || jsonElement.isJsonNull()) {
         return new Vector3f(vector);
      }

      if (jsonElement.isJsonArray()) {
         return animationArray(jsonElement.getAsJsonArray(), vector);
      }

      if (jsonElement.isJsonPrimitive()) {
         try {
            float asFloat = jsonElement.getAsFloat();
            return new Vector3f(asFloat, asFloat, asFloat);
         } catch (Exception failure) {
            return new Vector3f(vector);
         }
      } else {
         JsonObject asJsonObject = jsonElement.getAsJsonObject();
         if (asJsonObject.has("vector")) {
            return keyframeVector(asJsonObject.get("vector"), vector);
         } else if (asJsonObject.has("post")) {
            JsonElement jsonElement2 = asJsonObject.get("post");
            return jsonElement2.isJsonObject() && jsonElement2.getAsJsonObject().has("vector")
               ? keyframeVector(jsonElement2.getAsJsonObject().get("vector"), vector)
               : keyframeVector(jsonElement2, vector);
         } else if (asJsonObject.has("pre")) {
            JsonElement jsonElement3 = asJsonObject.get("pre");
            return jsonElement3.isJsonObject() && jsonElement3.getAsJsonObject().has("vector")
               ? keyframeVector(jsonElement3.getAsJsonObject().get("vector"), vector)
               : keyframeVector(jsonElement3, vector);
         } else {
            return new Vector3f(vector);
         }
      }
   }

   private static EnsureIndexHelper.Face faceUv(JsonObject json, String text, float value, float value2) {
      if (json.has(text) && json.get(text).isJsonObject()) {
         JsonObject asJsonObject = json.getAsJsonObject(text);
         JsonArray asJsonArray = asJsonObject.getAsJsonArray("uv");
         JsonArray asJsonArray2 = asJsonObject.getAsJsonArray("uv_size");
         if (asJsonArray != null && asJsonArray.size() >= 2) {
            float asFloat = asJsonArray.get(0).getAsFloat();
            float asFloat2 = asJsonArray.get(1).getAsFloat();
            float asFloat3 = asJsonArray2 != null && asJsonArray2.size() >= 2 ? asJsonArray2.get(0).getAsFloat() : 1.0F;
            float asFloat4 = asJsonArray2 != null && asJsonArray2.size() >= 2 ? asJsonArray2.get(1).getAsFloat() : 1.0F;
            return rect(asFloat, asFloat2, asFloat3, asFloat4, value, value2);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private Map<String, EnsureIndexHelper.AnimationPose> loadAnimationPose(Identifier texture, double value) {
      HashMap lookup = new HashMap();

      try {
         JsonObject json = this.animationRoot(texture);
         JsonObject asJsonObject = json.getAsJsonObject("animations");
         if (asJsonObject == null || asJsonObject.isEmpty()) {
            return lookup;
         }

         JsonObject asJsonObject2 = asJsonObject.entrySet().iterator().next().getValue().getAsJsonObject();
         double asDouble = asJsonObject2.has("animation_length") && !asJsonObject2.get("animation_length").isJsonNull()
            ? Math.max(0.001, asJsonObject2.get("animation_length").getAsDouble())
            : 1.0;
         int asBoolean = asJsonObject2.has("loop") && !asJsonObject2.get("loop").getAsBoolean() ? 0 : 1;
         double minimum = asBoolean != 0 ? value % asDouble : Math.min(value, asDouble);
         JsonObject asJsonObject3 = asJsonObject2.getAsJsonObject("bones");
         if (asJsonObject3 == null) {
            return lookup;
         }

         for (Entry entry : asJsonObject3.entrySet()) {
            if (((JsonElement)entry.getValue()).isJsonObject()) {
               JsonObject asJsonObject4 = ((JsonElement)entry.getValue()).getAsJsonObject();
               Vector3f vector = sampleAnimationTrack(asJsonObject4.get("rotation"), minimum, new Vector3f(), (asBoolean != 0), asDouble);
               Vector3f vector2 = sampleAnimationTrack(asJsonObject4.get("position"), minimum, new Vector3f(), (asBoolean != 0), asDouble);
               Vector3f vector3 = sampleAnimationTrack(asJsonObject4.get("scale"), minimum, new Vector3f(1.0F, 1.0F, 1.0F), (asBoolean != 0), asDouble);
               lookup.put((String)entry.getKey(), new EnsureIndexHelper.AnimationPose(vector, vector2, vector3));
            }
         }
      } catch (Exception failure) {
      }

      return lookup;
   }

   public void submitAttached(
      PoseStack poseStack, SubmitNodeCollector submitNodeCollector, PlayerModel playerModel, int value, String text, String text2, String text3, String text4, String text5
   ) {
      this.ensureIndex();
      this.submitAttachedId(poseStack, submitNodeCollector, playerModel, value, text, "wings");
      this.submitAttachedId(poseStack, submitNodeCollector, playerModel, value, text2, "backpacks");
      this.submitAttachedId(poseStack, submitNodeCollector, playerModel, value, text3, "hats");
      this.submitAttachedId(poseStack, submitNodeCollector, playerModel, value, text4, "masks");
      this.submitAttachedId(poseStack, submitNodeCollector, playerModel, value, text5, "pets");
   }

   private static EnsureIndexHelper.Mesh normalize(List<EnsureIndexHelper.V> vs, float[] floatValues) {
      if (vs.isEmpty()) {
         return EnsureIndexHelper.Mesh.empty();
      }

      float pOSITIVE_INFINITY = Float.POSITIVE_INFINITY;
      float pOSITIVE_INFINITY2 = Float.POSITIVE_INFINITY;
      float pOSITIVE_INFINITY3 = Float.POSITIVE_INFINITY;
      float nEGATIVE_INFINITY = Float.NEGATIVE_INFINITY;
      float nEGATIVE_INFINITY2 = Float.NEGATIVE_INFINITY;
      float nEGATIVE_INFINITY3 = Float.NEGATIVE_INFINITY;

      for (EnsureIndexHelper.V v : vs) {
         pOSITIVE_INFINITY = Math.min(pOSITIVE_INFINITY, v.x);
         pOSITIVE_INFINITY2 = Math.min(pOSITIVE_INFINITY2, v.y);
         pOSITIVE_INFINITY3 = Math.min(pOSITIVE_INFINITY3, v.z);
         nEGATIVE_INFINITY = Math.max(nEGATIVE_INFINITY, v.x);
         nEGATIVE_INFINITY2 = Math.max(nEGATIVE_INFINITY2, v.y);
         nEGATIVE_INFINITY3 = Math.max(nEGATIVE_INFINITY3, v.z);
      }

      float computedValue = (pOSITIVE_INFINITY + nEGATIVE_INFINITY) * 0.5F;
      float computedValue2 = (pOSITIVE_INFINITY2 + nEGATIVE_INFINITY2) * 0.5F;
      float computedValue3 = (pOSITIVE_INFINITY3 + nEGATIVE_INFINITY3) * 0.5F;
      float computedValue4 = Math.max(nEGATIVE_INFINITY - pOSITIVE_INFINITY, Math.max(nEGATIVE_INFINITY2 - pOSITIVE_INFINITY2, nEGATIVE_INFINITY3 - pOSITIVE_INFINITY3));
      float value = floatValues != null && floatValues.length > 0 ? floatValues[0] : 0.0F;
      float value2 = floatValues != null && floatValues.length > 1 ? floatValues[1] : 0.0F;
      float value3 = floatValues != null && floatValues.length > 2 ? floatValues[2] : 0.0F;
      int computedValue5 = Math.abs(value2) > 0.85F ? 1 : 0;
      return new EnsureIndexHelper.Mesh(List.copyOf(vs), computedValue, computedValue2, computedValue3, Math.max(0.05F, computedValue4), value, value2, value3, (computedValue5 != 0));
   }

   private static Vector3f animationArray(JsonArray jsonValues, Vector3f vector) {

      try {
         if (jsonValues.size() == 1) {
            float asFloat = jsonValues.get(0).getAsFloat();
            return new Vector3f(asFloat, asFloat, asFloat);
         }

         if (jsonValues.size() >= 3) {
            float asFloat2 = jsonValues.get(0).getAsFloat();
            JsonElement jsonElement = jsonValues.get(1);
            float asFloat3 = jsonElement.getAsFloat();
            float asFloat4 = jsonValues.get(2).getAsFloat();
            return new Vector3f(asFloat2, asFloat3, asFloat4);
         }
      } catch (Exception failure) {
      }

      return new Vector3f(vector);
   }

   private static Vector3f sampleSortedFrames(List<Entry<Double, JsonElement>> doubles, double value, Vector3f vector) {
      Entry entry = (Entry)doubles.get(0);
      Entry entry2 = (Entry)doubles.get(doubles.size() - 1);
      if (value <= (Double)entry.getKey()) {
         return keyframeVector((JsonElement)entry.getValue(), vector);
      }

      if (value >= (Double)entry2.getKey()) {
         return keyframeVector((JsonElement)entry2.getValue(), vector);
      }

      for (int value2 = 1; value2 < doubles.size(); value2++) {
         Entry entry3 = (Entry)doubles.get(value2);
         if (value <= (Double)entry3.getKey()) {
            entry2 = entry3;
            entry = (Entry)doubles.get(value2 - 1);
            break;
         }
      }

      double key = Math.max(1.0E-9, (Double)entry2.getKey() - (Double)entry.getKey());
      float key2 = (float)((value - (Double)entry.getKey()) / key);
      key2 = hasEasing((JsonElement)entry2.getValue()) ? applyEasing((JsonElement)entry2.getValue(), key2) : (key >= 0.12 ? smoothstep(key2) : key2);
      Vector3f value3 = keyframeVectorForStart((JsonElement)entry.getValue(), vector);
      Vector3f value4 = keyframeVectorForEnd((JsonElement)entry2.getValue(), vector);
      return value3.lerp(value4, key2, new Vector3f());
   }

   public void renderSelected(String text, String text2, String text3, String text4, String text5) {
      if (this.mc.player != null && this.mc.level != null && this.mc.gameRenderer != null) {
         this.ensureIndex();
         float gameTimeDeltaPartialTick = this.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         Vec3 position = CameraViewPoseHelper.interpolatedPosition(this.mc.player, gameTimeDeltaPartialTick);
         float yRot = this.mc.player.getYRot();
         this.renderId(text, position, yRot, "wings");
         this.renderId(text2, position, yRot, "backpacks");
         this.renderId(text3, position, yRot, "hats");
         this.renderId(text4, position, yRot, "masks");
         this.renderId(text5, position, yRot, "pets");
      }
   }

   private static float number(JsonObject json, String text, float result) {

      try {
         return json.has(text) && !json.get(text).isJsonNull() ? json.get(text).getAsFloat() : result;
      } catch (Exception failure) {
         return result;
      }
   }

   private static Vec3 anchor(Vec3 position, float value, String text) {
      double angleRadians = Math.toRadians(value);
      double sine = -Math.sin(angleRadians);
      double cosine = Math.cos(angleRadians);
      double cosine2 = Math.cos(angleRadians);
      double sine2 = Math.sin(angleRadians);

      return switch (text) {
         case "hats" -> position.add(0.0, 1.73, 0.0);
         case "masks" -> position.add(sine * 0.1, 1.58, cosine * 0.1);
         case "backpacks" -> position.add(-sine * 0.24, 1.05, -cosine * 0.24);
         case "wings" -> position.add(-sine * 0.16, 1.08, -cosine * 0.16);
         case "pets" -> position.add(cosine2 * 0.95 - sine * 0.25, 0.42, sine2 * 0.95 - cosine * 0.25);
         default -> position.add(0.0, 1.0, 0.0);
      };
   }

   private static boolean hasEasing(JsonElement jsonElement) {
      if (jsonElement != null && jsonElement.isJsonObject()) {
         JsonObject asJsonObject = jsonElement.getAsJsonObject();
         if (asJsonObject.has("easing")) {
            JsonElement asJsonObject2 = jsonElement.getAsJsonObject().get("easing");
            if (!asJsonObject2.isJsonNull()) {
               return true;
            }
         }
      }

      return false;
   }

   private static EnsureIndexHelper.Face rect(float value, float value2, float value3, float value4, float value5, float value6) {
      float intermediate = value / value5;
      float intermediate2 = value2 / value6;
      float intermediate3 = (value + value3) / value5;
      float intermediate4 = (value2 + value4) / value6;
      return new EnsureIndexHelper.Face(intermediate, intermediate2, intermediate3, intermediate4);
   }

   private EnsureIndexHelper.Metadata loadMetadata(Identifier texture) {
      try {
         Resource resourceManager = this.mc.getResourceManager().getResource(texture).orElseThrow();

         try (InputStreamReader inputStreamReader = new InputStreamReader(resourceManager.open(), StandardCharsets.UTF_8)) {
            JsonObject asJsonObject = JsonParser.parseReader(inputStreamReader).getAsJsonObject();
            return new EnsureIndexHelper.Metadata(
               number(asJsonObject, "scale", 1.0F),
               number(asJsonObject, "x", 0.0F),
               number(asJsonObject, "y", 0.0F),
               number(asJsonObject, "z", 0.0F),
               number(asJsonObject, "preview_scale", 1.0F),
               number(asJsonObject, "preview_yaw", -38.0F),
               number(asJsonObject, "preview_pitch", 22.0F),
               number(asJsonObject, "preview_y", 0.0F),
               asJsonObject.has("raw_offset") && !asJsonObject.get("raw_offset").isJsonNull()
                  ? asJsonObject.get("raw_offset").getAsBoolean()
                  : asJsonObject.has("pack_id") || asJsonObject.has("pack_type"),
               str(asJsonObject, "placement", "orbit")
            );
         }
      } catch (Exception failure) {
         return EnsureIndexHelper.Metadata.DEFAULT;
      }
   }

   private static float[] rawMeshBounds(JsonArray jsonValues) {
      float pOSITIVE_INFINITY = Float.POSITIVE_INFINITY;
      float pOSITIVE_INFINITY2 = Float.POSITIVE_INFINITY;
      float pOSITIVE_INFINITY3 = Float.POSITIVE_INFINITY;
      float nEGATIVE_INFINITY = Float.NEGATIVE_INFINITY;
      float nEGATIVE_INFINITY2 = Float.NEGATIVE_INFINITY;
      float nEGATIVE_INFINITY3 = Float.NEGATIVE_INFINITY;
      byte flag = 0;

      for (JsonElement jsonElement : jsonValues) {
         JsonArray asJsonArray = jsonElement.getAsJsonObject().getAsJsonArray("cubes");
         if (asJsonArray != null) {
            for (JsonElement jsonElement2 : asJsonArray) {
               JsonObject asJsonObject = jsonElement2.getAsJsonObject();
               JsonArray asJsonArray2 = asJsonObject.getAsJsonArray("origin");
               JsonArray asJsonArray3 = asJsonObject.getAsJsonArray("size");
               if (asJsonArray2 != null && asJsonArray3 != null && asJsonArray2.size() >= 3 && asJsonArray3.size() >= 3) {
                  float asFloat = asJsonObject.has("inflate") ? asJsonObject.get("inflate").getAsFloat() : 0.0F;
                  float asFloat2 = asJsonArray2.get(0).getAsFloat();
                  float asFloat3 = asJsonArray2.get(1).getAsFloat();
                  float asFloat4 = asJsonArray2.get(2).getAsFloat();
                  float asFloat5 = asJsonArray3.get(0).getAsFloat();
                  float asFloat6 = asJsonArray3.get(1).getAsFloat();
                  float asFloat7 = asJsonArray3.get(2).getAsFloat();
                  float computedValue = -(asFloat2 + asFloat5) - asFloat;
                  float computedValue2 = -asFloat2 + asFloat;
                  float computedValue3 = asFloat3 - asFloat;
                  float computedValue4 = asFloat3 + asFloat6 + asFloat;
                  float computedValue5 = asFloat4 - asFloat;
                  float computedValue6 = asFloat4 + asFloat7 + asFloat;
                  pOSITIVE_INFINITY = Math.min(pOSITIVE_INFINITY, Math.min(computedValue, computedValue2));
                  nEGATIVE_INFINITY = Math.max(nEGATIVE_INFINITY, Math.max(computedValue, computedValue2));
                  pOSITIVE_INFINITY2 = Math.min(pOSITIVE_INFINITY2, Math.min(computedValue3, computedValue4));
                  nEGATIVE_INFINITY2 = Math.max(nEGATIVE_INFINITY2, Math.max(computedValue3, computedValue4));
                  pOSITIVE_INFINITY3 = Math.min(pOSITIVE_INFINITY3, Math.min(computedValue5, computedValue6));
                  nEGATIVE_INFINITY3 = Math.max(nEGATIVE_INFINITY3, Math.max(computedValue5, computedValue6));
                  flag = 1;
               }
            }
         }
      }

      return flag == 0
         ? new float[]{0.0F, 0.0F, 0.0F, 0.0F}
         : new float[]{(pOSITIVE_INFINITY + nEGATIVE_INFINITY) * 0.5F * 0.0625F, (pOSITIVE_INFINITY2 + nEGATIVE_INFINITY2) * 0.5F * 0.0625F, (pOSITIVE_INFINITY3 + nEGATIVE_INFINITY3) * 0.5F * 0.0625F, 1.0F};
   }

   private static void bakeCube(JsonObject json, Matrix4f matrix, float value, float value2, List<EnsureIndexHelper.V> vs) {
      Vector3f asJsonArray = vec(json.getAsJsonArray("origin")).mul(0.0625F);
      Vector3f asJsonArray2 = vec(json.getAsJsonArray("size")).mul(0.0625F);
      float asFloat = json.has("inflate") ? json.get("inflate").getAsFloat() * 0.0625F : 0.0F;
      float x = -(asJsonArray.x + asJsonArray2.x) - asFloat;
      float y = asJsonArray.y - asFloat;
      float z = asJsonArray.z - asFloat;
      float x2 = -asJsonArray.x + asFloat;
      float y2 = asJsonArray.y + asJsonArray2.y + asFloat;
      float z2 = asJsonArray.z + asJsonArray2.z + asFloat;
      Vector3f asJsonArray3 = json.has("pivot") ? vec(json.getAsJsonArray("pivot")).mul(0.0625F) : new Vector3f();
      asJsonArray3.x = -asJsonArray3.x;
      Vector3f asJsonArray4 = json.has("rotation") ? vec(json.getAsJsonArray("rotation")) : new Vector3f();
      asJsonArray4.x = -asJsonArray4.x;
      asJsonArray4.y = -asJsonArray4.y;
      Matrix4f matrix2 = new Matrix4f(matrix);
      if (asJsonArray4.lengthSquared() > 0.0F) {
         matrix2.translate(asJsonArray3);
         matrix2.rotateZYX((float)Math.toRadians(asJsonArray4.z), (float)Math.toRadians(asJsonArray4.y), (float)Math.toRadians(asJsonArray4.x));
         matrix2.translate(-asJsonArray3.x, -asJsonArray3.y, -asJsonArray3.z);
      }

      EnsureIndexHelper.Face[] faceValues = cubeUvs(json, value, value2, Math.abs(asJsonArray2.x / 0.0625F), Math.abs(asJsonArray2.y / 0.0625F), Math.abs(asJsonArray2.z / 0.0625F));
      int x3 = Math.abs(asJsonArray2.x) < 1.0E-7F ? 1 : 0;
      int y3 = Math.abs(asJsonArray2.y) < 1.0E-7F ? 1 : 0;
      int z3 = Math.abs(asJsonArray2.z) < 1.0E-7F ? 1 : 0;
      if (x3 == 0 && y3 == 0 && z3 == 0 || z3 != 0) {
         face(vs, matrix2, x, y, z, x2, y, z, x2, y2, z, x, y2, z, faceValues[0]);
         face(vs, matrix2, x2, y, z2, x, y, z2, x, y2, z2, x2, y2, z2, faceValues[1]);
      }

      if (x3 == 0 && y3 == 0 && z3 == 0 || x3 != 0) {
         face(vs, matrix2, x, y, z2, x, y, z, x, y2, z, x, y2, z2, faceValues[2]);
         face(vs, matrix2, x2, y, z, x2, y, z2, x2, y2, z2, x2, y2, z, faceValues[3]);
      }

      if (x3 == 0 && y3 == 0 && z3 == 0 || y3 != 0) {
         face(vs, matrix2, x, y2, z, x2, y2, z, x2, y2, z2, x, y2, z2, faceValues[4]);
         face(vs, matrix2, x, y, z2, x2, y, z2, x2, y, z, x, y, z, faceValues[5]);
      }
   }

   private JsonObject animationRoot(Identifier texture) {
      String text = texture.toString();
      if (this.animationJson.containsKey(text)) {
         return this.animationJson.get(text);
      }

      JsonObject json = new JsonObject();

      try {
         Resource resourceManager = this.mc.getResourceManager().getResource(texture).orElse(null);
         if (resourceManager != null) {
            try (InputStreamReader inputStreamReader = new InputStreamReader(resourceManager.open(), StandardCharsets.UTF_8)) {
               json = JsonParser.parseReader(inputStreamReader).getAsJsonObject();
            }
         }
      } catch (Exception failure) {
      }

      this.animationJson.put(text, json);
      return json;
   }

   private static Matrix4f boneTransform(
      EnsureIndexHelper.Bone bone, Map<String, EnsureIndexHelper.Bone> lookup, Map<String, Matrix4f> lookup2, Map<String, EnsureIndexHelper.AnimationPose> lookup3
   ) {
      Matrix4f matrix = (Matrix4f)lookup2.get(bone.name);
      if (matrix != null) {
         return new Matrix4f(matrix);
      }

      Matrix4f matrix2 = !bone.parent.isBlank() && lookup.containsKey(bone.parent)
         ? boneTransform((EnsureIndexHelper.Bone)lookup.get(bone.parent), lookup, lookup2, lookup3)
         : new Matrix4f();
      EnsureIndexHelper.AnimationPose orDefault = lookup3.getOrDefault(bone.name, EnsureIndexHelper.AnimationPose.IDENTITY);
      if (orDefault.position.lengthSquared() > 0.0F) {
         matrix2.translate(-orDefault.position.x * 0.0625F, orDefault.position.y * 0.0625F, orDefault.position.z * 0.0625F);
      }

      float x = -bone.pivot.x * 0.0625F;
      float y = bone.pivot.y * 0.0625F;
      float z = bone.pivot.z * 0.0625F;
      matrix2.translate(x, y, z);
      matrix2.rotateZYX(
         (float)Math.toRadians(bone.rotation.z + orDefault.rotation.z),
         (float)Math.toRadians(-(bone.rotation.y + orDefault.rotation.y)),
         (float)Math.toRadians(-(bone.rotation.x + orDefault.rotation.x))
      );
      if (orDefault.scale.x != 1.0F || orDefault.scale.y != 1.0F || orDefault.scale.z != 1.0F) {
         matrix2.scale(orDefault.scale.x, orDefault.scale.y, orDefault.scale.z);
      }

      matrix2.translate(-x, -y, -z);
      lookup2.put(bone.name, new Matrix4f(matrix2));
      return matrix2;
   }

   private static Vector3f transform(Matrix4f matrix, float value, float value2, float value3) {
      Vector3f vector = new Vector3f(value, value2, value3);
      return matrix.transformPosition(vector);
   }

   public record AnimatedMesh(long frame, EnsureIndexHelper.Mesh mesh) {
      public EnsureIndexHelper.Mesh mesh() {
         return this.mesh;
      }

      public long frame() {
         return this.frame;
      }
   }

   public record AnimationPose(Vector3f rotation, Vector3f position, Vector3f scale) {
      private static final EnsureIndexHelper.AnimationPose IDENTITY = new EnsureIndexHelper.AnimationPose(
         new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F)
      );

      public Vector3f scale() {
         return this.scale;
      }

      public Vector3f rotation() {
         return this.rotation;
      }

      public Vector3f position() {
         return this.position;
      }
   }

   public record Asset(String id, String category, Identifier geometry, Identifier animation, Identifier texture, EnsureIndexHelper.Metadata metadata) {
      public Identifier texture() {
         return this.texture;
      }

      public Identifier animation() {
         return this.animation;
      }

      public String id() {
         return this.id;
      }

      public EnsureIndexHelper.Metadata metadata() {
         return this.metadata;
      }

      public String category() {
         return this.category;
      }
   }

   public record Bone(String name, String parent, Vector3f pivot, Vector3f rotation, JsonArray cubes) {
      public JsonArray cubes() {
         return this.cubes;
      }

      public Vector3f rotation() {
         return this.rotation;
      }

      public String name() {
         return this.name;
      }

      public String parent() {
         return this.parent;
      }

      public Vector3f pivot() {
         return this.pivot;
      }
   }

   public record Face(float u1, float v1, float u2, float v2) {
      public float u1() {
         return this.u1;
      }

      public float v2() {
         return this.v2;
      }

      public float v1() {
         return this.v1;
      }
   }

   public record Mesh(List<EnsureIndexHelper.V> vertices, float cx, float cy, float cz, float span, float rawCx, float rawCy, float rawCz, boolean playerSpaceMesh) {
      public float cx() {
         return this.cx;
      }

      public float span() {
         return this.span;
      }

      public float rawCy() {
         return this.rawCy;
      }

      public float cy() {
         return this.cy;
      }

      public float rawCz() {
         return this.rawCz;
      }

      public boolean playerSpaceMesh() {
         return this.playerSpaceMesh;
      }

      public List<EnsureIndexHelper.V> vertices() {
         return this.vertices;
      }

      public float rawCx() {
         return this.rawCx;
      }

      public float cz() {
         return this.cz;
      }

      public static EnsureIndexHelper.Mesh empty() {
         List<EnsureIndexHelper.V> vs = List.of();
         return new EnsureIndexHelper.Mesh(vs, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, false);
      }
   }

   public record Metadata(
      float scale, float x, float y, float z, float previewScale, float previewYaw, float previewPitch, float previewY, boolean rawOffset, String placement
   ) {
      private static final EnsureIndexHelper.Metadata DEFAULT = new EnsureIndexHelper.Metadata(
         1.0F, 0.0F, 0.0F, 0.0F, 1.0F, -38.0F, 22.0F, 0.0F, false, "orbit"
      );

      public float x() {
         return this.x;
      }

      public boolean rawOffset() {
         return this.rawOffset;
      }

      public float scale() {
         return this.scale;
      }

      public float previewYaw() {
         return this.previewYaw;
      }

      public float previewPitch() {
         return this.previewPitch;
      }

      public float y() {
         return this.y;
      }

      public float previewY() {
         return this.previewY;
      }

      public float z() {
         return this.z;
      }

      public float previewScale() {
         return this.previewScale;
      }

      public String placement() {
         return this.placement;
      }
   }

   public record PreviewFace(
      EnsureIndexHelper.PreviewVertex a, EnsureIndexHelper.PreviewVertex b, EnsureIndexHelper.PreviewVertex c, EnsureIndexHelper.PreviewVertex d, float depth
   ) {
      public float depth() {
         return this.depth;
      }

      public EnsureIndexHelper.PreviewVertex a() {
         return this.a;
      }

      public EnsureIndexHelper.PreviewVertex c() {
         return this.c;
      }

      public EnsureIndexHelper.PreviewVertex d() {
         return this.d;
      }
   }

   public record PreviewVertex(float x, float y, float u, float v, float depth) {
      public float depth() {
         return this.depth;
      }

      public float x() {
         return this.x;
      }

      public float y() {
         return this.y;
      }

      public float u() {
         return this.u;
      }
   }

   public record V(float x, float y, float z, float u, float v) {
      public float x() {
         return this.x;
      }

      public float u() {
         return this.u;
      }

      public float z() {
         return this.z;
      }

      public float v() {
         return this.v;
      }

      public float y() {
         return this.y;
      }
   }
}
