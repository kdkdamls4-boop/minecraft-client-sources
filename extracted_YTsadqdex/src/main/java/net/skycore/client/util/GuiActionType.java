package net.skycore.client.util;

public enum GuiActionType {
   HEADER,
   MODULE,
   EXPAND,
   BUTTON,
   BOOLEAN,
   NUMBER,
   MODE,
   MULTI,
   STRING,
   COLOR,
   KEYBIND,
   THEME,
   THEME_ACCENT,
   THEME_SLOT,
   THEME_NAME,
   THEME_ADD,
   SEARCH,
   LANGUAGE,
   VIEW,
   NAV_DOCK,
   CONFIG_NAME,
   CONFIG_SAVE,
   CONFIG_LOAD,
   CONFIG_FAVORITE,
   CONFIG_DELETE,
   COSMETIC,
   COSMETIC_NAV,
   COSMETIC_ADVANCED,
   COSMETIC_REFRESH,
   PICKER_SV,
   PICKER_HUE,
   PICKER_ALPHA,
   PICKER_HEX;

   public boolean isPicker() {
      if (this != PICKER_SV && this != PICKER_HUE) {
         if (this != PICKER_ALPHA) {
            if (this != PICKER_HEX) {
               return false;
            }
         }
      }

      return true;
   }
}
