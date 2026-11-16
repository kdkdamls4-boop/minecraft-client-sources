package deluxe.client.cosmetic;

import deluxe.client.cosmetic.BedrockBone;
import deluxe.client.cosmetic.BedrockVertex;
import deluxe.client.cosmetic.BedrockModel;
import deluxe.client.cosmetic.BedrockCube;
import deluxe.client.cosmetic.BedrockQuad;
import deluxe.client.cosmetic.ModelVec3;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;

public class BedrockGeometryLoader {
   private static BedrockBone method1385(JsonObject jsonObject, int local2040_1, int local2040_2) {
      String asString = jsonObject.get("name").getAsString();
      BedrockBone currentBedrockBone = new BedrockBone(asString);
      if (jsonObject.has("pivot")) {
         JsonArray asJsonArray = jsonObject.getAsJsonArray("pivot");
         currentBedrockBone.field01591 = -asJsonArray.get(0).getAsFloat();
         currentBedrockBone.field01592 = asJsonArray.get(1).getAsFloat();
         currentBedrockBone.field01593 = asJsonArray.get(2).getAsFloat();
      }

      if (jsonObject.has("rotation")) {
         JsonArray asJsonArray2 = jsonObject.getAsJsonArray("rotation");
         currentBedrockBone.method0856((float)Math.toRadians(-asJsonArray2.get(0).getAsFloat()));
         currentBedrockBone.method0862((float)Math.toRadians(-asJsonArray2.get(1).getAsFloat()));
         currentBedrockBone.method0863((float)Math.toRadians(asJsonArray2.get(2).getAsFloat()));
      }

      if (jsonObject.has("cubes")) {
         for (JsonElement jsonElement : jsonObject.getAsJsonArray("cubes")) {
            JsonObject asJsonObject = jsonElement.getAsJsonObject();
            BedrockCube bedrockCube = method1386(asJsonObject, local2040_1, local2040_2);
            currentBedrockBone.field01588.add(bedrockCube);
         }
      }

      return currentBedrockBone;
   }

   private static BedrockCube method1386(JsonObject jsonObject, int local2041_1, int local2041_2) {
      float[] floatArray = method1387(jsonObject, "origin", new float[]{0.0F, 0.0F, 0.0F});
      float[] floatArray2 = method1387(jsonObject, "size", new float[]{1.0F, 1.0F, 1.0F});
      float[] floatArray3 = method1387(jsonObject, "pivot", (float[])floatArray.clone());
      float[] floatArray4 = method1387(jsonObject, "rotation", new float[]{0.0F, 0.0F, 0.0F});
      float local2041_7 = jsonObject.has("inflate") ? jsonObject.get("inflate").getAsFloat() : 0.0F;
      boolean local2041_8 = jsonObject.has("mirror") && jsonObject.get("mirror").getAsBoolean();
      BedrockCube bedrockCube = new BedrockCube(floatArray2[0], floatArray2[1], floatArray2[2]);
      bedrockCube.field09832 = new ModelVec3(-floatArray3[0], floatArray3[1], floatArray3[2]);
      bedrockCube.field09833 = new ModelVec3(
         (float)Math.toRadians(-floatArray4[0]), (float)Math.toRadians(-floatArray4[1]), (float)Math.toRadians(floatArray4[2])
      );
      bedrockCube.field09834 = local2041_7;
      bedrockCube.field09835 = local2041_8;
      method1390(bedrockCube, floatArray, floatArray2, local2041_7, local2041_8, jsonObject, local2041_1, local2041_2);
      return bedrockCube;
   }

   private static float[] method1387(JsonObject jsonObject, String local2042_1, float[] floatArray) {
      if (!jsonObject.has(local2042_1)) {
         return floatArray;
      }

      JsonArray asJsonArray = jsonObject.getAsJsonArray(local2042_1);
      float[] floatArray2 = new float[asJsonArray.size()];

      for (int index = 0; index < asJsonArray.size(); index++) {
         floatArray2[index] = asJsonArray.get(index).getAsFloat();
      }

      return floatArray2;
   }

   private static BedrockQuad method1388(
      JsonObject jsonObject,
      String local2043_1,
      float local2043_2,
      float local2043_3,
      float local2043_4,
      float local2043_5,
      float local2043_6,
      float local2043_7,
      float local2043_8,
      float local2043_9,
      float local2043_10,
      int local2043_11,
      int local2043_12
   ) {
      if (!jsonObject.has(local2043_1)) {
         return null;
      }

      JsonObject asJsonObject = jsonObject.getAsJsonObject(local2043_1);
      JsonArray asJsonArray = asJsonObject.getAsJsonArray("uv");
      JsonArray asJsonArray2 = asJsonObject.getAsJsonArray("uv_size");
      float local2043_16 = asJsonArray.get(0).getAsFloat() / local2043_11;
      float local2043_17 = asJsonArray.get(1).getAsFloat() / local2043_12;
      float local2043_18 = asJsonArray2.get(0).getAsFloat() / local2043_11;
      float local2043_19 = asJsonArray2.get(1).getAsFloat() / local2043_12;
      BedrockVertex[] recoveredClass0057Array = method1393(
         local2043_1, local2043_2, local2043_3, local2043_4, local2043_5, local2043_6, local2043_7, local2043_16, local2043_17, local2043_18, local2043_19
      );
      return new BedrockQuad(recoveredClass0057Array, local2043_8, local2043_9, local2043_10);
   }

   private static BedrockModel method1389(JsonObject jsonObject) {
      JsonArray asJsonArray = jsonObject.getAsJsonArray("minecraft:geometry");
      if (asJsonArray != null && !asJsonArray.isEmpty()) {
         JsonObject asJsonObject = asJsonArray.get(0).getAsJsonObject();
         JsonObject asJsonObject2 = asJsonObject.getAsJsonObject("description");
         int local2044_4 = asJsonObject2.has("texture_width") ? asJsonObject2.get("texture_width").getAsInt() : 64;
         int local2044_5 = asJsonObject2.has("texture_height") ? asJsonObject2.get("texture_height").getAsInt() : 64;
         BedrockModel bedrockModel = new BedrockModel();
         bedrockModel.field03649 = local2044_4;
         bedrockModel.field03650 = local2044_5;
         JsonArray asJsonArray2 = asJsonObject.getAsJsonArray("bones");
         if (asJsonArray2 != null) {
            HashMap hashMap = new HashMap();

            for (JsonElement jsonElement : asJsonArray2) {
               JsonObject asJsonObject3 = jsonElement.getAsJsonObject();
               BedrockBone currentBedrockBone = method1385(asJsonObject3, local2044_4, local2044_5);
               hashMap.put(currentBedrockBone.field01589, currentBedrockBone);
            }

            for (JsonElement jsonElement2 : asJsonArray2) {
               JsonObject asJsonObject4 = jsonElement2.getAsJsonObject();
               String asString = asJsonObject4.get("name").getAsString();
               BedrockBone currentBedrockBone2 = (BedrockBone)hashMap.get(asString);
               if (asJsonObject4.has("parent")) {
                  String asString2 = asJsonObject4.get("parent").getAsString();
                  BedrockBone currentBedrockBone3 = (BedrockBone)hashMap.get(asString2);
                  if (currentBedrockBone3 != null) {
                     currentBedrockBone3.field01587.add(currentBedrockBone2);
                     currentBedrockBone2.bedrockBone = currentBedrockBone3;
                  }
               } else {
                  bedrockModel.field03648.add(currentBedrockBone2);
               }
            }
         }

         return bedrockModel;
      } else {
         System.err.println("[Cosmetics] No minecraft:geometry found in model");
         return null;
      }
   }

   private static void method1390(
      BedrockCube bedrockCube,
      float[] floatArray,
      float[] floatArray2,
      float local2045_3,
      boolean local2045_4,
      JsonObject jsonObject,
      int local2045_6,
      int local2045_7
   ) {
      float local2045_8 = floatArray[0];
      float local2045_9 = floatArray[1];
      float local2045_10 = floatArray[2];
      float local2045_11 = floatArray2[0];
      float local2045_12 = floatArray2[1];
      float local2045_13 = floatArray2[2];
      local2045_8 -= local2045_3;
      local2045_9 -= local2045_3;
      local2045_10 -= local2045_3;
      local2045_11 += local2045_3 * 2.0F;
      local2045_12 += local2045_3 * 2.0F;
      local2045_13 += local2045_3 * 2.0F;
      float local2045_14 = 0.0F;
      float local2045_15 = 0.0F;
      boolean local2045_16 = false;
      JsonObject jsonObject2 = null;
      if (jsonObject.has("uv")) {
         JsonElement jsonElement = jsonObject.get("uv");
         if (jsonElement.isJsonArray()) {
            JsonArray asJsonArray = jsonElement.getAsJsonArray();
            local2045_14 = asJsonArray.get(0).getAsFloat();
            local2045_15 = asJsonArray.get(1).getAsFloat();
         } else if (jsonElement.isJsonObject()) {
            local2045_16 = true;
            jsonObject2 = jsonElement.getAsJsonObject();
         }
      }

      float local2045_18 = -(local2045_8 + local2045_11) / 16.0F;
      float local2045_19 = local2045_9 / 16.0F;
      float local2045_20 = local2045_10 / 16.0F;
      float local2045_21 = -local2045_8 / 16.0F;
      float local2045_22 = (local2045_9 + local2045_12) / 16.0F;
      float local2045_23 = (local2045_10 + local2045_13) / 16.0F;
      if (local2045_16 && jsonObject2 != null) {
         bedrockCube.recoveredClass0278Array[0] = method1388(
            jsonObject2,
            "west",
            local2045_18,
            local2045_19,
            local2045_20,
            local2045_18,
            local2045_22,
            local2045_23,
            -1.0F,
            0.0F,
            0.0F,
            local2045_6,
            local2045_7
         );
         bedrockCube.recoveredClass0278Array[1] = method1388(
            jsonObject2,
            "east",
            local2045_21,
            local2045_19,
            local2045_20,
            local2045_21,
            local2045_22,
            local2045_23,
            1.0F,
            0.0F,
            0.0F,
            local2045_6,
            local2045_7
         );
         bedrockCube.recoveredClass0278Array[2] = method1388(
            jsonObject2,
            "down",
            local2045_18,
            local2045_19,
            local2045_20,
            local2045_21,
            local2045_19,
            local2045_23,
            0.0F,
            -1.0F,
            0.0F,
            local2045_6,
            local2045_7
         );
         bedrockCube.recoveredClass0278Array[3] = method1388(
            jsonObject2, "up", local2045_18, local2045_22, local2045_20, local2045_21, local2045_22, local2045_23, 0.0F, 1.0F, 0.0F, local2045_6, local2045_7
         );
         bedrockCube.recoveredClass0278Array[4] = method1388(
            jsonObject2,
            "north",
            local2045_18,
            local2045_19,
            local2045_20,
            local2045_21,
            local2045_22,
            local2045_20,
            0.0F,
            0.0F,
            -1.0F,
            local2045_6,
            local2045_7
         );
         bedrockCube.recoveredClass0278Array[5] = method1388(
            jsonObject2,
            "south",
            local2045_18,
            local2045_19,
            local2045_23,
            local2045_21,
            local2045_22,
            local2045_23,
            0.0F,
            0.0F,
            1.0F,
            local2045_6,
            local2045_7
         );
      } else {
         float local2045_24 = local2045_6;
         float local2045_25 = local2045_7;
         bedrockCube.recoveredClass0278Array[0] = method1392(
            local2045_18,
            local2045_19,
            local2045_20,
            local2045_18,
            local2045_22,
            local2045_23,
            -1.0F,
            0.0F,
            0.0F,
            local2045_14,
            local2045_15,
            local2045_13,
            local2045_12,
            local2045_11,
            local2045_24,
            local2045_25,
            "west"
         );
         bedrockCube.recoveredClass0278Array[1] = method1392(
            local2045_21,
            local2045_19,
            local2045_20,
            local2045_21,
            local2045_22,
            local2045_23,
            1.0F,
            0.0F,
            0.0F,
            local2045_14,
            local2045_15,
            local2045_13,
            local2045_12,
            local2045_11,
            local2045_24,
            local2045_25,
            "east"
         );
         bedrockCube.recoveredClass0278Array[2] = method1392(
            local2045_18,
            local2045_19,
            local2045_20,
            local2045_21,
            local2045_19,
            local2045_23,
            0.0F,
            -1.0F,
            0.0F,
            local2045_14,
            local2045_15,
            local2045_13,
            local2045_12,
            local2045_11,
            local2045_24,
            local2045_25,
            "down"
         );
         bedrockCube.recoveredClass0278Array[3] = method1392(
            local2045_18,
            local2045_22,
            local2045_20,
            local2045_21,
            local2045_22,
            local2045_23,
            0.0F,
            1.0F,
            0.0F,
            local2045_14,
            local2045_15,
            local2045_13,
            local2045_12,
            local2045_11,
            local2045_24,
            local2045_25,
            "up"
         );
         bedrockCube.recoveredClass0278Array[4] = method1392(
            local2045_18,
            local2045_19,
            local2045_20,
            local2045_21,
            local2045_22,
            local2045_20,
            0.0F,
            0.0F,
            -1.0F,
            local2045_14,
            local2045_15,
            local2045_13,
            local2045_12,
            local2045_11,
            local2045_24,
            local2045_25,
            "north"
         );
         bedrockCube.recoveredClass0278Array[5] = method1392(
            local2045_18,
            local2045_19,
            local2045_23,
            local2045_21,
            local2045_22,
            local2045_23,
            0.0F,
            0.0F,
            1.0F,
            local2045_14,
            local2045_15,
            local2045_13,
            local2045_12,
            local2045_11,
            local2045_24,
            local2045_25,
            "south"
         );
      }
   }

   public static BedrockModel method1391(String local2046_0) {
      try {
         JsonObject asJsonObject = JsonParser.parseString(local2046_0).getAsJsonObject();
         return method1389(asJsonObject);
      } catch (Exception exception) {
         System.err.println("[Cosmetics] Failed to parse Bedrock model: " + exception);
         return null;
      }
   }

   private static BedrockQuad method1392(
      float local2047_0,
      float local2047_1,
      float local2047_2,
      float local2047_3,
      float local2047_4,
      float local2047_5,
      float local2047_6,
      float local2047_7,
      float local2047_8,
      float local2047_9,
      float local2047_10,
      float local2047_11,
      float local2047_12,
      float local2047_13,
      float local2047_14,
      float local2047_15,
      String local2047_16
   ) {
      float local2047_17;
      float local2047_18;
      float local2047_19;
      float local2047_20;
      switch (local2047_16) {
         case "north":
            local2047_17 = (local2047_9 + local2047_11 + local2047_13) / local2047_14;
            local2047_18 = (local2047_10 + local2047_11) / local2047_15;
            local2047_19 = local2047_13 / local2047_14;
            local2047_20 = local2047_12 / local2047_15;
            break;
         case "south":
            local2047_17 = (local2047_9 + local2047_11 + local2047_13 + local2047_11) / local2047_14;
            local2047_18 = (local2047_10 + local2047_11) / local2047_15;
            local2047_19 = local2047_13 / local2047_14;
            local2047_20 = local2047_12 / local2047_15;
            break;
         case "east":
            local2047_17 = local2047_9 / local2047_14;
            local2047_18 = (local2047_10 + local2047_11) / local2047_15;
            local2047_19 = local2047_11 / local2047_14;
            local2047_20 = local2047_12 / local2047_15;
            break;
         case "west":
            local2047_17 = (local2047_9 + local2047_11 + local2047_13) / local2047_14;
            local2047_18 = (local2047_10 + local2047_11) / local2047_15;
            local2047_19 = local2047_11 / local2047_14;
            local2047_20 = local2047_12 / local2047_15;
            break;
         case "up":
            local2047_17 = (local2047_9 + local2047_11) / local2047_14;
            local2047_18 = local2047_10 / local2047_15;
            local2047_19 = local2047_13 / local2047_14;
            local2047_20 = local2047_11 / local2047_15;
            break;
         case "down":
            local2047_17 = (local2047_9 + local2047_11 + local2047_13) / local2047_14;
            local2047_18 = local2047_10 / local2047_15;
            local2047_19 = local2047_13 / local2047_14;
            local2047_20 = local2047_11 / local2047_15;
            break;
         default:
            local2047_17 = 0.0F;
            local2047_18 = 0.0F;
            local2047_19 = 0.0F;
            local2047_20 = 0.0F;
      }

      BedrockVertex[] recoveredClass0057Array = method1393(
         local2047_16, local2047_0, local2047_1, local2047_2, local2047_3, local2047_4, local2047_5, local2047_17, local2047_18, local2047_19, local2047_20
      );
      return new BedrockQuad(recoveredClass0057Array, local2047_6, local2047_7, local2047_8);
   }

   private static BedrockVertex[] method1393(
      String local2049_0,
      float local2049_1,
      float local2049_2,
      float local2049_3,
      float local2049_4,
      float local2049_5,
      float local2049_6,
      float local2049_7,
      float local2049_8,
      float local2049_9,
      float local2049_10
   ) {
      BedrockVertex[] recoveredClass0057Array = new BedrockVertex[4];
      float local2049_12 = local2049_7 + local2049_9;
      float local2049_13 = local2049_8 + local2049_10;
      switch (local2049_0) {
         case "north":
            recoveredClass0057Array[0] = new BedrockVertex(local2049_4, local2049_5, local2049_3, local2049_7, local2049_8);
            recoveredClass0057Array[1] = new BedrockVertex(local2049_1, local2049_5, local2049_3, local2049_12, local2049_8);
            recoveredClass0057Array[2] = new BedrockVertex(local2049_1, local2049_2, local2049_3, local2049_12, local2049_13);
            recoveredClass0057Array[3] = new BedrockVertex(local2049_4, local2049_2, local2049_3, local2049_7, local2049_13);
            break;
         case "south":
            recoveredClass0057Array[0] = new BedrockVertex(local2049_1, local2049_5, local2049_6, local2049_7, local2049_8);
            recoveredClass0057Array[1] = new BedrockVertex(local2049_4, local2049_5, local2049_6, local2049_12, local2049_8);
            recoveredClass0057Array[2] = new BedrockVertex(local2049_4, local2049_2, local2049_6, local2049_12, local2049_13);
            recoveredClass0057Array[3] = new BedrockVertex(local2049_1, local2049_2, local2049_6, local2049_7, local2049_13);
            break;
         case "east":
            recoveredClass0057Array[0] = new BedrockVertex(local2049_4, local2049_5, local2049_6, local2049_7, local2049_8);
            recoveredClass0057Array[1] = new BedrockVertex(local2049_4, local2049_5, local2049_3, local2049_12, local2049_8);
            recoveredClass0057Array[2] = new BedrockVertex(local2049_4, local2049_2, local2049_3, local2049_12, local2049_13);
            recoveredClass0057Array[3] = new BedrockVertex(local2049_4, local2049_2, local2049_6, local2049_7, local2049_13);
            break;
         case "west":
            recoveredClass0057Array[0] = new BedrockVertex(local2049_1, local2049_5, local2049_3, local2049_7, local2049_8);
            recoveredClass0057Array[1] = new BedrockVertex(local2049_1, local2049_5, local2049_6, local2049_12, local2049_8);
            recoveredClass0057Array[2] = new BedrockVertex(local2049_1, local2049_2, local2049_6, local2049_12, local2049_13);
            recoveredClass0057Array[3] = new BedrockVertex(local2049_1, local2049_2, local2049_3, local2049_7, local2049_13);
            break;
         case "up":
            recoveredClass0057Array[0] = new BedrockVertex(local2049_1, local2049_5, local2049_3, local2049_7, local2049_8);
            recoveredClass0057Array[1] = new BedrockVertex(local2049_1, local2049_5, local2049_6, local2049_7, local2049_13);
            recoveredClass0057Array[2] = new BedrockVertex(local2049_4, local2049_5, local2049_6, local2049_12, local2049_13);
            recoveredClass0057Array[3] = new BedrockVertex(local2049_4, local2049_5, local2049_3, local2049_12, local2049_8);
            break;
         case "down":
            recoveredClass0057Array[0] = new BedrockVertex(local2049_4, local2049_2, local2049_3, local2049_7, local2049_8);
            recoveredClass0057Array[1] = new BedrockVertex(local2049_4, local2049_2, local2049_6, local2049_7, local2049_13);
            recoveredClass0057Array[2] = new BedrockVertex(local2049_1, local2049_2, local2049_6, local2049_12, local2049_13);
            recoveredClass0057Array[3] = new BedrockVertex(local2049_1, local2049_2, local2049_3, local2049_12, local2049_8);
      }

      return recoveredClass0057Array;
   }
}
