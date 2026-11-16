package tokyo.ghoul.client.features.modules.autobuy.engine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import tokyo.ghoul.api.system.backend.ClientInfo;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class AutoBuyParsedItems {
   public static final String DEFAULT_PRICE = "1000";
   public static final int DEFAULT_MIN_DURABILITY = 80;

   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static final String STATE_FILE_NAME = "autobuy-state.json";
   private static final String CONFIG_DIRECTORY_NAME = "configs";
   private static final String AUTOBUY_CONFIG_DIRECTORY_NAME = "autobuy";
   private static final String EMBEDDED_ITEM_PATH_ROOT = "__embedded_autobuy__";
   private static final String EMBEDDED_ITEM_FILE_SUFFIX = ".json";
   private static final Map<String, ItemStack> ICON_STACK_CACHE = new HashMap<>();
   private static Object iconStackRegistryManager;
   private static final Pattern EXPERIENCE_BOTTLE_LEVEL_PATTERN = Pattern.compile("(\\d+)\\s*(?:ур\\.|ур|уровн|lvl|level)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
   private static final Pattern EXPERIENCE_BOTTLE_VALUE_PATTERN = Pattern.compile("holy-exp-bottle-value\"?\\s*[:=]\\s*\"?(\\d+)");
   private static final Pattern EXPERIENCE_BOTTLE_TOOLTIP_VALUE_PATTERN = Pattern.compile("(\\d+)\\s*(?:опыт|exp)\\s*\\(", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

   private static final Pattern DURABILITY_TOOLTIP_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");
   private static final Pattern PYROTECHNIC_NAME_PATTERN = Pattern.compile("pyrotechnic-item:\\{name:\"([^\"]+)\"");
   private static final Pattern KRINGE_ITEM_TYPE_PATTERN = Pattern.compile("kringeItems:\\{type:\"([^\"]+)\"");

   public static List<AutoBuyParsedItemData> loadItems() {
      Map<String, JsonObject> stateEntries = loadStateEntries();
      Map<String, AutoBuyParsedItemData> itemsByKey = new LinkedHashMap<>();

      for (ParsedItemTemplate template : embeddedTemplates()) {
         putParsedItem(itemsByKey, applyState(template, stateEntries));
      }

      return new ArrayList<>(itemsByKey.values());
   }

   public static List<ParsedItemTemplate> embeddedTemplates() {
      return ParsedItemRegistry.ALL;
   }

   public static void persistEnabled(Path path, boolean enabled) {
      updateStateEntry(resolveStateKey(path), state -> state.addProperty("enabled", enabled));
   }

   public static void persistPrice(Path path, String price) {
      updateStateEntry(resolveStateKey(path), state -> state.addProperty("price", normalizeStoredPrice(price)));
   }

   public static void persistMinDurability(Path path, int durabilityPercent) {
      updateStateEntry(resolveStateKey(path), state -> state.addProperty("minDurability", clampPercent(durabilityPercent)));
   }

   public static Path resolveParserDirectory() {
      return Path.of(ClientInfo.CONFIG_PATH_OTHER, "item-parser");
   }

   public static String normalizeStoredPrice(String text) {
      String sanitized = sanitizePriceInput(text);
      return sanitized.isBlank() ? DEFAULT_PRICE : sanitized;
   }

   public static String sanitizePriceInput(String text) {
      if (text == null || text.isBlank()) {
         return "";
      }
      StringBuilder digits = new StringBuilder();
      for (int i = 0; i < text.length(); i++) {
         char current = text.charAt(i);
         if (Character.isDigit(current)) {
            digits.append(current);
         }
      }
      return digits.toString();
   }

   public static int clampPercent(int value) {
      return Math.max(0, Math.min(100, value));
   }

   public static String normalizeDisplayName(String text) {
      if (text == null || text.isEmpty()) {
         return "";
      }

      String normalized = text
         .replace("\u1D00", "a")
         .replace("\u0299", "b")
         .replace("\u1D04", "c")
         .replace("\u1D05", "d")
         .replace("\u1D07", "e")
         .replace("\u0493", "f")
         .replace("\u0262", "g")
         .replace("\u029C", "h")
         .replace("\u026A", "i")
         .replace("\u1D0A", "j")
         .replace("\u1D0B", "k")
         .replace("\u029F", "l")
         .replace("\u1D0D", "m")
         .replace("\u0274", "n")
         .replace("\u1D0F", "o")
         .replace("\u1D18", "p")
         .replace("\u01EB", "q")
         .replace("\u0280", "r")
         .replace("\uA731", "s")
         .replace("\u0455", "s")
         .replace("\u1D1B", "t")
         .replace("\u1D1C", "u")
         .replace("\u1D20", "v")
         .replace("\u1D21", "w")
         .replace("\u028F", "y")
         .replace("\u1D22", "z")
         .replaceAll("(?i)eternity", "eternity")
         .replaceAll("(?i)infinity", "infinity");
      normalized = normalized.replaceFirst("^\\s*[-\\u2013\\u2014]+\\s*", "");
      normalized = normalized.replaceFirst("\\s*[-\\u2013\\u2014]+\\s*$", "");
      normalized = normalized.replaceAll("\\s{2,}", " ").trim();
      return normalized;
   }

   public static String getAuctionSearchName(AutoBuyParsedItemData item) {
      if (item == null) {
         return "";
      }
      return getAuctionSearchName(item.displayName(), item.baseName());
   }

   public static String getRussianAuctionSearchName(AutoBuyParsedItemData item) {
      if (item == null) {
         return "";
      }

      String configuredSearchName = getConfiguredRussianAuctionSearchName(item);
      if (!configuredSearchName.isBlank()) {
         return configuredSearchName;
      }

      return getAuctionSearchName(item.displayName(), item.baseName());
   }

   private static String getConfiguredRussianAuctionSearchName(AutoBuyParsedItemData item) {
      return switch (resolveStateKey(item.path())) {
         case "4a1cac3b8398fc0f24bcbc2247f991053844ecbc40097dd17681cb694f9f7eb4" -> "\u0434\u0438\u043d\u0430\u043c\u0438\u0442 \u0430";
         case "4c3841e9ed72faa4095db204850ab688641138fa07a66d468e9a8998d26f7427" -> "\u0434\u0438\u043d\u0430\u043c\u0438\u0442 \u0431";
         case "94d528fa3d8ab2c03cff806f18ba60e970ca3ab4b9d7b0b569c0bcb98441e6e3" -> "\u0434\u0438\u043d\u0430\u043c\u0438\u0442 \u04312";
         case "fcbeeac41bdc301d152083f8a45f342bca84438c173f4f658c3f55ba93e80056" -> "\u043d\u0430\u0434\u0451\u0436\u043d\u044b\u0439 \u0441\u0442\u0438\u043b\u043b\u0435\u0440";
         case "704db3e241a4f3dc4a7cee8689e48821e086772b1ff1f3b61cd9f2b7262b362f" -> "\u0440\u0430\u0437\u0440\u044b\u0432\u043d\u0430\u044f \u0432\u043e\u043b\u043d\u0430";
         case "32031b0cf37d693cd2922c2bdc2b1563a08bb1d37ad6207ec5d966d3f9494b69" -> "\u04414 \u0432\u0437\u0440\u044b\u0432\u0447\u0430\u0442\u043a\u0430";
         case "488dc562778a3c19c2a8fb9db5afc4b6a9829dbd044327351af157fe85a7da15" -> "\u0441\u0442\u0438\u043b\u043b\u0435\u0440";
         case "c6d51af9c6ee35e1c6d054787e2073a0f117011d3225906e6710fddb2d2b359e" -> "\u0431\u0440\u043e\u043d\u0435\u0432\u0430\u044f \u044d\u043b\u0438\u0442\u0440\u0430";
         case "f512af7b987cb6dd3b4ef78d7c22919b143fbdb858c4b21f7599197bc7dbc8fb" -> "\u043e\u043f\u044b\u0442 100";
         case "963317ad9b07956eda178e9558296a5c786c4764428d0b2b2a76cd86706f8d76" -> "\u043e\u043f\u044b\u0442 50";
         case "4679c0cf78698beedf39e7ad38e9e16dcb60fa8c8ddf9dd1ada5592221e54a68" -> "\u043e\u043f\u044b\u0442 30";
         case "d3cbbbca0b364be7da3809f3389ffcda19d3676da4a4450ba2d5425a62ff6a9e" -> "\u043e\u043f\u044b\u0442 15";
         case "02610a3e4fb5fb89b8e70deb52810adf13e36d2ffbd9f82a8d29ea738db660e0" -> "\u0441\u0444\u0435\u0440\u0430 \u043d\u0430 \u0441\u043a\u043e\u0440\u043e\u0441\u0442\u044c 3";
         case "1d18f4ee22b422a6e05a7708406fc0ae4d051a0c8d61ca6362f52e5afc43d11d" -> "\u0441\u0444\u0435\u0440\u0430 \u043d\u0430 \u0443\u0440\u043e\u043d 3";
         case "f4c72fd9a30015947ba036898b5d6c052d490ef3f60ed0bfea6b79d1fbd1cfee" -> "\u0440\u044e\u043a\u0437\u0430\u043a 1 \u0443\u0440\u043e\u0432\u0435\u043d\u044c";
         case "95f48a9a5c8d0e9ebb5c519df11c80bc31885d6e54c804336acce1352b065437" -> "\u0440\u044e\u043a\u0437\u0430\u043a 2 \u0443\u0440\u043e\u0432\u0435\u043d\u044c";
         case "0892cf8ff291f7e59c07fe6857125189297c6b5e38c422455f92fb863190a23a" -> "\u0440\u044e\u043a\u0437\u0430\u043a 3 \u0443\u0440\u043e\u0432\u0435\u043d\u044c";
         case "d97163ad7b93acf6f37d0e1691b5c6f11fd8efccd38fc82ce3fb0cb2b245b300" -> "\u0440\u044e\u043a\u0437\u0430\u043a 4 \u0443\u0440\u043e\u0432\u0435\u043d\u044c";
         case "d7880f977c37115570049e2130251bbf07529c7eb158ab1ce6e4fb78dfdaf3a8" -> "\u0437\u0435\u043b\u044c\u0435 \u043f\u043e\u0431\u0435\u0434\u0438\u0442\u0435\u043b\u044f";
         case "c3ab8ff13720e8ad9047dd39466b3c8974e592c2fa383d4a3960714caef0c4f2" -> "\u043a\u0443\u0431\u0438\u043a";
         case "658943c8d16f0db53bec08bc64c97dd16feaf46a9e667ced62dcfbc518a64e2c" -> "\u0442\u043d\u0442-\u043f\u0443\u0448\u043a\u0430";
         default -> "";
      };
   }

   public static String getAuctionSearchName(String displayName, String baseName) {
      String sortName = getSortName(displayName, baseName);
      if (sortName.isBlank()) {
         return "";
      }

      String aliasKey = normalizeAuctionAliasKey(sortName);

      if (sortName.equalsIgnoreCase("броневая элитра") || sortName.equalsIgnoreCase("броневые элитры")) {
         return "броневая элитра";
      }
      if (sortName.equalsIgnoreCase("легендарная сфера") || (sortName.contains("сфера") && sortName.contains("скорост"))) {
         return "сфера на скорость 3";
      }
      if (sortName.equalsIgnoreCase("мифическая сфера") || (sortName.contains("сфера") && sortName.contains("урон"))) {
         return "сфера на урон 3";
      }

      if (isReliableStillerName(aliasKey)) {
         return "надёжный стиллер";
      }
      if (isEndFragmentEggName(aliasKey)) {
         return "осколочное яйцо энд";
      }

      String dynamiteSearchName = getDynamiteSearchName(sortName);
      if (!dynamiteSearchName.isEmpty()) {
         return dynamiteSearchName;
      }

      String backpackSearchName = getBackpackSearchName(sortName);
      if (!backpackSearchName.isEmpty()) {
         return backpackSearchName;
      }

      int experienceLevel = extractFirstNumber(sortName);
      if (sortName.toLowerCase(Locale.ROOT).contains("опыт") && experienceLevel != Integer.MAX_VALUE) {
         return "опыт " + experienceLevel;
      }

      return sortName;
   }

   private static String getDynamiteSearchName(String name) {
      if (name == null || name.isBlank()) {
         return "";
      }

      String dynamiteA = "динамит a";
      String dynamiteB = "динамит b";
      String searchA = "динамит а";
      String searchB = "динамит б";

      if (name.equalsIgnoreCase(dynamiteA) || name.equalsIgnoreCase(searchA)) {
         return searchA;
      }
      if (name.equalsIgnoreCase(dynamiteB) || name.equalsIgnoreCase(searchB)) {
         return searchB;
      }

      if (name.contains("(v в радиусе)") || name.contains("радиус")) {
         return "динамит 5 радиус";
      }
      return "";
   }

   public static boolean matchesAuctionStack(AutoBuyParsedItemData item, ItemStack stack, int fallbackMinDurability) {
      return matchesAuctionStack(item, stack, fallbackMinDurability, false, false);
   }

   public static boolean matchesAuctionStack(AutoBuyParsedItemData item, ItemStack stack, int fallbackMinDurability, boolean relaxed) {
      return matchesAuctionStack(item, stack, fallbackMinDurability, relaxed, false);
   }

   public static boolean matchesAutoSetupAuctionStack(AutoBuyParsedItemData item, ItemStack stack, int fallbackMinDurability, boolean relaxed) {
      return matchesAuctionStack(item, stack, fallbackMinDurability, relaxed, true);
   }

   public static String diagnoseAuctionStack(
      AutoBuyParsedItemData item,
      ItemStack stack,
      int fallbackMinDurability,
      boolean relaxed
   ) {
      return diagnoseAuctionStack(item, stack, fallbackMinDurability, relaxed, false);
   }

   public static String diagnoseAutoSetupAuctionStack(
      AutoBuyParsedItemData item,
      ItemStack stack,
      int fallbackMinDurability,
      boolean relaxed
   ) {
      return diagnoseAuctionStack(item, stack, fallbackMinDurability, relaxed, true);
   }

   private static boolean matchesAuctionStack(
      AutoBuyParsedItemData item,
      ItemStack stack,
      int fallbackMinDurability,
      boolean relaxed,
      boolean allowConfiguredAuctionSearchIdentity
   ) {
      if (item == null || stack == null || stack.isEmpty()) {
         return false;
      }

      String stackItemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
      if (!matchesItemId(item, stack, stackItemId)) {
         return false;
      }
      if (isPlainTotem(item)) {
         return true;
      }

      if (allowConfiguredAuctionSearchIdentity && matchesConfiguredAuctionSearchIdentity(item, stack)) {
         return true;
      }

      if (!matchesCustomIdentity(item, stack)) {
         return false;
      }

      if (!meetsDurabilityRequirement(item, stack, fallbackMinDurability)) {
         return false;
      }

      if (!matchesSphereRequirements(item, stack)) {
         return false;
      }
      if (!matchesExperienceBottleRequirements(item, stack)) {
         return false;
      }
      if (!matchesExplosiveRequirements(item, stack)) {
         return false;
      }

      MatchingProfile matchingProfile = determineMatchingProfile(item);
      if (matchingProfile == MatchingProfile.ATTRIBUTES && !matchesAttributes(item, stack)) {
         return false;
      }
      if (matchingProfile == MatchingProfile.POTION && !matchesPotionEffects(item, stack)) {
         return false;
      }
      if (matchingProfile == MatchingProfile.STRICT_CUSTOM && !matchesCustomData(item, stack)) {
         return false;
      }

      boolean displayMatches = matchesDisplayIdentity(item, stack);
      boolean autoBuyKeyMatches = matchesAutoBuyKey(item, stack);
      boolean configuredAuctionIdentityMatches = matchesConfiguredAuctionSearchIdentity(item, stack);
      boolean identityMatches = displayMatches || autoBuyKeyMatches || configuredAuctionIdentityMatches;

      if (relaxed) {
         if (requiresEnchantmentMatch(item) && !matchesEnchantments(item, stack)) {
            return false;
         }
         if (requiresPotionMatch(item) && !matchesPotionEffects(item, stack)) {
            return false;
         }
         return identityMatches;
      }

       if (requiresEnchantmentMatch(item)) {
          if (!matchesEnchantments(item, stack)) {
             return false;
          }
          return identityMatches || matchesStaticTooltip(item, stack);
       }

      if (requiresPotionMatch(item)) {
         if (!matchesPotionEffects(item, stack)) {
            return false;
         }
         return identityMatches || matchesStaticTooltip(item, stack);
      }

      if (requiresDescriptionMatch(item)) {
         return identityMatches || matchesStaticTooltip(item, stack);
      }

      return identityMatches || matchesStaticTooltip(item, stack);
   }

   private static String diagnoseAuctionStack(
      AutoBuyParsedItemData item,
      ItemStack stack,
      int fallbackMinDurability,
      boolean relaxed,
      boolean allowConfiguredAuctionSearchIdentity
   ) {
      if (item == null) {
         return "FAIL:item=null";
      }
      if (stack == null) {
         return "FAIL:stack=null";
      }
      if (stack.isEmpty()) {
         return "FAIL:stack=empty";
      }

      String stackItemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
      if (!matchesItemId(item, stack, stackItemId)) {
         return "FAIL:itemId expected=" + item.itemId() + " actual=" + stackItemId;
      }
      if (isPlainTotem(item)) {
         return "PASS:plain_totem";
      }

      if (allowConfiguredAuctionSearchIdentity && matchesConfiguredAuctionSearchIdentity(item, stack)) {
         return "PASS:configured_search_identity";
      }

      if (!matchesCustomIdentity(item, stack)) {
         return "FAIL:custom_identity";
      }
      if (!meetsDurabilityRequirement(item, stack, fallbackMinDurability)) {
         return "FAIL:durability requirement=" + fallbackMinDurability;
      }
      if (!matchesSphereRequirements(item, stack)) {
         return "FAIL:sphere_requirements";
      }
      if (!matchesExperienceBottleRequirements(item, stack)) {
         return "FAIL:experience_requirements";
      }
      if (!matchesExplosiveRequirements(item, stack)) {
         return "FAIL:explosive_requirements";
      }

      MatchingProfile matchingProfile = determineMatchingProfile(item);
      if (matchingProfile == MatchingProfile.ATTRIBUTES && !matchesAttributes(item, stack)) {
         return "FAIL:attributes";
      }
      if (matchingProfile == MatchingProfile.POTION && !matchesPotionEffects(item, stack)) {
         return "FAIL:potion_profile";
      }
      if (matchingProfile == MatchingProfile.STRICT_CUSTOM && !matchesCustomData(item, stack)) {
         return "FAIL:strict_custom";
      }

      boolean displayMatches = matchesDisplayIdentity(item, stack);
      boolean autoBuyKeyMatches = matchesAutoBuyKey(item, stack);
      boolean configuredAuctionIdentityMatches = matchesConfiguredAuctionSearchIdentity(item, stack);
      boolean staticTooltipMatches = matchesStaticTooltip(item, stack);
      boolean identityMatches = displayMatches || autoBuyKeyMatches || configuredAuctionIdentityMatches;
      String identityState = "display=" + displayMatches
         + ",autoKey=" + autoBuyKeyMatches
         + ",configuredSearch=" + configuredAuctionIdentityMatches
         + ",staticTooltip=" + staticTooltipMatches;

      if (relaxed) {
         if (requiresEnchantmentMatch(item) && !matchesEnchantments(item, stack)) {
            return "FAIL:enchantments(relaxed) " + identityState;
         }
         if (requiresPotionMatch(item) && !matchesPotionEffects(item, stack)) {
            return "FAIL:potion_effects(relaxed) " + identityState;
         }
         return identityMatches ? "PASS:relaxed_identity " + identityState : "FAIL:relaxed_identity " + identityState;
      }

      if (requiresEnchantmentMatch(item)) {
         if (!matchesEnchantments(item, stack)) {
            return "FAIL:enchantments " + identityState;
         }
         return identityMatches || staticTooltipMatches
            ? "PASS:enchantments " + identityState
            : "FAIL:identity_after_enchantments " + identityState;
      }

      if (requiresPotionMatch(item)) {
         if (!matchesPotionEffects(item, stack)) {
            return "FAIL:potion_effects " + identityState;
         }
         return identityMatches || staticTooltipMatches
            ? "PASS:potion_effects " + identityState
            : "FAIL:identity_after_potion " + identityState;
      }

      if (requiresDescriptionMatch(item)) {
         return identityMatches || staticTooltipMatches
            ? "PASS:description " + identityState
            : "FAIL:description " + identityState;
      }

      return identityMatches || staticTooltipMatches
         ? "PASS:identity " + identityState
         : "FAIL:identity " + identityState;
   }

   private static boolean matchesConfiguredAuctionSearchIdentity(AutoBuyParsedItemData item, ItemStack stack) {
      String expectedSearchName = getConfiguredRussianAuctionSearchName(item);
      if (expectedSearchName.isBlank()) {
         return false;
      }

      String expected = normalizeAuctionStackSearchText(expectedSearchName);
      if (expected.isBlank()) {
         return false;
      }

      for (String actual : getAuctionStackSearchLines(stack)) {
         if (matchesAuctionSearchLine(expected, actual)) {
            return true;
         }
      }
      return false;
   }

   private static boolean matchesAuctionSearchLine(String expected, String actual) {
      if (expected == null || expected.isBlank() || actual == null || actual.isBlank()) {
         return false;
      }
      if (actual.equals(expected)) {
         return true;
      }
      return actual.startsWith(expected + " ")
         || actual.startsWith(expected + " (")
         || actual.contains(" " + expected + " ")
         || actual.contains(" " + expected + " (");
   }

   private static List<String> getAuctionStackSearchLines(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return List.of();
      }

      List<String> lines = new ArrayList<>();
      addAuctionStackSearchLine(lines, stack.getHoverName().getString());
      addAuctionStackSearchLine(lines, stack.getItem().getName(stack).getString());

      Minecraft client = Minecraft.getInstance();
      if (client == null || client.player == null) {
         return lines;
      }

      Item.TooltipContext tooltipContext = client.level != null ? Item.TooltipContext.of(client.level) : Item.TooltipContext.EMPTY;
      for (Component line : stack.getTooltipLines(tooltipContext, client.player, TooltipFlag.ADVANCED)) {
         addAuctionStackSearchLine(lines, line.getString());
      }
      return lines;
   }

   private static void addAuctionStackSearchLine(List<String> lines, String text) {
      String normalized = normalizeAuctionStackSearchText(text);
      if (isDynamicAuctionSearchLine(normalized)) {
         return;
      }
      if (!normalized.isBlank() && !lines.contains(normalized)) {
         lines.add(normalized);
      }
   }

   private static boolean isDynamicAuctionSearchLine(String normalized) {
      if (normalized == null || normalized.isBlank()) {
         return true;
      }

      return normalized.contains("\u043f\u0440\u043e\u0434\u0430\u0432\u0435\u0446")
         || normalized.contains("\u0438\u0441\u0442\u0435\u043a\u0430\u0435\u0442")
         || normalized.contains("\u0446\u0435\u043d\u0430")
         || normalized.contains("\u043d\u0430\u0436\u043c\u0438\u0442\u0435")
         || normalized.contains("\u043a\u0443\u043f\u0438\u0442")
         || normalized.contains("\u043b\u043a\u043c")
         || normalized.contains("\u043f\u043a\u043c")
         || normalized.contains("seller")
         || normalized.contains("expires")
         || normalized.contains("price")
         || normalized.contains("click")
         || normalized.contains("buy");
   }

   private static String normalizeAuctionStackSearchText(String text) {
      return normalizeDisplayName(defaultString(text))
         .toLowerCase(Locale.ROOT)
         .replace('\u0451', '\u0435')
         .replace("dynamite", "\u0434\u0438\u043d\u0430\u043c\u0438\u0442")
         .replace("\u0434\u0438\u043d\u0430\u043c\u0438\u0442 a", "\u0434\u0438\u043d\u0430\u043c\u0438\u0442 \u0430")
         .replace("\u0434\u0438\u043d\u0430\u043c\u0438\u0442 b2", "\u0434\u0438\u043d\u0430\u043c\u0438\u0442 \u04312")
         .replace("\u0434\u0438\u043d\u0430\u043c\u0438\u0442 b", "\u0434\u0438\u043d\u0430\u043c\u0438\u0442 \u0431")
         .trim();
   }

   private static MatchingProfile determineMatchingProfile(AutoBuyParsedItemData item) {
      if (item == null) {
         return MatchingProfile.GENERIC;
      }
      String declaredProfile = defaultString(item.matchingProfile()).trim().toLowerCase(Locale.ROOT);
      if (!declaredProfile.isBlank()) {
         return switch (declaredProfile) {
            case "auto_buy_key" -> MatchingProfile.AUTO_BUY_KEY;
            case "pyrotechnic" -> MatchingProfile.PYROTECHNIC;
            case "attributes" -> MatchingProfile.ATTRIBUTES;
            case "potion" -> MatchingProfile.POTION;
            case "enchantments" -> MatchingProfile.ENCHANTMENTS;
            case "strict_custom" -> MatchingProfile.STRICT_CUSTOM;
            default -> MatchingProfile.GENERIC;
         };
      }
      if (item.autoBuyKey() != null && !item.autoBuyKey().isBlank()) {
         return MatchingProfile.AUTO_BUY_KEY;
      }
      String custom = defaultString(item.customData());
      if (custom.contains("pyrotechnic-item")) {
         return MatchingProfile.PYROTECHNIC;
      }
      if (!item.attributes().isEmpty()) {
         return MatchingProfile.ATTRIBUTES;
      }
      if (!item.potionEffects().isEmpty()) {
         return MatchingProfile.POTION;
      }
      if (requiresStrictCustomDataComparison(normalizeComparableCustomData(custom))) {
         return MatchingProfile.STRICT_CUSTOM;
      }
      if (!item.enchantments().isEmpty()) {
         return MatchingProfile.ENCHANTMENTS;
      }
      return MatchingProfile.GENERIC;
   }

   private static boolean matchesCustomIdentity(AutoBuyParsedItemData item, ItemStack stack) {
      if (item == null || stack == null || stack.isEmpty()) {
         return false;
      }

      String expectedCustomData = normalizeComparableCustomData(item.customData());
      if (expectedCustomData.isEmpty()) {
         return true;
      }

      String actualCustomData = normalizeComparableCustomData(getCustomDataString(stack));

      String expectedPyroName = extractPyrotechnicName(expectedCustomData);
      if (!expectedPyroName.isBlank()) {
         String actualPyroName = extractPyrotechnicName(actualCustomData);
         return !actualPyroName.isBlank() && expectedPyroName.equals(actualPyroName);
      }

      String expectedKringeType = extractKringeItemType(expectedCustomData);
      if (!expectedKringeType.isBlank()) {
         String actualKringeType = extractKringeItemType(actualCustomData);
         return !actualKringeType.isBlank() && expectedKringeType.equals(actualKringeType);
      }

      if (requiresProtectionStoneMarker(expectedCustomData)) {
         return hasProtectionStoneMarker(actualCustomData);
      }

      if (requiresTntCannonMarker(expectedCustomData)) {
         return hasTntCannonMarker(actualCustomData);
      }

      if (requiresStrictCustomDataComparison(expectedCustomData)) {
         return expectedCustomData.equals(actualCustomData);
      }

      return true;
   }

   private static boolean requiresStrictCustomDataComparison(String expectedCustomData) {
      if (expectedCustomData == null || expectedCustomData.isBlank()) {
         return false;
      }

      return expectedCustomData.contains("kringeItems")
         || expectedCustomData.contains("protectionstones:ispsblock")
         || expectedCustomData.contains("litetntcannon:tnt-cannon")
         || expectedCustomData.contains("litebackpacks:backpack-uuid")
         || expectedCustomData.contains("SpecialEgg")
         || expectedCustomData.contains("SpecialEgg_Pattern")
         || expectedCustomData.contains("itemServiceId:")
         || expectedCustomData.contains("sphereEffect:")
         || expectedCustomData.contains("cubeevent:cube");
   }

   private enum MatchingProfile {
      GENERIC,
      AUTO_BUY_KEY,
      PYROTECHNIC,
      ATTRIBUTES,
      POTION,
      ENCHANTMENTS,
      STRICT_CUSTOM
   }

   private static AutoBuyParsedItemData tryLoadItem(Path path, Map<String, JsonObject> stateEntries) {
      try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
         JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
         return createParsedItemData(root, path, stateEntries);
      } catch (Exception ignored) {
         return null;
      }
   }

   private static void putParsedItem(Map<String, AutoBuyParsedItemData> itemsByKey, AutoBuyParsedItemData item) {
      if (itemsByKey == null || item == null) {
         return;
      }

      String key = resolveStateKey(item.path());
      if (key.isBlank()) {
         key = item.itemId() + "|" + item.resolvedDisplayName() + "|" + item.baseName();
      }
      itemsByKey.put(key, item);
   }

   private static AutoBuyParsedItemData createParsedItemData(JsonObject root, Path path, Map<String, JsonObject> stateEntries) {
      ParsedItemTemplate template = templateFromJson(root, path, null);
      return template == null ? null : applyState(template, stateEntries);
   }

   private static ParsedItemTemplate templateFromJson(JsonObject root, Path path, String precomputedStateKey) {
      if (root == null) {
         return null;
      }

      JsonObject item = root.getAsJsonObject("item");
      if (item == null) {
         return null;
      }

      String displayName = getJsonString(item, "displayName");
      String baseName = getJsonString(item, "baseName");
      if ((displayName == null || displayName.isBlank()) && (baseName == null || baseName.isBlank())) {
         return null;
      }

      String stateKey = precomputedStateKey != null && !precomputedStateKey.isBlank()
         ? precomputedStateKey
         : buildStateKey(item);

      JsonObject autoBuy = root.has("autoBuy") && root.get("autoBuy").isJsonObject()
         ? root.getAsJsonObject("autoBuy")
         : null;

      String resolvedDisplayName = displayName != null && !displayName.isBlank()
         ? displayName
         : (baseName != null && !baseName.isBlank() ? baseName : "");

      return new ParsedItemTemplate(
         stateKey,
         path,
         normalizeDisplayName(resolvedDisplayName),
         normalizeDisplayName(defaultString(baseName)),
         defaultString(getJsonString(item, "itemId")),
         defaultString(getJsonString(item, "matchingProfile")),
         readStringArray(item.getAsJsonArray("matchingMarkers")),
         defaultString(getJsonString(item, "autoBuyKey")),
         defaultString(getJsonString(item, "customData")),
         defaultString(getJsonString(item, "rawComponentNbt")),
         defaultString(getJsonString(item, "cleanedComponentNbt")),
         defaultString(getJsonString(item, "fullStackNbt")),
         readStringArray(item.getAsJsonArray("tooltip")),
         readEnchantments(item.getAsJsonArray("enchantments")),
         readAttributes(item.getAsJsonArray("attributes")),
         readPotionEffects(item.getAsJsonArray("potionEffects")),
         getJsonBoolean(item, "damageable", false),
         getJsonIntRaw(item, "maxDamage", 0),
         getJsonBoolean(autoBuy, "enabled", false),
         getJsonString(autoBuy, "price"),
         getJsonIntRaw(autoBuy, "minDurability", DEFAULT_MIN_DURABILITY)
      );
   }

   private static AutoBuyParsedItemData applyState(ParsedItemTemplate template, Map<String, JsonObject> stateEntries) {
      if (template == null) {
         return null;
      }

      JsonObject state = stateEntries == null ? null : stateEntries.get(template.stateKey());
      ItemStack iconStack = resolveItemStack(template);

      String displayName = template.displayName();
      if (displayName == null || displayName.isBlank()) {
         String baseName = template.baseName();
         displayName = baseName != null && !baseName.isBlank()
            ? baseName
            : normalizeDisplayName(iconStack.getHoverName().getString());
      }

      String statePrice = state == null ? null : getJsonString(state, "price");
      String resolvedPrice = normalizeStoredPrice(statePrice != null ? statePrice : template.defaultPrice());
      boolean resolvedEnabled = state == null
         ? template.defaultEnabled()
         : getJsonBoolean(state, "enabled", template.defaultEnabled());
      int resolvedMinDurability = state != null && state.has("minDurability")
         ? clampPercent(getJsonIntRaw(state, "minDurability", template.defaultMinDurability()))
         : clampPercent(template.defaultMinDurability());

      return new AutoBuyParsedItemData(
         template.embeddedPath(),
         displayName,
         template.baseName(),
         template.itemId(),
         template.matchingProfile(),
         template.matchingMarkers(),
         template.autoBuyKey(),
         template.customData(),
         template.rawComponentNbt(),
         template.cleanedComponentNbt(),
         template.fullStackNbt(),
         template.tooltip(),
         template.enchantments(),
         template.attributes(),
         template.potionEffects(),
         template.damageable(),
         template.maxDamage(),
         resolvedEnabled,
         resolvedPrice,
         resolvedMinDurability,
         iconStack
      );
   }

   private static Map<String, JsonObject> loadStateEntries() {
      Path stateFile = resolveStateFile();
      migrateLegacyStateFile(stateFile);
      JsonObject root = loadStateRoot(stateFile);
      if (root == null) {
         return Map.of();
      }

      JsonObject items = root.has("items") && root.get("items").isJsonObject()
         ? root.getAsJsonObject("items")
         : null;
      if (items == null) {
         return Map.of();
      }

      Map<String, JsonObject> loaded = new HashMap<>();
      for (Map.Entry<String, JsonElement> entry : items.entrySet()) {
         if (entry.getValue() != null && entry.getValue().isJsonObject()) {
            loaded.put(entry.getKey(), entry.getValue().getAsJsonObject());
         }
      }
      return loaded;
   }

   private static void updateStateEntry(String stateKey, java.util.function.Consumer<JsonObject> updater) {
      if (stateKey == null || stateKey.isBlank() || updater == null) {
         return;
      }

      Path stateFile = resolveStateFile();
      migrateLegacyStateFile(stateFile);
      try {
         Files.createDirectories(stateFile.getParent());
         JsonObject root = loadStateRoot(stateFile);
         if (root == null) {
            root = new JsonObject();
         }

         JsonObject items = root.has("items") && root.get("items").isJsonObject()
            ? root.getAsJsonObject("items")
            : new JsonObject();
         JsonObject state = items.has(stateKey) && items.get(stateKey).isJsonObject()
            ? items.getAsJsonObject(stateKey)
            : new JsonObject();
         updater.accept(state);
         items.add(stateKey, state);
         root.add("items", items);
         Files.writeString(stateFile, GSON.toJson(root), StandardCharsets.UTF_8);
      } catch (Exception ignored) {
      }
   }

   private static JsonObject loadStateRoot(Path preferredStateFile) {
      JsonObject root = tryReadStateRoot(preferredStateFile);
      if (root != null) {
         return root;
      }

      Path legacyStateFile = resolveLegacyStateFile();
      JsonObject legacyRoot = tryReadStateRoot(legacyStateFile);
      if (legacyRoot != null) {
         try {
            if (preferredStateFile != null) {
               Files.createDirectories(preferredStateFile.getParent());
               Files.writeString(preferredStateFile, GSON.toJson(legacyRoot), StandardCharsets.UTF_8);
            }
         } catch (Exception ignored) {
         }
      }
      return legacyRoot;
   }

   private static JsonObject tryReadStateRoot(Path stateFile) {
      if (stateFile == null || !Files.isRegularFile(stateFile)) {
         return null;
      }

      try (Reader reader = Files.newBufferedReader(stateFile, StandardCharsets.UTF_8)) {
         JsonElement parsed = JsonParser.parseReader(reader);
         return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
      } catch (Exception ignored) {
         return null;
      }
   }

   private static Path resolveStateFile() {
      return Path.of(ClientInfo.CONFIG_PATH_OTHER, STATE_FILE_NAME);
   }

   private static Path resolveLegacyStateFile() {
      return Path.of(ClientInfo.CONFIG_PATH_OTHER, STATE_FILE_NAME);
   }

   private static Path resolveClientDirectory() {
      return Path.of(ClientInfo.CONFIG_PATH_OTHER);
   }

   private static void migrateLegacyStateFile(Path stateFile) {
      Path legacyStateFile = resolveLegacyStateFile();
      if (stateFile == null
         || legacyStateFile == null
         || Files.isRegularFile(stateFile)
         || !Files.isRegularFile(legacyStateFile)) {
         return;
      }

      try {
         Files.createDirectories(stateFile.getParent());
         Files.copy(legacyStateFile, stateFile, StandardCopyOption.COPY_ATTRIBUTES);
      } catch (Exception ignored) {
      }
   }

   private static String resolveStateKey(Path path) {
      if (path == null) {
         return "";
      }

      String embeddedStateKey = extractEmbeddedStateKey(path);
      if (!embeddedStateKey.isBlank()) {
         return embeddedStateKey;
      }
      if (!Files.isRegularFile(path)) {
         return "";
      }

      try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
         JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
         JsonObject item = root.getAsJsonObject("item");
         return item == null ? "" : buildStateKey(item);
      } catch (Exception ignored) {
         return "";
      }
   }

   private static Path buildEmbeddedItemPath(String stateKey) {
      return Path.of(EMBEDDED_ITEM_PATH_ROOT, stateKey + EMBEDDED_ITEM_FILE_SUFFIX);
   }

   private static String extractEmbeddedStateKey(Path path) {
      if (path == null || path.getNameCount() < 2) {
         return "";
      }
      if (!EMBEDDED_ITEM_PATH_ROOT.equals(path.getName(0).toString())) {
         return "";
      }

      String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
      if (!fileName.endsWith(EMBEDDED_ITEM_FILE_SUFFIX)) {
         return "";
      }

      String stateKey = fileName.substring(0, fileName.length() - EMBEDDED_ITEM_FILE_SUFFIX.length());
      return stateKey.matches("[0-9a-f]{64}") ? stateKey : "";
   }

   private static String getSortName(String displayName, String baseName) {
      String resolvedName = (displayName != null && !displayName.isBlank()) ? displayName : defaultString(baseName);
      return normalizeDisplayName(resolvedName)
         .toLowerCase(Locale.ROOT)
         .replace('\u0451', '\u0435')
         .replace("\u00AB", "")
         .replace("\u00BB", "")
         .replaceAll("\\s{2,}", " ")
         .trim();
   }

   private static String getBackpackSearchName(String name) {
      if (!name.toLowerCase(Locale.ROOT).contains("рюкзак")) {
         return "";
      }
      String lower = name.toLowerCase(Locale.ROOT);
      if (lower.contains("(i уровень)") || lower.contains("(1 уровень)") || lower.contains("(i)")) {
         return "рюкзак 1 уровень";
      }
      if (lower.contains("(ii уровень)") || lower.contains("(2 уровень)") || lower.contains("(ii)")) {
         return "рюкзак 2 уровень";
      }
      if (lower.contains("(iii уровень)") || lower.contains("(3 уровень)") || lower.contains("(iii)")) {
         return "рюкзак 3 уровень";
      }
      if (lower.contains("(iv уровень)") || lower.contains("(4 уровень)") || lower.contains("(iv)")) {
         return "рюкзак 4 уровень";
      }
      if (lower.contains("(v уровень)") || lower.contains("(5 уровень)") || lower.contains("(v)")) {
         return "рюкзак 5 уровень";
      }
      return "";
   }

   private static String normalizeAuctionAliasKey(String text) {
      return normalizeDisplayName(defaultString(text))
         .toLowerCase(Locale.ROOT)
         .replace('\u0451', '\u0435')
         .replaceAll("\\s{2,}", " ")
         .trim();
   }

   private static boolean isReliableStillerName(String aliasKey) {
      return aliasKey.contains("\u043d\u0430\u0434\u0435\u0436\u043d")
         && aliasKey.contains("\u0441\u0442\u0438\u043b\u043b\u0435\u0440");
   }

   private static boolean isEndFragmentEggName(String aliasKey) {
      return aliasKey.contains("\u043e\u0441\u043a\u043e\u043b\u043e\u0447")
         && aliasKey.contains("\u044f\u0439\u0446")
         && aliasKey.contains("\u044d\u043d\u0434\u0435\u0440");
   }

   private static int extractFirstNumber(String text) {
      StringBuilder digits = new StringBuilder();
      for (int i = 0; i < text.length(); i++) {
         char current = text.charAt(i);
         if (Character.isDigit(current)) {
            digits.append(current);
         } else if (!digits.isEmpty()) {
            break;
         }
      }

      if (digits.isEmpty()) {
         return Integer.MAX_VALUE;
      }

      try {
         return Integer.parseInt(digits.toString());
      } catch (NumberFormatException ignored) {
         return Integer.MAX_VALUE;
      }
   }

   private static synchronized ItemStack resolveItemStack(ParsedItemTemplate template) {
      Minecraft client = Minecraft.getInstance();
      if (client.level != null) {
         Object registryManager = client.level.registryAccess();
         if (registryManager != iconStackRegistryManager) {
            ICON_STACK_CACHE.clear();
            iconStackRegistryManager = registryManager;
         }

         String cacheKey = defaultString(template.stateKey());
         ItemStack cached = ICON_STACK_CACHE.get(cacheKey);
         if (cached != null) {
            return cached;
         }

         for (String stackNbt : List.of(
            defaultString(template.fullStackNbt()),
            defaultString(template.cleanedComponentNbt())
         )) {
            if (stackNbt.isBlank()) {
               continue;
            }
            try {
               CompoundTag parsed = TagParser.parseCompoundFully(stackNbt);
               if (!parsed.contains("id")) {
                  continue;
               }
               ItemStack decoded = ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, client.level.registryAccess()), parsed).result().orElse(ItemStack.EMPTY);
               if (!decoded.isEmpty()) {
                  ICON_STACK_CACHE.put(cacheKey, decoded);
                  return decoded;
               }
            } catch (Exception ignored) {
            }
         }
      }

      String itemId = template.itemId();
      Identifier identifier = itemId == null || itemId.isBlank() ? null : Identifier.tryParse(itemId);
      if (identifier == null) {
         return new ItemStack(Items.BARRIER);
      }

      Item item = BuiltInRegistries.ITEM.getValue(identifier);
      if (item == Items.AIR) {
         return new ItemStack(Items.BARRIER);
      }
      ItemStack fallback = new ItemStack(item);
      if (client.level != null) {
         ICON_STACK_CACHE.put(defaultString(template.stateKey()), fallback);
      }
      return fallback;
   }

   private static List<String> readStringArray(JsonArray array) {
      List<String> values = new ArrayList<>();
      if (array == null) {
         return values;
      }

      for (JsonElement element : array) {
         if (element != null && !element.isJsonNull()) {
            values.add(element.getAsString());
         }
      }
      return values;
   }

   private static List<AutoBuyParsedItemData.EnchantmentData> readEnchantments(JsonArray array) {
      List<AutoBuyParsedItemData.EnchantmentData> values = new ArrayList<>();
      if (array == null) {
         return values;
      }

      for (JsonElement element : array) {
         if (element == null || !element.isJsonObject()) {
            continue;
         }
         JsonObject entry = element.getAsJsonObject();
         values.add(new AutoBuyParsedItemData.EnchantmentData(
            defaultString(getJsonString(entry, "id")),
            getJsonIntRaw(entry, "level", 0)
         ));
      }
      return values;
   }

   private static List<AutoBuyParsedItemData.AttributeData> readAttributes(JsonArray array) {
      List<AutoBuyParsedItemData.AttributeData> values = new ArrayList<>();
      if (array == null) {
         return values;
      }

      for (JsonElement element : array) {
         if (element == null || !element.isJsonObject()) {
            continue;
         }
         JsonObject entry = element.getAsJsonObject();
         values.add(new AutoBuyParsedItemData.AttributeData(
            defaultString(getJsonString(entry, "attribute")),
            defaultString(getJsonString(entry, "slot")),
            defaultString(getJsonString(entry, "modifierId")),
            getJsonDouble(entry, "value", 0.0D),
            defaultString(getJsonString(entry, "operation"))
         ));
      }
      return values;
   }

   private static List<AutoBuyParsedItemData.PotionEffectData> readPotionEffects(JsonArray array) {
      List<AutoBuyParsedItemData.PotionEffectData> values = new ArrayList<>();
      if (array == null) {
         return values;
      }

      for (JsonElement element : array) {
         if (element == null || !element.isJsonObject()) {
            continue;
         }
         JsonObject entry = element.getAsJsonObject();
         values.add(new AutoBuyParsedItemData.PotionEffectData(
            defaultString(getJsonString(entry, "type")),
            defaultString(getJsonString(entry, "id")),
            defaultString(getJsonString(entry, "translationKey")),
            getJsonIntRaw(entry, "duration", 0),
            getJsonIntRaw(entry, "amplifier", 0),
            getJsonBoolean(entry, "ambient", false),
            getJsonBoolean(entry, "showParticles", false),
            getJsonBoolean(entry, "showIcon", false),
            defaultString(getJsonString(entry, "value")),
            defaultString(getJsonString(entry, "color"))
         ));
      }
      return values;
   }

   private static boolean meetsDurabilityRequirement(AutoBuyParsedItemData item, ItemStack stack, int fallbackMinDurability) {
      if (!item.damageable()) {
         return true;
      }
      if (fallbackMinDurability < 0) {
         return true;
      }

      int threshold = clampPercent(fallbackMinDurability > 0 ? fallbackMinDurability : item.minDurability());
      int durabilityPercent = extractTooltipDurabilityPercent(stack);
      if (durabilityPercent == Integer.MAX_VALUE && stack.isDamageableItem() && stack.getMaxDamage() > 0) {
         int maxDamage = stack.getMaxDamage();
         int remaining = Math.max(0, maxDamage - stack.getDamageValue());
         durabilityPercent = Math.round(remaining * 100f / Math.max(1, maxDamage));
      }
      if (durabilityPercent == Integer.MAX_VALUE) {
         return true;
      }
      return durabilityPercent >= threshold;
   }

   private static boolean matchesItemId(AutoBuyParsedItemData item, ItemStack stack, String stackItemId) {
      if (stackItemId.equals(item.itemId())) {
         return true;
      }

      return isMysterySpawnEgg(item)
         && (stack.getItem() instanceof SpawnEggItem || stackItemId.endsWith("_spawn_egg"));
   }

   private static boolean matchesDisplayIdentity(AutoBuyParsedItemData item, ItemStack stack) {
      boolean strictIdentity = requiresStrictDisplayIdentity(item);
      List<String> expectedNames = new ArrayList<>(2);
      String expectedDisplayName = getSortName(item.displayName(), "");
      String expectedBaseName = getSortName(item.baseName(), "");
      boolean hasCustomDisplayName = !expectedDisplayName.isBlank() && !expectedDisplayName.equals(expectedBaseName);
      if (!expectedDisplayName.isBlank()) {
         expectedNames.add(expectedDisplayName);
      }
      if (!hasCustomDisplayName && !expectedBaseName.isBlank() && !expectedBaseName.equals(expectedDisplayName)) {
         expectedNames.add(expectedBaseName);
      }
      if (expectedNames.isEmpty()) {
         return true;
      }

      List<String> actualNames = new ArrayList<>(3);
      String actualDisplayName = getSortName(stack.getHoverName().getString(), "");
      String actualBaseName = getSortName(stack.getItem().getName(stack).getString(), "");
      String actualTooltipTitle = getTooltipTitle(stack);
      if (!actualDisplayName.isBlank()) {
         actualNames.add(actualDisplayName);
      }
      if (!actualBaseName.isBlank() && !actualBaseName.equals(actualDisplayName)) {
         actualNames.add(actualBaseName);
      }
      if (!actualTooltipTitle.isBlank()
         && !actualTooltipTitle.equals(actualDisplayName)
         && !actualTooltipTitle.equals(actualBaseName)) {
         actualNames.add(actualTooltipTitle);
      }

      for (String expectedName : expectedNames) {
         for (String actualName : actualNames) {
            if (strictIdentity ? expectedName.equals(actualName) : matchesLooseText(expectedName, actualName)) {
               return true;
            }
         }
      }

      return false;
   }

   private static String getTooltipTitle(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return "";
      }

      Minecraft client = Minecraft.getInstance();
      if (client == null || client.player == null) {
         return "";
      }

      Item.TooltipContext tooltipContext = client.level != null ? Item.TooltipContext.of(client.level) : Item.TooltipContext.EMPTY;
      for (Component line : stack.getTooltipLines(tooltipContext, client.player, TooltipFlag.ADVANCED)) {
         String normalized = getSortName(line.getString(), "");
         if (!normalized.isBlank()) {
            return normalized;
         }
      }

      return "";
   }

   private static boolean matchesEnchantments(AutoBuyParsedItemData item, ItemStack stack) {
      if (item.enchantments().isEmpty()) {
         return true;
      }
      if (shouldIgnoreEnchantmentsForMatching(item)) {
         return true;
      }

      Set<String> expected = new HashSet<>();
      for (AutoBuyParsedItemData.EnchantmentData enchantment : item.enchantments()) {
         expected.add(enchantment.id() + ":" + enchantment.level());
      }

      Set<String> actual = new HashSet<>();
      ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
      if (enchants != null) {
         for (var entry : enchants.entrySet()) {
            String id = entry.getKey().unwrapKey().map(k -> k.identifier().toString()).orElse(entry.getKey().getRegisteredName());
            actual.add(id + ":" + entry.getIntValue());
         }
      }
      ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
      if (stored != null) {
         for (var entry : stored.entrySet()) {
            String id = entry.getKey().unwrapKey().map(k -> k.identifier().toString()).orElse(entry.getKey().getRegisteredName());
            actual.add(id + ":" + entry.getIntValue());
         }
      }
      return actual.containsAll(expected);
   }

   private static boolean matchesAutoBuyKey(AutoBuyParsedItemData item, ItemStack stack) {
      if (item == null || item.autoBuyKey() == null || item.autoBuyKey().isBlank()) {
         return false;
      }

      String actualKey = AutoBuyUtil.getKey(stack);
      return !actualKey.isBlank() && item.autoBuyKey().equals(actualKey);
   }

   private static boolean requiresEnchantmentMatch(AutoBuyParsedItemData item) {
      return item != null && !item.enchantments().isEmpty() && !shouldIgnoreEnchantmentsForMatching(item);
   }

   private static boolean requiresStrictDisplayIdentity(AutoBuyParsedItemData item) {
      return item != null && "minecraft:elytra".equals(item.itemId());
   }

   private static boolean shouldIgnoreEnchantmentsForMatching(AutoBuyParsedItemData item) {
      return item != null && ("minecraft:elytra".equals(item.itemId()) || isPlainTotem(item));
   }

   private static boolean isPlainTotem(AutoBuyParsedItemData item) {
      if (item == null || !"minecraft:totem_of_undying".equals(item.itemId())) {
         return false;
      }

      String normalizedDisplayName = getSortName(item.displayName(), "");
      String normalizedBaseName = getSortName(item.baseName(), "");
      if (normalizedBaseName.isBlank()) {
         return false;
      }
      if (normalizedDisplayName.isBlank()) {
         return true;
      }
      return normalizedDisplayName.equals(normalizedBaseName);
   }

   private static boolean hasDefaultDisplayIdentity(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }

      String normalizedDisplayName = getSortName(stack.getHoverName().getString(), "");
      String normalizedBaseName = getSortName(stack.getItem().getName(stack).getString(), "");
      if (normalizedBaseName.isBlank()) {
         return false;
      }
      if (normalizedDisplayName.isBlank()) {
         return true;
      }
      return normalizedDisplayName.equals(normalizedBaseName);
   }

   private static boolean requiresPotionMatch(AutoBuyParsedItemData item) {
      return item != null && item.enchantments().isEmpty() && !item.potionEffects().isEmpty();
   }

   private static boolean requiresDescriptionMatch(AutoBuyParsedItemData item) {
      if (item == null) {
         return false;
      }

      if (item.enchantments().isEmpty()) {
         return !extractDescriptionTooltipLines(filterStaticTooltipLines(item.tooltip())).isEmpty()
            || isArmorLikeItem(item)
            || !filterStaticTooltipLines(item.tooltip()).isEmpty();
      }

      return false;
   }

   private static boolean isArmorLikeItem(AutoBuyParsedItemData item) {
      if (item == null) {
         return false;
      }
      if ("minecraft:elytra".equals(item.itemId())) {
         return true;
      }
      ItemStack iconStack = item.iconStack();
      return iconStack != null && iconStack.has(DataComponents.EQUIPPABLE);
   }

   private static boolean matchesSphereRequirements(AutoBuyParsedItemData item, ItemStack stack) {
      String sortName = getSortName(item.displayName(), item.baseName()).toLowerCase(Locale.ROOT);
      List<String> actualDescription = extractDescriptionTooltipLines(getStaticTooltipLines(stack));
      if (sortName.contains("легендарн")) {
         return containsSphereDescriptionMarker(actualDescription, "скорость 3", "скорость iii");
      }
      if (sortName.contains("мифическ")) {
         return (containsSphereDescriptionMarker(actualDescription, "сила 3", "сила iii") || containsSphereDescriptionMarker(actualDescription, "урон 3", "урон iii"))
            && (containsSphereDescriptionMarker(actualDescription, "добыча 2", "добыча ii"));
      }
      return true;
   }

   private static boolean matchesExperienceBottleRequirements(AutoBuyParsedItemData item, ItemStack stack) {
      if (item == null || stack == null || stack.isEmpty()) {
         return false;
      }
      if (!"minecraft:experience_bottle".equals(item.itemId())) {
         return true;
      }

      int expectedLevel = extractExperienceBottleLevel(item);
      if (expectedLevel != Integer.MAX_VALUE) {
         int actualLevel = extractExperienceBottleLevel(stack);
         if (actualLevel != expectedLevel) {
            return false;
         }
      }

      int expectedValue = extractExperienceBottleValue(item.customData());
      if (expectedValue == Integer.MAX_VALUE) {
         expectedValue = extractExperienceBottleValue(item.cleanedComponentNbt());
      }
      if (expectedValue == Integer.MAX_VALUE) {
         return true;
      }

      int actualValue = extractExperienceBottleValue(getCustomDataString(stack));
      if (actualValue == Integer.MAX_VALUE) {
         actualValue = extractExperienceBottleTooltipValue(stack);
      }
      return actualValue == expectedValue;
   }

   private static boolean matchesExplosiveRequirements(AutoBuyParsedItemData item, ItemStack stack) {
      if (item == null || stack == null || stack.isEmpty()) {
         return false;
      }
      if (!"minecraft:tnt".equals(item.itemId())) {
         return true;
      }

      String expectedCustomData = normalizeComparableCustomData(item.customData());
      String actualCustomData = normalizeComparableCustomData(getCustomDataString(stack));
      String expectedPyroName = extractPyrotechnicName(expectedCustomData);
      if (!expectedPyroName.isBlank()) {
         String actualPyroName = extractPyrotechnicName(actualCustomData);
         if (actualPyroName.isBlank() || !actualPyroName.equals(expectedPyroName)) {
            return false;
         }
      } else if (expectedCustomData.contains("pyrotechnic-item") && !actualCustomData.contains("pyrotechnic-item")) {
         return false;
      }

      List<String> expectedDescription = extractDescriptionTooltipLines(filterStaticTooltipLines(item.tooltip()));
      if (expectedDescription.isEmpty()) {
         return true;
      }

      Minecraft client = Minecraft.getInstance();
      Item.TooltipContext tooltipContext = client.level != null ? Item.TooltipContext.of(client.level) : Item.TooltipContext.EMPTY;
      List<String> actualRaw = new ArrayList<>();
      for (Component line : stack.getTooltipLines(tooltipContext, client.player, TooltipFlag.ADVANCED)) {
         actualRaw.add(line.getString());
      }

      List<String> actualDescription = extractDescriptionTooltipLines(filterStaticTooltipLines(actualRaw));
      return containsAllExpectedLinesExact(actualDescription, expectedDescription);
   }

   private static boolean hasTooltipMarker(ItemStack stack, String expectedLine) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }

      String normalizedExpected = normalizeTooltipLine(expectedLine);
      if (normalizedExpected.isEmpty()) {
         return false;
      }

      Minecraft client = Minecraft.getInstance();
      Item.TooltipContext tooltipContext = client.level != null ? Item.TooltipContext.of(client.level) : Item.TooltipContext.EMPTY;
      for (Component line : stack.getTooltipLines(tooltipContext, client.player, TooltipFlag.ADVANCED)) {
         if (matchesLooseText(normalizedExpected, normalizeTooltipLine(line.getString()))) {
            return true;
         }
      }

      return false;
   }

   private static List<String> getStaticTooltipLines(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return List.of();
      }

      Minecraft client = Minecraft.getInstance();
      if (client == null || client.player == null) {
         return List.of();
      }

      Item.TooltipContext tooltipContext = client.level != null ? Item.TooltipContext.of(client.level) : Item.TooltipContext.EMPTY;
      List<String> actualRaw = new ArrayList<>();
      for (Component line : stack.getTooltipLines(tooltipContext, client.player, TooltipFlag.ADVANCED)) {
         actualRaw.add(line.getString());
      }
      return filterStaticTooltipLines(actualRaw);
   }

   private static boolean containsSphereDescriptionMarker(List<String> actualDescription, String... expectedMarkers) {
      if (actualDescription == null || actualDescription.isEmpty() || expectedMarkers == null || expectedMarkers.length == 0) {
         return false;
      }

      Set<String> normalizedActual = new HashSet<>();
      for (String actualLine : actualDescription) {
         String normalized = normalizeSphereEffectMarker(actualLine);
         if (!normalized.isBlank()) {
            normalizedActual.add(normalized);
         }
      }

      for (String expectedMarker : expectedMarkers) {
         String normalizedExpected = normalizeSphereEffectMarker(expectedMarker);
         if (!normalizedExpected.isBlank() && normalizedActual.contains(normalizedExpected)) {
            return true;
         }
      }

      return false;
   }

   private static String normalizeSphereEffectMarker(String text) {
      String normalized = normalizeTooltipLine(text)
         .replace("\u00a0", " ")
         .replaceFirst("^[\\-\u2013\u2014]+\\s*", "")
         .replaceAll("\\biii\\b", "3")
         .replaceAll("\\bii\\b", "2")
         .replaceAll("\\bi\\b", "1")
         .replaceAll("\\s{2,}", " ")
         .trim();
      return normalized;
   }

   private static String extractPyrotechnicName(String customData) {
      String normalized = defaultString(customData);
      if (normalized.isBlank()) {
         return "";
      }

      Matcher matcher = PYROTECHNIC_NAME_PATTERN.matcher(normalized);
      if (!matcher.find()) {
         return "";
      }
      return defaultString(matcher.group(1)).trim();
   }

   private static String extractKringeItemType(String customData) {
      String normalized = defaultString(customData);
      if (normalized.isBlank()) {
         return "";
      }

      Matcher matcher = KRINGE_ITEM_TYPE_PATTERN.matcher(normalized);
      if (!matcher.find()) {
         return "";
      }
      return defaultString(matcher.group(1)).trim();
   }

   private static boolean requiresProtectionStoneMarker(String customData) {
      return defaultString(customData).contains("protectionstones:ispsblock");
   }

   private static boolean hasProtectionStoneMarker(String customData) {
      String normalized = defaultString(customData);
      return normalized.contains("protectionstones:ispsblock")
         && normalized.matches("(?s).*protectionstones:ispsblock\"?\\s*[:=]\\s*1b.*");
   }

   private static boolean requiresTntCannonMarker(String customData) {
      return defaultString(customData).contains("litetntcannon:tnt-cannon");
   }

   private static boolean hasTntCannonMarker(String customData) {
      String normalized = defaultString(customData);
      return normalized.contains("litetntcannon:tnt-cannon")
         && normalized.matches("(?s).*litetntcannon:tnt-cannon\"?\\s*[:=]\\s*1b.*");
   }

   private static int extractExperienceBottleLevel(AutoBuyParsedItemData item) {
      int level = extractExperienceBottleLevel(defaultString(item.displayName()));
      if (level != Integer.MAX_VALUE) {
         return level;
      }
      level = extractExperienceBottleLevel(defaultString(item.baseName()));
      if (level != Integer.MAX_VALUE) {
         return level;
      }

      for (String line : item.tooltip()) {
         level = extractExperienceBottleLevel(line);
         if (level != Integer.MAX_VALUE) {
            return level;
         }
      }
      return Integer.MAX_VALUE;
   }

   private static int extractExperienceBottleLevel(ItemStack stack) {
      int level = extractExperienceBottleLevel(stack.getHoverName().getString());
      if (level != Integer.MAX_VALUE) {
         return level;
      }
      level = extractExperienceBottleLevel(stack.getItem().getName(stack).getString());
      if (level != Integer.MAX_VALUE) {
         return level;
      }

      Minecraft client = Minecraft.getInstance();
      Item.TooltipContext tooltipContext = client.level != null ? Item.TooltipContext.of(client.level) : Item.TooltipContext.EMPTY;
      for (Component line : stack.getTooltipLines(tooltipContext, client.player, TooltipFlag.ADVANCED)) {
         level = extractExperienceBottleLevel(line.getString());
         if (level != Integer.MAX_VALUE) {
            return level;
         }
      }
      return Integer.MAX_VALUE;
   }

   private static int extractExperienceBottleLevel(String text) {
      String normalized = normalizeTooltipLine(text);
      Matcher matcher = EXPERIENCE_BOTTLE_LEVEL_PATTERN.matcher(normalized);
      if (!matcher.find()) {
         return Integer.MAX_VALUE;
      }

      try {
         return Integer.parseInt(matcher.group(1));
      } catch (NumberFormatException ignored) {
         return Integer.MAX_VALUE;
      }
   }

   private static int extractExperienceBottleValue(String text) {
      String source = defaultString(text);
      if (source.isBlank()) {
         return Integer.MAX_VALUE;
      }

      Matcher matcher = EXPERIENCE_BOTTLE_VALUE_PATTERN.matcher(source);
      if (!matcher.find()) {
         return Integer.MAX_VALUE;
      }

      try {
         return Integer.parseInt(matcher.group(1));
      } catch (NumberFormatException ignored) {
         return Integer.MAX_VALUE;
      }
   }

   private static int extractExperienceBottleTooltipValue(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return Integer.MAX_VALUE;
      }

      Minecraft client = Minecraft.getInstance();
      Item.TooltipContext tooltipContext = client.level != null ? Item.TooltipContext.of(client.level) : Item.TooltipContext.EMPTY;
      for (Component line : stack.getTooltipLines(tooltipContext, client.player, TooltipFlag.ADVANCED)) {
         String normalized = normalizeTooltipLine(line.getString());
         Matcher matcher = EXPERIENCE_BOTTLE_TOOLTIP_VALUE_PATTERN.matcher(normalized);
         if (!matcher.find()) {
            continue;
         }

         try {
            return Integer.parseInt(matcher.group(1));
         } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
         }
      }

      return Integer.MAX_VALUE;
   }

   private static boolean matchesCustomData(AutoBuyParsedItemData item, ItemStack stack) {
      String expected = normalizeComparableCustomData(item.customData());
      if (expected.isEmpty()) {
         return true;
      }

      String actual = normalizeComparableCustomData(getCustomDataString(stack));
      return expected.equals(actual);
   }

   private static boolean matchesAttributes(AutoBuyParsedItemData item, ItemStack stack) {
      if (item.attributes().isEmpty()) {
         return true;
      }

      ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
      if (modifiers == null) {
         return false;
      }

      Set<String> actual = new HashSet<>();
      for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
         AttributeModifier modifier = entry.modifier();
         String attributeId = entry.attribute().unwrapKey().map(key -> key.identifier().toString()).orElse(entry.attribute().getRegisteredName());
         String slotName = entry.slot() != null ? entry.slot().getSerializedName() : "any";
         actual.add(formatComparableAttribute(attributeId, slotName, modifier.amount(), modifier.operation().name()));
      }

      for (AutoBuyParsedItemData.AttributeData attribute : item.attributes()) {
         String expected = formatComparableAttribute(attribute.attribute(), attribute.slot(), attribute.value(), attribute.operation());
         if (!actual.contains(expected)) {
            return false;
         }
      }
      return true;
   }

   private static boolean matchesPotionEffects(AutoBuyParsedItemData item, ItemStack stack) {
      if (item.potionEffects().isEmpty()) {
         return true;
      }

      Set<String> actual = new HashSet<>();
      PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
      if (potionContents != null) {
         potionContents.potion().ifPresent(potion ->
            actual.add(formatPotionEffectSignature(
               "basePotion",
               potion.unwrapKey().map(key -> key.identifier().toString()).orElse(potion.getRegisteredName()),
               "",
               0,
               0,
               false,
               false,
               false,
               "",
               String.format(Locale.ROOT, "#%06X", potionContents.getColor() & 0xFFFFFF)
            ))
         );

         for (MobEffectInstance effect : potionContents.getAllEffects()) {
            String id = effect.getEffect().unwrapKey().map(key -> key.identifier().toString()).orElse(effect.getEffect().getRegisteredName());
            actual.add(formatPotionEffectSignature(
               "effect",
               id,
               effect.getEffect().value().getDescriptionId(),
               effect.getDuration(),
               effect.getAmplifier(),
               effect.isAmbient(),
               effect.isVisible(),
               effect.showIcon(),
               "",
               ""
            ));
         }

         potionContents.customColor().ifPresent(color ->
            actual.add(formatPotionEffectSignature(
               "customColor",
               "",
               "",
               0,
               0,
               false,
               false,
               false,
               "",
               String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF)
            ))
         );
      }

      for (AutoBuyParsedItemData.PotionEffectData effect : item.potionEffects()) {
         String expected = formatPotionEffectSignature(
            effect.type(),
            effect.id(),
            effect.translationKey(),
            effect.duration(),
            effect.amplifier(),
            effect.ambient(),
            effect.showParticles(),
            effect.showIcon(),
            effect.value(),
            effect.color()
         );
         if (!actual.contains(expected)) {
            return false;
         }
      }
      return true;
   }

   private static boolean matchesStaticTooltip(AutoBuyParsedItemData item, ItemStack stack) {
      List<String> expected = filterStaticTooltipLines(item.tooltip());
      if (expected.isEmpty()) {
         return true;
      }

      Minecraft client = Minecraft.getInstance();
      Item.TooltipContext tooltipContext = client.level != null ? Item.TooltipContext.of(client.level) : Item.TooltipContext.EMPTY;
      List<String> actualRaw = new ArrayList<>();
      for (Component line : stack.getTooltipLines(tooltipContext, client.player, TooltipFlag.ADVANCED)) {
         actualRaw.add(line.getString());
      }

      List<String> actualFiltered = filterStaticTooltipLines(actualRaw);
      if (requiresStrictDisplayIdentity(item)) {
         return containsAllExpectedLinesExact(actualFiltered, expected);
      }

      if (containsAllExpectedLines(actualFiltered, expected)) {
         return true;
      }

      List<String> expectedDescription = extractDescriptionTooltipLines(expected);
      List<String> actualDescription = extractDescriptionTooltipLines(actualFiltered);
      if (!expectedDescription.isEmpty() && containsAllExpectedLines(actualDescription, expectedDescription)) {
         return true;
      }

      if (!isFlexibleMysteryItem(item)) {
         return false;
      }

      List<String> flexibleExpected = stripMysteryVariableLines(expectedDescription);
      List<String> flexibleActual = stripMysteryVariableLines(actualDescription);
      return !flexibleExpected.isEmpty() && containsAllExpectedLines(flexibleActual, flexibleExpected);
   }

   private static boolean containsAllExpectedLines(List<String> actualLines, List<String> expectedLines) {
      if (expectedLines == null || expectedLines.isEmpty()) {
         return true;
      }
      if (actualLines == null || actualLines.isEmpty()) {
         return false;
      }

      for (String expectedLine : expectedLines) {
         if (defaultString(expectedLine).isBlank()) {
            continue;
         }

         boolean matched = false;
         for (String actualLine : actualLines) {
            if (matchesLooseText(expectedLine, actualLine)) {
               matched = true;
               break;
            }
         }

         if (!matched) {
            return false;
         }
      }

      return true;
   }

   private static boolean containsAllExpectedLinesExact(List<String> actualLines, List<String> expectedLines) {
      if (expectedLines == null || expectedLines.isEmpty()) {
         return true;
      }
      if (actualLines == null || actualLines.isEmpty()) {
         return false;
      }

      Set<String> actual = new HashSet<>();
      for (String line : actualLines) {
         String normalized = normalizeTooltipLine(line);
         if (!normalized.isBlank()) {
            actual.add(normalized);
         }
      }

      for (String expectedLine : expectedLines) {
         String normalizedExpected = normalizeTooltipLine(expectedLine);
         if (normalizedExpected.isBlank()) {
            continue;
         }
         if (!actual.contains(normalizedExpected)) {
            return false;
         }
      }

      return true;
   }

   private static boolean matchesLooseText(String expectedText, String actualText) {
      String expected = getSortName(expectedText, "");
      String actual = getSortName(actualText, "");
      if (expected.isEmpty()) {
         return true;
      }
      if (actual.isEmpty()) {
         return false;
      }
      if (expected.equals(actual)) {
         return true;
      }
      if (Math.min(expected.length(), actual.length()) >= 4
         && (actual.contains(expected) || expected.contains(actual))) {
         return true;
      }

      return actualContainsExpectedTokens(expected, actual);
   }

   private static boolean actualContainsExpectedTokens(String expectedText, String actualText) {
      List<String> expectedTokens = tokenizeComparableText(expectedText);
      if (expectedTokens.isEmpty()) {
         return true;
      }

      List<String> actualTokens = tokenizeComparableText(actualText);
      if (actualTokens.isEmpty()) {
         return false;
      }

      for (String expectedToken : expectedTokens) {
         boolean matched = false;
         for (String actualToken : actualTokens) {
            if (matchesComparableToken(expectedToken, actualToken)) {
               matched = true;
               break;
            }
         }

         if (!matched) {
            return false;
         }
      }

      return true;
   }

   private static List<String> tokenizeComparableText(String text) {
      String normalized = normalizeDisplayName(defaultString(text))
         .toLowerCase(Locale.ROOT)
         .replace('\u0451', '\u0435')
         .replaceAll("[^\\p{L}\\p{Nd}%]+", " ")
         .replaceAll("\\s{2,}", " ")
         .trim();
      if (normalized.isEmpty()) {
         return List.of();
      }

      List<String> tokens = new ArrayList<>();
      for (String token : normalized.split(" ")) {
         if (!token.isBlank()) {
            tokens.add(token);
         }
      }
      return tokens;
   }

   private static boolean matchesComparableToken(String expectedToken, String actualToken) {
      if (expectedToken.equals(actualToken)) {
         return true;
      }

      return Math.min(expectedToken.length(), actualToken.length()) >= 3
         && (actualToken.contains(expectedToken) || expectedToken.contains(actualToken));
   }

   private static int extractTooltipDurabilityPercent(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return Integer.MAX_VALUE;
      }

      Minecraft client = Minecraft.getInstance();
      if (client == null || client.player == null) {
         return Integer.MAX_VALUE;
      }

      Item.TooltipContext tooltipContext = client.level != null ? Item.TooltipContext.of(client.level) : Item.TooltipContext.EMPTY;
      for (Component line : stack.getTooltipLines(tooltipContext, client.player, TooltipFlag.ADVANCED)) {
         String normalized = normalizeTooltipLine(line.getString());
         if (!normalized.contains("\u043f\u0440\u043e\u0447\u043d\u043e\u0441\u0442") && !normalized.contains("durability")) {
            continue;
         }

         Matcher matcher = DURABILITY_TOOLTIP_PATTERN.matcher(normalized);
         if (!matcher.find()) {
            continue;
         }

         try {
            int current = Integer.parseInt(matcher.group(1));
            int max = Integer.parseInt(matcher.group(2));
            if (max <= 0) {
               continue;
            }
            return Math.round(current * 100f / max);
         } catch (NumberFormatException ignored) {
         }
      }

      return Integer.MAX_VALUE;
   }

   private static List<String> filterStaticTooltipLines(List<String> rawLines) {
      List<String> filtered = new ArrayList<>();
      if (rawLines == null) {
         return filtered;
      }

      for (String line : rawLines) {
         String normalized = normalizeTooltipLine(line);
         if (normalized.isEmpty()) {
            continue;
         }
         if (isDynamicTooltipLine(normalized)) {
            continue;
         }
         filtered.add(normalized);
      }
      return filtered;
   }

   private static boolean isDynamicTooltipLine(String normalized) {
      return normalized.contains("продавец")
         || normalized.contains("seller")
         || normalized.contains("истекает")
         || normalized.contains("expires")
         || normalized.contains("цена")
         || normalized.contains("price")
         || normalized.contains("нажмите для покупки")
         || normalized.contains("нажмите чтобы купить")
         || normalized.contains("click")
         || normalized.contains("durability")
         || normalized.contains("прочность")
         || normalized.contains("component")
         || normalized.contains("minecraft:")
         || normalized.contains(" / ");
   }

   private static List<String> extractDescriptionTooltipLines(List<String> normalizedStaticLines) {
      List<String> description = new ArrayList<>();
      if (normalizedStaticLines == null || normalizedStaticLines.size() <= 1) {
         return description;
      }

      for (int i = 1; i < normalizedStaticLines.size(); i++) {
         String line = defaultString(normalizedStaticLines.get(i));
         if (line.isEmpty()) {
            continue;
         }
         if (isAttributeTooltipBoundary(line)) {
            break;
         }
         description.add(line);
      }

      return description;
   }

   private static List<String> stripMysteryVariableLines(List<String> lines) {
      List<String> filtered = new ArrayList<>();
      if (lines == null) {
         return filtered;
      }

      for (String line : lines) {
         String normalized = defaultString(line).trim();
         if (normalized.isEmpty() || isVariableMysteryLine(normalized)) {
            continue;
         }
         filtered.add(normalized);
      }

      return filtered;
   }

   private static boolean isVariableMysteryLine(String normalizedLine) {
      return normalizedLine.contains("%");
   }

   private static boolean isAttributeTooltipBoundary(String normalizedLine) {
      return normalizedLine.contains("когда в главной руке")
         || normalizedLine.contains("когда надето")
         || normalizedLine.contains("когда в руке")
         || normalizedLine.contains("when on")
         || normalizedLine.contains("when equipped")
         || normalizedLine.startsWith("+");
   }

   private static String normalizeTooltipLine(String line) {
      return normalizeDisplayName(defaultString(line))
         .toLowerCase(Locale.ROOT)
         .replace('\u0451', '\u0435')
         .replace("\u00A4", "")
         .replaceAll("\\s{2,}", " ")
         .trim();
   }

   private static String formatPotionEffectSignature(
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
      return defaultString(type) + "|" + defaultString(id) + "|" + defaultString(translationKey) + "|" + duration + "|" + amplifier
         + "|" + ambient + "|" + showParticles + "|" + showIcon + "|" + defaultString(value) + "|" + defaultString(color);
   }

   private static String getCustomDataString(ItemStack stack) {
      if (stack == null || stack.isEmpty()) return "";
      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      return customData != null ? customData.copyTag().toString() : "";
   }

   private static String normalizeComparableCustomData(String customData) {
      String normalized = defaultString(customData);
      if (normalized.isBlank()) {
         return "";
      }

      normalized = replaceNamedSection(normalized, "display:", "display:*");
      normalized = normalized.replaceAll("Damage:-?\\d+[a-zA-Z]*", "Damage:*");
      normalized = normalized.replaceAll("UUID:\\[I;[-0-9,\\s]+\\]", "UUID:[I;UUID]");
      normalized = normalized.replaceAll("Id:\\[I;[-0-9,\\s]+\\]", "Id:[I;UUID]");
      normalized = normalized.replaceAll("SkullOwnerOrig:\\[I;[-0-9,\\s]+\\]", "SkullOwnerOrig:[I;UUID]");
      normalized = normalized.replaceAll("\"litebackpacks:backpack-uuid\":\"[^\"]+\"", "\"litebackpacks:backpack-uuid\":\"*\"");
      normalized = normalized.replaceAll("litebackpacks:backpack-uuid:\"[^\"]+\"", "litebackpacks:backpack-uuid:\"*\"");
      normalized = normalized.replaceAll("SpecialEgg:'\\[[^']*\\]'", "SpecialEgg:'[*]'");
      normalized = normalized.replaceAll("SpecialEgg:\\[[^\\]]*\\]", "SpecialEgg:[*]");
      normalized = normalized.replaceAll("SpecialEgg_Pattern:\"[^\"]+\"", "SpecialEgg_Pattern:\"*\"");
      normalized = normalized.replaceAll("SpecialEgg_Pattern:'[^']+'", "SpecialEgg_Pattern:'*'");
      normalized = normalized.replaceAll("\\s{2,}", " ").trim();
      return normalized;
   }

   private static boolean isFlexibleMysteryItem(AutoBuyParsedItemData item) {
      return isMysterySpawnEgg(item) || isMysterySpawner(item);
   }

   private static boolean isMysterySpawnEgg(AutoBuyParsedItemData item) {
      String aliasKey = normalizeAuctionAliasKey(getSortName(item.displayName(), item.baseName()));
      return aliasKey.contains("\u0437\u0430\u0433\u0430\u0434\u043e\u0447")
         && aliasKey.contains("\u044f\u0439\u0446")
         && aliasKey.contains("\u043f\u0440\u0438\u0437\u044b\u0432");
   }

   private static boolean isMysterySpawner(AutoBuyParsedItemData item) {
      String aliasKey = normalizeAuctionAliasKey(getSortName(item.displayName(), item.baseName()));
      return aliasKey.contains("\u0437\u0430\u0433\u0430\u0434\u043e\u0447")
         && aliasKey.contains("\u0441\u043f\u0430\u0432\u043d\u0435\u0440");
   }

   private static String formatComparableAttribute(String attribute, String slot, double value, String operation) {
      return defaultString(attribute) + "|" + defaultString(slot) + "|" + Double.toString(value) + "|" + defaultString(operation);
   }

   private static String replaceNamedSection(String source, String keyPrefix, String replacement) {
      if (source == null || source.isEmpty() || keyPrefix == null || keyPrefix.isEmpty()) {
         return defaultString(source);
      }

      String result = source;
      int start = result.indexOf(keyPrefix);
      while (start >= 0) {
         int valueStart = start + keyPrefix.length();
         if (valueStart >= result.length()) {
            break;
         }

         char opening = result.charAt(valueStart);
         char closing = switch (opening) {
            case '{' -> '}';
            case '[' -> ']';
            case '(' -> ')';
            default -> 0;
         };
         if (closing == 0) {
            start = result.indexOf(keyPrefix, valueStart);
            continue;
         }

         int end = findBalancedSectionEnd(result, valueStart, opening, closing);
         if (end < 0) {
            break;
         }

         result = result.substring(0, start) + replacement + result.substring(end + 1);
         start = result.indexOf(keyPrefix, start + replacement.length());
      }

      return result;
   }

   private static int findBalancedSectionEnd(String source, int startIndex, char opening, char closing) {
      int depth = 0;
      boolean escaped = false;
      boolean quoted = false;
      for (int i = startIndex; i < source.length(); i++) {
         char current = source.charAt(i);
         if (quoted) {
            if (escaped) {
               escaped = false;
               continue;
            }
            if (current == '\\') {
               escaped = true;
               continue;
            }
            if (current == '"') {
               quoted = false;
            }
            continue;
         }

         if (current == '"') {
            quoted = true;
            continue;
         }

         if (current == opening) {
            depth++;
            continue;
         }

         if (current == closing) {
            depth--;
            if (depth == 0) {
               return i;
            }
         }
      }

      return -1;
   }

   private static String buildStateKey(JsonObject item) {
      if (item == null) {
         return "";
      }

      StringBuilder fingerprint = new StringBuilder(512);
      fingerprint.append(defaultString(getJsonString(item, "itemId"))).append('\n');
      fingerprint.append(normalizeDisplayName(defaultString(getJsonString(item, "displayName")))).append('\n');
      fingerprint.append(normalizeDisplayName(defaultString(getJsonString(item, "baseName")))).append('\n');
      fingerprint.append(defaultString(getJsonString(item, "matchingProfile"))).append('\n');
      fingerprint.append(readStringArray(item.getAsJsonArray("matchingMarkers"))).append('\n');
      fingerprint.append(defaultString(getJsonString(item, "autoBuyKey"))).append('\n');
      fingerprint.append(defaultString(getJsonString(item, "customData"))).append('\n');
      fingerprint.append(defaultString(getJsonString(item, "rawComponentNbt"))).append('\n');
      fingerprint.append(defaultString(getJsonString(item, "cleanedComponentNbt"))).append('\n');
      fingerprint.append(defaultString(getJsonString(item, "fullStackNbt"))).append('\n');
      fingerprint.append(readStringArray(item.getAsJsonArray("tooltip"))).append('\n');
      fingerprint.append(readEnchantments(item.getAsJsonArray("enchantments"))).append('\n');
      fingerprint.append(readAttributes(item.getAsJsonArray("attributes"))).append('\n');
      fingerprint.append(readPotionEffects(item.getAsJsonArray("potionEffects"))).append('\n');
      fingerprint.append(getJsonBoolean(item, "damageable", false)).append('\n');
      fingerprint.append(getJsonIntRaw(item, "maxDamage", 0));
      return sha256Hex(fingerprint.toString());
   }

   private static String sha256Hex(String value) {
      try {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         byte[] bytes = digest.digest(defaultString(value).getBytes(StandardCharsets.UTF_8));
         StringBuilder builder = new StringBuilder(bytes.length * 2);
         for (byte current : bytes) {
            builder.append(Character.forDigit((current >> 4) & 0xF, 16));
            builder.append(Character.forDigit(current & 0xF, 16));
         }
         return builder.toString();
      } catch (Exception ignored) {
         return Integer.toHexString(defaultString(value).hashCode());
      }
   }

   private static String getJsonString(JsonObject object, String key) {
      if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
         return null;
      }
      return object.get(key).getAsString();
   }

   private static boolean getJsonBoolean(JsonObject object, String key, boolean fallback) {
      if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
         return fallback;
      }
      try {
         return object.get(key).getAsBoolean();
      } catch (Exception ignored) {
         return fallback;
      }
   }

   private static int getJsonIntRaw(JsonObject object, String key, int fallback) {
      if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
         return fallback;
      }
      try {
         return object.get(key).getAsInt();
      } catch (Exception ignored) {
         return fallback;
      }
   }

   private static double getJsonDouble(JsonObject object, String key, double fallback) {
      if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
         return fallback;
      }
      try {
         return object.get(key).getAsDouble();
      } catch (Exception ignored) {
         return fallback;
      }
   }

   private static String defaultString(String value) {
      return value == null ? "" : value;
   }

   private AutoBuyParsedItems() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
