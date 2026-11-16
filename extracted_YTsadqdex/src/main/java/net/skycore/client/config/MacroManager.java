package net.skycore.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class MacroManager {
   private Path file;
   private boolean initialized;
   private final List<MacroManager.Macro> macros;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

   public synchronized List<MacroManager.Macro> getMacros() {
      return List.copyOf(this.macros);
   }

   private void load() {
      if (this.file != null && Files.isRegularFile(this.file)) {
         try {
            JsonObject asJsonObject = JsonParser.parseString(Files.readString(this.file)).getAsJsonObject();
            JsonElement jsonElement = asJsonObject.get("macros");
            if (jsonElement == null || !jsonElement.isJsonArray()) {
               return;
            }

            this.macros.clear();

            for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
               if (jsonElement2.isJsonObject()) {
                  JsonObject asJsonObject2 = jsonElement2.getAsJsonObject();
                  if (asJsonObject2.has("name") && asJsonObject2.has("key") && asJsonObject2.has("cmd")) {
                     try {
                        this.macros.add(new MacroManager.Macro(asJsonObject2.get("name").getAsString(), asJsonObject2.get("key").getAsInt(), asJsonObject2.get("cmd").getAsString()));
                     } catch (Exception failure) {
                     }
                  }
               }
            }
         } catch (Exception failure2) {
         }
      }
   }

   public synchronized void addMacro(String text, int value, String text2) {
      if (text != null && text2 != null) {
         String text3 = text.trim();
         var macros2 = this.macros;
         Predicate<net.skycore.client.config.MacroManager.Macro> predicate = macro -> {
            String text4 = macro.name();
            return text4.equalsIgnoreCase(text3);
         };
         macros2.removeIf(predicate);
         this.macros.add(new MacroManager.Macro(text3, value, text2));
         this.save();
      }
   }

   public synchronized void initialize(Path file2) {
      if (!this.initialized) {
         this.file = file2;
         this.load();
         this.initialized = true;
      }
   }

   public MacroManager() {
      this.macros = new ArrayList<>();
   }

   public synchronized void clear() {
      if (!this.macros.isEmpty()) {
         this.macros.clear();
         this.save();
      }
   }

   private void save() {
      if (this.file != null) {
         try {
            Path parent = this.file.getParent();
            if (parent != null) {
               Files.createDirectories(parent);
            }

            JsonArray jsonValues = new JsonArray();

            for (MacroManager.Macro macro : this.macros) {
               JsonObject json = new JsonObject();
               json.addProperty("name", macro.name());
               json.addProperty("key", macro.keyCode());
               json.addProperty("cmd", macro.text());
               jsonValues.add(json);
            }

            JsonObject json2 = new JsonObject();
            json2.add("macros", jsonValues);
            Files.writeString(this.file, this.gson.toJson(json2));
         } catch (Exception failure) {
         }
      }
   }

   public synchronized boolean removeMacro(String text) {
      if (text == null) {
         return false;
      }

      boolean condition = this.macros.removeIf(macro -> {
         String text2 = macro.name();
         String text3 = text.trim();
         return text2.equalsIgnoreCase(text3);
      });
      if (condition) {
         this.save();
      }

      return condition;
   }

   public record Macro(String name, int keyCode, String text) {
      public int keyCode() {
         return this.keyCode;
      }

      public String text() {
         return this.text;
      }

      public String name() {
         return this.name;
      }
   }
}
