package net.skycore.client.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class PackMetadataNormalizer {
   private PackMetadataNormalizer() {
   }

   private static int[] range(JsonElement jsonElement) {
      if (jsonElement == null) {
         return null;
      }

      try {
         int value;
         int value2;
         if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isNumber()) {
            value = value2 = jsonElement.getAsInt();
         } else if (jsonElement.isJsonArray() && jsonElement.getAsJsonArray().size() == 2) {
            value = jsonElement.getAsJsonArray().get(0).getAsInt();
            value2 = jsonElement.getAsJsonArray().get(1).getAsInt();
         } else {
            if (!jsonElement.isJsonObject()) {
               return null;
            }

            value = jsonElement.getAsJsonObject().get("min_inclusive").getAsInt();
            value2 = jsonElement.getAsJsonObject().get("max_inclusive").getAsInt();
         }

         return value >= 0 && value <= value2 ? new int[]{value, value2} : null;
      } catch (RuntimeException failure) {
         return null;
      }
   }

   public static void normalize(JsonObject json) {
      if (json.has("overlays") && json.get("overlays").isJsonObject()) {
         JsonElement asJsonObject = json.getAsJsonObject("overlays").get("entries");
         if (asJsonObject != null && asJsonObject.isJsonArray()) {
            JsonArray asJsonArray = asJsonObject.getAsJsonArray();
            byte flag = 0;

            for (JsonElement jsonElement : asJsonArray) {
               if (jsonElement.isJsonObject()) {
                  JsonObject asJsonObject2 = jsonElement.getAsJsonObject();
                  int[] intValues = range(asJsonObject2.get("formats"));
                  if (intValues != null && intValues[0] <= 64) {
                     flag = 1;
                  }
               }
            }

            for (JsonElement jsonElement2 : asJsonArray) {
               if (jsonElement2.isJsonObject()) {
                  JsonObject asJsonObject3 = jsonElement2.getAsJsonObject();
                  if (!asJsonObject3.has("min_format") && !asJsonObject3.has("max_format")) {
                     int[] intValues2 = range(asJsonObject3.get("formats"));
                     if (intValues2 != null && intValues2[1] > 64) {
                        asJsonObject3.addProperty("min_format", intValues2[0]);
                        asJsonObject3.addProperty("max_format", intValues2[1]);
                        if (flag == 0) {
                           asJsonObject3.remove("formats");
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
