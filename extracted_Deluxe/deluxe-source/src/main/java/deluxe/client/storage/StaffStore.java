package deluxe.client.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class StaffStore {
   private static final Map<String, String> field14861 = new LinkedHashMap<>();
   private static final Map<String, String> field14862 = new LinkedHashMap<>();
   private static boolean field14863;
   private static Path field14864;

   public static boolean method4323(String local7287_0) {
      method4330();
      if (local7287_0 != null && !local7287_0.trim().isEmpty() && !field14861.containsKey(local7287_0.toLowerCase(Locale.ROOT))) {
         field14861.put(local7287_0.toLowerCase(Locale.ROOT), local7287_0);
         method4324();
         return true;
      } else {
         return false;
      }
   }

   private static void method4324() {
      method4330();

      try {
         Files.createDirectories(field14864.getParent());
         JsonObject jsonObject = new JsonObject();
         JsonArray jsonArray = new JsonArray();

         for (String local7288_3 : field14861.values()) {
            jsonArray.add(local7288_3);
         }

         JsonArray jsonArray2 = new JsonArray();

         for (String local7288_4 : field14862.values()) {
            jsonArray2.add(local7288_4);
         }

         jsonObject.add("names", jsonArray);
         jsonObject.add("prefixes", jsonArray2);
         Files.writeString(field14864, jsonObject.toString(), StandardCharsets.UTF_8);
      } catch (Exception exception) {
      }
   }

   public static boolean method4325(String local7289_0) {
      method4330();
      if (local7289_0 != null && !local7289_0.isEmpty()) {
         for (String local7289_2 : field14862.keySet()) {
            if (local7289_0.toLowerCase(Locale.ROOT).contains(local7289_2)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static ArrayList<String> method4326() {
      method4330();
      return new ArrayList<>(field14862.values());
   }

   public static ArrayList<String> method4327() {
      method4330();
      return new ArrayList<>(field14861.values());
   }

   public static boolean method4328(String local7292_0) {
      method4330();
      return local7292_0 != null && field14861.containsKey(local7292_0.toLowerCase(Locale.ROOT));
   }

   public static void method4329() {
      method4330();
      field14861.clear();
      field14862.clear();
      method4324();
   }

   private static void method4330() {
      if (!field14863) {
         field14863 = true;
         field14864 = FabricLoader.getInstance().getConfigDir().resolve("deluxe").resolve("staffs.json");

         try {
            if (!Files.exists(field14864)) {
               return;
            }

            JsonObject asJsonObject = JsonParser.parseString(Files.readString(field14864, StandardCharsets.UTF_8)).getAsJsonObject();

            for (JsonElement jsonElement : asJsonObject.getAsJsonArray("names")) {
               field14861.put(jsonElement.getAsString().toLowerCase(Locale.ROOT), jsonElement.getAsString());
            }

            for (JsonElement jsonElement2 : asJsonObject.getAsJsonArray("prefixes")) {
               field14862.put(jsonElement2.getAsString().toLowerCase(Locale.ROOT), jsonElement2.getAsString());
            }
         } catch (Exception exception) {
         }
      }
   }

   public static boolean method4331(String local7295_0) {
      method4330();
      if (local7295_0 != null
         && (field14861.remove(local7295_0.toLowerCase(Locale.ROOT)) != null || field14862.remove(local7295_0.toLowerCase(Locale.ROOT)) != null)) {
         method4324();
         return true;
      } else {
         return false;
      }
   }

   private StaffStore() {
   }

   public static boolean method4332(String local7298_0) {
      method4330();
      if (local7298_0 != null && !local7298_0.trim().isEmpty() && !field14862.containsKey(local7298_0.toLowerCase(Locale.ROOT))) {
         field14862.put(local7298_0.toLowerCase(Locale.ROOT), local7298_0);
         method4324();
         return true;
      } else {
         return false;
      }
   }
}
