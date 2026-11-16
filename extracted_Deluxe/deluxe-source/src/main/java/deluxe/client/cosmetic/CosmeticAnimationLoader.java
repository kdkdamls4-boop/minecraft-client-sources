package deluxe.client.cosmetic;

import deluxe.client.cosmetic.BedrockModel;
import deluxe.client.cosmetic.CosmeticWarmup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class CosmeticAnimationLoader {
   private static final Map<Integer, CosmeticAnimationState> field13653 = new ConcurrentHashMap<>();

   private static float method3947(JsonObject jsonObject, String local6553_1, float local6553_2) {
      try {
         return jsonObject.has(local6553_1) ? jsonObject.get(local6553_1).getAsFloat() : local6553_2;
      } catch (Exception exception) {
         return local6553_2;
      }
   }

   private static int method3948(JsonObject jsonObject, String local6554_1, int local6554_2) {
      try {
         return jsonObject.has(local6554_1) ? jsonObject.get(local6554_1).getAsInt() : local6554_2;
      } catch (Exception exception) {
         return local6554_2;
      }
   }

   private static float[] method3949(JsonElement jsonElement, float[] floatArray) {
      if (jsonElement == null || jsonElement.isJsonNull()) {
         return (float[])floatArray.clone();
      }

      if (jsonElement.isJsonPrimitive()) {
         float local6555_2;
         try {
            local6555_2 = jsonElement.getAsFloat();
         } catch (Exception exception) {
            return (float[])floatArray.clone();
         }

         return new float[]{local6555_2, local6555_2, local6555_2};
      } else {
         if (!jsonElement.isJsonArray()) {
            return (float[])floatArray.clone();
         }

         JsonArray asJsonArray = jsonElement.getAsJsonArray();
         float[] floatArray2 = (float[])floatArray.clone();

         for (int index = 0; index < Math.min(3, asJsonArray.size()); index++) {
            try {
               floatArray2[index] = asJsonArray.get(index).getAsFloat();
            } catch (Exception exception2) {
            }
         }

         return floatArray2;
      }
   }

   private static JsonObject method3950(JsonObject jsonObject, String local6556_1) {
      return jsonObject != null && jsonObject.has(local6556_1) && jsonObject.get(local6556_1).isJsonObject()
         ? jsonObject.getAsJsonObject(local6556_1)
         : null;
   }

   private static Map<String, CosmeticAnimationState.Nested0534> method3951(JsonObject jsonObject) {
      Map<String, CosmeticAnimationState.Nested0534> local6557_1 = new LinkedHashMap<>();
      JsonObject jsonObject2 = method3950(jsonObject, "animation");
      JsonObject jsonObject3 = jsonObject2 == null ? null : method3950(jsonObject2, "animations");
      if (jsonObject3 == null) {
         return local6557_1;
      }

      for (Entry<String, JsonElement> local6557_5 : jsonObject3.entrySet()) {
         if (local6557_5.getValue().isJsonObject()) {
            JsonObject jsonObject4 = local6557_5.getValue().getAsJsonObject();
            boolean local6557_7 = method3953(jsonObject4, "loop", true);
            float local6557_8 = method3947(jsonObject4, "animation_length", 1.0F);
            Map<String, CosmeticAnimationState.Nested0535> local6557_9 = new LinkedHashMap<>();
            JsonObject jsonObject5 = method3950(jsonObject4, "bones");
            if (jsonObject5 != null) {
               for (Entry<String, JsonElement> local6557_12 : jsonObject5.entrySet()) {
                  if (local6557_12.getValue().isJsonObject()) {
                     JsonObject asJsonObject = local6557_12.getValue().getAsJsonObject();
                     local6557_9.put(
                        local6557_12.getKey(),
                        new CosmeticAnimationState.Nested0535(
                           method3952(asJsonObject.get("rotation"), new float[]{0.0F, 0.0F, 0.0F}),
                           method3952(asJsonObject.get("position"), new float[]{0.0F, 0.0F, 0.0F}),
                           method3952(asJsonObject.get("scale"), new float[]{1.0F, 1.0F, 1.0F})
                        )
                     );
                  }
               }
            }

            local6557_1.put(local6557_5.getKey(), new CosmeticAnimationState.Nested0534(local6557_5.getKey(), local6557_7, local6557_8, local6557_9));
         }
      }

      return local6557_1;
   }

   private static CosmeticAnimationState.Nested0533 method3952(JsonElement jsonElement, float[] floatArray) {
      CosmeticAnimationState.Nested0533 local6558_2 = new CosmeticAnimationState.Nested0533();
      if (jsonElement == null || jsonElement.isJsonNull()) {
         return local6558_2;
      }

      if (jsonElement.isJsonArray() || jsonElement.isJsonPrimitive()) {
         local6558_2.field15560 = method3949(jsonElement, floatArray);
         return local6558_2;
      }

      if (!jsonElement.isJsonObject()) {
         return local6558_2;
      }

      JsonObject asJsonObject = jsonElement.getAsJsonObject();

      for (Entry<String, JsonElement> local6558_5 : asJsonObject.entrySet()) {
         try {
            float local6558_6 = Float.parseFloat(local6558_5.getKey());
            local6558_2.field15559.put(local6558_6, method3958(local6558_5.getValue(), floatArray));
         } catch (NumberFormatException exception) {
         }
      }

      return local6558_2;
   }

   private static boolean method3953(JsonObject jsonObject, String local6560_1, boolean local6560_2) {
      try {
         return jsonObject.has(local6560_1) ? jsonObject.get(local6560_1).getAsBoolean() : local6560_2;
      } catch (Exception exception) {
         return local6560_2;
      }
   }

   private CosmeticAnimationLoader() {
   }

   private static CosmeticAnimationState method3954(CosmeticEntry cosmeticEntry) {
      try (InputStream inputStream = new ByteArrayInputStream(CosmeticPreviewAtlas.method3631(cosmeticEntry.modelResource()))) {
         if (inputStream == null) {
            return null;
         }

         JsonObject jsonObject = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
         JsonObject jsonObject2 = method3950(jsonObject, "model");
         if (jsonObject2 == null) {
            return null;
         }

         BedrockModel bedrockModel = BedrockGeometryLoader.method1391(jsonObject2.toString());
         return bedrockModel == null
            ? null
            : new CosmeticAnimationState(
               cosmeticEntry.id(),
               method3959(jsonObject, "name", cosmeticEntry.name()),
               method3948(jsonObject, "pos", 1),
               method3947(jsonObject, "scale", 1.0F),
               method3947(jsonObject, "x", 0.0F),
               method3947(jsonObject, "y", 0.0F),
               method3947(jsonObject, "z", 0.0F),
               method3947(jsonObject, "yaw", 0.0F),
               method3947(jsonObject, "pitch", 0.0F),
               method3947(jsonObject, "roll", 0.0F),
               bedrockModel,
               method3951(jsonObject)
            );
      } catch (Exception exception) {
         System.err.println("[Cosmetics] failed to load " + cosmeticEntry.name() + ": " + exception);
         return null;
      }
   }

   public static CosmeticAnimationState method3955(CosmeticEntry cosmeticEntry) {
      if (cosmeticEntry != null && cosmeticEntry.modelResource() != null) {
         CosmeticAnimationState cosmeticAnimationState = field13653.get(cosmeticEntry.id());
         if (cosmeticAnimationState == null) {
            CosmeticWarmup.method2912(cosmeticEntry);
         }

         return cosmeticAnimationState;
      } else {
         return null;
      }
   }

   public static CosmeticAnimationLoader.Nested0513 method3957(CosmeticEntry cosmeticEntry) {
      CosmeticAnimationState cosmeticAnimationState = method3955(cosmeticEntry);
      return cosmeticAnimationState == null ? null : new CosmeticAnimationLoader.Nested0513(cosmeticAnimationState.field15529, cosmeticAnimationState.field15530, cosmeticAnimationState.field15531);
   }

   private static float[] method3958(JsonElement jsonElement, float[] floatArray) {
      if (jsonElement != null && jsonElement.isJsonObject()) {
         JsonObject asJsonObject = jsonElement.getAsJsonObject();
         if (asJsonObject.has("post")) {
            return method3949(asJsonObject.get("post"), floatArray);
         }

         if (asJsonObject.has("pre")) {
            return method3949(asJsonObject.get("pre"), floatArray);
         }

         if (asJsonObject.has("vector")) {
            return method3949(asJsonObject.get("vector"), floatArray);
         }
      }

      return method3949(jsonElement, floatArray);
   }

   private static String method3959(JsonObject jsonObject, String local6567_1, String local6567_2) {
      try {
         return jsonObject.has(local6567_1) ? jsonObject.get(local6567_1).getAsString() : local6567_2;
      } catch (Exception exception) {
         return local6567_2;
      }
   }

   public static boolean method3960(CosmeticEntry cosmeticEntry) {
      if (field13653.containsKey(cosmeticEntry.id())) {
         return true;
      }

      CosmeticAnimationState cosmeticAnimationState = method3954(cosmeticEntry);
      return cosmeticAnimationState != null
         && CosmeticPreviewAtlas.method3632(cosmeticEntry.textureId(), cosmeticEntry.textureResource(), () -> field13653.put(cosmeticEntry.id(), cosmeticAnimationState));
   }

   public record Nested0513(float x, float y, float z) {
   }
}
