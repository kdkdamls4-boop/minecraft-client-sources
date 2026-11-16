package deluxe.client.util;

import deluxe.client.module.misc.YTMODEModule;

import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.item.ItemStack;

public enum GlintMode {
   AUTO,
   OFF,
   ON;

   boolean shouldRender(ItemStack itemStack, Glint glint) {
      return this.shouldRender(itemStack, glint != Glint.NONE);
   }

   public boolean shouldRender(ItemStack itemStack, boolean local5965_2) {
      if (YTMODEModule.method1426().isEnabled()) {
         return false;
      }

      return switch (this) {
         case AUTO -> itemStack != null && (itemStack.hasGlint() || local5965_2);
         case OFF -> false;
         case ON -> true;
      };
   }
}
