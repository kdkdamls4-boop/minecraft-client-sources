package deluxe.mixin;

import deluxe.client.util.MerchantScreenAccess;

import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MerchantScreen.class)
public class MerchantScreenMixin implements MerchantScreenAccess {
   @Shadow
   private int selectedIndex;

   @Shadow
   private void syncRecipeIndex() {
      throw new AssertionError();
   }

   @Override
   public int deluxe$getSelectedIndex() {
      return this.selectedIndex;
   }

   @Override
   public void deluxe$selectTrade(int newSelectedIndex) {
      this.selectedIndex = newSelectedIndex;
      this.syncRecipeIndex();
   }
}
