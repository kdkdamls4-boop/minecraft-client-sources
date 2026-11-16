package tokyo.ghoul.client.autobuy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ItemDatabase {
   public static final List<ItemEntry> LIST = new ArrayList<>();

   private ItemDatabase() {
   }

   public static void reload() {
      LIST.clear();
      LIST.addAll(loadBundledItems("/assets/desire/autobuy-items.json", null, "HW"));
   }

   private static List<ItemEntry> loadBundledItems(String primaryRes, String fallbackRes, String defaultServer) {
      ArrayList<ItemEntry> list = new ArrayList<>();

      InputStream stream = ItemDatabase.class.getResourceAsStream(primaryRes);
      if (stream == null && fallbackRes != null) {
         stream = ItemDatabase.class.getResourceAsStream(fallbackRes);
      }

      if (stream == null) {
         return list;
      }

      try (InputStream in = stream) {
         String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
         JsonObject root = JsonParser.parseString(json).getAsJsonObject();
         String srv = getString(root, "server", defaultServer);
         if (root.has("items") && root.get("items").isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray("items")) {
               JsonObject obj = el.getAsJsonObject();
               ItemEntry entry = new ItemEntry();
               entry.server = getString(obj, "server", srv);
               entry.itemId = getString(obj, "itemId", "");
               entry.displayName = getString(obj, "displayName", null);
               entry.searchQuery = getString(obj, "searchQuery", null);
               entry.loreKeyword = getString(obj, "loreKeyword", null);
               entry.requireCustom = getBoolean(obj, "custom");
               entry.matchAnySword = getBoolean(obj, "matchAnySword");
               if (obj.has("isArmor")) {
                  entry.isArmor = getBoolean(obj, "isArmor");
               }
               if (obj.has("markers") && obj.get("markers").isJsonArray()) {
                  for (JsonElement mEl : obj.getAsJsonArray("markers")) {
                     JsonObject mObj = mEl.getAsJsonObject();
                     String type = getString(mObj, "type", "");
                     String key = getString(mObj, "key", "");
                     String val = getString(mObj, "value", "");
                     switch (type) {
                        case "PBV":
                           entry.markers.add(Marker.pbv(key, val));
                           break;
                        case "CUSTOM_DATA":
                           entry.markers.add(Marker.customData(key, val));
                           break;
                        case "CUSTOM_ENCH":
                           entry.markers.add(Marker.customEnch(key));
                           break;
                     }
                  }
               }

               list.add(entry);
            }
         }
      } catch (Throwable t) {
         t.printStackTrace();
      }

      return list;
   }

   private static String getString(JsonObject o, String key, String def) {
      return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : def;
   }

   private static boolean getBoolean(JsonObject o, String key) {
      return o.has(key) && !o.get(key).isJsonNull() && o.get(key).getAsBoolean();
   }
}
