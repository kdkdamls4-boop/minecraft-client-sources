package tokyo.ghoul.client.autobuy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

enum AuctionSortMode {
   CHEAPEST("сначала дешевые"),
   EXPENSIVE("сначала дорогие"),
   CHEAPEST_PER_UNIT("сначала дешевые за ед. товара"),
   EXPENSIVE_PER_UNIT("сначала дорогие за ед. товара"),
   NEWEST("сначала новые"),
   OLDEST("сначала старые");

   static final String SORT_KEYWORD = "сортировка";
   private static final String HOPPER_ID = "minecraft:hopper";
   private static final String POSITIVE_MARKERS = "✔✓☑➜▶►»›>*";
   private static final String NEGATIVE_MARKERS = "✖✕✗✘×-";
   private static final AuctionSortMode[] ALL_VALUES = values();
   private static final AuctionSortMode[] MATCH_ORDER = new AuctionSortMode[]{CHEAPEST_PER_UNIT, EXPENSIVE_PER_UNIT, CHEAPEST, EXPENSIVE, NEWEST, OLDEST};
   private final String label;
   private final String normalizedLabel;

   AuctionSortMode(String label) {
      this.label = label;
      this.normalizedLabel = TextNormalizer.normalize(label);
   }

   int clicksTo(AuctionSortMode target) {
      return (target.ordinal() - this.ordinal() + ALL_VALUES.length) % ALL_VALUES.length;
   }

   String label() {
      return this.label;
   }

   static int findButton(ChestMenu handler) {
      int totalSlots = handler.getRowCount() * 9;

      for (int i = 0; i < totalSlots; i++) {
         ItemStack stack = handler.getSlot(i).getItem();
         if (!stack.isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (itemId.equals(HOPPER_ID) && isSortButton(stack)) {
               return i;
            }
         }
      }

      for (int i = 0; i < totalSlots; i++) {
         ItemStack stack = handler.getSlot(i).getItem();
         if (!stack.isEmpty() && isSortButton(stack)) {
            return i;
         }
      }

      return -1;
   }

   static AuctionSortMode current(ChestMenu handler) {
      int slot = findButton(handler);
      return slot < 0 ? null : current(handler.getSlot(slot).getItem());
   }

   static AuctionSortMode current(ItemStack stack) {
      ArrayList<AuctionSortMode.ModeLine> lines = new ArrayList<>();
      ItemLore lore = stack.get(DataComponents.LORE);
      if (lore == null) {
         return null;
      }

      for (Component lineText : lore.lines()) {
         String raw = lineText.getString();
         AuctionSortMode mode = matchMode(raw);
         if (mode != null) {
            lines.add(new AuctionSortMode.ModeLine(mode, raw, extractMarker(raw)));
         }
      }

      if (lines.isEmpty()) {
         return null;
      }

      HashMap<Character, Integer> markerCounts = new HashMap<>();

      for (AuctionSortMode.ModeLine line : lines) {
         if (line.marker != 0) {
            markerCounts.merge(line.marker, 1, Integer::sum);
         }
      }

      AuctionSortMode bestMode = null;
      int bestScore = Integer.MIN_VALUE;
      boolean tie = false;

      for (AuctionSortMode.ModeLine line : lines) {
         int score = scoreLine(line, markerCounts, lines.size());
         if (score > bestScore) {
            bestMode = line.mode;
            bestScore = score;
            tie = false;
         } else if (score == bestScore) {
            tie = true;
         }
      }

      return !tie && bestScore > 0 ? bestMode : null;
   }

   private static boolean isSortButton(ItemStack stack) {
      if (TextNormalizer.keywordIn(stack.getHoverName().getString(), SORT_KEYWORD)) {
         return true;
      }

      ItemLore lore = stack.get(DataComponents.LORE);
      if (lore == null) {
         return false;
      }

      for (Component line : lore.lines()) {
         if (TextNormalizer.keywordIn(line.getString(), SORT_KEYWORD)) {
            return true;
         }
      }

      return false;
   }

   private static AuctionSortMode matchMode(String raw) {
      String norm = TextNormalizer.normalize(raw);
      if (norm.isEmpty()) {
         return null;
      }

      for (AuctionSortMode mode : MATCH_ORDER) {
         if (norm.contains(mode.normalizedLabel)) {
            return mode;
         }
      }

      return null;
   }

   private static char extractMarker(String raw) {
      String s = raw == null ? "" : raw.trim();
      if (s.isEmpty()) {
         return '\u0000';
      }

      char first = s.charAt(0);
      return Character.isLetterOrDigit(first) ? '\u0000' : first;
   }

   private static int scoreLine(AuctionSortMode.ModeLine line, Map<Character, Integer> markerCounts, int totalLines) {
      byte score = 1;
      if (line.marker != 0) {
         int count = markerCounts.getOrDefault(line.marker, 0);
         if (count == 1 && totalLines > 1 && markerCounts.size() > 1) {
            score += 20;
         }

         if (POSITIVE_MARKERS.indexOf(line.marker) >= 0) {
            score += 20;
         }

         if (NEGATIVE_MARKERS.indexOf(line.marker) >= 0) {
            score -= 20;
         }
      }

      return score;
   }

   private static final class ModeLine {
      private final AuctionSortMode mode;
      private final String raw;
      private final char marker;

      private ModeLine(AuctionSortMode mode, String raw, char marker) {
         this.mode = mode;
         this.raw = raw;
         this.marker = marker;
      }
   }
}
