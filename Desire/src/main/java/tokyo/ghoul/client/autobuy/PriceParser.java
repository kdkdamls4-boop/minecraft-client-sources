package tokyo.ghoul.client.autobuy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class PriceParser {
   private static final Pattern UNIT_PRICE_PATTERN = Pattern.compile("(?i)Цена за 1 ед\\.?:\\s*\\$?\\s*([\\d\\s\\u00A0,]+)");
   private static final Pattern TOTAL_PRICE_PATTERN = Pattern.compile("(?i)(?:^|\\n|\\r)Цена:\\s*\\$?\\s*([\\d\\s\\u00A0,]+)");
   private static final Pattern DURABILITY_PATTERN = Pattern.compile("(?:Прочность|Прочка|Durability)[^\\d]*([\\d]+)\\s*%");
   private static final Pattern MINECRAFT_ID_PATTERN = Pattern.compile("(minecraft|[a-z_]+):([a-z0-9_]+)");
   private static final Pattern SELLER_PATTERN = Pattern.compile("(?iu)(?:Продавец|Seller|Игрок|Владелец)[:\\s]+(?:(?:\\[[^\\]]+\\]|\\([^\\)]+\\)|[^\\w\\s])\\s*)*([A-Za-z0-9_]{2,32})");

   private PriceParser() {
   }

   public static List<String> getTooltipLines(ItemStack stack) {
      ArrayList<String> lines = new ArrayList<>();
      if (stack == null || stack.isEmpty()) {
         return lines;
      }

      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && mc.player != null) {
            Item.TooltipContext ctx = Item.TooltipContext.of(mc.level.registryAccess());
            List<Component> tooltip = stack.getTooltipLines(ctx, mc.player, TooltipFlag.NORMAL);
            if (tooltip != null) {
               for (Component t : tooltip) {
                  if (t != null) {
                     lines.add(t.getString());
                  }
               }
            }
         }
      } catch (Throwable ignored) {
      }

      if (lines.isEmpty()) {
         ItemLore loreComp = stack.get(DataComponents.LORE);
         if (loreComp != null) {
            for (Component line : loreComp.lines()) {
               if (line != null) {
                  lines.add(line.getString());
               }
            }
         }
      }

      if (lines.isEmpty()) {
         try {
            CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
            if (comp != null && !comp.isEmpty()) {
               CompoundTag compound = comp.copyTag();
               if (compound != null && compound.get("display") instanceof CompoundTag display) {
                  if (display.get("Lore") instanceof ListTag loreList) {
                     for (Tag el : loreList) {
                        lines.add(elementToString(el));
                     }
                  }
               }
            }
         } catch (Throwable ignored) {
         }
      }

      return lines;
   }

   public static long[] extractPrices(List<String> rawLines, int count) {
      long unitPrice = -1L;
      long totalPrice = -1L;

      for (String rawLine : rawLines) {
         if (rawLine == null) continue;
         String clean = stripFormatting(rawLine).replace('\u00A0', ' ').trim();
         if (clean.isEmpty()) continue;
         String lower = clean.toLowerCase(Locale.ROOT);

         if (lower.contains("цена за") || lower.contains("цена/шт") || lower.contains("цена за шт")) {
            long p = parseTrailingPrice(clean);
            if (p > 0L) {
               unitPrice = p;
            }
         } else if (lower.contains("цена")) {
            long p = parseTrailingPrice(clean);
            if (p > 0L) {
               totalPrice = p;
            }
         }
      }

      int c = Math.max(1, count);
      if (unitPrice <= 0L && totalPrice > 0L) {
         unitPrice = totalPrice / c;
      }
      if (totalPrice <= 0L && unitPrice > 0L) {
         totalPrice = unitPrice * c;
      }

      return new long[]{unitPrice, totalPrice};
   }

   private static long parseTrailingPrice(String line) {
      int colon = line.lastIndexOf(':');
      String after = (colon >= 0 && colon < line.length() - 1) ? line.substring(colon + 1) : line;

      int dollar = after.indexOf('$');
      if (dollar >= 0 && dollar < after.length() - 1) {
         String afterDollar = after.substring(dollar + 1).replaceAll("[^0-9]", "");
         if (!afterDollar.isEmpty()) {
            try {
               return Long.parseLong(afterDollar);
            } catch (NumberFormatException ignored) {
            }
         }
      }

      String digits = after.replaceAll("[^0-9]", "");
      if (!digits.isEmpty()) {
         try {
            return Long.parseLong(digits);
         } catch (NumberFormatException ignored) {
         }
      }

      return -1L;
   }

   public static Lot parse(int slot, ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         if (stack.getItem() == Items.BARRIER) {
            return Lot.barrier(slot);
         }

         String name = stripFormatting(stack.getHoverName().getString()).trim();
         List<String> rawLines = getTooltipLines(stack);

         StringBuilder sb = new StringBuilder();
         for (String l : rawLines) {
            sb.append(stripFormatting(l)).append('\n');
         }
         String lore = sb.toString();

         int count = Math.max(1, stack.getCount());
         long[] prices = extractPrices(rawLines, count);
         long unitPrice = prices[0];
         long totalPrice = prices[1];

         if (unitPrice <= 0L) {
            // Fallback to regex if line structure was unexpected
            unitPrice = parseLongRegex(UNIT_PRICE_PATTERN, lore);
            totalPrice = parseLongRegex(TOTAL_PRICE_PATTERN, lore);
            if (unitPrice <= 0L && totalPrice > 0L) {
               unitPrice = totalPrice / count;
            }
         }

         if (unitPrice <= 0L) {
            return null;
         }

         String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

         int durability = parseDurability(stack, lore);
         String seller = extractSeller(lore);

         HashMap<String, String> pbv = new HashMap<>();
         HashSet<String> customEnch = new HashSet<>();
         HashMap<String, String> customData = new HashMap<>();
         boolean hasMarkers = extractMarkers(stack, pbv, customEnch, customData);
         Lot lot = new Lot(slot, itemId, name, lore, seller, count, unitPrice, totalPrice, durability, false, hasMarkers, pbv, customEnch, customData);
         lot.potionEffects = extractPotionEffects(stack);
         return lot;
      } else {
         return null;
      }
   }

   public static Lot parseForMatch(ItemStack stack) {
      if (stack != null && !stack.isEmpty() && stack.getItem() != Items.BARRIER) {
         String name = stripFormatting(stack.getHoverName().getString()).trim();
         List<String> rawLines = getTooltipLines(stack);

         StringBuilder sb = new StringBuilder();
         for (String l : rawLines) {
            sb.append(stripFormatting(l)).append('\n');
         }
         String lore = sb.toString();

         String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
         int durability = parseDurability(stack, lore);
         HashMap<String, String> pbv = new HashMap<>();
         HashSet<String> customEnch = new HashSet<>();
         HashMap<String, String> customData = new HashMap<>();
         boolean hasMarkers = extractMarkers(stack, pbv, customEnch, customData);
         Lot lot = new Lot(-1, itemId, name, lore, extractSeller(lore), stack.getCount(), 0L, 0L, durability, false, hasMarkers, pbv, customEnch, customData);
         lot.potionEffects = extractPotionEffects(stack);
         return lot;
      } else {
         return null;
      }
   }

   public static String stripFormatting(String s) {
      if (s == null) return "";
      return s.replaceAll("(?i)§x(§[0-9a-f]){6}", "")
              .replaceAll("§.", "")
              .replaceAll("(?i)&[0-9a-fk-orx]", "")
              .replaceAll("(?i)&#[0-9a-f]{6}", "");
   }

   public static String extractSeller(String lore) {
      if (lore == null || lore.isBlank()) {
         return "";
      }

      String clean = stripFormatting(lore).replace('\u00A0', ' ');
      String[] lines = clean.split("\n");

      for (String rawLine : lines) {
         String line = rawLine.trim();
         if (line.isEmpty()) continue;

         String lineLower = line.toLowerCase(Locale.ROOT);

         // Check confirmation chest pattern: e.g. "купить ... у Steve" or "покупка у игрока Steve"
         Matcher confirmMatcher = Pattern.compile("(?iu)(?:купить|покупка|покупаете|приобрести)[^\\n]*?\\bу(?:\\s+игрока)?\\s+([A-Za-z0-9_]{2,32})").matcher(line);
         if (confirmMatcher.find()) {
            String cand = confirmMatcher.group(1);
            if (!isRankOrNoiseWord(cand.toLowerCase(Locale.ROOT))) {
               return cand;
            }
         }

         // Check if line contains a seller indicator
         boolean isSellerLine = lineLower.contains("продавец")
                 || lineLower.contains("выставил")
                 || lineLower.contains("seller")
                 || lineLower.contains("продает")
                 || lineLower.contains("продаёт")
                 || lineLower.contains("владелец")
                 || lineLower.contains("торговец")
                 || lineLower.contains("игрок:")
                 || lineLower.contains("игрок :")
                 || lineLower.contains("ник:")
                 || lineLower.contains("ник :")
                 || lineLower.contains("никнейм")
                 || lineLower.contains("у игрока")
                 || lineLower.contains("от игрока")
                 || lineLower.contains("продажа от")
                 || lineLower.contains("продажа:");

         if (!isSellerLine) continue;

         // Extract substring after indicator
         String after = line;
         int colonIdx = after.indexOf(':');
         if (colonIdx != -1) {
            after = after.substring(colonIdx + 1);
         } else {
            Matcher kwMatcher = Pattern.compile("(?iu)(?:продавец|выставил\\(а\\)|выставил|seller|продает|продаёт|владелец|торговец|у\\s+игрока|от\\s+игрока|купить\\s+у|покупка\\s+у|игрок|никнейм|ник|продажа\\s+от|от)").matcher(after);
            if (kwMatcher.find()) {
               after = after.substring(kwMatcher.end());
            }
         }

         // Remove time in parentheses, e.g. (1д 2ч), (12ч), (30м), (1 дн.)
         after = after.replaceAll("\\s*\\([^)]*\\)", " ");
         // Remove bracketed tags like [VIP], [TITAN], [Клан], [Игрок], <...>, {...}
         after = after.replaceAll("\\[[^\\]]*\\]", " ").replaceAll("<[^>]*>", " ").replaceAll("\\{[^}]*\\}", " ");
         // Remove dollar and special currency/rank symbols like ☤
         after = after.replace("$", " ").replace("☤", " ");
         // Remove all non-alphanumeric characters, turning symbols, emojis, and decorations into space
         after = after.replaceAll("[^A-Za-z0-9_\\s]", " ");

         // Split remaining words
         String[] tokens = after.trim().split("[\\s\\u00A0]+");
         for (String token : tokens) {
            String cleanToken = token.trim();
            if (cleanToken.length() < 2 || cleanToken.length() > 32) continue;

            String tokLower = cleanToken.toLowerCase(Locale.ROOT);
            if (isRankOrNoiseWord(tokLower)) continue;

            // Found valid player nickname!
            return cleanToken;
         }
      }

      return "";
   }

   private static boolean isRankOrNoiseWord(String w) {
      return w.equals("vip") || w.equals("premium") || w.equals("titan") || w.equals("hero")
              || w.equals("dragon") || w.equals("sponsor") || w.equals("bog") || w.equals("lord")
              || w.equals("legend") || w.equals("overlord") || w.equals("elite") || w.equals("media")
              || w.equals("yt") || w.equals("youtube") || w.equals("moder") || w.equals("helper")
              || w.equals("dhelper") || w.equals("sthelper") || w.equals("curator") || w.equals("admin")
              || w.equals("stadmin") || w.equals("glmoder") || w.equals("builder") || w.equals("tester")
              || w.equals("player") || w.equals("default") || w.equals("owner") || w.equals("coder")
              || w.equals("seller") || w.equals("item") || w.equals("cost") || w.equals("price")
              || w.equals("time") || w.equals("buy") || w.equals("day") || w.equals("hour")
              || w.equals("vampire") || w.equals("baron") || w.equals("king") || w.equals("prince")
              || w.equals("duke") || w.equals("marshal") || w.equals("paladin") || w.equals("gladiator")
              || w.equals("assassin") || w.equals("phantom") || w.equals("eclipse") || w.equals("custom")
              || w.equals("staff") || w.equals("magister") || w.equals("imperator") || w.equals("chiter")
              || w.equals("игрок") || w.equals("спонсор") || w.equals("титан") || w.equals("герой")
              || w.equals("дракон") || w.equals("бог") || w.equals("лорд") || w.equals("легенда")
              || w.equals("оверлорд") || w.equals("элита") || w.equals("модер") || w.equals("хелпер")
              || w.equals("админ") || w.equals("мажор") || w.equals("вип") || w.equals("премиум")
              || w.equals("предмета") || w.equals("лот") || w.equals("лота") || w.equals("стажер")
              || w.equals("стажёр") || w.equals("модератор") || w.equals("куратор")
              || w.equals("барон") || w.equals("страж") || w.equals("глава") || w.equals("аспид")
              || w.equals("сквид") || w.equals("принц") || w.equals("князь") || w.equals("герцог");
   }

   private static Set<String> extractPotionEffects(ItemStack stack) {
      HashSet<String> effects = new HashSet<>();

      try {
         PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
         if (potion != null) {
            for (MobEffectInstance inst : potion.getAllEffects()) {
               effects.add(inst.getEffect().getRegisteredName());
            }
         }
      } catch (Throwable ignored) {
      }

      return effects;
   }

   private static boolean extractMarkers(ItemStack stack, Map<String, String> pbv, Set<String> ench, Map<String, String> custom) {
      boolean hasCustom = false;

      try {
         if (stack.get(DataComponents.CUSTOM_NAME) != null
            || stack.get(DataComponents.ITEM_NAME) != null
            || stack.get(DataComponents.LORE) != null) {
            hasCustom = true;
         }
      } catch (Throwable ignored) {
      }

      try {
         ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
         if (enchantments != null && !enchantments.isEmpty()) {
            hasCustom = true;
         }
      } catch (Throwable ignored) {
      }

      try {
         CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
         if (comp == null || comp.isEmpty()) {
            return hasCustom;
         }

         hasCustom = true;
         CompoundTag compound = comp.copyTag();
         if (compound == null) {
            return hasCustom;
         }

         if (compound.get("PublicBukkitValues") instanceof CompoundTag pbvCompound) {
            for (String key : pbvCompound.keySet()) {
               pbv.put(key, elementToString(pbvCompound.get(key)));
            }
         }

         Tag enchElement = compound.get("Enchantments");
         if (enchElement instanceof ListTag enchList) {
            for (Tag el : enchList) {
               if (el instanceof CompoundTag itemEnch) {
                  Tag idEl = itemEnch.get("id");
                  if (idEl != null) {
                     String idStr = elementToString(idEl);
                     if (idStr != null && !idStr.isEmpty() && !idStr.startsWith("minecraft:")) {
                        ench.add(idStr);
                     }
                  }
               }
            }
         }

         for (String key : compound.keySet()) {
            if (!key.equals("PublicBukkitValues")
               && !key.equals("Enchantments")
               && !key.equals("display")
               && !key.equals("HideFlags")
               && !key.startsWith("VV|")) {
               Tag val = compound.get(key);
               if (val != null) {
                  if (val instanceof CompoundTag nested) {
                     for (String subKey : nested.keySet()) {
                        Tag subVal = nested.get(subKey);
                        if (subVal != null && !(subVal instanceof CompoundTag) && !(subVal instanceof ListTag)) {
                           custom.put(key + "." + subKey, elementToString(subVal));
                        }
                     }
                  } else if (!(val instanceof ListTag)) {
                     custom.put(key, elementToString(val));
                  }
               }
            }
         }
      } catch (Throwable ignored) {
      }

      return hasCustom;
   }

   private static String elementToString(Tag el) {
      if (el == null) {
         return "";
      }

      String s = el.toString();
      if (!(el instanceof StringTag)) {
         if (!s.isEmpty()) {
            char last = s.charAt(s.length() - 1);
            if ("bBsSlLfFdD".indexOf(last) >= 0 && s.length() > 1) {
               String trimmed = s.substring(0, s.length() - 1);
               if (trimmed.matches("-?\\d+(\\.\\d+)?")) {
                  return trimmed;
               }
            }
         }

         return s;
      } else {
         if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            s = s.substring(1, s.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
         } else if (s.length() >= 2 && s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'') {
            s = s.substring(1, s.length() - 1).replace("\\'", "'").replace("\\\\", "\\");
         }

         return s;
      }
   }

   private static int parseDurability(ItemStack stack, String lore) {
      Matcher matcher = DURABILITY_PATTERN.matcher(lore);
      if (matcher.find()) {
         try {
            return clampPercent(Integer.parseInt(matcher.group(1)));
         } catch (NumberFormatException ignored) {
         }
      }

      if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
         int remaining = stack.getMaxDamage() - stack.getDamageValue();
         return clampPercent((int)Math.round(remaining * 100.0 / stack.getMaxDamage()));
      } else {
         return 100;
      }
   }

   private static int clampPercent(int v) {
      return Math.max(0, Math.min(100, v));
   }

   private static long parseLongRegex(Pattern pattern, String text) {
      Matcher matcher = pattern.matcher(text);
      if (!matcher.find()) {
         return -1L;
      }

      String cleaned = matcher.group(1).replaceAll("[^\\d]", "");
      if (cleaned.isEmpty()) {
         return -1L;
      }

      try {
         return Long.parseLong(cleaned);
      } catch (NumberFormatException ignored) {
         return -1L;
      }
   }
}
