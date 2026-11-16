package tokyo.ghoul.client.autobuy;

import java.util.List;

public final class ItemMatcher {
   private ItemMatcher() {
   }

   public static boolean matches(ItemEntry entry, Lot lot) {
      return matchesInternal(entry, lot, false);
   }

   public static boolean matchesForParser(ItemEntry entry, Lot lot) {
      return matchesInternal(entry, lot, true);
   }

   private static boolean matchesInternal(ItemEntry entry, Lot lot, boolean forParser) {
      if (entry == null || lot == null) {
         return false;
      }

      if (entry.matchAnySword) {
         if (lot.itemId == null || !lot.itemId.endsWith("_sword")) {
            return false;
         }
      } else if (entry.itemId == null || !entry.itemId.equalsIgnoreCase(lot.itemId)) {
         return false;
      }

      List<Marker> markers = entry.resolveMarkers();
      if (markers != null && !markers.isEmpty()) {
         boolean markerFound = false;
         for (Marker m : markers) {
            if (markerMatches(m, lot)) {
               markerFound = true;
               break;
            }
         }
         if (!markerFound) {
            return false;
         }
      } else if (entry.requireCustom && !lot.hasMarkers) {
         return false;
      }

      if (!forParser) {
         int reqDurability = entry.minDurabilityPercent;
         if (reqDurability <= 0 && (entry.isArmor() || lot.isArmor())) {
            reqDurability = AutoBuyConfig.cfg().armorDurabilityPercent;
         }
         if (reqDurability > 0 && lot.durability < reqDurability) {
            return false;
         }
      }

      if (entry.potionEffect != null && !entry.potionEffect.isBlank()) {
         if (lot.potionEffects == null || !lot.potionEffects.contains(entry.potionEffect)) {
            return false;
         }
      }

      boolean hasDisplayName = entry.displayName != null && !entry.displayName.isBlank();
      if (hasDisplayName) {
         boolean nameMatch = TextNormalizer.nameMatches(entry.displayName, lot.name);
         boolean loreMatch = TextNormalizer.keywordIn(lot.lore, entry.displayName);
         boolean queryMatch = entry.searchQuery != null && !entry.searchQuery.isBlank()
               && (TextNormalizer.keywordIn(lot.name, entry.searchQuery) || TextNormalizer.keywordIn(lot.lore, entry.searchQuery));
         if (!nameMatch && !loreMatch && !queryMatch) {
            if (!entry.matchAnySword) {
               return false;
            }
         }
      }

      if (entry.loreKeyword != null && !entry.loreKeyword.isBlank()) {
         boolean inLore = TextNormalizer.keywordIn(lot.lore, entry.loreKeyword);
         boolean inName = TextNormalizer.keywordIn(lot.name, entry.loreKeyword);
         if (!inLore && !inName) {
            return false;
         }
      }

      return true;
   }

   public static boolean markerMatches(Marker marker, Lot lot) {
      if (marker != null && marker.type != null && marker.key != null && lot != null) {
         switch (marker.type) {
            case PBV:
               String pbvVal = lot.pbv.get(marker.key);
               if (pbvVal == null) {
                  return false;
               }
               return marker.value == null || marker.value.isEmpty() || pbvVal.equals(marker.value);
            case CUSTOM_ENCH:
               return lot.customEnch.contains(marker.key);
            case CUSTOM_DATA:
               String dataVal = lot.customData.get(marker.key);
               if (dataVal == null) {
                  return false;
               }
               return marker.value == null || marker.value.isEmpty() || dataVal.equals(marker.value);
            default:
               return false;
         }
      } else {
         return false;
      }
   }
}
