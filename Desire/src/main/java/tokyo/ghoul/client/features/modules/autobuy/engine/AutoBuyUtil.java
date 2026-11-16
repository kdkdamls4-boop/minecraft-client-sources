package tokyo.ghoul.client.features.modules.autobuy.engine;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AutoBuyUtil {
   private static final Pattern PATTERN_FUNTIME = Pattern.compile("\\$\\s*([0-9][\\d,]*)");
   private static final Pattern PATTERN_CURRENCY_AMOUNT_FIXED = Pattern.compile("([0-9][\\d\\s,]*)\\s*(?:[Вв]?\u00a4|[$])");
   private static final Pattern PATTERN_HOLYWORLD = Pattern.compile("Цена:(?:.*?\\{\"text\":\"([\\d ]+)\")");

   public static int getPrice(ItemStack itemStack) {
      int tooltipPrice = getPriceFromTooltip(itemStack);
      return tooltipPrice != Integer.MAX_VALUE ? tooltipPrice : getPriceFromNbt(itemStack);
   }

   public static int getPriceFromNbt(ItemStack itemStack) {
      if (itemStack == null || itemStack.isEmpty()) return Integer.MAX_VALUE;
      CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
      if (customData == null) return Integer.MAX_VALUE;
      String nbt = customData.copyTag().toString();

      Matcher matcher = PATTERN_FUNTIME.matcher(nbt);
      if (matcher.find()) {
         String amount = matcher.group(1);
         return Integer.parseInt(amount.replace(",", ""));
      }

      matcher = PATTERN_HOLYWORLD.matcher(nbt);
      if (matcher.find()) {
         return Integer.parseInt(matcher.group(1).replaceAll(" ", ""));
      }

      return Integer.MAX_VALUE;
   }

   public static boolean checkDon(ItemStack itemStack) {
      if (itemStack == null || itemStack.isEmpty()) return false;
      return itemStack.getHoverName().getString().contains("⭐") || itemStack.getHoverName().getString().contains("✦");
   }

   public static String getKey(ItemStack itemStack) {
      if (itemStack == null || itemStack.isEmpty()) return "";
      CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
      if (customData != null && customData.copyTag().contains("kringeItems")) {
         return customData.copyTag().get("kringeItems").toString();
      }
      return "";
   }

   public static List<Component> displayLines(ItemStack itemStack) {
      if (itemStack == null || itemStack.isEmpty()) {
         return List.of();
      }

      ItemLore lore = itemStack.get(DataComponents.LORE);
      List<Component> lines = new ArrayList<>(lore == null ? 1 : lore.lines().size() + 1);
      lines.add(itemStack.getHoverName());
      if (lore != null) {
         lines.addAll(lore.lines());
      }
      return lines;
   }

   public static boolean isAuction(AbstractContainerMenu container) {
      if (container == null || container.slots.size() != 90) return false;
      ItemStack stack49 = container.getSlot(49).getItem();
      return stack49 != null && stack49.getItem() == Items.NETHER_STAR;
   }

   public static Slot findAuctionSortSlot(AbstractContainerMenu container) {
      if (container == null) {
         return null;
      }

      for (Slot slot : container.slots) {
         if (slot != null && slot.getItem() != null && !slot.getItem().isEmpty() && slot.getItem().getItem() == Items.HOPPER) {
            return slot;
         }
      }
      return null;
   }

   public static boolean isAuctionSortSelected(Slot sortSlot, AuctionSort sort) {
      return sort != null && sort == getSelectedAuctionSort(sortSlot);
   }

   public static AuctionSort getSelectedAuctionSort(Slot sortSlot) {
      String selectedText = getSelectedAuctionSortText(sortSlot);
      AuctionSort result = null;
      for (AuctionSort sort : AuctionSort.values()) {
         if (selectedText.contains(sort.tooltipText)
            && (result == null || sort.tooltipText.length() > result.tooltipText.length())) {
            result = sort;
         }
      }
      return result;
   }

   public static String getSelectedAuctionSortText(Slot sortSlot) {
      if (sortSlot == null || sortSlot.getItem() == null || sortSlot.getItem().isEmpty()) {
         return "";
      }

      Minecraft client = Minecraft.getInstance();
      Item.TooltipContext tooltipContext = client.level != null ? Item.TooltipContext.of(client.level) : Item.TooltipContext.EMPTY;
      for (Component line : sortSlot.getItem().getTooltipLines(tooltipContext, client.player, TooltipFlag.ADVANCED)) {
         String text = line.getString();
         if (containsSelectionMark(text)) {
            return normalizeAuctionSortText(text);
         }
      }
      return "";
   }

   private static boolean containsSelectionMark(String text) {
      return text != null && (text.indexOf('\u2714') >= 0 || text.indexOf('\u2713') >= 0 || text.indexOf('\u2611') >= 0);
   }

   private static String normalizeAuctionSortText(String text) {
      return text == null ? "" : text.toLowerCase(Locale.ROOT).replace('ё', 'е').trim();
   }

   public enum AuctionSort {
      CHEAPEST("сначала дешевые"),
      MOST_EXPENSIVE("сначала дорогие"),
      CHEAPEST_UNIT("сначала дешевые за ед. товара"),
      MOST_EXPENSIVE_UNIT("сначала дорогие за ед. товара"),
      NEWEST("сначала новые"),
      OLDEST("сначала старые");

      public final String tooltipText;

      AuctionSort(String tooltipText) {
         this.tooltipText = tooltipText;
      }

      public AuctionSort next() {
         AuctionSort[] values = values();
         return values[(ordinal() + 1) % values.length];
      }
   }

   public static boolean isWaitBuy(AbstractContainerMenu container) {
      if (container == null || container.slots.size() != 63) return false;
      ItemStack stack0 = container.getSlot(0).getItem();
      return stack0 != null && stack0.getItem() == Items.STAINED_GLASS_PANE.lime();
   }

   public static boolean isOwnAuctionListing(ItemStack itemStack) {
      if (itemStack == null || itemStack.isEmpty()) {
         return false;
      }

      Minecraft client = Minecraft.getInstance();
      Item.TooltipContext tooltipContext = client.level != null ? Item.TooltipContext.of(client.level) : Item.TooltipContext.EMPTY;
      List<Component> tooltip = itemStack.getTooltipLines(tooltipContext, client.player, TooltipFlag.ADVANCED);
      for (Component line : tooltip) {
         String normalized = normalizeTooltipLine(line.getString());
         if (normalized.contains("нажмите, чтобы забрать")
            || normalized.contains("нажмите чтобы забрать")
            || normalized.contains("click to claim")
            || normalized.contains("click to collect")) {
            return true;
         }
      }

      return false;
   }

   public static int getPriceFromTooltip(ItemStack itemStack) {
      if (itemStack == null || itemStack.isEmpty()) return Integer.MAX_VALUE;
      Minecraft client = Minecraft.getInstance();
      Item.TooltipContext tooltipContext = client.level != null ? Item.TooltipContext.of(client.level) : Item.TooltipContext.EMPTY;
      List<Component> tooltip = itemStack.getTooltipLines(tooltipContext, client.player, TooltipFlag.ADVANCED);
      for (Component line : tooltip) {
         String normalized = normalizeTooltipLine(line.getString());
         if (!isTotalPriceLine(normalized)) {
            continue;
         }

         Matcher matcher = PATTERN_CURRENCY_AMOUNT_FIXED.matcher(normalized);
         if (matcher.find()) {
            String amount = matcher.group(1).replace(" ", "").replace(",", "");
            try {
               return Integer.parseInt(amount);
            } catch (NumberFormatException ignored) {
            }
         }
      }

      return Integer.MAX_VALUE;
   }

   public static boolean isTotalPriceLine(String normalized) {
      if (normalized == null || normalized.isBlank()) {
         return false;
      }
      if (containsPriceWord(normalized)) {
         return !isUnitPriceLine(normalized) && !isAuctionBidPriceLine(normalized);
      }

      boolean containsPrice = normalized.contains("цена") || normalized.contains("price");
      if (!containsPrice) {
         return false;
      }

      return !isUnitPriceLine(normalized) && !isAuctionBidPriceLine(normalized);
   }

   public static boolean isUnitPriceLine(String normalized) {
      if (normalized == null || normalized.isBlank()) {
         return false;
      }
      if (containsPriceWord(normalized) && normalized.contains(" 1 ")) {
         return true;
      }

      return normalized.contains("цена за 1 ед")
         || normalized.contains("цена за 1 шт")
         || normalized.contains("цена за штуку")
         || normalized.contains("цена за ед")
         || normalized.contains("price per 1")
         || normalized.contains("price for 1")
         || normalized.contains("price per item")
         || normalized.contains("price for item");
   }

   public static boolean isAuctionBidPriceLine(String normalized) {
      return normalized != null
         && (normalized.contains("текущая цена")
         || normalized.contains("цена шага")
         || normalized.contains("current price")
         || normalized.contains("bid step")
         || normalized.contains("step price"));
   }

   public static boolean containsPriceWord(String normalized) {
      return normalized != null
         && (normalized.contains("цена")
         || normalized.contains("price"));
   }

   public static String normalizeTooltipLine(String value) {
      return value == null ? "" : value
         .toLowerCase(Locale.ROOT)
         .replace('ё', 'е')
         .replace("в¤", "\u00a4");
   }

   private AutoBuyUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
