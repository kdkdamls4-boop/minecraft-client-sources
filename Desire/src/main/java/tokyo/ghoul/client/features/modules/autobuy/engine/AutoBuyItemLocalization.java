package tokyo.ghoul.client.features.modules.autobuy.engine;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public final class AutoBuyItemLocalization {
   private static final String RESOURCE_ROOT = "assets/desire/lang/autobuy/";

   public enum Language {
      RUSSIAN, ENGLISH, POLISH
   }

   private static final Map<Language, Map<String, String>> TRANSLATIONS = loadTranslations();
   private static final Map<String, String> NAME_TO_KEY = buildReverseIndex();

   public static String getDisplayName(AutoBuyParsedItemData item) {
      return getDisplayName(item, Language.RUSSIAN);
   }

   public static String getDisplayName(AutoBuyParsedItemData item, Language language) {
      if (item == null) {
         return "";
      }
      return getDisplayName(item.path(), item.resolvedDisplayName(), language);
   }

   public static String getDisplayName(Path path, String fallbackName, Language language) {
      String key = extractStateKey(path);
      String translated = getTranslation(key, language);
      if (!translated.isBlank()) {
         return translated;
      }

      String reverseKey = NAME_TO_KEY.get(normalizeName(fallbackName));
      translated = getTranslation(reverseKey, language);
      if (!translated.isBlank()) {
         return translated;
      }

      return fallbackName == null ? "" : fallbackName;
   }

   public static String getHistoryDisplayName(String rawName) {
      return getDisplayName(null, rawName, Language.RUSSIAN);
   }

   public static List<String> getSearchableNames(AutoBuyParsedItemData item, Language language) {
      LinkedHashSet<String> names = new LinkedHashSet<>();
      if (item == null) {
         return List.of();
      }

      addName(names, item.resolvedDisplayName());
      addName(names, item.baseName());
      addName(names, getDisplayName(item, language));

      String key = extractStateKey(item.path());
      if (!key.isBlank()) {
         for (Map<String, String> translations : TRANSLATIONS.values()) {
            addName(names, translations.get(key));
         }
      }

      return new ArrayList<>(names);
   }

   private static void addName(LinkedHashSet<String> names, String value) {
      if (value != null && !value.isBlank()) {
         names.add(value);
      }
   }

   private static Map<Language, Map<String, String>> loadTranslations() {
      Map<Language, Map<String, String>> translations = new EnumMap<>(Language.class);
      translations.put(Language.RUSSIAN, loadLanguageFile("items_ru.properties"));
      translations.put(Language.ENGLISH, loadLanguageFile("items_en.properties"));
      translations.put(Language.POLISH, loadLanguageFile("items_pl.properties"));
      return translations;
   }

   private static Map<String, String> loadLanguageFile(String fileName) {
      Properties properties = new Properties();
      try (InputStream stream = AutoBuyItemLocalization.class.getClassLoader().getResourceAsStream(RESOURCE_ROOT + fileName)) {
         if (stream == null) {
            return Map.of();
         }
         try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            properties.load(reader);
         }
      } catch (Exception ignored) {
         return Map.of();
      }

      Map<String, String> loaded = new LinkedHashMap<>();
      for (String key : properties.stringPropertyNames()) {
         String value = properties.getProperty(key);
         if (value != null && !value.isBlank()) {
            loaded.put(key, value.trim());
         }
      }
      return loaded;
   }

   private static Map<String, String> buildReverseIndex() {
      Map<String, String> index = new LinkedHashMap<>();
      for (Map<String, String> translations : TRANSLATIONS.values()) {
         for (Map.Entry<String, String> entry : translations.entrySet()) {
            String normalized = normalizeName(entry.getValue());
            if (!normalized.isBlank()) {
               index.putIfAbsent(normalized, entry.getKey());
            }
         }
      }
      return index;
   }

   private static String getTranslation(String stateKey, Language language) {
      if (stateKey == null || stateKey.isBlank()) {
         return "";
      }

      Map<String, String> localized = TRANSLATIONS.get(language);
      if (localized != null) {
         String value = localized.get(stateKey);
         if (value != null && !value.isBlank()) {
            return value;
         }
      }

      Map<String, String> russian = TRANSLATIONS.get(Language.RUSSIAN);
      if (russian == null) {
         return "";
      }

      String fallback = russian.get(stateKey);
      return fallback == null ? "" : fallback;
   }

   private static String extractStateKey(Path path) {
      if (path == null || path.getNameCount() < 2) {
         return "";
      }
      if (!"__embedded_autobuy__".equals(path.getName(0).toString())) {
         return "";
      }

      String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
      if (!fileName.endsWith(".json")) {
         return "";
      }

      String stateKey = fileName.substring(0, fileName.length() - 5);
      return stateKey.matches("[0-9a-f]{64}") ? stateKey : "";
   }

   private static String normalizeName(String value) {
      return (value == null ? "" : value)
         .toLowerCase(Locale.ROOT)
         .replace('ё', 'е')
         .trim();
   }

   private AutoBuyItemLocalization() {
   }
}
