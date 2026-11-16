package tokyo.ghoul.client.features.modules.autobuy.engine;

import java.nio.file.Path;
import java.util.List;

public record ParsedItemTemplate(
   String stateKey,
   Path embeddedPath,
   String displayName,
   String baseName,
   String itemId,
   String matchingProfile,
   List<String> matchingMarkers,
   String autoBuyKey,
   String customData,
   String rawComponentNbt,
   String cleanedComponentNbt,
   String fullStackNbt,
   List<String> tooltip,
   List<AutoBuyParsedItemData.EnchantmentData> enchantments,
   List<AutoBuyParsedItemData.AttributeData> attributes,
   List<AutoBuyParsedItemData.PotionEffectData> potionEffects,
   boolean damageable,
   int maxDamage,
   boolean defaultEnabled,
   String defaultPrice,
   int defaultMinDurability
) {
   public ParsedItemTemplate(
      String stateKey,
      Path embeddedPath,
      String displayName,
      String baseName,
      String itemId,
      String autoBuyKey,
      String customData,
      String rawComponentNbt,
      String cleanedComponentNbt,
      String fullStackNbt,
      List<String> tooltip,
      List<AutoBuyParsedItemData.EnchantmentData> enchantments,
      List<AutoBuyParsedItemData.AttributeData> attributes,
      List<AutoBuyParsedItemData.PotionEffectData> potionEffects,
      boolean damageable,
      int maxDamage,
      boolean defaultEnabled,
      String defaultPrice,
      int defaultMinDurability
   ) {
      this(
         stateKey,
         embeddedPath,
         displayName,
         baseName,
         itemId,
         "",
         List.of(),
         autoBuyKey,
         customData,
         rawComponentNbt,
         cleanedComponentNbt,
         fullStackNbt,
         tooltip,
         enchantments,
         attributes,
         potionEffects,
         damageable,
         maxDamage,
         defaultEnabled,
         defaultPrice,
         defaultMinDurability
      );
   }

   public static String concat(String... parts) {
      if (parts == null || parts.length == 0) {
         return "";
      }
      int totalLen = 0;
      for (String p : parts) {
         if (p != null) totalLen += p.length();
      }
      StringBuilder sb = new StringBuilder(totalLen);
      for (String p : parts) {
         if (p != null) sb.append(p);
      }
      return sb.toString();
   }
}
