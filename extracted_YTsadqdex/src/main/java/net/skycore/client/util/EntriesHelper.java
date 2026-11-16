package net.skycore.client.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.module.render.CosmeticsModule;

public final class EntriesHelper {
   public static final String BACKPACKS = "backpacks";
   private volatile List<EntriesHelper.Entry> entries;
   public static final String WINGS = "wings";
   public static final String SWORDS = "swords";
   private volatile List<EntriesHelper.SwordEntry> swords;
   public static final String HEAD = "head";
   public static final String ACCESSORIES = "accessories";
   private final ModuleManager modules;
   public static final String PETS = "pets";

   public List<EntriesHelper.Entry> entries(String text, String text2) {
      this.ensureLoaded();
      String text3;
      if (text2 == null) {
         text3 = "";
      } else {
         String text4 = text2.trim();
         text3 = text4.toLowerCase(Locale.ROOT);
      }

      String text5 = text3;
      var stream = this.entries.stream();
      Predicate<net.skycore.client.util.EntriesHelper.Entry> predicate = entry -> {
         String text6 = entry.category();
         return matchesNav(text6, text);
      };
      var stream2 = stream.filter(predicate);
      Predicate<net.skycore.client.util.EntriesHelper.Entry> predicate2 = entry2 -> {
         if (!text5.isEmpty()) {
            String text7 = entry2.name();
            if (!text7.toLowerCase(Locale.ROOT).contains(text5)) {
               String text8 = entry2.id();
               String text9 = text8.toLowerCase(Locale.ROOT);
               if (!text9.contains(text5)) {
                  return false;
               }
            }
         }

         return true;
      };
      return stream2.filter(predicate2).toList();
   }

   private static String clean(String text, String text2) {
      if (text != null && !text.isBlank() && text.indexOf(65533) < 0) {
         return text;
      }

      return humanize(text2);
   }

   private static List<EntriesHelper.Entry> loadCosmetics() {
      ArrayList elements = new ArrayList();
      Minecraft instance = Minecraft.getInstance();
      Identifier texture = Identifier.parse("skycore:cosmetics_port/catalog.json");

      try {
         Resource resourceManager = instance.getResourceManager().getResource(texture).orElseThrow();

         try (InputStreamReader inputStreamReader = new InputStreamReader(resourceManager.open(), StandardCharsets.UTF_8)) {
            JsonObject asJsonObject = JsonParser.parseReader(inputStreamReader).getAsJsonObject();

            for (JsonElement jsonElement : asJsonObject.getAsJsonArray("items")) {
               JsonObject asJsonObject2 = jsonElement.getAsJsonObject();
               String text = str(asJsonObject2, "id", "");
               String text2 = str(asJsonObject2, "category", "");
               String text3 = str(asJsonObject2, "path", text2 + "/" + text);
               String text4 = str(asJsonObject2, "display_name_en", str(asJsonObject2, "display_name", text));
               if (!text.isBlank() && !text3.isBlank()) {
                  Identifier texture2 = Identifier.parse("skycore:cosmetics_port/" + text3 + "/texture.png");
                  Identifier texture3 = Identifier.parse("skycore:cosmetics_port/" + text3 + "/geometry.json");
                  Identifier texture4 = Identifier.parse("skycore:cosmetics_port/" + text3 + "/meta.json");
                  elements.add(new EntriesHelper.Entry(text, text2.toLowerCase(Locale.ROOT), text3, clean(text4, text), texture2, texture3, texture4));
               }
            }
         }
      } catch (Exception failure) {
         System.err.println("[Skycore/Cosmetics] catalog load failed: " + failure.getMessage());
      }

      elements.sort(Comparator.<EntriesHelper.Entry>comparingInt(entry -> {
         return categoryOrder(entry.category());
      }).thenComparing(EntriesHelper.Entry::name, String.CASE_INSENSITIVE_ORDER));
      return List.copyOf(elements);
   }

   private static String humanize(String text) {
      String text2 = text.replace('_', ' ');
      String text3 = text2.replace('-', ' ');
      String text4 = text3.trim();
      return text4.isEmpty() ? text : Character.toUpperCase(text4.charAt(0)) + text4.substring(1);
   }

   public boolean hasSwordCosmetics() {
      this.ensureLoaded();
      return !this.swords.isEmpty();
   }

   private static int categoryOrder(String text) {
      return switch (text) {
         case "hats" -> 0;
         case "wings" -> 1;
         case "backpacks" -> 2;
         case "masks" -> 3;
         case "pets" -> 4;
         default -> 5;
      };
   }

   public boolean isSwordSelected(EntriesHelper.SwordEntry swordEntry) {
      CosmeticsModule cosmeticsModule = this.module();
      if (cosmeticsModule != null && swordEntry != null) {
         String text = swordEntry.name();
         if (text.equalsIgnoreCase(cosmeticsModule.selectedSword())) {
            return true;
         }
      }

      return false;
   }

   public EntriesHelper(ModuleManager modules2) {
      this.modules = modules2;
   }

   private static List<EntriesHelper.SwordEntry> loadSwords() {
      ArrayList elements = new ArrayList();
      Minecraft instance = Minecraft.getInstance();
      Identifier texture = Identifier.parse("skycore:item_replacer/sword/catalog.json");

      try {
         Resource resourceManager = instance.getResourceManager().getResource(texture).orElseThrow();

         try (InputStreamReader inputStreamReader = new InputStreamReader(resourceManager.open(), StandardCharsets.UTF_8)) {
            JsonObject asJsonObject = JsonParser.parseReader(inputStreamReader).getAsJsonObject();

            for (JsonElement jsonElement : asJsonObject.getAsJsonArray("items")) {
               JsonObject asJsonObject2 = jsonElement.getAsJsonObject();
               String text = str(asJsonObject2, "name", "");
               String text2 = str(asJsonObject2, "model", "");
               String text3 = str(asJsonObject2, "texture", "");
               if (text3.startsWith("item/")) {
                  text3 = text3.substring(5);
               }

               if (!text.isBlank() && !text3.isBlank()) {
                  elements.add(new EntriesHelper.SwordEntry(text, text2, Identifier.parse("skycore:textures/item_replacer/sword/" + text3 + ".png")));
               }
            }
         }
      } catch (Exception failure) {
         System.err.println("[Skycore/Cosmetics] sword catalog load failed: " + failure.getMessage());
      }

      return List.copyOf(elements);
   }

   public boolean enabled() {
      CosmeticsModule cosmeticsModule = this.module();
      if (cosmeticsModule != null) {
         if (cosmeticsModule.isEnabled()) {
            return true;
         }
      }

      return false;
   }

   public EntriesHelper.Entry selectedForNav(String text) {
      this.ensureLoaded();
      CosmeticsModule cosmeticsModule = this.module();
      if (cosmeticsModule == null) {
         return null;
      }

      Iterator iterator = this.entries.iterator();

      while (true) {
         if (!iterator.hasNext()) {
            return null;
         }

         EntriesHelper.Entry element = (EntriesHelper.Entry)iterator.next();
         if (matchesNav(element.category(), text)) {
            String text2 = element.id();
            if (text2.equalsIgnoreCase(cosmeticsModule.selected(element.category()))) {
               return element;
            }
         }
      }
   }

   public List<EntriesHelper.SwordEntry> swords(String text) {
      this.ensureLoaded();
      String text2;
      if (text == null) {
         text2 = "";
      } else {
         String text3 = text.trim();
         text2 = text3.toLowerCase(Locale.ROOT);
      }

      String text4 = text2;
      var stream = this.swords.stream();
      Predicate<net.skycore.client.util.EntriesHelper.SwordEntry> predicate = swordEntry -> {
         if (!text4.isEmpty()) {
            String text5 = swordEntry.name();
            if (!text5.toLowerCase(Locale.ROOT).contains(text4)) {
               return false;
            }
         }

         return true;
      };
      return stream.filter(predicate).toList();
   }

   public void select(EntriesHelper.Entry entry) {
      CosmeticsModule cosmeticsModule = this.module();
      if (cosmeticsModule != null && entry != null) {
         if (this.isSelected(entry)) {
            cosmeticsModule.select(entry.category(), "");
         } else {
            String text = entry.category();
            cosmeticsModule.select(text, entry.id());
            if (!cosmeticsModule.isEnabled()) {
               cosmeticsModule.setEnabled(true);
            }
         }
      }
   }

   public boolean isSelected(EntriesHelper.Entry entry) {
      CosmeticsModule cosmeticsModule = this.module();
      if (cosmeticsModule != null && entry != null) {
         String text = entry.id();
         String text2 = cosmeticsModule.selected(entry.category());
         if (text.equalsIgnoreCase(text2)) {
            return true;
         }
      }

      return false;
   }

   private CosmeticsModule module() {
      return this.modules.get(CosmeticsModule.class).orElse(null);
   }

   private static String str(JsonObject json, String text, String text2) {
      if (json.has(text)) {
         JsonElement jsonElement = json.get(text);
         if (!jsonElement.isJsonNull()) {
            JsonElement jsonElement2 = json.get(text);
            return jsonElement2.getAsString();
         }
      }

      return text2;
   }

   public List<EntriesHelper.Entry> accessories(String text) {
      this.ensureLoaded();
      String text2;
      if (text == null) {
         text2 = "";
      } else {
         text2 = text.trim().toLowerCase(Locale.ROOT);
      }

      String text3 = text2;
      var stream = this.entries.stream();
      return stream.filter(entry -> {
         if (!text3.isEmpty()) {
            String text4 = entry.name();
            if (!text4.toLowerCase(Locale.ROOT).contains(text3)) {
               text4 = entry.id();
               if (!text4.toLowerCase(Locale.ROOT).contains(text3)) {
                  return false;
               }
            }
         }

         return true;
      }).toList();
   }

   public void selectSword(EntriesHelper.SwordEntry swordEntry) {
      CosmeticsModule cosmeticsModule = this.module();
      if (cosmeticsModule != null && swordEntry != null) {
         String text = this.isSwordSelected(swordEntry) ? "" : swordEntry.name();
         cosmeticsModule.selectSword(text, this.isSwordSelected(swordEntry) ? "" : swordEntry.model());
         if (!cosmeticsModule.isEnabled()) {
            cosmeticsModule.setEnabled(true);
         }
      }
   }

   private synchronized void ensureLoaded() {
      if (this.entries == null || this.swords == null) {
         this.entries = loadCosmetics();
         var swordEntrys = loadSwords();
         this.swords = swordEntrys;
      }
   }

   private static boolean matchesNav(String text, String text2) {
      if ("head".equals(text2)) {
         if (!"hats".equals(text)) {
            if (!"masks".equals(text)) {
               return false;
            }
         }

         return true;
      } else {
         if (text2 != null) {
            if (text2.equals(text)) {
               return true;
            }
         }

         return false;
      }
   }

   public record Entry(String id, String category, String path, String name, Identifier texture, Identifier geometry, Identifier meta) {
      public String path() {
         return this.path;
      }

      public String name() {
         return this.name;
      }

      public String id() {
         return this.id;
      }

      public Identifier meta() {
         return this.meta;
      }

      public Identifier geometry() {
         return this.geometry;
      }

      public Identifier texture() {
         return this.texture;
      }

      public String category() {
         return this.category;
      }
   }

   public record SwordEntry(String name, String model, Identifier texture) {
      public String name() {
         return this.name;
      }

      public String model() {
         return this.model;
      }

      public Identifier texture() {
         return this.texture;
      }
   }
}
