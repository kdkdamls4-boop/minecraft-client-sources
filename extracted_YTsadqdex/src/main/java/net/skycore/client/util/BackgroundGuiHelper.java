package net.skycore.client.util;

public record BackgroundGuiHelper(
   String id, String name, int accent, int base, int backgroundHud, int backgroundGui, int textPrimary, int textSecondary, int textTertiary
) {
   public int backgroundGui() {
      return this.backgroundGui;
   }

   public int textPrimary() {
      return this.textPrimary;
   }

   public int textTertiary() {
      return this.textTertiary;
   }

   public int accent() {
      return this.accent;
   }

   public String name() {
      return this.name;
   }

   public int backgroundHud() {
      return this.backgroundHud;
   }

   public int base() {
      return this.base;
   }

   public String id() {
      return this.id;
   }

   public int textSecondary() {
      return this.textSecondary;
   }
}
