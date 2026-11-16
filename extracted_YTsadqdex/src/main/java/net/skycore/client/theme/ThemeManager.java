package net.skycore.client.theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.skycore.client.util.BackgroundGuiHelper;

public final class ThemeManager {
   private static final int BUILTIN_COUNT = 5;
   private Integer backgroundGuiOverride;
   private Integer textOverride;
   private BackgroundGuiHelper current;
   private final List<BackgroundGuiHelper> themes = new ArrayList<>(
      List.of(
         new BackgroundGuiHelper("rose", "Rose", -20575, -1207038195, -1207038195, -1207038195, -1, -1191182337, 2063597567),
         new BackgroundGuiHelper("orange", "Orange", -14943, -1207038195, -1207038195, -1207038195, -1, -1191182337, 2063597567),
         new BackgroundGuiHelper("sun", "Sun", -4959, -1207038195, -1207038195, -1207038195, -1, -1191182337, 2063597567),
         new BackgroundGuiHelper("lime", "Lime", -4194399, -1207038195, -1207038195, -1207038195, -1, -1191182337, 2063597567),
         new BackgroundGuiHelper("skycore", "Skycore", -10388481, -1072425709, -1072425709, -1072425709, -1, -1023410177, -2097152001)
      )
   );
   private Integer secondaryOverride;
   private Integer backgroundHudOverride;
   private Integer tertiaryOverride;
   private Integer accentOverride;

   public List<BackgroundGuiHelper> customThemes() {
      int value = 1;
      value = (byte)-1;
      var themes2 = this.themes;
      value = Math.min(5, this.themes.size());
      var backgroundGuiHelpers = themes2.subList(value, this.themes.size());
      return Collections.unmodifiableList(backgroundGuiHelpers);
   }

   private BackgroundGuiHelper find(String text) {
      if (text == null) {
         return null;
      }

      Iterator iterator = this.themes.iterator();

      BackgroundGuiHelper backgroundGuiHelper;
      String text2;
      do {
         if (!iterator.hasNext()) {
            return null;
         }

         backgroundGuiHelper = (BackgroundGuiHelper)iterator.next();
         if (backgroundGuiHelper.id().equalsIgnoreCase(text)) {
            break;
         }

         text2 = backgroundGuiHelper.name();
      } while (!text2.equalsIgnoreCase(text));

      return backgroundGuiHelper;
   }

   private void clearOverrides() {
      this.accentOverride = null;
      this.textOverride = null;
      this.secondaryOverride = null;
      this.tertiaryOverride = null;
      this.backgroundHudOverride = null;
      this.backgroundGuiOverride = null;
   }

   public List<BackgroundGuiHelper> themes() {
      return Collections.unmodifiableList(this.themes);
   }

   public int textTertiary() {
      int value = 0;
      value = (byte)0;
      if (this.tertiaryOverride == null) {
         value = this.current.textTertiary();
      } else {
         value = this.tertiaryOverride;
      }

      return value;
   }

   public void addLoadedPreset(BackgroundGuiHelper backgroundGuiHelper) {
      if (backgroundGuiHelper != null && backgroundGuiHelper.id() != null) {
         if (backgroundGuiHelper.name() != null) {
            String text = backgroundGuiHelper.id();
            if (this.find(text) == null) {
               this.themes.add(backgroundGuiHelper);
               return;
            }
         }
      }
   }

   public void setSlot(int slot, int accentOverride2) {
      if (!this.isBuiltin(this.current.id())) {
         BackgroundGuiHelper backgroundGuiHelper = new BackgroundGuiHelper(
            this.current.id(),
            this.current.name(),
            slot == 1 ? accentOverride2 | 0xFF000000 : this.current.accent(),
            slot == 6 ? accentOverride2 : this.current.base(),
            slot == 5 ? accentOverride2 : this.current.backgroundHud(),
            slot == 6 ? accentOverride2 : this.current.backgroundGui(),
            slot == 2 ? accentOverride2 : this.current.textPrimary(),
            slot == 3 ? accentOverride2 : this.current.textSecondary(),
            slot == 4 ? accentOverride2 : this.current.textTertiary()
         );
         int value = this.themes.indexOf(this.current);
         this.themes.set(value, backgroundGuiHelper);
         this.current = backgroundGuiHelper;
         this.clearOverrides();
      } else {
         switch (slot) {
            case 1:
               this.accentOverride = accentOverride2 | 0xFF000000;
               break;
            case 2:
               this.textOverride = accentOverride2;
               break;
            case 3:
               this.secondaryOverride = accentOverride2;
               break;
            case 4:
               this.tertiaryOverride = accentOverride2;
               break;
            case 5:
               this.backgroundHudOverride = accentOverride2;
               break;
            case 6:
               this.backgroundGuiOverride = accentOverride2;
               break;
            default:
               throw new IllegalArgumentException("Unknown theme slot: " + slot);
         }
      }
   }

   public Integer backgroundGuiOverride() {
      return this.backgroundGuiOverride;
   }

   public ThemeManager() {
      this.current = this.themes.get(3);
   }

   public int textSecondary() {
      int value = 0;
      value = (byte)0;
      if (this.secondaryOverride == null) {
         value = this.current.textSecondary();
      } else {
         value = this.secondaryOverride;
      }

      return value;
   }

   public int slot(int slot) {
      return switch (slot) {
         case 1 -> this.accent();
         case 2 -> {
            yield this.textPrimary();
         }
         case 3 -> {
            yield this.textSecondary();
         }
         case 4 -> {
            yield this.textTertiary();
         }
         case 5 -> {
            yield this.backgroundHud();
         }
         case 6 -> this.backgroundGui();
         default -> {
            String text = "Unknown theme slot: " + slot;
            throw new IllegalArgumentException(text);
         }
      };
   }

   public boolean isBuiltin(String text) {
      if (text == null) {
         return false;
      }

      int value = 0;

      while (true) {
         int size = this.themes.size();
         if (value >= Math.min(5, size)) {
            return false;
         }

         String text2 = this.themes.get(value).id();
         if (text2.equalsIgnoreCase(text)) {
            return true;
         }

         value++;
      }
   }

   public Integer textOverride() {
      return this.textOverride;
   }

   public Integer tertiaryOverride() {
      return this.tertiaryOverride;
   }

   public int textPrimary() {
      int value = 0;
      value = (byte)0;
      if (this.textOverride == null) {
         value = this.current.textPrimary();
      } else {
         value = this.textOverride;
      }

      return value;
   }

   public Integer secondaryOverride() {
      return this.secondaryOverride;
   }

   public int accent() {
      int value = 0;
      if (this.accentOverride == null) {
         value = this.current.accent();
      } else {
         value = this.accentOverride;
      }

      return value;
   }

   public void clearCustomThemes() {

      while (true) {
         if (this.themes.size() <= 5) {
            if (!this.themes.contains(this.current)) {
               this.current = this.themes.get(3);
               this.clearOverrides();
            }

            return;
         }

         this.themes.remove(this.themes.size() - 1);
      }
   }

   public boolean select(String text) {
      BackgroundGuiHelper backgroundGuiHelper = this.find(text);
      if (backgroundGuiHelper == null) {
         return false;
      }

      this.current = backgroundGuiHelper;
      this.clearOverrides();
      return true;
   }

   public int base() {
      return this.backgroundGui();
   }

   public int backgroundHud() {
      int intermediate;
      if (this.backgroundHudOverride == null) {
         intermediate = this.current.backgroundHud();
      } else {
         intermediate = this.backgroundHudOverride;
      }

      return intermediate;
   }

   public BackgroundGuiHelper addPreset(String text) {
      String text2 = text == null ? "" : text.trim();
      if (text2.isEmpty()) {
         return null;
      }

      String text3 = text2.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
      if (text3.isEmpty()) {
         text3 = "preset";
      }

      String text4 = text3;
      int value = 2;

      while (this.find(text4) != null) {
         text4 = text3 + "-" + value++;
      }

      BackgroundGuiHelper backgroundGuiHelper = new BackgroundGuiHelper(
         text4,
         text2,
         this.accent(),
         this.backgroundGui(),
         this.backgroundHud(),
         this.backgroundGui(),
         this.textPrimary(),
         this.textSecondary(),
         this.textTertiary()
      );
      this.themes.add(backgroundGuiHelper);
      this.current = backgroundGuiHelper;
      this.clearOverrides();
      return backgroundGuiHelper;
   }

   public void setAccent(int accent) {
      this.setSlot(1, accent | 0xFF000000);
   }

   public Integer backgroundHudOverride() {
      return this.backgroundHudOverride;
   }

   public BackgroundGuiHelper current() {
      return this.current;
   }

   public String currentId() {
      return this.current.id();
   }

   public Integer accentOverride() {
      return this.accentOverride;
   }

   public boolean removeCustom(String text) {
      if (text != null && !this.isBuiltin(text)) {
         for (int value = 5; value < this.themes.size(); value++) {
            BackgroundGuiHelper backgroundGuiHelper = this.themes.get(value);
            if (backgroundGuiHelper.id().equalsIgnoreCase(text) || backgroundGuiHelper.name().equalsIgnoreCase(text)) {
               int value2 = backgroundGuiHelper == this.current ? 1 : 0;
               this.themes.remove(value);
               if (value2 != 0) {
                  this.current = this.themes.get(Math.min(3, this.themes.size() - 1));
                  this.clearOverrides();
               }

               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public int backgroundGui() {
      return this.backgroundGuiOverride == null ? this.current.backgroundGui() : this.backgroundGuiOverride;
   }
}
