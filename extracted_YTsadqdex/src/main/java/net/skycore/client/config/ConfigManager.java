package net.skycore.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.skycore.client.hud.NotificationsHud;
import net.skycore.client.module.BindMode;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.module.combat.ElytraTargetModule;
import net.skycore.client.module.render.InterfaceModule;
import net.skycore.client.profile.LocalProfile;
import net.skycore.client.setting.Setting;
import net.skycore.client.theme.ThemeManager;
import net.skycore.client.util.BackgroundGuiHelper;
import net.skycore.client.setting.ColorSetting;
import net.skycore.client.setting.KeyBindSetting;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.StringSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.setting.BooleanSetting;

public final class ConfigManager {
   private String categoryDock;
   private final ModuleManager modules;
   private int mainMenuBackground;
   private String activeProfile;
   private final LinkedHashSet<String> favoriteModules;
   private final ThemeManager themes;
   private String cosmeticSubcategory;
   private final Path file;
   private final Map<String, ConfigManager.DraggablePosition> draggablePositions;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private final Map<String, ConfigManager.PanelPosition> panelPositions = new LinkedHashMap<>();

   private JsonObject profileMeta(String text) {
      Path path = this.resolveExistingProfile(text);
      if (path != null && Files.isRegularFile(path)) {
         try {
            String text2 = Files.readString(path);
            JsonElement jsonElement = JsonParser.parseString(text2);
            JsonObject asJsonObject = jsonElement.getAsJsonObject();
            if (asJsonObject.has("meta")) {
               JsonElement jsonElement2 = asJsonObject.get("meta");
               if (jsonElement2.isJsonObject()) {
                  return asJsonObject.getAsJsonObject("meta");
               }
            }

            return null;
         } catch (Exception failure) {
            return null;
         }
      } else {
         return null;
      }
   }

   private void loadMainMenu(JsonObject json) {
      if (json != null && json.has("background")) {
         try {
            JsonElement jsonElement = json.get("background");
            int asInt = jsonElement.getAsInt();
            this.mainMenuBackground = Math.max(0, asInt);
         } catch (Exception failure) {
         }
      }
   }

   public void setPanelPosition(String panelPosition, float panelPosition2, float panelPosition3) {
      if (panelPosition != null) {
         if (Float.isFinite(panelPosition2)) {
            if (Float.isFinite(panelPosition3)) {
               this.panelPositions.put(panelPosition, new ConfigManager.PanelPosition(clamp01(panelPosition2), clamp01(panelPosition3)));
            }
         }
      }
   }

   public boolean profileExists(String text) {
      Path path = this.resolveExistingProfile(text);
      return path != null && Files.isRegularFile(path);
   }

   private void loadPanels(JsonObject json) {
      if (json != null) {
         var entries = json.entrySet();

         for (Entry entry : entries) {
            try {
               JsonObject asJsonObject = ((JsonElement)entry.getValue()).getAsJsonObject();
               float asFloat = asJsonObject.get("x").getAsFloat();
               float asFloat2 = asJsonObject.get("y").getAsFloat();
               this.setPanelPosition((String)entry.getKey(), asFloat, asFloat2);
            } catch (Exception failure) {
            }
         }
      }
   }

   public ConfigManager.ProfileInfo profileInfo(String text) {
      JsonObject json = this.profileMeta(text);
      String text2 = currentUsername();
      boolean condition = false;
      if (json != null) {
         if (json.has("author")) {
            try {
               String asString = json.get("author").getAsString();
               if (asString != null && !asString.isBlank()) {
                  text2 = asString.trim();
               }
            } catch (Exception failure) {
            }
         }

         if (json.has("favorite")) {
            try {
               condition = json.get("favorite").getAsBoolean();
            } catch (Exception failure2) {
            }
         }
      }

      return new ConfigManager.ProfileInfo(text, text2, condition);
   }

   private void loadModuleMeta(ClientModule clientModule, JsonObject json) {
      if (json.has("keyBind")) {
         try {
            JsonPrimitive jsonPrimitive = requirePrimitive(json.get("keyBind"));
            if (!jsonPrimitive.isNumber()) {
               throw new IllegalArgumentException("keyBind is not numeric");
            }

            clientModule.setKeyBind(jsonPrimitive.getAsInt());
         } catch (Exception failure) {
         }
      }

      if (json.has("bindMode")) {
         try {
            JsonPrimitive jsonPrimitive2 = requirePrimitive(json.get("bindMode"));
            if (!jsonPrimitive2.isString()) {
               throw new IllegalArgumentException("bindMode is not string");
            }

            clientModule.setBindMode(BindMode.valueOf(jsonPrimitive2.getAsString()));
         } catch (Exception failure2) {
         }
      }
   }

   public ConfigManager(ModuleManager modules2, ThemeManager themes2, Path file2) {
      this.draggablePositions = new LinkedHashMap<>();
      this.favoriteModules = new LinkedHashSet<>();
      this.cosmeticSubcategory = "accessories";
      this.categoryDock = "BOTTOM";
      this.modules = modules2;
      this.themes = themes2;
      this.file = file2;
   }

   private void loadTheme(JsonObject json) {
      if (json != null) {
         try {
            if (json.has("id")) {
               JsonPrimitive jsonPrimitive = requirePrimitive(json.get("id"));
               if (jsonPrimitive.isString()) {
                  this.themes.select(jsonPrimitive.getAsString());
               }
            }

            if (json.has("accent")) {
               this.themes.setSlot(1, json.get("accent").getAsInt());
            }

            if (json.has("text")) {
               this.themes.setSlot(2, json.get("text").getAsInt());
            }

            if (json.has("secondary")) {
               this.themes.setSlot(3, json.get("secondary").getAsInt());
            }

            if (json.has("tertiary")) {
               this.themes.setSlot(4, json.get("tertiary").getAsInt());
            }

            if (json.has("backgroundHud")) {
               this.themes.setSlot(5, json.get("backgroundHud").getAsInt());
            }

            if (json.has("backgroundGui")) {
               this.themes.setSlot(6, json.get("backgroundGui").getAsInt());
            }
         } catch (Exception failure) {
         }
      }
   }

   private void loadModules(JsonObject json) {
      if (json != null) {
         for (ClientModule clientModule : this.modules.getModules()) {
            JsonElement name = json.get(clientModule.getName());
            if ("No Render".equals(clientModule.getName())) {
               name = mergedNoRenderConfig(json, name);
            }

            if (name != null && name.isJsonObject()) {
               JsonObject asJsonObject = name.getAsJsonObject();
               this.loadModuleMeta(clientModule, asJsonObject);
               JsonObject asJsonObject2 = asJsonObject.has("settings") && asJsonObject.get("settings").isJsonObject() ? asJsonObject.getAsJsonObject("settings") : null;
               if (asJsonObject2 != null) {
                  for (Setting setting : clientModule.getSettings()) {
                     JsonElement name2 = asJsonObject2.get(setting.getName());
                     if (name2 == null && "World Tweaks".equals(clientModule.getName()) && "Aurora Intensity".equals(setting.getName())) {
                        for (Entry entry : asJsonObject2.entrySet()) {
                           if (!"Intensity".equalsIgnoreCase((String)entry.getKey())
                              && "Intensity".equalsIgnoreCase(Translations.english((String)entry.getKey()))) {
                              name2 = (JsonElement)entry.getValue();
                              break;
                           }
                        }
                     }

                     if (name2 == null) {
                        for (Entry entry2 : asJsonObject2.entrySet()) {
                           if ((
                                 !"World Tweaks".equals(clientModule.getName())
                                    || !"Intensity".equals(setting.getName())
                                    || "Intensity".equalsIgnoreCase((String)entry2.getKey())
                              )
                              && setting.getName().equalsIgnoreCase(Translations.english((String)entry2.getKey()))) {
                              name2 = (JsonElement)entry2.getValue();
                              break;
                           }
                        }
                     }

                     if (name2 != null) {
                        try {
                           this.readSetting(setting, name2);
                        } catch (Exception failure) {
                           setting.reset();
                        }
                     }
                  }
               }

               try {
                  if (asJsonObject.has("enabled")) {
                     JsonPrimitive jsonPrimitive = requirePrimitive(asJsonObject.get("enabled"));
                     if (!jsonPrimitive.isBoolean()) {
                        throw new IllegalArgumentException("enabled is not boolean");
                     }

                     clientModule.setEnabled(jsonPrimitive.getAsBoolean());
                  }
               } catch (Exception failure2) {
               }
            }
         }
      }
   }

   public int mainMenuBackground() {
      return this.mainMenuBackground;
   }

   private void readSetting(Setting<?> setting, JsonElement jsonElement) {
      if (setting instanceof BooleanSetting booleanSetting) {
         JsonPrimitive jsonPrimitive = requirePrimitive(jsonElement);
         if (!jsonPrimitive.isBoolean()) {
            throw new IllegalArgumentException("boolean setting is not boolean");
         }

         booleanSetting.set(jsonPrimitive.getAsBoolean());
      } else if (setting instanceof NumberSetting numberSetting) {
         JsonPrimitive jsonPrimitive2 = requirePrimitive(jsonElement);
         if (!jsonPrimitive2.isNumber()) {
            throw new IllegalArgumentException("number setting is not numeric");
         }

         double asDouble = jsonPrimitive2.getAsDouble();
         if (Double.isFinite(asDouble) && !(asDouble < numberSetting.getMin()) && !(asDouble > numberSetting.getMax())) {
            numberSetting.set(asDouble);
         } else {
            throw new IllegalArgumentException("number setting outside range");
         }
      } else if (setting instanceof ModeSetting modeSetting) {
         JsonPrimitive jsonPrimitive3 = requirePrimitive(jsonElement);
         if (!jsonPrimitive3.isString()) {
            throw new IllegalArgumentException("mode setting is not string");
         }

         String asString = Translations.english(jsonPrimitive3.getAsString());
         if (setting == ElytraTargetModule.INSTANCE.predictionMode) {
            if (asString.equalsIgnoreCase("От ротации")) {
               asString = "По взгляду";
            } else if (asString.equalsIgnoreCase("От скорости")) {
               asString = "По движению";
            }
         }

         if (!containsIgnoreCase(modeSetting.getModes(), asString)) {
            throw new IllegalArgumentException("unknown mode");
         }

         modeSetting.set(asString);
      } else if (setting instanceof StringSetting stringSetting) {
         JsonPrimitive jsonPrimitive4 = requirePrimitive(jsonElement);
         if (!jsonPrimitive4.isString()) {
            throw new IllegalArgumentException("string setting is not string");
         }

         stringSetting.set(jsonPrimitive4.getAsString());
      } else if (setting instanceof ColorSetting colorSetting) {
         JsonPrimitive jsonPrimitive5 = requirePrimitive(jsonElement);
         if (!jsonPrimitive5.isNumber()) {
            throw new IllegalArgumentException("color setting is not numeric");
         }

         colorSetting.set(jsonPrimitive5.getAsInt());
      } else if (setting instanceof KeyBindSetting keyBindSetting) {
         JsonPrimitive jsonPrimitive6 = requirePrimitive(jsonElement);
         if (!jsonPrimitive6.isNumber()) {
            throw new IllegalArgumentException("keybind setting is not numeric");
         }

         keyBindSetting.set(jsonPrimitive6.getAsInt());
      } else {
         if (setting instanceof MultiSelectSetting multiSelectSetting) {
            if (!jsonElement.isJsonArray()) {
               throw new IllegalArgumentException("multi setting is not array");
            }

            LinkedHashSet entries = new LinkedHashSet();

            for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
               JsonPrimitive jsonPrimitive7 = requirePrimitive(jsonElement2);
               if (!jsonPrimitive7.isString()) {
                  throw new IllegalArgumentException("multi entry is not string");
               }

               String asString2 = Translations.english(jsonPrimitive7.getAsString());
               if (!containsIgnoreCase(multiSelectSetting.getModes(), asString2)) {
                  throw new IllegalArgumentException("unknown multi mode");
               }

               entries.add(canonicalMode(multiSelectSetting.getModes(), asString2));
            }

            multiSelectSetting.set(entries);
         }
      }
   }

   private static boolean containsIgnoreCase(Iterable<String> strings, String text) {
      return canonicalMode(strings, text) != null;
   }

   public String cosmeticSubcategory() {
      return this.cosmeticSubcategory;
   }

   private void loadInternal() {
      if (this.file != null && Files.isRegularFile(this.file)) {
         try {
            JsonObject asJsonObject = JsonParser.parseString(Files.readString(this.file)).getAsJsonObject();
            this.loadModules(asJsonObject.getAsJsonObject("modules"));
            this.loadFavorites(asJsonObject.getAsJsonArray("favorites"));
            this.loadCustomThemes(asJsonObject.getAsJsonArray("customThemes"));
            this.loadTheme(asJsonObject.getAsJsonObject("theme"));
            this.loadPanels(asJsonObject.getAsJsonObject("panels"));
            this.loadDraggables(asJsonObject.getAsJsonObject("draggables"));
            this.loadMainMenu(asJsonObject.getAsJsonObject("mainMenu"));
            JsonObject asJsonObject2 = asJsonObject.getAsJsonObject("clickGui");
            if (asJsonObject2 != null && asJsonObject2.has("cosmeticSubcategory")) {
               this.setCosmeticSubcategory(asJsonObject2.get("cosmeticSubcategory").getAsString());
            }

            this.setCategoryDock(asJsonObject2 != null && asJsonObject2.has("categoryDock") ? asJsonObject2.get("categoryDock").getAsString() : "BOTTOM");
         } catch (Exception failure) {
         }
      }
   }

   public void save() {
      if (this.file != null) {
         try {
            Path parent = this.file.getParent();
            if (parent != null) {
               Files.createDirectories(parent);
            }

            JsonObject json = new JsonObject();
            json.add("modules", this.saveModules());
            JsonArray jsonValues = new JsonArray();

            for (String text : this.favoriteModules) {
               jsonValues.add(text);
            }

            json.add("favorites", jsonValues);
            JsonArray jsonValues2 = new JsonArray();

            for (BackgroundGuiHelper backgroundGuiHelper : this.themes.customThemes()) {
               JsonObject json2 = new JsonObject();
               json2.addProperty("id", backgroundGuiHelper.id());
               json2.addProperty("name", backgroundGuiHelper.name());
               json2.addProperty("accent", backgroundGuiHelper.accent());
               json2.addProperty("base", backgroundGuiHelper.base());
               json2.addProperty("backgroundHud", backgroundGuiHelper.backgroundHud());
               json2.addProperty("backgroundGui", backgroundGuiHelper.backgroundGui());
               json2.addProperty("textPrimary", backgroundGuiHelper.textPrimary());
               json2.addProperty("textSecondary", backgroundGuiHelper.textSecondary());
               json2.addProperty("textTertiary", backgroundGuiHelper.textTertiary());
               jsonValues2.add(json2);
            }

            json.add("customThemes", jsonValues2);
            JsonObject json3 = new JsonObject();
            json3.addProperty("id", this.themes.currentId());
            if (this.themes.accentOverride() != null) {
               json3.addProperty("accent", this.themes.accentOverride());
            }

            if (this.themes.textOverride() != null) {
               json3.addProperty("text", this.themes.textOverride());
            }

            if (this.themes.secondaryOverride() != null) {
               json3.addProperty("secondary", this.themes.secondaryOverride());
            }

            if (this.themes.tertiaryOverride() != null) {
               json3.addProperty("tertiary", this.themes.tertiaryOverride());
            }

            if (this.themes.backgroundHudOverride() != null) {
               json3.addProperty("backgroundHud", this.themes.backgroundHudOverride());
            }

            if (this.themes.backgroundGuiOverride() != null) {
               json3.addProperty("backgroundGui", this.themes.backgroundGuiOverride());
            }

            json.add("theme", json3);
            JsonObject json4 = new JsonObject();
            this.panelPositions.forEach((text2, panelPosition) -> {
               JsonObject json5 = new JsonObject();
               float x = panelPosition.x();
               json5.addProperty("x", x);
               x = panelPosition.y();
               json5.addProperty("y", x);
               json4.add(text2, json5);
            });
            json.add("panels", json4);
            JsonObject json6 = new JsonObject();
            this.draggablePositions.forEach((text3, draggablePosition) -> {
               JsonObject json7 = new JsonObject();
               float x2 = draggablePosition.x();
               json7.addProperty("x", x2);
               x2 = draggablePosition.y();
               Float value = x2;
               json7.addProperty("y", value);
               json6.add(text3, json7);
            });
            json.add("draggables", json6);
            JsonObject json8 = new JsonObject();
            json8.addProperty("background", this.mainMenuBackground);
            json.add("mainMenu", json8);
            JsonObject json9 = new JsonObject();
            json9.addProperty("cosmeticSubcategory", this.cosmeticSubcategory);
            json9.addProperty("categoryDock", this.categoryDock);
            json.add("clickGui", json9);
            Files.writeString(this.file, this.gson.toJson(json));
         } catch (Exception failure) {
         }
      }
   }

   public void setCosmeticSubcategory(String cosmeticSubcategory2) {
      String text = "swords".equals(cosmeticSubcategory2) ? "swords" : "accessories";
      this.cosmeticSubcategory = text;
   }

   private void loadFavorites(JsonArray jsonValues) {
      this.favoriteModules.clear();
      if (jsonValues != null) {
         for (JsonElement jsonElement : jsonValues) {
            try {
               JsonPrimitive jsonPrimitive = requirePrimitive(jsonElement);
               if (jsonPrimitive.isString()) {
                  String asString = jsonPrimitive.getAsString();
                  this.favoriteModules.add("Removals".equalsIgnoreCase(asString) ? "No Render" : asString);
               }
            } catch (Exception failure) {
            }
         }
      }
   }

   private Path resolveExistingProfile(String text) {
      Path path = this.profilePath(text);
      if (path != null) {
         LinkOption[] linkOptionValues = new LinkOption[0];
         if (Files.isRegularFile(path, linkOptionValues)) {
            return path;
         }
      }

      Path path2 = this.legacyProfilePath(text);
      if (path2 != null) {
         LinkOption[] linkOptionValues2 = new LinkOption[0];
         if (Files.isRegularFile(path2, linkOptionValues2)) {
            return path2;
         }
      }

      return path;
   }

   private void loadDraggables(JsonObject json) {
      if (json != null) {
         var entries = json.entrySet();
         Iterator iterator = entries.iterator();

         while (true) {
            if (!iterator.hasNext()) {
               return;
            }

            Entry element = (Entry)iterator.next();

            try {
               JsonObject asJsonObject = ((JsonElement)element.getValue()).getAsJsonObject();
               float asFloat = asJsonObject.get("x").getAsFloat();
               JsonElement jsonElement = asJsonObject.get("y");
               float asFloat2 = jsonElement.getAsFloat();
               this.setDraggablePosition((String)element.getKey(), asFloat, asFloat2);
            } catch (Exception failure) {
            }
         }
      }
   }

   public String categoryDock() {
      return this.categoryDock;
   }

   private static String canonicalMode(Iterable<String> strings, String text) {
      if (text == null) {
         return null;
      }

      Iterator iterator = strings.iterator();

      while (iterator.hasNext()) {
         String element = (String)iterator.next();
         if (element.equalsIgnoreCase(text)) {
            return element;
         }
      }

      return null;
   }

   private static String currentUsername() {

      try {
         LocalProfile instance = LocalProfile.getInstance();
         String text = instance.profile("username");
         if (text != null) {
            if (!text.isBlank()) {
               return text.trim();
            }
         }

         return LocalProfile.DISPLAY_NAME;
      } catch (Exception failure) {
         return LocalProfile.DISPLAY_NAME;
      }
   }

   public void load() {
      ClientModule.runSilently(() -> {
         this.loadInternal();
      });
   }

   private void loadCustomThemes(JsonArray jsonValues) {
      this.themes.clearCustomThemes();
      if (jsonValues != null) {
         for (JsonElement jsonElement : jsonValues) {
            try {
               JsonObject asJsonObject = jsonElement.getAsJsonObject();
               this.themes
                  .addLoadedPreset(
                     new BackgroundGuiHelper(
                        asJsonObject.get("id").getAsString(),
                        asJsonObject.get("name").getAsString(),
                        asJsonObject.get("accent").getAsInt(),
                        asJsonObject.get("base").getAsInt(),
                        asJsonObject.has("backgroundHud") ? asJsonObject.get("backgroundHud").getAsInt() : asJsonObject.get("base").getAsInt(),
                        asJsonObject.has("backgroundGui") ? asJsonObject.get("backgroundGui").getAsInt() : asJsonObject.get("base").getAsInt(),
                        asJsonObject.get("textPrimary").getAsInt(),
                        asJsonObject.get("textSecondary").getAsInt(),
                        asJsonObject.get("textTertiary").getAsInt()
                     )
                  );
            } catch (Exception failure) {
            }
         }
      }
   }

   private JsonElement writeSetting(Setting<?> setting) {
      if (!(setting instanceof MultiSelectSetting multiSelectSetting)) {
         return this.gson.toJsonTree(setting.get());
      } else {
         JsonArray jsonValues = new JsonArray();
         var strings = multiSelectSetting.get();
         Iterator iterator = strings.iterator();

         while (true) {
            if (!iterator.hasNext()) {
               return jsonValues;
            }

            String element = (String)iterator.next();
            jsonValues.add(element);
         }
      }
   }

   private static JsonElement mergedNoRenderConfig(JsonObject json, JsonElement jsonElement) {
      JsonObject asJsonObject = json.getAsJsonObject("Removals");
      JsonObject asJsonObject2 = jsonElement != null && jsonElement.isJsonObject() ? jsonElement.getAsJsonObject() : null;
      JsonObject asJsonObject3 = asJsonObject2 != null && asJsonObject2.has("settings") ? asJsonObject2.getAsJsonObject("settings") : null;
      if (asJsonObject3 != null && asJsonObject3.has("Overlay")) {
         return jsonElement;
      }

      if (asJsonObject == null && asJsonObject2 == null) {
         return jsonElement;
      }

      JsonObject json2 = asJsonObject != null ? asJsonObject.deepCopy() : asJsonObject2.deepCopy();
      JsonObject asJsonObject4 = json2.has("settings") && json2.get("settings").isJsonObject() ? json2.getAsJsonObject("settings") : new JsonObject();
      boolean configEnabled = configEnabled(asJsonObject2);
      boolean configEnabled2 = configEnabled(asJsonObject);
      boolean condition = configEnabled;
      if (asJsonObject3 != null) {
         for (Entry entry : asJsonObject3.entrySet()) {
            if ("Camera Clip".equals(Translations.english((String)entry.getKey())) && ((JsonElement)entry.getValue()).isJsonPrimitive()) {
               condition = configEnabled && ((JsonElement)entry.getValue()).getAsBoolean();
               break;
            }
         }
      }

      asJsonObject4.addProperty("Camera Clip", Boolean.valueOf(condition));
      if (!configEnabled2) {
         asJsonObject4.add("Overlay", new JsonArray());
         asJsonObject4.add("World", new JsonArray());
         asJsonObject4.add("Sounds", new JsonArray());
      }

      json2.add("settings", asJsonObject4);
      json2.addProperty("enabled", configEnabled || configEnabled2);
      if (asJsonObject2 != null) {
         if (asJsonObject2.has("keyBind")) {
            json2.add("keyBind", asJsonObject2.get("keyBind").deepCopy());
         }

         if (asJsonObject2.has("bindMode")) {
            json2.add("bindMode", asJsonObject2.get("bindMode").deepCopy());
         }
      }

      return json2;
   }

   public boolean loadProfile(String text) {
      Path path = this.resolveExistingProfile(text);
      if (path != null && Files.isRegularFile(path) && this.file != null) {
         try {
            Path parent = this.file.getParent();
            if (parent != null) {
               Files.createDirectories(parent);
            }

            Files.copy(path, this.file, StandardCopyOption.REPLACE_EXISTING);
            this.load();
            this.activeProfile = cleanProfileName(text);
            this.modules.get(InterfaceModule.class).ifPresent(interfaceModule -> {
               if (interfaceModule.isNotificationsEnabled()) {
                  NotificationsHud notificationsHud = interfaceModule.notifications();
                  notificationsHud.onConfigLoaded(this.activeProfile);
               }
            });
            return true;
         } catch (Exception failure) {
            return false;
         }
      } else {
         return false;
      }
   }

   public List<ConfigManager.ProfileInfo> listProfileInfos() {
      var stream = this.listProfiles().stream();
      Function<java.lang.String, net.skycore.client.config.ConfigManager.ProfileInfo> function = this::profileInfo;
      var stream2 = stream.map(function);
      return stream2.toList();
   }

   private Path legacyProfilePath(String text) {
      String text2 = cleanProfileName(text);
      if (text2 == null) {
         return null;
      }

      Path profilesDirectory = this.getProfilesDirectory();
      String text3 = text2 + ".json";
      return profilesDirectory.resolve(text3);
   }

   private static String cleanProfileName(String text) {
      if (text != null) {
         if (!text.isBlank()) {
            if (text.indexOf(47) < 0) {
               if (text.indexOf(92) < 0 && !text.contains("..")) {
                  String text2 = text.trim();
                  return text2.toLowerCase(Locale.ROOT);
               }
            }
         }
      }

      return null;
   }

   public boolean isProfileFavorite(String text) {
      ConfigManager.ProfileInfo profileInfo = this.profileInfo(text);
      return profileInfo.favorite();
   }

   private JsonObject saveModules() {
      JsonObject json = new JsonObject();

      for (ClientModule clientModule : this.modules.getModules()) {
         JsonObject json2 = new JsonObject();
         json2.addProperty("enabled", clientModule.isEnabled());
         json2.addProperty("keyBind", clientModule.getKeyBind());
         json2.addProperty("bindMode", clientModule.getBindMode().name());
         JsonObject json3 = new JsonObject();

         for (Setting setting : clientModule.getSettings()) {
            json3.add(setting.getName(), this.writeSetting(setting));
         }

         json2.add("settings", json3);
         json.add(clientModule.getName(), json2);
      }

      return json;
   }

   public boolean saveProfile(String text) {
      Path path = this.profilePath(text);
      if (path != null && this.file != null) {
         try {
            boolean profileFavorite = this.isProfileFavorite(text);
            this.save();
            Files.createDirectories(path.getParent());
            JsonObject asJsonObject = JsonParser.parseString(Files.readString(this.file)).getAsJsonObject();
            JsonObject asJsonObject2 = asJsonObject.has("meta") && asJsonObject.get("meta").isJsonObject() ? asJsonObject.getAsJsonObject("meta") : new JsonObject();
            asJsonObject2.addProperty("author", currentUsername());
            asJsonObject2.addProperty("favorite", profileFavorite);
            asJsonObject.add("meta", asJsonObject2);
            Files.writeString(path, this.gson.toJson(asJsonObject));
            this.activeProfile = cleanProfileName(text);
            return true;
         } catch (Exception failure) {
            return false;
         }
      } else {
         return false;
      }
   }

   public void setMainMenuBackground(int mainMenuBackground2) {
      this.mainMenuBackground = Math.max(0, mainMenuBackground2);
   }

   public ConfigManager.PanelPosition getPanelPosition(String text) {
      return this.panelPositions.get(text);
   }

   private static float clamp01(float value) {
      float minimum = Math.min(1.0F, value);
      return Math.max(0.0F, minimum);
   }

   public void setCategoryDock(String categoryDock2) {
      if (categoryDock2 == null) {
         this.categoryDock = "BOTTOM";
      } else {
         switch (categoryDock2.toUpperCase(Locale.ROOT)) {
            case "TOP":
            case "BOTTOM":
            case "LEFT":
            case "RIGHT":
               this.categoryDock = categoryDock2.toUpperCase(Locale.ROOT);
               break;
            default:
               this.categoryDock = "BOTTOM";
         }
      }
   }

   private Path profilePath(String text) {
      String text2 = cleanProfileName(text);
      if (text2 == null) {
         return null;
      }

      Path profilesDirectory = this.getProfilesDirectory();
      String text3 = text2 + ".cfg";
      return profilesDirectory.resolve(text3);
   }

   public String profileAuthor(String text) {
      return this.profileInfo(text).author();
   }

   public List<String> listProfiles() {
      Path profilesDirectory = this.getProfilesDirectory();
      if (!Files.isDirectory(profilesDirectory)) {
         return List.of();
      }

      try (var stream = Files.list(profilesDirectory)) {
         return stream.filter(path -> {
            Path fileName = path.getFileName();
            String text = fileName.toString();
            String text2 = text.toLowerCase(Locale.ROOT);
            return text2.endsWith(".cfg") || text2.endsWith(".json");
         }).map(path2 -> {
            String text3 = path2.getFileName().toString();
            return text3.replaceFirst("(?i)\\.(cfg|json)$", "");
         }).distinct().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
      } catch (Exception failure) {
         return List.of();
      }
   }

   public void toggleFavorite(ClientModule clientModule) {
      if (clientModule != null) {
         if (this.isFavorite(clientModule)) {
            this.favoriteModules.removeIf(text -> {
               return text.equalsIgnoreCase(clientModule.getName());
            });
         } else {
            this.favoriteModules.add(clientModule.getName());
         }
      }
   }

   public void setDraggablePosition(String draggablePosition, float draggablePosition2, float draggablePosition3) {
      if (draggablePosition != null) {
         if (Float.isFinite(draggablePosition2)) {
            if (Float.isFinite(draggablePosition3)) {
               var draggablePositions2 = this.draggablePositions;
               float intermediate = clamp01(draggablePosition2);
               ConfigManager.DraggablePosition draggablePosition4 = new ConfigManager.DraggablePosition(intermediate, clamp01(draggablePosition3));
               draggablePositions2.put(draggablePosition, draggablePosition4);
            }
         }
      }
   }

   public Path getProfilesDirectory() {
      Path path;
      if (this.file == null) {
         path = null;
      } else {
         path = this.file.getParent();
      }

      Path path2 = path;
      Path path3;
      if (path2 == null) {
         String[] stringValues = new String[0];
         path3 = Path.of("configs", stringValues);
      } else {
         path3 = path2.resolve("profiles");
      }

      return path3;
   }

   public String activeProfile() {
      return this.activeProfile;
   }

   public void toggleProfileFavorite(String text) {
      Path path = this.resolveExistingProfile(text);
      if (path != null && Files.isRegularFile(path)) {
         try {
            JsonObject asJsonObject = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            JsonObject asJsonObject2 = asJsonObject.has("meta") && asJsonObject.get("meta").isJsonObject() ? asJsonObject.getAsJsonObject("meta") : new JsonObject();
            ConfigManager.ProfileInfo profileInfo = this.profileInfo(text);
            asJsonObject2.addProperty("author", profileInfo.author());
            asJsonObject2.addProperty("favorite", !profileInfo.favorite());
            asJsonObject.add("meta", asJsonObject2);
            Files.writeString(path, this.gson.toJson(asJsonObject));
         } catch (Exception failure) {
         }
      }
   }

   private static JsonPrimitive requirePrimitive(JsonElement jsonElement) {
      if (jsonElement != null && !jsonElement.isJsonNull()) {
         if (jsonElement.isJsonPrimitive()) {
            return jsonElement.getAsJsonPrimitive();
         }
      }

      throw new IllegalArgumentException("expected primitive");
   }

   public ConfigManager.DraggablePosition getDraggablePosition(String text) {
      return this.draggablePositions.get(text);
   }

   public boolean deleteProfile(String text) {
      Path path = this.profilePath(text);
      Path path2 = this.legacyProfilePath(text);

      try {
         boolean condition = false;
         if (path != null) {
            condition = false | Files.deleteIfExists(path);
         }

         if (path2 != null) {
            condition |= Files.deleteIfExists(path2);
         }

         if (this.activeProfile != null) {
            String activeProfile2 = this.activeProfile;
            String text2 = cleanProfileName(text);
            if (activeProfile2.equalsIgnoreCase(text2)) {
               this.activeProfile = null;
            }
         }

         return condition;
      } catch (Exception failure) {
         return false;
      }
   }

   private static boolean configEnabled(JsonObject json) {

      try {
         if (json != null) {
            if (json.has("enabled") && json.get("enabled").getAsBoolean()) {
               return true;
            }
         }

         return false;
      } catch (Exception failure) {
         return false;
      }
   }

   public boolean isFavorite(ClientModule clientModule) {
      if (clientModule == null) {
         return false;
      }


      for (String text : this.favoriteModules) {
         if (text.equalsIgnoreCase(clientModule.getName())) {
            return true;
         }
      }

      return false;
   }

   public record DraggablePosition(float x, float y) {
      public float y() {
         return this.y;
      }

      public float x() {
         return this.x;
      }
   }

   public record PanelPosition(float x, float y) {
      public float y() {
         return this.y;
      }

      public float x() {
         return this.x;
      }
   }

   public record ProfileInfo(String name, String author, boolean favorite) {
      public String name() {
         return this.name;
      }

      public String author() {
         return this.author;
      }
   }
}
