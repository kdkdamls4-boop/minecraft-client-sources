package net.skycore.client.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.skycore.client.mixin.MinecraftGetRightClickDelayAccessor;

public final class AccountManager {
   public static final AccountManager INSTANCE = new AccountManager();
   private String selected;
   private final Map<String, String> alts;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private boolean initialized;
   private Path file;

   public synchronized boolean select(String text) {
      String text2 = sanitize(text);
      if (isValidName(text2)) {
         if (this.alts.containsKey(normalize(text2))) {
            String text3 = this.alts.get(normalize(text2));
            this.selected = text3;
            this.save();
            applySession(this.selected);
            return true;
         }
      }

      return false;
   }

   private void save() {
      if (this.file != null) {
         try {
            Path parent = this.file.getParent();
            if (parent != null) {
               Files.createDirectories(parent);
            }

            JsonArray jsonValues = new JsonArray();

            for (String text : this.alts.values()) {
               jsonValues.add(text);
            }

            JsonObject json = new JsonObject();
            json.add("alts", jsonValues);
            if (this.selected != null) {
               json.addProperty("selected", this.selected);
            }

            Files.writeString(this.file, this.gson.toJson(json));
         } catch (Exception failure) {
         }
      }
   }

   public static void applySession(String text) {
      String text2 = sanitize(text);
      if (isValidName(text2)) {
         Minecraft instance = Minecraft.getInstance();
         if (instance != null) {
            UUID uUID = offlineUuid(text2);
            Optional<String> optional = Optional.empty();
            Optional<String> optional2 = Optional.empty();
            User user = new User(text2, uUID, "", optional, optional2);
            MinecraftGetRightClickDelayAccessor minecraftGetRightClickDelayAccessor = (MinecraftGetRightClickDelayAccessor)instance;
            minecraftGetRightClickDelayAccessor.skycore$setUser(user);
         }
      }
   }

   public synchronized String selected() {
      return this.selected;
   }

   private AccountManager() {
      this.alts = new LinkedHashMap<>();
   }

   public static UUID offlineUuid(String text) {
      String text2 = "OfflinePlayer:" + text;
      return UUID.nameUUIDFromBytes(text2.getBytes(StandardCharsets.UTF_8));
   }

   public synchronized boolean add(String text) {
      String text2 = sanitize(text);
      if (!isValidName(text2)) {
         return false;
      }

      String text3 = normalize(text2);
      if (this.alts.containsKey(text3)) {
         return false;
      }

      this.alts.put(text3, text2);
      this.save();
      SkinService.prefetch(text2);
      return true;
   }

   private static String normalize(String text) {
      String text2 = text.trim();
      return text2.toLowerCase(Locale.ROOT);
   }

   private void load() {
      if (this.file != null && Files.isRegularFile(this.file)) {
         try {
            JsonObject asJsonObject = JsonParser.parseString(Files.readString(this.file)).getAsJsonObject();
            JsonElement jsonElement = asJsonObject.get("alts");
            if (jsonElement != null && jsonElement.isJsonArray()) {
               for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
                  if (jsonElement2.isJsonPrimitive() && jsonElement2.getAsJsonPrimitive().isString()) {
                     String asString = sanitize(jsonElement2.getAsString());
                     if (isValidName(asString)) {
                        this.alts.putIfAbsent(normalize(asString), asString);
                     }
                  }
               }
            }

            if (asJsonObject.has("selected") && asJsonObject.get("selected").isJsonPrimitive()) {
               String asString2 = sanitize(asJsonObject.get("selected").getAsString());
               if (isValidName(asString2) && this.alts.containsKey(normalize(asString2))) {
                  this.selected = this.alts.get(normalize(asString2));
               }
            }

            for (String text : new ArrayList<>(this.alts.values())) {
               SkinService.prefetch(text);
            }
         } catch (Exception failure) {
         }
      }
   }

   public synchronized void initialize(Path file2) {
      if (!this.initialized) {
         this.file = file2;
         this.load();
         this.initialized = true;
         if (this.selected != null) {
            applySession(this.selected);
         } else {
            this.syncSelectedWithSession();
         }
      }
   }

   public synchronized boolean remove(String text) {
      if (text == null) {
         return false;
      }

      String text2 = normalize(text);
      String text3 = this.alts.remove(text2);
      if (text3 == null) {
         return false;
      }

      if (this.selected != null && normalize(this.selected).equals(text2)) {
         this.selected = this.alts.isEmpty() ? null : this.alts.values().iterator().next();
         if (this.selected != null) {
            applySession(this.selected);
         }
      }

      this.save();
      return true;
   }

   public static boolean isValidName(String text) {
      return text != null && text.matches("[A-Za-z0-9_]{1,16}");
   }

   private static String sanitize(String text) {
      if (text == null) {
         return null;
      }

      String text2 = text.trim();
      return text2.isEmpty() ? null : text2;
   }

   public synchronized void ensureSession() {
      if (this.selected != null) {
         Minecraft instance = Minecraft.getInstance();
         if (instance != null) {
            if (instance.getUser() != null) {
               String selected2 = this.selected;
               User user = instance.getUser();
               String name = user.getName();
               if (!selected2.equalsIgnoreCase(name)) {
                  applySession(this.selected);
               }

               return;
            }
         }

         applySession(this.selected);
      }
   }

   public synchronized List<String> alts() {
      var values = this.alts.values();
      return List.copyOf(values);
   }

   private void syncSelectedWithSession() {
      Minecraft instance = Minecraft.getInstance();
      if (instance != null && instance.getUser() != null) {
         String user = sanitize(instance.getUser().getName());
         if (isValidName(user)) {
            String text = normalize(user);
            if (this.alts.containsKey(text)) {
               this.selected = this.alts.get(text);
            } else if (this.selected == null && !this.alts.isEmpty()) {
               this.selected = this.alts.values().iterator().next();
            }
         }
      }
   }
}
