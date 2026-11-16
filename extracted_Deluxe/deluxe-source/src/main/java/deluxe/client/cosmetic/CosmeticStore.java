package deluxe.client.cosmetic;

import deluxe.client.cosmetic.CosmeticWarmup;
import deluxe.client.cosmetic.CosmeticAccess;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.loader.api.FabricLoader;

public final class CosmeticStore {
   private static final Gson field12358 = new GsonBuilder().setPrettyPrinting().create();
   private static final Path field12359 = FabricLoader.getInstance().getConfigDir().resolve("deluxe").resolve("cosmetics.json");
   private static final Map<CosmeticCategory, Integer> field12360 = new EnumMap<>(CosmeticCategory.class);

   public static boolean method3512(CosmeticEntry cosmeticEntry) {
      return cosmeticEntry != null && method3514(cosmeticEntry.category()) == cosmeticEntry.id();
   }

   private CosmeticStore() {
   }

   public static void method3513(CosmeticEntry cosmeticEntry) {
      if (cosmeticEntry != null && CosmeticAccess.method3033(cosmeticEntry)) {
         if (method3512(cosmeticEntry)) {
            field12360.remove(cosmeticEntry.category());
         } else {
            field12360.put(cosmeticEntry.category(), cosmeticEntry.id());
            CosmeticWarmup.method2912(cosmeticEntry);
         }

         method3516();
      }
   }

   public static int method3514(CosmeticCategory cosmeticCategory) {
      return field12360.getOrDefault(cosmeticCategory, -1);
   }

   private static void method3515() {
      field12360.clear();
      if (Files.isRegularFile(field12359)) {
         try {
            JsonObject asJsonObject = JsonParser.parseString(Files.readString(field12359, StandardCharsets.UTF_8)).getAsJsonObject();

            for (CosmeticCategory cosmeticCategory : CosmeticCategory.values()) {
               String local5749_5 = cosmeticCategory.name().toLowerCase();
               if (asJsonObject.has(local5749_5) && asJsonObject.get(local5749_5).isJsonPrimitive()) {
                  CosmeticEntry cosmeticEntry = CosmeticCatalog.method0518(asJsonObject.get(local5749_5).getAsInt());
                  if (cosmeticEntry == null || cosmeticEntry.category() == cosmeticCategory) {
                     field12360.put(cosmeticCategory, asJsonObject.get(local5749_5).getAsInt());
                  }
               }
            }
         } catch (Exception exception) {
         }
      }
   }

   private static void method3516() {
      try {
         Files.createDirectories(field12359.getParent());
         JsonObject jsonObject = new JsonObject();

         for (Entry<CosmeticCategory, Integer> local5750_2 : field12360.entrySet()) {
            jsonObject.addProperty(local5750_2.getKey().name().toLowerCase(), local5750_2.getValue());
         }

         Files.writeString(field12359, field12358.toJson(jsonObject), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      } catch (Exception exception) {
      }
   }

   public static void method3517(CosmeticCategory cosmeticCategory) {
      field12360.remove(cosmeticCategory);
      method3516();
   }

   static {
      method3515();
   }

   public static int method3518() {
      int local5753_0 = 0;

      for (CosmeticCategory cosmeticCategory : CosmeticCategory.values()) {
         if (method3519(cosmeticCategory) != null) {
            local5753_0++;
         }
      }

      return local5753_0;
   }

   public static CosmeticEntry method3519(CosmeticCategory cosmeticCategory) {
      CosmeticEntry cosmeticEntry = CosmeticCatalog.method0518(method3514(cosmeticCategory));
      return cosmeticEntry != null && cosmeticEntry.category() == cosmeticCategory && CosmeticAccess.method3033(cosmeticEntry) ? cosmeticEntry : null;
   }

   public static void method3520() {
      field12360.clear();
      method3516();
   }
}
