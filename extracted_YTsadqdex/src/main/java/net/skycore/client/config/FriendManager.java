package net.skycore.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public final class FriendManager {
   public static final FriendManager INSTANCE = new FriendManager();
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private long revision;
   private Path file;
   private final Map<String, LocalDateTime> addedTimes;
   private boolean initialized;
   private final Map<String, String> friends = new LinkedHashMap<>();

   public synchronized void initialize(Path file2) {
      if (!this.initialized) {
         this.file = file2;
         this.load();
         this.initialized = true;
      }
   }

   private FriendManager() {
      this.addedTimes = new LinkedHashMap<>();
   }

   public static boolean isValidName(String text) {
      return text != null && text.matches("[A-Za-z0-9_]{1,16}");
   }

   public synchronized boolean isFriend(String text) {
      if (text != null) {
         var friends2 = this.friends;
         String text2 = normalize(text);
         if (friends2.containsKey(text2)) {
            return true;
         }
      }

      return false;
   }

   public synchronized long revision() {
      return this.revision;
   }

   private static String sanitize(String text) {
      if (text == null) {
         return null;
      }

      String text2 = text.trim();
      return text2.isEmpty() ? null : text2;
   }

   public synchronized List<String> getFriendDescriptions() {
      var entries = this.friends.entrySet();
      var stream = entries.stream();
      Function<java.util.Map.Entry, java.lang.String> value = entry -> {
         String value2 = (String)entry.getValue();
         String key = String.valueOf(this.addedTimes.get(entry.getKey()));
         key = String.valueOf(key);
         return value2 + " (" + key + ")";
      };
      var stream2 = stream.map(value);
      return stream2.toList();
   }

   public synchronized boolean add(String text) {
      String text2 = sanitize(text);
      if (!isValidName(text2)) {
         return false;
      }

      String text3 = normalize(text2);
      if (this.friends.containsKey(text3)) {
         return false;
      }

      this.friends.put(text3, text2);
      this.addedTimes.put(text3, LocalDateTime.now());
      this.revision++;
      this.save();
      return true;
   }

   public synchronized boolean remove(String text) {
      if (text == null) {
         return false;
      }

      int value = this.friends.remove(normalize(text)) != null ? 1 : 0;
      if (value != 0) {
         this.addedTimes.remove(normalize(text));
         long timestamp = this.revision + 1L;
         this.revision = timestamp;
         this.save();
      }

      return (value != 0);
   }

   private void load() {
      if (this.file != null && Files.isRegularFile(this.file)) {
         try {
            JsonObject asJsonObject = JsonParser.parseString(Files.readString(this.file)).getAsJsonObject();
            JsonElement jsonElement = asJsonObject.get("friends");
            if (jsonElement == null || !jsonElement.isJsonArray()) {
               return;
            }

            for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
               JsonElement asJsonObject2 = jsonElement2.isJsonObject() ? jsonElement2.getAsJsonObject().get("name") : jsonElement2;
               if (asJsonObject2 != null && asJsonObject2.isJsonPrimitive() && asJsonObject2.getAsJsonPrimitive().isString()) {
                  String asString = sanitize(asJsonObject2.getAsString());
                  if (isValidName(asString)) {
                     this.friends.putIfAbsent(normalize(asString), asString);
                     LocalDateTime localDateTime = LocalDateTime.now();
                     if (jsonElement2.isJsonObject() && jsonElement2.getAsJsonObject().has("addedTime")) {
                        try {
                           localDateTime = LocalDateTime.parse(jsonElement2.getAsJsonObject().get("addedTime").getAsString());
                        } catch (RuntimeException failure) {
                        }
                     }

                     this.addedTimes.putIfAbsent(normalize(asString), localDateTime);
                  }
               }
            }

            this.revision++;
         } catch (Exception failure2) {
         }
      }
   }

   public synchronized Collection<String> getFriends() {
      var values = this.friends.values();
      return List.copyOf(values);
   }

   private void save() {
      if (this.file != null) {
         try {
            Path parent = this.file.getParent();
            if (parent != null) {
               Files.createDirectories(parent);
            }

            JsonArray jsonValues = new JsonArray();

            for (String text : this.friends.values()) {
               JsonObject json = new JsonObject();
               json.addProperty("name", text);
               json.addProperty("addedTime", this.addedTimes.get(normalize(text)).toString());
               jsonValues.add(json);
            }

            JsonObject json2 = new JsonObject();
            json2.add("friends", jsonValues);
            Files.writeString(this.file, this.gson.toJson(json2));
         } catch (Exception failure) {
         }
      }
   }

   private static String normalize(String text) {
      String text2 = text.trim();
      return text2.toLowerCase(Locale.ROOT);
   }

   public synchronized boolean clear() {
      if (this.friends.isEmpty()) {
         return false;
      }

      this.friends.clear();
      this.addedTimes.clear();
      this.revision++;
      this.save();
      return true;
   }
}
