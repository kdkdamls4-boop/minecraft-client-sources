package net.skycore.client.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.module.render.CosmeticsModule;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class ResolvePrimaryTextureKeyHelper implements SpecialModelRenderer<ResolvePrimaryTextureKeyHelper.Argument> {
   public static final ResolvePrimaryTextureKeyHelper INSTANCE = new ResolvePrimaryTextureKeyHelper();
   private final Minecraft mc = Minecraft.getInstance();
   private final Map<String, ResolvePrimaryTextureKeyHelper.Model> cache = new HashMap<>();
   private static final float PX = 0.0625F;

   private static String resolvePrimaryTextureKey(JsonObject json) {
      JsonObject asJsonObject = json.getAsJsonObject("textures");
      if (asJsonObject != null) {
         if (!asJsonObject.entrySet().isEmpty()) {
            if (asJsonObject.has("0")) {
               return "0";
            }

            for (Entry entry : asJsonObject.entrySet()) {
               Object key = entry.getKey();
               if (!"particle".equals(key)) {
                  return (String)entry.getKey();
               }
            }

            var entries = asJsonObject.entrySet();
            Entry element = (Entry)entries.iterator().next();
            return (String)element.getKey();
         }
      }

      return null;
   }

   public void clear() {
      this.cache.clear();
   }

   private static ResolvePrimaryTextureKeyHelper.P rotate(
      float value, float value2, float value3, float value4, float value5, float value6, ResolvePrimaryTextureKeyHelper.Rotation rotation
   ) {
      if (rotation == ResolvePrimaryTextureKeyHelper.Rotation.NONE) {
         return new ResolvePrimaryTextureKeyHelper.P(value, value2, value3, value4, value5, value6);
      }

      float computedValue = value - rotation.ox;
      float computedValue2 = value2 - rotation.oy;
      float computedValue3 = value3 - rotation.oz;
      float sine = (float)Math.sin(rotation.angle);
      float cosine = (float)Math.cos(rotation.angle);
      float value7 = value;
      float value8 = value2;
      float value9 = value3;
      float value10 = value4;
      float value11 = value5;
      float value12 = value6;
      switch (rotation.axis) {
         case "x":
            value8 = rotation.oy + computedValue2 * cosine - computedValue3 * sine;
            value9 = rotation.oz + computedValue2 * sine + computedValue3 * cosine;
            value11 = value5 * cosine - value6 * sine;
            value12 = value5 * sine + value6 * cosine;
            break;
         case "y":
            value7 = rotation.ox + computedValue * cosine + computedValue3 * sine;
            value9 = rotation.oz - computedValue * sine + computedValue3 * cosine;
            value10 = value4 * cosine + value6 * sine;
            value12 = -value4 * sine + value6 * cosine;
            break;
         case "z":
            value7 = rotation.ox + computedValue * cosine - computedValue2 * sine;
            value8 = rotation.oy + computedValue * sine + computedValue2 * cosine;
            value10 = value4 * cosine - value5 * sine;
            value11 = value4 * sine + value5 * cosine;
      }

      return new ResolvePrimaryTextureKeyHelper.P(value7, value8, value9, value10, value11, value12);
   }

   private static Vector3f vec(JsonArray jsonValues, float value) {
      if (jsonValues == null) {
         return new Vector3f(value, value, value);
      }

      Vector3f vector;
      float intermediate;
      if (jsonValues.size() > 0) {
         intermediate = jsonValues.get(0).getAsFloat();
      } else {
         intermediate = value;
      }

      float intermediate2;
      if (jsonValues.size() > 1) {
         intermediate2 = jsonValues.get(1).getAsFloat();
      } else {
         intermediate2 = value;
      }

      float value2;
      if (jsonValues.size() > 2) {
         JsonElement jsonElement = jsonValues.get(2);
         value2 = jsonElement.getAsFloat();
      } else {
         value2 = value;
      }

      vector = new Vector3f(intermediate, intermediate2, value2);
      return vector;
   }

   private ResolvePrimaryTextureKeyHelper() {
   }

   public ResolvePrimaryTextureKeyHelper.Argument extractArgument(ItemStack itemStack) {
      return selected(ItemDisplayContext.GUI);
   }

   private static void bakeElement(JsonObject json, List<ResolvePrimaryTextureKeyHelper.Vertex> vertexs, String text) {
      JsonArray asJsonArray = json.getAsJsonArray("from");
      JsonArray asJsonArray2 = json.getAsJsonArray("to");
      JsonObject asJsonObject = json.getAsJsonObject("faces");
      if (asJsonArray != null && asJsonArray2 != null && asJsonObject != null) {
         float asFloat = asJsonArray.get(0).getAsFloat() * 0.0625F;
         float asFloat2 = asJsonArray.get(1).getAsFloat() * 0.0625F;
         float asFloat3 = asJsonArray.get(2).getAsFloat() * 0.0625F;
         float asFloat4 = asJsonArray2.get(0).getAsFloat() * 0.0625F;
         float asFloat5 = asJsonArray2.get(1).getAsFloat() * 0.0625F;
         float asFloat6 = asJsonArray2.get(2).getAsFloat() * 0.0625F;
         ResolvePrimaryTextureKeyHelper.Rotation asJsonObject2 = readRotation(json.getAsJsonObject("rotation"));
         addFace(asJsonObject, "north", vertexs, asJsonObject2, text, asFloat4, asFloat2, asFloat3, asFloat, asFloat2, asFloat3, asFloat, asFloat5, asFloat3, asFloat4, asFloat5, asFloat3, 0.0F, 0.0F, -1.0F);
         addFace(asJsonObject, "south", vertexs, asJsonObject2, text, asFloat, asFloat2, asFloat6, asFloat4, asFloat2, asFloat6, asFloat4, asFloat5, asFloat6, asFloat, asFloat5, asFloat6, 0.0F, 0.0F, 1.0F);
         addFace(asJsonObject, "west", vertexs, asJsonObject2, text, asFloat, asFloat2, asFloat3, asFloat, asFloat2, asFloat6, asFloat, asFloat5, asFloat6, asFloat, asFloat5, asFloat3, -1.0F, 0.0F, 0.0F);
         addFace(asJsonObject, "east", vertexs, asJsonObject2, text, asFloat4, asFloat2, asFloat6, asFloat4, asFloat2, asFloat3, asFloat4, asFloat5, asFloat3, asFloat4, asFloat5, asFloat6, 1.0F, 0.0F, 0.0F);
         addFace(asJsonObject, "up", vertexs, asJsonObject2, text, asFloat, asFloat5, asFloat6, asFloat4, asFloat5, asFloat6, asFloat4, asFloat5, asFloat3, asFloat, asFloat5, asFloat3, 0.0F, 1.0F, 0.0F);
         addFace(asJsonObject, "down", vertexs, asJsonObject2, text, asFloat, asFloat2, asFloat3, asFloat4, asFloat2, asFloat3, asFloat4, asFloat2, asFloat6, asFloat, asFloat2, asFloat6, 0.0F, -1.0F, 0.0F);
      }
   }

   private Identifier resolveTexture(JsonObject json, String text) {
      JsonObject asJsonObject = json.getAsJsonObject("textures");
      if (asJsonObject != null && !asJsonObject.entrySet().isEmpty() && text != null && asJsonObject.has(text)) {
         String asString = asJsonObject.get(text).getAsString();
         if (asString.startsWith("skycore:")) {
            asString = asString.substring("skycore:".length());
         }

         if (asString.startsWith("textures/")) {
            asString = asString.substring("textures/".length());
         }

         if (asString.startsWith("item_replacer/sword/")) {
            asString = asString.substring("item_replacer/sword/".length());
         }

         if (asString.startsWith("item/")) {
            asString = asString.substring("item/".length());
         }

         Identifier texture = Identifier.parse("skycore:textures/item_replacer/sword/" + asString + ".png");
         return this.mc.getResourceManager().getResource(texture).isPresent() ? texture : null;
      } else {
         return null;
      }
   }

   private ResolvePrimaryTextureKeyHelper.Model load(String text) {
      Identifier texture = Identifier.parse("skycore:item_replacer/sword/models/" + text + ".json");

      try {
         Resource resourceManager = this.mc.getResourceManager().getResource(texture).orElseThrow();

         try (InputStreamReader inputStreamReader = new InputStreamReader(resourceManager.open(), StandardCharsets.UTF_8)) {
            JsonObject asJsonObject = JsonParser.parseReader(inputStreamReader).getAsJsonObject();
            String text2 = resolvePrimaryTextureKey(asJsonObject);
            Identifier texture2 = this.resolveTexture(asJsonObject, text2);
            if (texture2 == null) {
               return ResolvePrimaryTextureKeyHelper.Model.empty();
            }

            ArrayList elements = new ArrayList();
            JsonArray asJsonArray = asJsonObject.getAsJsonArray("elements");
            if (asJsonArray != null) {
               String text3 = text2 == null ? null : "#" + text2;

               for (JsonElement jsonElement : asJsonArray) {
                  bakeElement(jsonElement.getAsJsonObject(), elements, text3);
               }
            }

            var asJsonObject2 = readDisplays(asJsonObject.getAsJsonObject("display"));
            return new ResolvePrimaryTextureKeyHelper.Model(List.copyOf(elements), texture2, asJsonObject2);
         }
      } catch (Throwable failure) {
         System.err.println("[Skycore/Swords] Failed to load " + text + ": " + failure.getMessage());
         return ResolvePrimaryTextureKeyHelper.Model.empty();
      }
   }

   @Override
   public void getExtents(Consumer<Vector3fc> consumer) {
      Vector3f vector = new Vector3f(-1.5F, -1.5F, -1.5F);
      consumer.accept(vector);
      consumer.accept(new Vector3f(1.5F, 1.5F, 1.5F));
   }

   public static boolean shouldReplace(ItemStack itemStack) {
      if (itemStack != null) {
         if (!itemStack.isEmpty()) {
            if (itemStack.is(ItemTags.SWORDS)) {
               CosmeticsModule cosmeticsModule = SkycoreClient.modules().get(CosmeticsModule.class).orElse(null);
               if (cosmeticsModule != null && cosmeticsModule.isEnabled()) {
                  if (!cosmeticsModule.selectedSwordModel().isBlank()) {
                     return true;
                  }
               }

               return false;
            }
         }
      }

      return false;
   }

   public void submit(ResolvePrimaryTextureKeyHelper.Argument argument, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int value, int value2, boolean condition, int value3) {
      if (argument != null && argument.modelName != null) {
         if (!argument.modelName.isBlank()) {
            var cache2 = this.cache;
            String modelName = argument.modelName;
            Function<java.lang.String, net.skycore.client.util.ResolvePrimaryTextureKeyHelper.Model> function = this::load;
            ResolvePrimaryTextureKeyHelper.Model model = (ResolvePrimaryTextureKeyHelper.Model)cache2.computeIfAbsent(modelName, function);
            if (!model.vertices.isEmpty()) {
               if (model.texture != null) {
                  poseStack.pushPose();
                  submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(model.texture), (pose, vertexConsumer) -> {
                     Iterator iterator = model.vertices.iterator();

                     while (true) {
                        if (!iterator.hasNext()) {
                           return;
                        }

                        ResolvePrimaryTextureKeyHelper.Vertex element = (ResolvePrimaryTextureKeyHelper.Vertex)iterator.next();
                        VertexConsumer vertexConsumer2 = vertexConsumer.addVertex(pose, element.x, element.y, element.z);
                        VertexConsumer vertexConsumer3 = vertexConsumer2.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                        VertexConsumer vertexConsumer4 = vertexConsumer3.setUv(element.u, element.v);
                        VertexConsumer vertexConsumer5 = vertexConsumer4.setOverlay(OverlayTexture.NO_OVERLAY);
                        VertexConsumer vertexConsumer6 = vertexConsumer5.setLight(value);
                        vertexConsumer6.setNormal(pose, element.nx, element.ny, element.nz);
                     }
                  });
                  poseStack.popPose();
                  return;
               }
            }

            return;
         }
      }
   }

   public static ResolvePrimaryTextureKeyHelper.Argument selected(ItemDisplayContext itemDisplayContext) {
      ModuleManager moduleManager = SkycoreClient.modules();
      var optional = moduleManager.get(CosmeticsModule.class);
      CosmeticsModule cosmeticsModule = (CosmeticsModule)optional.orElse(null);
      if (cosmeticsModule != null && !cosmeticsModule.selectedSwordModel().isBlank()) {
         String text = cosmeticsModule.selectedSword();
         String text2 = cosmeticsModule.selectedSwordModel();
         return new ResolvePrimaryTextureKeyHelper.Argument(text, text2, itemDisplayContext);
      } else {
         return null;
      }
   }

   private static Map<String, ResolvePrimaryTextureKeyHelper.Display> readDisplays(JsonObject json) {
      if (json == null) {
         return Map.of();
      }

      HashMap lookup = new HashMap();
      var entries = json.entrySet();
      Iterator iterator = entries.iterator();

      while (iterator.hasNext()) {
         Entry element = (Entry)iterator.next();
         if (((JsonElement)element.getValue()).isJsonObject()) {
            JsonObject asJsonObject = ((JsonElement)element.getValue()).getAsJsonObject();
            String key = (String)element.getKey();
            Vector3f asJsonArray = vec(asJsonObject.getAsJsonArray("rotation"), 0.0F);
            JsonArray asJsonArray2 = asJsonObject.getAsJsonArray("translation");
            Vector3f vector = vec(asJsonArray2, 0.0F);
            JsonArray asJsonArray3 = asJsonObject.getAsJsonArray("scale");
            lookup.put(key, new ResolvePrimaryTextureKeyHelper.Display(asJsonArray, vector, vec(asJsonArray3, 1.0F)));
         }
      }

      return Map.copyOf(lookup);
   }

   private static ResolvePrimaryTextureKeyHelper.Rotation readRotation(JsonObject json) {
      if (json != null) {
         if (json.has("origin")) {
            if (json.has("axis") && json.has("angle")) {
               JsonArray asJsonArray = json.getAsJsonArray("origin");
               float asFloat = asJsonArray.get(0).getAsFloat() * 0.0625F;
               float asFloat2 = asJsonArray.get(1).getAsFloat() * 0.0625F;
               JsonElement jsonElement = asJsonArray.get(2);
               float asFloat3 = jsonElement.getAsFloat() * 0.0625F;
               JsonElement jsonElement2 = json.get("axis");
               String asString = jsonElement2.getAsString();
               double asFloat4 = json.get("angle").getAsFloat();
               float angleRadians = (float)Math.toRadians(asFloat4);
               return new ResolvePrimaryTextureKeyHelper.Rotation(asFloat, asFloat2, asFloat3, asString, angleRadians);
            }
         }
      }

      return ResolvePrimaryTextureKeyHelper.Rotation.NONE;
   }

   private static void addFace(
      JsonObject json,
      String text,
      List<ResolvePrimaryTextureKeyHelper.Vertex> vertexs,
      ResolvePrimaryTextureKeyHelper.Rotation rotation,
      String text2,
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
      float value13,
      float value14,
      float value15
   ) {
      if (json.has(text)) {
         JsonObject asJsonObject = json.getAsJsonObject(text);
         if (text2 == null || !asJsonObject.has("texture") || text2.equals(asJsonObject.get("texture").getAsString())) {
            JsonArray asJsonArray = asJsonObject.getAsJsonArray("uv");
            if (asJsonArray != null && asJsonArray.size() >= 4) {
               float asFloat = asJsonArray.get(0).getAsFloat() * 0.0625F;
               float asFloat2 = asJsonArray.get(1).getAsFloat() * 0.0625F;
               float asFloat3 = asJsonArray.get(2).getAsFloat() * 0.0625F;
               float asFloat4 = asJsonArray.get(3).getAsFloat() * 0.0625F;
               ResolvePrimaryTextureKeyHelper.P p = rotate(value, value2, value3, value13, value14, value15, rotation);
               ResolvePrimaryTextureKeyHelper.P p2 = rotate(value4, value5, value6, value13, value14, value15, rotation);
               ResolvePrimaryTextureKeyHelper.P p3 = rotate(value7, value8, value9, value13, value14, value15, rotation);
               ResolvePrimaryTextureKeyHelper.P p4 = rotate(value10, value11, value12, value13, value14, value15, rotation);
               vertexs.add(new ResolvePrimaryTextureKeyHelper.Vertex(p.x, p.y, p.z, asFloat, asFloat4, p.nx, p.ny, p.nz));
               vertexs.add(new ResolvePrimaryTextureKeyHelper.Vertex(p2.x, p2.y, p2.z, asFloat3, asFloat4, p2.nx, p2.ny, p2.nz));
               vertexs.add(new ResolvePrimaryTextureKeyHelper.Vertex(p3.x, p3.y, p3.z, asFloat3, asFloat2, p3.nx, p3.ny, p3.nz));
               vertexs.add(new ResolvePrimaryTextureKeyHelper.Vertex(p4.x, p4.y, p4.z, asFloat, asFloat2, p4.nx, p4.ny, p4.nz));
            }
         }
      }
   }

   private static String displayKey(ItemDisplayContext itemDisplayContext) {
      return switch (itemDisplayContext) {
         case THIRD_PERSON_RIGHT_HAND -> "thirdperson_righthand";
         case THIRD_PERSON_LEFT_HAND -> "thirdperson_lefthand";
         case FIRST_PERSON_RIGHT_HAND -> "firstperson_righthand";
         case FIRST_PERSON_LEFT_HAND -> "firstperson_lefthand";
         case GROUND -> "ground";
         case GUI -> "gui";
         case HEAD -> "head";
         case FIXED, ON_SHELF -> "fixed";
         default -> "";
      };
   }

   public boolean setupLayer(LayerRenderState upLayer, ResolvePrimaryTextureKeyHelper.Argument upLayer2) {
      if (upLayer != null && upLayer2 != null && upLayer2.modelName != null && !upLayer2.modelName.isBlank()) {
         ResolvePrimaryTextureKeyHelper.Model model = this.cache.computeIfAbsent(upLayer2.modelName, this::load);
         if (!model.vertices.isEmpty() && model.texture != null) {
            ResolvePrimaryTextureKeyHelper.Display display = model.displays.get(displayKey(upLayer2.context));
            if (display != null) {
               upLayer.setItemTransform(new ItemTransform(new Vector3f(display.rotation), new Vector3f(display.translation).mul(0.0625F), new Vector3f(display.scale)));
            } else {
               upLayer.setItemTransform(ItemTransform.NO_TRANSFORM);
            }

            upLayer.setupSpecialModel(this, upLayer2);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public record Argument(String label, String modelName, ItemDisplayContext context) {
      public String modelName() {
         return this.modelName;
      }

      public ItemDisplayContext context() {
         return this.context;
      }

      public String label() {
         return this.label;
      }
   }

   public record Display(Vector3f rotation, Vector3f translation, Vector3f scale) {
      public Vector3f translation() {
         return this.translation;
      }

      public Vector3f scale() {
         return this.scale;
      }

      public Vector3f rotation() {
         return this.rotation;
      }
   }

   public record Model(List<ResolvePrimaryTextureKeyHelper.Vertex> vertices, Identifier texture, Map<String, ResolvePrimaryTextureKeyHelper.Display> displays) {
      public static ResolvePrimaryTextureKeyHelper.Model empty() {
         List<ResolvePrimaryTextureKeyHelper.Vertex> vertexs = List.of();
         return new ResolvePrimaryTextureKeyHelper.Model(vertexs, null, Map.of());
      }

      public List<ResolvePrimaryTextureKeyHelper.Vertex> vertices() {
         return this.vertices;
      }

      public Map<String, ResolvePrimaryTextureKeyHelper.Display> displays() {
         return this.displays;
      }

      public Identifier texture() {
         return this.texture;
      }
   }

   public record P(float x, float y, float z, float nx, float ny, float nz) {
      public float nz() {
         return this.nz;
      }

      public float nx() {
         return this.nx;
      }

      public float z() {
         return this.z;
      }

      public float ny() {
         return this.ny;
      }

      public float x() {
         return this.x;
      }

      public float y() {
         return this.y;
      }
   }

   public record Rotation(float ox, float oy, float oz, String axis, float angle) {
      static final ResolvePrimaryTextureKeyHelper.Rotation NONE = new ResolvePrimaryTextureKeyHelper.Rotation(0.0F, 0.0F, 0.0F, "", 0.0F);

      public String axis() {
         return this.axis;
      }

      public float ox() {
         return this.ox;
      }

      public float oz() {
         return this.oz;
      }

      public float oy() {
         return this.oy;
      }

      public float angle() {
         return this.angle;
      }
   }

   public record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
      public float nz() {
         return this.nz;
      }

      public float u() {
         return this.u;
      }

      public float x() {
         return this.x;
      }

      public float v() {
         return this.v;
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

      public float z() {
         return this.z;
      }
   }
}
