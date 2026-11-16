package tokyo.ghoul.client.features.modules.autobuy.engine;

import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.List;

public record AutoBuyParsedItemData(
   Path path,
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
   List<EnchantmentData> enchantments,
   List<AttributeData> attributes,
   List<PotionEffectData> potionEffects,
   boolean damageable,
   int maxDamage,
   boolean enabled,
   String price,
   int minDurability,
   ItemStack iconStack
) {
   public AutoBuyParsedItemData(
      Path path,
      String displayName,
      String baseName,
      String itemId,
      String autoBuyKey,
      String customData,
      String rawComponentNbt,
      String cleanedComponentNbt,
      String fullStackNbt,
      List<String> tooltip,
      List<EnchantmentData> enchantments,
      List<AttributeData> attributes,
      List<PotionEffectData> potionEffects,
      boolean damageable,
      int maxDamage,
      boolean enabled,
      String price,
      int minDurability,
      ItemStack iconStack
   ) {
      this(
         path,
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
         enabled,
         price,
         minDurability,
         iconStack
      );
   }

   public String resolvedDisplayName() {
      if (displayName != null && !displayName.isBlank()) {
         return displayName;
      }
      return baseName == null ? "" : baseName;
   }

   public record EnchantmentData(String id, int level) {
   }

   public record AttributeData(String attribute, String slot, String modifierId, double value, String operation) {
   }

   public record PotionEffectData(
      String type,
      String id,
      String translationKey,
      int duration,
      int amplifier,
      boolean ambient,
      boolean showParticles,
      boolean showIcon,
      String value,
      String color
   ) {
   }
}
