package deluxe.client.cosmetic;

import deluxe.client.DeluxeClient;
import deluxe.client.cosmetic.CosmeticWarmup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;

public final class CosmeticCatalog {
   private static final List<CosmeticEntry> field00252 = new ArrayList<>();
   private static final Map<Integer, CosmeticEntry> field00253 = new HashMap<>();
   private static final Map<CosmeticCategory, List<CosmeticEntry>> field00254 = new EnumMap<>(CosmeticCategory.class);
   private static int field00255;

   private CosmeticCatalog() {
   }

   public static boolean method0516() {
      try {
         ArrayList<CosmeticEntry> local662_0 = new ArrayList<>();

         for (JsonElement jsonElement : JsonParser.parseString(new String(CosmeticPreviewAtlas.method3631("catalog.json"), StandardCharsets.UTF_8))
            .getAsJsonArray()) {
            JsonObject asJsonObject = jsonElement.getAsJsonObject();
            JsonArray asJsonArray = asJsonObject.getAsJsonArray("transform");
            JsonArray jsonArray = asJsonObject.has("align") ? asJsonObject.getAsJsonArray("align") : null;
            CosmeticPlacement cosmeticPlacement = new CosmeticPlacement(
               asJsonObject.get("kind").getAsString(),
               asJsonObject.get("anchor").getAsString(),
               asJsonArray.get(0).getAsFloat(),
               asJsonArray.get(1).getAsFloat(),
               asJsonArray.get(2).getAsFloat(),
               asJsonArray.get(3).getAsFloat(),
               asJsonArray.get(4).getAsFloat(),
               asJsonArray.get(5).getAsFloat(),
               asJsonArray.get(6).getAsFloat(),
               jsonArray == null ? 0.0F : jsonArray.get(0).getAsFloat(),
               jsonArray == null ? 0.0F : jsonArray.get(1).getAsFloat(),
               jsonArray == null ? 1.0F : jsonArray.get(2).getAsFloat(),
               asJsonObject.has("flying") && asJsonObject.get("flying").getAsBoolean(),
               new ArrayList<>()
            );
            if (asJsonObject.has("targets")) {
               for (JsonElement jsonElement2 : asJsonObject.getAsJsonArray("targets")) {
                  cosmeticPlacement.targets().add(jsonElement2.getAsString());
               }
            }

            local662_0.add(
               new CosmeticEntry(
                  asJsonObject.get("id").getAsInt(),
                  CosmeticCategory.valueOf(asJsonObject.get("category").getAsString().toUpperCase(Locale.ROOT)),
                  asJsonObject.get("name").getAsString(),
                  asJsonObject.has("beta") && asJsonObject.get("beta").getAsBoolean(),
                  asJsonObject.get("model").getAsString(),
                  null,
                  asJsonObject.get("preview").getAsString(),
                  cosmeticPlacement
               )
            );
         }

         MinecraftClient.getInstance().execute(() -> {
            for (CosmeticEntry cosmeticEntry : local662_0) {
               if (!field00253.containsKey(cosmeticEntry.id())) {
                  field00252.add(cosmeticEntry);
                  field00253.put(cosmeticEntry.id(), cosmeticEntry);
                  field00254.computeIfAbsent(cosmeticEntry.category(), cosmeticCategory -> new ArrayList<>()).add(cosmeticEntry);
               }
            }

            field00255++;
            CosmeticWarmup.method2911();
         });
         return true;
      } catch (Exception exception) {
         DeluxeClient.logger().warn("[Cosmetics] catalog.json not loaded: {}", exception.toString());
         return false;
      }
   }

   public static CosmeticEntry method0518(int local664_0) {
      return field00253.get(local664_0);
   }

   private static void method0519(int local665_0, CosmeticCategory cosmeticCategory, String local665_2, boolean local665_3) {
      String local665_4 = "models/" + cosmeticCategory.method4311() + "/" + local665_0 + "/";
      CosmeticEntry cosmeticEntry = new CosmeticEntry(
         local665_0, cosmeticCategory, local665_2, local665_3, local665_4 + "model.json", local665_4 + "texture.png", local665_4 + "preview.png", null
      );
      field00252.add(cosmeticEntry);
      field00253.put(local665_0, cosmeticEntry);
      field00254.computeIfAbsent(cosmeticCategory, cosmeticCategory2 -> new ArrayList<>()).add(cosmeticEntry);
   }

   static {
      method0519(36, CosmeticCategory.BODYWEAR, "Louis Vuitton", true);
      method0519(37, CosmeticCategory.BODYWEAR, "Gucci", false);
      method0519(38, CosmeticCategory.BODYWEAR, "Adidas", false);
      method0519(39, CosmeticCategory.BODYWEAR, "Nike", false);
      method0519(40, CosmeticCategory.BODYWEAR, "Supreme", false);
      method0519(41, CosmeticCategory.BODYWEAR, "Vlone", false);
      method0519(42, CosmeticCategory.BODYWEAR, "Bape", false);
      method0519(43, CosmeticCategory.BODYWEAR, "Balenciaga", true);
      method0519(44, CosmeticCategory.BODYWEAR, "Chrome Hearts", false);
      method0519(95, CosmeticCategory.BODYWEAR, "PePe", false);
      method0519(137, CosmeticCategory.BODYWEAR, "Катана - ila_studio", true);
      method0519(25, CosmeticCategory.WINGS, "Aurum", false);
      method0519(61, CosmeticCategory.WINGS, "Angel", false);
      method0519(62, CosmeticCategory.WINGS, "Dragon", false);
      method0519(63, CosmeticCategory.WINGS, "Diamond", false);
      method0519(64, CosmeticCategory.WINGS, "Spider", false);
      method0519(65, CosmeticCategory.WINGS, "Easter", true);
      method0519(82, CosmeticCategory.WINGS, "Fluger", true);
      method0519(86, CosmeticCategory.WINGS, "Трезубец - Броя", true);
      method0519(91, CosmeticCategory.WINGS, "Аквыч", false);
      method0519(99, CosmeticCategory.WINGS, "Dark", false);
      method0519(123, CosmeticCategory.WINGS, "Pulse", false);
      method0519(124, CosmeticCategory.WINGS, "Ghoul", false);
      method0519(136, CosmeticCategory.WINGS, "Ягуар", false);
      method0519(48, CosmeticCategory.HAT, "Armour", false);
      method0519(49, CosmeticCategory.HAT, "Capybara", false);
      method0519(50, CosmeticCategory.HAT, "Chicken", false);
      method0519(53, CosmeticCategory.HAT, "Pepe", false);
      method0519(57, CosmeticCategory.HAT, "Sheep", false);
      method0519(80, CosmeticCategory.HAT, "Fluger", true);
      method0519(88, CosmeticCategory.HAT, "Аквыч", false);
      method0519(92, CosmeticCategory.HAT, "Bear", false);
      method0519(96, CosmeticCategory.HAT, "ПеПе", false);
      method0519(97, CosmeticCategory.HAT, "Happy", false);
      method0519(98, CosmeticCategory.HAT, "Нимб", true);
      method0519(45, CosmeticCategory.PET, "Angel", false);
      method0519(46, CosmeticCategory.PET, "Demon", false);
      method0519(47, CosmeticCategory.PET, "Buddy", false);
      method0519(81, CosmeticCategory.PET, "Fluger Door", true);
      method0519(84, CosmeticCategory.PET, "Diamond", true);
      method0519(90, CosmeticCategory.PET, "Аквыч", true);
      method0519(93, CosmeticCategory.PET, "Bee", false);
      method0519(126, CosmeticCategory.PET, "1 Год", false);
      method0519(132, CosmeticCategory.PET, "Ягуар", false);
      method0519(133, CosmeticCategory.PET, "Cergifff", false);
      method0519(134, CosmeticCategory.PET, "Ila_Studio", false);
      method0519(135, CosmeticCategory.PET, "Creeper", false);

      for (CosmeticCategory cosmeticCategory : CosmeticCategory.values()) {
         field00254.computeIfAbsent(cosmeticCategory, cosmeticCategory2 -> new ArrayList<>());
      }
   }

   public static int method0522() {
      return field00255;
   }

   public static List<CosmeticEntry> method0523(CosmeticCategory cosmeticCategory) {
      return Collections.unmodifiableList(field00254.getOrDefault(cosmeticCategory, List.of()));
   }

   public static List<CosmeticEntry> method0524() {
      return Collections.unmodifiableList(field00252);
   }
}
