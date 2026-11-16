package net.skycore.client.module.render;

import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.StringSetting;
import net.skycore.client.util.EnsureIndexHelper;

public final class CosmeticsModule extends ClientModule {
   private final EnsureIndexHelper renderer;
   private final StringSetting masks;
   private final StringSetting swordModel;
   private final StringSetting hats;
   private final StringSetting sword;
   private final StringSetting backpacks;
   private final StringSetting wings = this.hidden("Cosmetic Wings");
   private final StringSetting pets;

   public EnsureIndexHelper renderer() {
      return this.renderer;
   }

   private StringSetting hidden(String text) {
      return new StringSetting(text, "", 256);
   }

   public String selectedSword() {
      return this.sword.get();
   }

   public CosmeticsModule() {
      super("Cosmetics", "Local cosmetics ported from the source client", ModuleCategory.RENDER);
      this.backpacks = this.hidden("Cosmetic Backpacks");
      this.hats = this.hidden("Cosmetic Hats");
      this.masks = this.hidden("Cosmetic Masks");
      this.pets = this.hidden("Cosmetic Pets");
      this.sword = this.hidden("Cosmetic Sword");
      this.swordModel = this.hidden("Cosmetic Sword Model");
      this.renderer = new EnsureIndexHelper();
      this.addSettings(this.wings, this.backpacks, this.hats, this.masks, this.pets, this.sword, this.swordModel);
   }

   public void selectSword(String text, String text2) {
      String text3 = text == null ? "" : text;
      this.sword.set(text3);
      this.swordModel.set(text2 == null ? "" : text2);
   }

   public String selected(String text) {
      if (text == null) {
         return "";
      }

      return switch (text.toLowerCase()) {
         case "wings" -> (String)this.wings.get();
         case "backpacks" -> (String)this.backpacks.get();
         case "hats" -> (String)this.hats.get();
         case "masks" -> (String)this.masks.get();
         case "pets" -> (String)this.pets.get();
         default -> "";
      };
   }

   public void renderWorld() {
   }

   public void select(String text, String text2) {
      if (text != null) {
         String text3 = text2 == null ? "" : text2;
         switch (text.toLowerCase()) {
            case "wings":
               this.wings.set(text3);
               break;
            case "backpacks":
               this.backpacks.set(text3);
               break;
            case "hats":
               this.hats.set(text3);
               break;
            case "masks":
               this.masks.set(text3);
               break;
            case "pets":
               this.pets.set(text3);
         }
      }
   }

   public String selectedSwordModel() {
      return this.swordModel.get();
   }
}
