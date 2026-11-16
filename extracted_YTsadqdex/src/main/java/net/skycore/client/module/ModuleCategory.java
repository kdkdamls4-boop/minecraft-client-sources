package net.skycore.client.module;

public enum ModuleCategory {
   COMBAT("Combat"),
   MOVEMENT("Movement"),
   RENDER("Render"),
   PLAYER("Player"),
   MISC("Misc");

   private final String displayName;

   ModuleCategory(String displayName2) {
      this.displayName = displayName2;
   }

   public String displayName() {
      return this.displayName;
   }
}
