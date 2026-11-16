package net.skycore.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public final class NameList {
   private final Map<String, String> entries;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private Path file;

   public synchronized void set(String text, String text2) {
      if (text != null) {
         if (!text.isBlank()) {
            var entries2 = this.entries;
            String text3 = text.trim();
            String text4 = text3.toLowerCase(Locale.ROOT);
            String text5;
            if (text2 == null) {
               text5 = text.trim();
            } else {
               text5 = text2.trim();
            }

            entries2.put(text4, text5);
            this.save();
            return;
         }
      }
   }

   public synchronized Map<String, String> getEntries() {
      return Map.copyOf(this.entries);
   }

   private void save() {
      if (this.file != null) {
         try {
            Files.createDirectories(this.file.getParent());
            JsonArray jsonValues = new JsonArray();

            for (Entry entry : this.entries.entrySet()) {
               if (((String)entry.getKey()).equalsIgnoreCase((String)entry.getValue())) {
                  jsonValues.add((String)entry.getValue());
               } else {
                  JsonObject json = new JsonObject();
                  json.addProperty("name", ((String)entry.getValue()).contains(" ") ? (String)entry.getKey() : (String)entry.getValue());
                  json.addProperty("name", (String)entry.getKey());
                  json.addProperty("display", (String)entry.getValue());
                  jsonValues.add(json);
               }
            }

            jsonValues = new JsonArray();

            for (Entry entry2 : this.entries.entrySet()) {
               JsonObject json2 = new JsonObject();
               json2.addProperty("name", (String)entry2.getKey());
               json2.addProperty("display", (String)entry2.getValue());
               jsonValues.add(json2);
            }

            Files.writeString(this.file, this.gson.toJson(jsonValues));
         } catch (Exception failure) {
         }
      }
   }

   public synchronized String getDisplay(String text) {
      if (text == null) {
         return null;
      }

      return this.entries.get(text.toLowerCase(Locale.ROOT));
   }

   public synchronized boolean add(String text) {
      String text2 = text == null ? "" : text.trim();
      if (text2.isEmpty()) {
         return false;
      }

      String text3 = text2.toLowerCase(Locale.ROOT);
      if (this.entries.containsKey(text3)) {
         return false;
      }

      this.entries.put(text3, text2);
      this.save();
      return true;
   }

   public synchronized boolean remove(String text) {
      if (text == null) {
         return false;
      }

      var entries2 = this.entries;
      String text2 = text.toLowerCase(Locale.ROOT);
      int value = entries2.remove(text2) != null ? 1 : 0;
      if (value != 0) {
         this.save();
      }

      return (value != 0);
   }

   public synchronized Collection<String> getItems() {
      var values = this.entries.values();
      return List.copyOf(values);
   }

   public synchronized void clear() {
      this.entries.clear();
      this.save();
   }

   public NameList() {
      this.entries = new LinkedHashMap<>();
   }

   public void initialize(Path file2) {
      this.file = file2;
      this.load();
   }

   private void load() {
      if (this.file != null && Files.isRegularFile(this.file)) {
         try {
            JsonArray asJsonArray = JsonParser.parseString(Files.readString(this.file)).getAsJsonArray();
            this.entries.clear();

            for (JsonElement jsonElement : asJsonArray) {
               if (jsonElement.isJsonPrimitive()) {
                  this.add(jsonElement.getAsString());
               } else if (jsonElement.isJsonObject()) {
                  String asJsonObject = jsonElement.getAsJsonObject().has("name") ? jsonElement.getAsJsonObject().get("name").getAsString() : null;
                  String asJsonObject2 = jsonElement.getAsJsonObject().has("display") ? jsonElement.getAsJsonObject().get("display").getAsString() : asJsonObject;
                  if (asJsonObject != null) {
                     this.set(asJsonObject, asJsonObject2);
                  }
               }
            }
         } catch (Exception failure) {
         }
      }
   }

   public synchronized boolean contains(String text) {
      if (text != null) {
         var entries2 = this.entries;
         String text2 = text.toLowerCase(Locale.ROOT);
         if (entries2.containsKey(text2)) {
            return true;
         }
      }

      return false;
   }
}
