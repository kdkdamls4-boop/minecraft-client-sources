package deluxe.client.storage;

import deluxe.client.event.EventListener;
import deluxe.client.util.InputBinding;
import deluxe.client.MinecraftAccess;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.storage.ConfigStore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;

public final class MacroStore implements MinecraftAccess {
   private static MacroStore macroStore;
   private final LinkedHashMap<String, MacroStore.Nested0522> field14414 = new LinkedHashMap<>();
   private final HashSet<String> field14415 = new HashSet<>();
   private Path field14416;
   private final EventListener<PlayerTickEvent> field14417 = playerTickEvent -> {
      if (mc.player != null && !this.field14414.isEmpty()) {
         for (MacroStore.Nested0522 local7038_3 : this.field14414.values()) {
            if (!local7038_3.key().method2749(mc.getWindow().getHandle())) {
               this.field14415.remove(local7038_3.name());
            } else if (this.field14415.add(local7038_3.name())) {
               if (local7038_3.chatLine().startsWith("/")) {
                  mc.player.networkHandler.sendChatCommand(local7038_3.chatLine().substring(1));
               } else {
                  mc.player.networkHandler.sendChatMessage(local7038_3.chatLine());
               }
            }
         }
      }
   };

   public InputBinding method4208(String local7035_1) {
      if (local7035_1.length() == 2 && (local7035_1.charAt(0) == 'M' || local7035_1.charAt(0) == 'm') && Character.isDigit(local7035_1.charAt(1))) {
         return InputBinding.mouseButton(Character.getNumericValue(local7035_1.charAt(1)) - 1);
      }

      for (int code = 32; code <= 348; code++) {
         if (InputBinding.keyboardKey(code).getDisplayName().equalsIgnoreCase(local7035_1)) {
            return InputBinding.keyboardKey(code);
         }
      }

      return InputBinding.UNBOUND;
   }

   private void method4209() {
      try {
         Files.createDirectories(this.field14416.getParent());
         JsonArray jsonArray = new JsonArray();

         for (MacroStore.Nested0522 local7036_3 : this.field14414.values()) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", local7036_3.name());
            jsonObject.addProperty("key", local7036_3.key().getDevice().name() + ":" + local7036_3.key().getCode());
            jsonObject.addProperty("text", local7036_3.chatLine());
            jsonArray.add(jsonObject);
         }

         Files.writeString(this.field14416, jsonArray.toString(), StandardCharsets.UTF_8);
      } catch (Exception exception) {
      }
   }

   public void method4210(String local7037_1, InputBinding inputBinding, String local7037_3) {
      this.field14414.put(local7037_1.toLowerCase(Locale.ROOT), new MacroStore.Nested0522(local7037_1, inputBinding, local7037_3));
      this.method4209();
   }

   private MacroStore() {
      this.field14416 = FabricLoader.getInstance().getConfigDir().resolve("deluxe").resolve("macros.json");

      try {
         if (!Files.exists(this.field14416)) {
            return;
         }

         for (JsonElement jsonElement : JsonParser.parseString(Files.readString(this.field14416, StandardCharsets.UTF_8)).getAsJsonArray()) {
            this.field14414
               .put(
                  jsonElement.getAsJsonObject().get("name").getAsString().toLowerCase(Locale.ROOT),
                  new MacroStore.Nested0522(
                     jsonElement.getAsJsonObject().get("name").getAsString(),
                     ConfigStore.parseBinding(jsonElement.getAsJsonObject().get("key").getAsString()),
                     jsonElement.getAsJsonObject().get("text").getAsString()
                  )
               );
         }
      } catch (Exception exception) {
      }
   }

   public boolean method4212(String local7040_1) {
      if (this.field14414.remove(local7040_1.toLowerCase(Locale.ROOT)) == null) {
         return false;
      }

      this.method4209();
      return true;
   }

   public static MacroStore method4213() {
      if (macroStore == null) {
         macroStore = new MacroStore();
      }

      return macroStore;
   }

   public ArrayList<MacroStore.Nested0522> method4214() {
      return new ArrayList<>(this.field14414.values());
   }

   public void method4215() {
      this.field14414.clear();
      this.field14415.clear();
      this.method4209();
   }

   public record Nested0522(String name, InputBinding key, String chatLine) {
   }
}
