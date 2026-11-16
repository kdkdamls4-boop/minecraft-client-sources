package deluxe.client.cosmetic;

import deluxe.client.DeluxeClient;
import deluxe.client.cosmetic.CosmeticWarmup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class CosmeticModel {
   private static final Map<Integer, CosmeticModel> field06162 = new ConcurrentHashMap<>();
   private static final String[] field06163 = new String[]{"north", "south", "east", "west", "up", "down"};
   private static final float[] field06164 = new float[]{0.0F, 0.0F, 0.0F};
   private static final float[] field06165 = new float[]{1.0F, 1.0F, 1.0F};
   private final ArrayList<CosmeticModel.Nested0416> field06166;
   private final ArrayList<CosmeticModel.Nested0411> field06167;
   private final CosmeticModel.Nested0412 field06168;
   private final CosmeticModel.Nested0412 field06169;
   private final float[] field06170;
   private final HashMap<String, Transformation> field06171;
   private final float[] field06172 = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
   private final Vector3fc[] field06173;
   private final boolean field06174;
   private static final MatrixStack field06175 = new MatrixStack();
   private static final Quaternionf field06176 = new Quaternionf();
   private final CosmeticModel.Nested0411[] nested0411Array;
   private final CosmeticModel.Nested0414[][][] field06178;
   private final CosmeticModel.Nested0414[][][] field06179;
   private final float[] field06180;
   private final float[][] field06181 = new float[3][3];
   private final float[][] field06182 = new float[3][3];
   private final Quaternionf field06183;
   private float field06184;
   private float field06185;
   private boolean field06186;
   private int field06187;
   private int field06188;

   private void method1868(MatrixStack matrixStack, VertexConsumer vertexConsumer, int local2867_3, float local2867_4, float local2867_5, int local2867_6) {
      this.method1890(local2867_4, local2867_5);
      CosmeticModel.Nested0417 local2867_7 = this.field06166.get(local2867_6).animation();
      if (local2867_7 != null) {
         int local2867_8 = local2867_7.frameAt((int)(local2867_4 * 20.0F));
         this.field06187 = local2867_8 % local2867_7.columns();
         this.field06188 = local2867_8 / local2867_7.columns();
      }

      for (CosmeticModel.Nested0411 local2867_9 : this.field06167) {
         this.method1887(local2867_9, matrixStack, vertexConsumer, local2867_3, local2867_7, local2867_6);
      }
   }

   private static float[] method1870(JsonArray jsonArray, int local2869_1) {
      float[] floatArray = new float[local2869_1];

      for (int currentIndex = 0; currentIndex < Math.min(local2869_1, jsonArray.size()); currentIndex++) {
         floatArray[currentIndex] = jsonArray.get(currentIndex).getAsFloat();
      }

      return floatArray;
   }

   private static void method1871(CosmeticModel.Nested0411 local2870_0, ArrayList<CosmeticModel.Nested0411> local2870_1) {
      local2870_1.add(local2870_0);

      for (CosmeticModel.Nested0411 local2870_3 : local2870_0.children()) {
         method1871(local2870_3, local2870_1);
      }
   }

   private static float[] method1872(
      CosmeticModel.Nested0414[] nested0414Array, float local2871_1, float local2871_2, float[] floatArray, float[] floatArray2
   ) {
      if (nested0414Array.length == 0) {
         return floatArray;
      }

      if (nested0414Array.length == 1) {
         return nested0414Array[0].value();
      }

      CosmeticModel.Nested0414 local2871_5 = nested0414Array[nested0414Array.length - 1];
      CosmeticModel.Nested0414 local2871_6 = nested0414Array[0];
      float local2871_7 = local2871_5.time() - local2871_2;
      float local2871_8 = local2871_6.time();

      for (int currentIndex = 0; currentIndex < nested0414Array.length - 1; currentIndex++) {
         if (local2871_1 >= nested0414Array[currentIndex].time() && local2871_1 <= nested0414Array[currentIndex + 1].time()) {
            local2871_5 = nested0414Array[currentIndex];
            local2871_6 = nested0414Array[currentIndex + 1];
            local2871_7 = local2871_5.time();
            local2871_8 = local2871_6.time();
            break;
         }
      }

      if (local2871_1 > nested0414Array[nested0414Array.length - 1].time()) {
         local2871_5 = nested0414Array[nested0414Array.length - 1];
         local2871_6 = nested0414Array[0];
         local2871_7 = local2871_5.time();
         local2871_8 = local2871_6.time() + local2871_2;
      }

      if (local2871_5.step()) {
         return local2871_5.value();
      }

      float local2871_9 = local2871_1 < local2871_7 ? local2871_1 + local2871_2 : local2871_1;
      float local2871_10 = local2871_8 == local2871_7 ? 0.0F : MathHelper.clamp((local2871_9 - local2871_7) / (local2871_8 - local2871_7), 0.0F, 1.0F);
      floatArray2[0] = MathHelper.lerp(local2871_10, local2871_5.value()[0], local2871_6.value()[0]);
      floatArray2[1] = MathHelper.lerp(local2871_10, local2871_5.value()[1], local2871_6.value()[1]);
      floatArray2[2] = MathHelper.lerp(local2871_10, local2871_5.value()[2], local2871_6.value()[2]);
      return floatArray2;
   }

   public static boolean method1873(CosmeticEntry cosmeticEntry) {
      if (field06162.containsKey(cosmeticEntry.id())) {
         return true;
      }

      try {
         JsonObject asJsonObject = JsonParser.parseString(new String(CosmeticPreviewAtlas.method3631(cosmeticEntry.modelResource()), StandardCharsets.UTF_8))
            .getAsJsonObject();
         ArrayList<CosmeticModel.Nested0416> local2872_2 = new ArrayList<>();

         for (JsonElement jsonElement : asJsonObject.getAsJsonArray("textures")) {
            JsonObject asJsonObject2 = jsonElement.getAsJsonObject();
            String asString = asJsonObject2.get("file").getAsString();
            CosmeticModel.Nested0417 local2872_7 = null;
            if (asJsonObject2.has("animation")) {
               JsonObject asJsonObject3 = asJsonObject2.getAsJsonObject("animation");
               JsonArray asJsonArray = asJsonObject3.getAsJsonArray("frames");
               int[] intArray = new int[asJsonArray.size()];
               int[] intArray2 = new int[asJsonArray.size()];
               int local2872_12 = 0;

               for (int currentIndex = 0; currentIndex < asJsonArray.size(); currentIndex++) {
                  intArray[currentIndex] = asJsonArray.get(currentIndex).getAsJsonArray().get(0).getAsInt();
                  intArray2[currentIndex] = Math.max(1, asJsonArray.get(currentIndex).getAsJsonArray().get(1).getAsInt());
                  local2872_12 += intArray2[currentIndex];
               }

               local2872_7 = new CosmeticModel.Nested0417(
                  asJsonObject3.get("columns").getAsInt(),
                  asJsonObject3.get("rows").getAsInt(),
                  asJsonObject3.get("imageWidth").getAsInt(),
                  asJsonObject3.get("imageHeight").getAsInt(),
                  intArray,
                  intArray2,
                  local2872_12
               );
            }

            local2872_2.add(
               new CosmeticModel.Nested0416(
                  Identifier.of("deluxe", "cosmetics/" + asString),
                  asString,
                  asJsonObject2.get("uvWidth").getAsFloat(),
                  asJsonObject2.get("uvHeight").getAsFloat(),
                  local2872_7
               )
            );
         }

         ArrayList<CosmeticModel.Nested0411> local2872_3 = new ArrayList<>();
         int[] intArray3 = new int[]{0};

         for (JsonElement jsonElement2 : asJsonObject.getAsJsonArray("bones")) {
            local2872_3.add(method1893(jsonElement2.getAsJsonObject(), field06164, local2872_2, intArray3));
         }

         JsonObject jsonObject = asJsonObject.has("animations") ? asJsonObject.getAsJsonObject("animations") : new JsonObject();
         HashMap<String, Transformation> local2872_6 = new HashMap<>();
         if (asJsonObject.has("display")) {
            for (Entry<String, JsonElement> local2872_8 : asJsonObject.getAsJsonObject("display").entrySet()) {
               float[] floatArray = method1870(local2872_8.getValue().getAsJsonArray(), 9);
               local2872_6.put(
                  local2872_8.getKey(),
                  new Transformation(
                     new Vector3f(floatArray[0], floatArray[1], floatArray[2]),
                     new Vector3f(floatArray[3], floatArray[4], floatArray[5]),
                     new Vector3f(floatArray[6], floatArray[7], floatArray[8])
                  )
               );
            }
         }

         CosmeticModel cosmeticModel = new CosmeticModel(
            local2872_2,
            local2872_3,
            jsonObject.has("idle") ? method1883(jsonObject.getAsJsonObject("idle")) : null,
            jsonObject.has("walk") ? method1883(jsonObject.getAsJsonObject("walk")) : null,
            asJsonObject.has("displayHead") ? method1870(asJsonObject.getAsJsonArray("displayHead"), 9) : null,
            local2872_6
         );

         for (int currentIndex2 = 0; currentIndex2 < local2872_2.size(); currentIndex2++) {
            if (!CosmeticPreviewAtlas.method3632(
               local2872_2.get(currentIndex2).id(),
               local2872_2.get(currentIndex2).file(),
               currentIndex2 == local2872_2.size() - 1 ? () -> field06162.put(cosmeticEntry.id(), cosmeticModel) : null
            )) {
               return false;
            }
         }

         return !local2872_2.isEmpty();
      } catch (Exception exception) {
         DeluxeClient.logger().warn("[Cosmetics] model {} not loaded: {}", cosmeticEntry.modelResource(), exception.toString());
         return false;
      }
   }

   private static void method1874(MatrixStack matrixStack, float local2873_1, float local2873_2, float local2873_3) {
      if (local2873_3 != 0.0F) {
         matrixStack.multiply(field06176.rotationZ(local2873_3 * (float) (Math.PI / 180.0)));
      }

      if (local2873_2 != 0.0F) {
         matrixStack.multiply(field06176.rotationY(local2873_2 * (float) (Math.PI / 180.0)));
      }

      if (local2873_1 != 0.0F) {
         matrixStack.multiply(field06176.rotationX(local2873_1 * (float) (Math.PI / 180.0)));
      }
   }

   public void method1875(MatrixStack matrixStack) {
      if (this.field06170 != null) {
         matrixStack.translate(this.field06170[3], this.field06170[4], this.field06170[5]);
         matrixStack.multiply(this.field06183);
         matrixStack.scale(this.field06170[6], this.field06170[7], this.field06170[8]);
      }

      matrixStack.translate(-0.5F, -0.5F, -0.5F);
   }

   public void method1877(MatrixStack matrixStack, ItemDisplayContext itemDisplayContext) {
      float local2876_3 = Math.max(1.0F, this.field06172[3] - this.field06172[0]);
      float local2876_4 = Math.max(1.0F, this.field06172[4] - this.field06172[1]);
      float local2876_5 = Math.min(18.0F / local2876_3, 22.0F / local2876_4);
      matrixStack.translate((itemDisplayContext.isLeftHand() ? -1.5F : 1.5F) / 16.0F, (itemDisplayContext.isFirstPerson() ? 1.5F : 0.0F) / 16.0F, -0.125F);
      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(itemDisplayContext.isLeftHand() ? 90.0F : -90.0F));
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(itemDisplayContext.isLeftHand() ? -8.0F : 8.0F));
      matrixStack.scale(local2876_5, local2876_5, local2876_5);
      matrixStack.translate(
         -(this.field06172[0] + this.field06172[3]) / 32.0F,
         -(this.field06172[1] + this.field06172[4]) / 32.0F,
         -(this.field06172[2] + this.field06172[5]) / 32.0F
      );
   }

   private CosmeticModel(
      ArrayList<CosmeticModel.Nested0416> local2877_1,
      ArrayList<CosmeticModel.Nested0411> local2877_2,
      CosmeticModel.Nested0412 local2877_3,
      CosmeticModel.Nested0412 local2877_4,
      float[] floatArray,
      HashMap<String, Transformation> local2877_6
   ) {
      this.field06166 = local2877_1;
      this.field06167 = local2877_2;
      this.field06168 = local2877_3;
      this.field06169 = local2877_4;
      this.field06170 = floatArray;
      this.field06171 = local2877_6;
      ArrayList<CosmeticModel.Nested0411> local2877_7 = new ArrayList<>();

      for (CosmeticModel.Nested0411 local2877_9 : local2877_2) {
         method1871(local2877_9, local2877_7);
      }

      int local2877_8 = 0;

      for (CosmeticModel.Nested0411 local2877_10 : local2877_7) {
         local2877_8 = Math.max(local2877_8, local2877_10.index() + 1);
      }

      this.nested0411Array = new CosmeticModel.Nested0411[local2877_8];
      this.field06178 = new CosmeticModel.Nested0414[local2877_8][][];
      this.field06179 = new CosmeticModel.Nested0414[local2877_8][][];

      for (CosmeticModel.Nested0411 local2877_10 : local2877_7) {
         this.nested0411Array[local2877_10.index()] = local2877_10;
         this.field06178[local2877_10.index()] = local2877_3 == null ? null : local2877_3.tracksByBone().get(local2877_10.uuid());
         this.field06179[local2877_10.index()] = local2877_4 == null ? null : local2877_4.tracksByBone().get(local2877_10.uuid());
      }

      this.field06180 = new float[local2877_8 * 9];
      this.field06183 = floatArray == null
         ? null
         : new Quaternionf()
            .rotationXYZ(floatArray[0] * (float) (Math.PI / 180.0), floatArray[1] * (float) (Math.PI / 180.0), floatArray[2] * (float) (Math.PI / 180.0));

      for (CosmeticModel.Nested0411 local2877_10 : local2877_2) {
         this.method1891(local2877_10);
      }

      if (this.field06172[0] == Float.MAX_VALUE) {
         System.arraycopy(new float[]{0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 16.0F}, 0, this.field06172, 0, 6);
      }

      this.field06173 = new Vector3fc[8];

      for (int currentIndex = 0; currentIndex < 8; currentIndex++) {
         this.field06173[currentIndex] = new Vector3f(
            this.field06172[(currentIndex & 1) == 0 ? 0 : 3] / 16.0F,
            this.field06172[(currentIndex & 2) == 0 ? 1 : 4] / 16.0F,
            this.field06172[(currentIndex & 4) == 0 ? 2 : 5] / 16.0F
         );
      }

      this.field06174 = local2877_1.stream().anyMatch(local2881_0 -> local2881_0.animation() != null);
   }

   public boolean method1879() {
      return this.field06174;
   }

   public Transformation method1880(ItemDisplayContext itemDisplayContext) {
      return this.field06171.getOrDefault(itemDisplayContext.asString(), Transformation.IDENTITY);
   }

   public void method1882(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int local2882_3, float local2882_4, float local2882_5) {
      for (int currentIndex = 0; currentIndex < this.field06166.size(); currentIndex++) {
         int currentIndex2 = currentIndex;
         orderedRenderCommandQueue.submitCustom(matrixStack, RenderLayers.entityCutoutNoCull(this.field06166.get(currentIndex).id()), (entry, vertexConsumer) -> {
            MatrixStack matrixStack2 = field06175;
            matrixStack2.peek().getPositionMatrix().set(entry.getPositionMatrix());
            matrixStack2.peek().getNormalMatrix().set(entry.getNormalMatrix());
            this.method1868(matrixStack2, vertexConsumer, local2882_3, local2882_4, local2882_5, currentIndex2);
         });
      }
   }

   private static CosmeticModel.Nested0412 method1883(JsonObject jsonObject) {
      HashMap<String, CosmeticModel.Nested0414[][]> local2883_1 = new HashMap<>();

      for (Entry<String, JsonElement> local2883_3 : jsonObject.getAsJsonObject("tracks").entrySet()) {
         ArrayList<CosmeticModel.Nested0414> local2883_4 = new ArrayList<>();
         ArrayList<CosmeticModel.Nested0414> local2883_5 = new ArrayList<>();
         ArrayList<CosmeticModel.Nested0414> local2883_6 = new ArrayList<>();

         for (JsonElement jsonElement : local2883_3.getValue().getAsJsonArray()) {
            JsonObject asJsonObject = jsonElement.getAsJsonObject();
            CosmeticModel.Nested0414 local2883_10 = new CosmeticModel.Nested0414(
               asJsonObject.get("time").getAsFloat(), method1870(asJsonObject.getAsJsonArray("value"), 3), asJsonObject.get("step").getAsBoolean()
            );
            switch (asJsonObject.get("channel").getAsString()) {
               case "position":
                  local2883_4.add(local2883_10);
                  break;
               case "scale":
                  local2883_6.add(local2883_10);
                  break;
               default:
                  local2883_5.add(local2883_10);
            }
         }

         local2883_1.put(
            local2883_3.getKey(),
            new CosmeticModel.Nested0414[][]{
               local2883_4.toArray(CosmeticModel.Nested0414[]::new),
               local2883_5.toArray(CosmeticModel.Nested0414[]::new),
               local2883_6.toArray(CosmeticModel.Nested0414[]::new)
            }
         );
      }

      return new CosmeticModel.Nested0412(jsonObject.get("length").getAsFloat(), local2883_1);
   }

   public void method1884(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int local2885_3, float local2885_4, float local2885_5) {
      for (int currentIndex = 0; currentIndex < this.field06166.size(); currentIndex++) {
         VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayers.entityCutoutNoCull(this.field06166.get(currentIndex).id()));
         this.method1868(matrixStack, vertexConsumer, local2885_3, local2885_4, local2885_5, currentIndex);
      }
   }

   private static void method1886(CosmeticModel.Nested0414[] nested0414Array, float local2887_1, float local2887_2, float[] floatArray, float[] floatArray2) {
      float[] floatArray3 = method1872(nested0414Array, local2887_1, local2887_2, floatArray, floatArray2);
      if (floatArray3 != floatArray2) {
         System.arraycopy(floatArray3, 0, floatArray2, 0, 3);
      }
   }

   private void method1887(
      CosmeticModel.Nested0411 local2888_1,
      MatrixStack matrixStack,
      VertexConsumer vertexConsumer,
      int local2888_4,
      CosmeticModel.Nested0417 local2888_5,
      int local2888_6
   ) {
      float[] floatArray = this.field06180;
      int local2888_8 = local2888_1.index() * 9;
      matrixStack.push();
      matrixStack.translate(floatArray[local2888_8], floatArray[local2888_8 + 1], floatArray[local2888_8 + 2]);
      method1874(matrixStack, floatArray[local2888_8 + 3], floatArray[local2888_8 + 4], floatArray[local2888_8 + 5]);
      matrixStack.scale(floatArray[local2888_8 + 6], floatArray[local2888_8 + 7], floatArray[local2888_8 + 8]);

      for (CosmeticModel.Nested0413 local2888_10 : local2888_1.cubes()) {
         this.method1894(local2888_10, matrixStack, vertexConsumer, local2888_4, local2888_5, local2888_6);
      }

      for (CosmeticModel.Nested0411 local2888_10 : local2888_1.children()) {
         this.method1887(local2888_10, matrixStack, vertexConsumer, local2888_4, local2888_5, local2888_6);
      }

      matrixStack.pop();
   }

   public static CosmeticModel method1889(CosmeticEntry cosmeticEntry) {
      CosmeticModel cosmeticModel = field06162.get(cosmeticEntry.id());
      if (cosmeticModel == null) {
         CosmeticWarmup.method2912(cosmeticEntry);
      }

      return cosmeticModel;
   }

   private void method1890(float local2891_1, float local2891_2) {
      if (!this.field06186 || this.field06184 != local2891_1 || this.field06185 != local2891_2) {
         float local2891_3 = this.field06168 != null && !(this.field06168.length() <= 0.0F) ? local2891_1 % this.field06168.length() : 0.0F;
         float local2891_4 = this.field06169 != null && !(this.field06169.length() <= 0.0F) ? local2891_1 % this.field06169.length() : 0.0F;
         float[] floatArray = this.field06180;
         float[] floatArray2 = this.field06181[0];
         float[] floatArray3 = this.field06181[1];
         float[] floatArray4 = this.field06181[2];
         float[] floatArray5 = this.field06182[0];
         float[] floatArray6 = this.field06182[1];
         float[] floatArray7 = this.field06182[2];

         for (int currentIndex = 0; currentIndex < this.nested0411Array.length; currentIndex++) {
            CosmeticModel.Nested0411 local2891_13 = this.nested0411Array[currentIndex];
            CosmeticModel.Nested0414[][] nested0414ArrayArray = this.field06178[currentIndex];
            CosmeticModel.Nested0414[][] nested0414ArrayArray2 = this.field06179[currentIndex];
            if (nested0414ArrayArray != null) {
               method1886(nested0414ArrayArray[0], local2891_3, this.field06168.length(), field06164, floatArray2);
               method1886(nested0414ArrayArray[1], local2891_3, this.field06168.length(), field06164, floatArray3);
               method1886(nested0414ArrayArray[2], local2891_3, this.field06168.length(), field06165, floatArray4);
            } else {
               System.arraycopy(field06164, 0, floatArray2, 0, 3);
               System.arraycopy(field06164, 0, floatArray3, 0, 3);
               System.arraycopy(field06165, 0, floatArray4, 0, 3);
            }

            if (nested0414ArrayArray2 != null) {
               method1886(nested0414ArrayArray2[0], local2891_4, this.field06169.length(), field06164, floatArray5);
               method1886(nested0414ArrayArray2[1], local2891_4, this.field06169.length(), field06164, floatArray6);
               method1886(nested0414ArrayArray2[2], local2891_4, this.field06169.length(), field06165, floatArray7);
            } else {
               System.arraycopy(field06164, 0, floatArray5, 0, 3);
               System.arraycopy(field06164, 0, floatArray6, 0, 3);
               System.arraycopy(field06165, 0, floatArray7, 0, 3);
            }

            int local2891_16 = currentIndex * 9;

            for (int currentIndex2 = 0; currentIndex2 < 3; currentIndex2++) {
               floatArray[local2891_16 + currentIndex2] = (
                     local2891_13.offset()[currentIndex2] + MathHelper.lerp(local2891_2, floatArray2[currentIndex2], floatArray5[currentIndex2])
                  )
                  / 16.0F;
               floatArray[local2891_16 + 3 + currentIndex2] = local2891_13.rotation()[currentIndex2]
                  + MathHelper.lerp(local2891_2, floatArray3[currentIndex2], floatArray6[currentIndex2]);
               floatArray[local2891_16 + 6 + currentIndex2] = MathHelper.lerp(local2891_2, floatArray4[currentIndex2], floatArray7[currentIndex2]);
            }
         }

         this.field06184 = local2891_1;
         this.field06185 = local2891_2;
         this.field06186 = true;
      }
   }

   private void method1891(CosmeticModel.Nested0411 local2892_1) {
      for (CosmeticModel.Nested0413 local2892_3 : local2892_1.cubes()) {
         for (int currentIndex = 0; currentIndex < 3; currentIndex++) {
            this.field06172[currentIndex] = Math.min(this.field06172[currentIndex], Math.min(local2892_3.from()[currentIndex], local2892_3.to()[currentIndex]));
            this.field06172[currentIndex + 3] = Math.max(
               this.field06172[currentIndex + 3], Math.max(local2892_3.from()[currentIndex], local2892_3.to()[currentIndex])
            );
         }
      }

      for (CosmeticModel.Nested0411 local2892_3 : local2892_1.children()) {
         this.method1891(local2892_3);
      }
   }

   public Vector3fc[] method1892() {
      return this.field06173;
   }

   private static CosmeticModel.Nested0411 method1893(
      JsonObject jsonObject, float[] floatArray, ArrayList<CosmeticModel.Nested0416> local2894_2, int[] intArray
   ) {
      int local2894_4 = (int)(intArray[0]++);
      float[] floatArray2 = method1870(jsonObject.getAsJsonArray("origin"), 3);
      ArrayList<CosmeticModel.Nested0413> local2894_6 = new ArrayList<>();

      for (JsonElement jsonElement : jsonObject.getAsJsonArray("cubes")) {
         JsonObject asJsonObject = jsonElement.getAsJsonObject();
         float[] floatArray3 = method1870(asJsonObject.getAsJsonArray("from"), 3);
         float[] floatArray4 = method1870(asJsonObject.getAsJsonArray("to"), 3);
         float[] floatArray5 = method1870(asJsonObject.getAsJsonArray("origin"), 3);
         float[] floatArray6 = method1870(asJsonObject.getAsJsonArray("rotation"), 3);
         float local2894_14 = (Math.min(floatArray3[0], floatArray4[0]) - floatArray5[0]) / 16.0F;
         float local2894_15 = (Math.min(floatArray3[1], floatArray4[1]) - floatArray5[1]) / 16.0F;
         float local2894_16 = (Math.min(floatArray3[2], floatArray4[2]) - floatArray5[2]) / 16.0F;
         float local2894_17 = (Math.max(floatArray3[0], floatArray4[0]) - floatArray5[0]) / 16.0F;
         float local2894_18 = (Math.max(floatArray3[1], floatArray4[1]) - floatArray5[1]) / 16.0F;
         float local2894_19 = (Math.max(floatArray3[2], floatArray4[2]) - floatArray5[2]) / 16.0F;
         boolean local2894_20 = local2894_17 - local2894_14 < 5.0E-4F;
         boolean local2894_21 = local2894_18 - local2894_15 < 5.0E-4F;
         boolean local2894_22 = local2894_19 - local2894_16 < 5.0E-4F;
         boolean[] booleanArray = new boolean[local2894_2.size()];
         ArrayList<CosmeticModel.Nested0415> local2894_24 = new ArrayList<>();

         for (JsonElement jsonElement2 : asJsonObject.getAsJsonArray("faces")) {
            JsonObject asJsonObject2 = jsonElement2.getAsJsonObject();
            float[] floatArray7 = method1870(asJsonObject2.getAsJsonArray("uv"), 4);
            int local2894_29 = 0;

            while (local2894_29 < 5 && !field06163[local2894_29].equals(asJsonObject2.get("dir").getAsString())) {
               local2894_29++;
            }

            int asInt = asJsonObject2.get("texture").getAsInt();
            boolean local2894_31 = asInt >= 0 && asInt < local2894_2.size();
            boolean local2894_32 = !local2894_31
               || (local2894_29 <= 1 ? local2894_20 || local2894_21 : (local2894_29 <= 3 ? local2894_21 || local2894_22 : local2894_20 || local2894_22));
            float local2894_33 = local2894_31 ? local2894_2.get(asInt).uvWidth() : 1.0F;
            float local2894_34 = local2894_31 ? local2894_2.get(asInt).uvHeight() : 1.0F;
            float local2894_36 = 0.0F;
            float local2894_37 = 0.0F;
            float local2894_38 = 0.0F;
            float[] floatArray8;
            switch (local2894_29) {
               case 0:
                  floatArray8 = new float[]{
                     local2894_17,
                     local2894_18,
                     local2894_16,
                     local2894_17,
                     local2894_15,
                     local2894_16,
                     local2894_14,
                     local2894_15,
                     local2894_16,
                     local2894_14,
                     local2894_18,
                     local2894_16
                  };
                  local2894_38 = -1.0F;
                  break;
               case 1:
                  floatArray8 = new float[]{
                     local2894_14,
                     local2894_18,
                     local2894_19,
                     local2894_14,
                     local2894_15,
                     local2894_19,
                     local2894_17,
                     local2894_15,
                     local2894_19,
                     local2894_17,
                     local2894_18,
                     local2894_19
                  };
                  local2894_38 = 1.0F;
                  break;
               case 2:
                  floatArray8 = new float[]{
                     local2894_17,
                     local2894_18,
                     local2894_19,
                     local2894_17,
                     local2894_15,
                     local2894_19,
                     local2894_17,
                     local2894_15,
                     local2894_16,
                     local2894_17,
                     local2894_18,
                     local2894_16
                  };
                  local2894_36 = 1.0F;
                  break;
               case 3:
                  floatArray8 = new float[]{
                     local2894_14,
                     local2894_18,
                     local2894_16,
                     local2894_14,
                     local2894_15,
                     local2894_16,
                     local2894_14,
                     local2894_15,
                     local2894_19,
                     local2894_14,
                     local2894_18,
                     local2894_19
                  };
                  local2894_36 = -1.0F;
                  break;
               case 4:
                  floatArray8 = new float[]{
                     local2894_14,
                     local2894_18,
                     local2894_16,
                     local2894_14,
                     local2894_18,
                     local2894_19,
                     local2894_17,
                     local2894_18,
                     local2894_19,
                     local2894_17,
                     local2894_18,
                     local2894_16
                  };
                  local2894_37 = 1.0F;
                  break;
               default:
                  floatArray8 = new float[]{
                     local2894_14,
                     local2894_15,
                     local2894_19,
                     local2894_14,
                     local2894_15,
                     local2894_16,
                     local2894_17,
                     local2894_15,
                     local2894_16,
                     local2894_17,
                     local2894_15,
                     local2894_19
                  };
                  local2894_37 = -1.0F;
            }

            if (!local2894_32) {
               booleanArray[asInt] = true;
            }

            local2894_24.add(
               new CosmeticModel.Nested0415(
                  local2894_29,
                  floatArray7[0] / local2894_33,
                  floatArray7[1] / local2894_34,
                  floatArray7[2] / local2894_33,
                  floatArray7[3] / local2894_34,
                  Math.floorMod(asJsonObject2.get("rotation").getAsInt() / 90, 4),
                  asInt,
                  local2894_32,
                  floatArray8,
                  local2894_36,
                  local2894_37,
                  local2894_38
               )
            );
         }

         Quaternionf quaternionf = floatArray6[0] == 0.0F && floatArray6[1] == 0.0F && floatArray6[2] == 0.0F
            ? null
            : new Quaternionf()
               .rotationZ(floatArray6[2] * (float) (Math.PI / 180.0))
               .rotateY(floatArray6[1] * (float) (Math.PI / 180.0))
               .rotateX(floatArray6[0] * (float) (Math.PI / 180.0));
         float[] floatArray9 = new float[]{
            (floatArray5[0] - floatArray2[0]) / 16.0F, (floatArray5[1] - floatArray2[1]) / 16.0F, (floatArray5[2] - floatArray2[2]) / 16.0F
         };
         local2894_6.add(
            new CosmeticModel.Nested0413(floatArray3, floatArray4, floatArray5, floatArray6, local2894_24, floatArray9, quaternionf, booleanArray)
         );
      }

      ArrayList<CosmeticModel.Nested0411> local2894_7 = new ArrayList<>();

      for (JsonElement jsonElement3 : jsonObject.getAsJsonArray("children")) {
         local2894_7.add(method1893(jsonElement3.getAsJsonObject(), floatArray2, local2894_2, intArray));
      }

      float[] floatArray10 = new float[]{floatArray2[0] - floatArray[0], floatArray2[1] - floatArray[1], floatArray2[2] - floatArray[2]};
      return new CosmeticModel.Nested0411(
         local2894_4,
         jsonObject.get("uuid").getAsString(),
         floatArray2,
         floatArray10,
         method1870(jsonObject.getAsJsonArray("rotation"), 3),
         local2894_6,
         local2894_7
      );
   }

   private void method1894(
      CosmeticModel.Nested0413 local2895_1,
      MatrixStack matrixStack,
      VertexConsumer vertexConsumer,
      int local2895_4,
      CosmeticModel.Nested0417 local2895_5,
      int local2895_6
   ) {
      if (local2895_1.hasTexture()[local2895_6]) {
         matrixStack.push();
         matrixStack.translate(local2895_1.offset()[0], local2895_1.offset()[1], local2895_1.offset()[2]);
         if (local2895_1.rotationQuaternion() != null) {
            matrixStack.multiply(local2895_1.rotationQuaternion());
         }

         net.minecraft.client.util.math.MatrixStack.Entry entry = matrixStack.peek();

         for (CosmeticModel.Nested0415 local2895_9 : local2895_1.faces()) {
            if (local2895_9.texture() == local2895_6 && !local2895_9.skip()) {
               float local2895_10 = local2895_9.u1();
               float local2895_11 = local2895_9.v1();
               float local2895_12 = local2895_9.method0509();
               float local2895_13 = local2895_9.method0514();
               if (local2895_5 != null) {
                  local2895_10 = (this.field06187 + local2895_10) / local2895_5.columns();
                  local2895_12 = (this.field06187 + local2895_12) / local2895_5.columns();
                  local2895_11 = (this.field06188 + local2895_11) / local2895_5.rows();
                  local2895_13 = (this.field06188 + local2895_13) / local2895_5.rows();
                  float local2895_14 = 0.5F / local2895_5.imageWidth() * Math.signum(local2895_12 - local2895_10);
                  float local2895_15 = 0.5F / local2895_5.imageHeight() * Math.signum(local2895_13 - local2895_11);
                  local2895_10 += local2895_14;
                  local2895_12 -= local2895_14;
                  local2895_11 += local2895_15;
                  local2895_13 -= local2895_15;
               }

               float[] floatArray = local2895_9.corners();

               for (int currentIndex = 0; currentIndex < 4; currentIndex++) {
                  int local2895_16 = (currentIndex + local2895_9.uvTurns()) % 4;
                  float local2895_17 = local2895_16 <= 1 ? local2895_10 : local2895_12;
                  float local2895_18 = local2895_16 != 0 && local2895_16 != 3 ? local2895_13 : local2895_11;
                  vertexConsumer.vertex(entry, floatArray[currentIndex * 3], floatArray[currentIndex * 3 + 1], floatArray[currentIndex * 3 + 2])
                     .color(255, 255, 255, 255)
                     .texture(local2895_17, local2895_18)
                     .overlay(OverlayTexture.DEFAULT_UV)
                     .light(local2895_4)
                     .normal(entry, local2895_9.nx(), local2895_9.ny(), local2895_9.nz());
               }
            }
         }

         matrixStack.pop();
      }
   }

   private record Nested0411(
      int index,
      String uuid,
      float[] origin,
      float[] offset,
      float[] rotation,
      ArrayList<CosmeticModel.Nested0413> cubes,
      ArrayList<CosmeticModel.Nested0411> children
   ) {
   }

   private record Nested0412(float length, HashMap<String, CosmeticModel.Nested0414[][]> tracksByBone) {
   }

   private record Nested0413(
      float[] from,
      float[] to,
      float[] origin,
      float[] rotation,
      ArrayList<CosmeticModel.Nested0415> faces,
      float[] offset,
      Quaternionf rotationQuaternion,
      boolean[] hasTexture
   ) {
   }

   private record Nested0414(float time, float[] value, boolean step) {
   }

   private record Nested0415(
      int direction,
      float u1,
      float v1,
      float method0509,
      float method0514,
      int uvTurns,
      int texture,
      boolean skip,
      float[] corners,
      float nx,
      float ny,
      float nz
   ) {
   }

   private record Nested0416(Identifier id, String file, float uvWidth, float uvHeight, CosmeticModel.Nested0417 animation) {
   }

   private record Nested0417(int columns, int rows, int imageWidth, int imageHeight, int[] frameIndexes, int[] frameTicks, int totalTicks) {
      int frameAt(int local2967_1) {
         int local2967_2 = this.totalTicks == 0 ? 0 : Math.floorMod(local2967_1, this.totalTicks);

         for (int currentIndex = 0; currentIndex < this.frameIndexes.length; currentIndex++) {
            if (local2967_2 < this.frameTicks[currentIndex]) {
               return this.frameIndexes[currentIndex];
            }

            local2967_2 -= this.frameTicks[currentIndex];
         }

         return this.frameIndexes.length == 0 ? 0 : this.frameIndexes[this.frameIndexes.length - 1];
      }
   }
}
