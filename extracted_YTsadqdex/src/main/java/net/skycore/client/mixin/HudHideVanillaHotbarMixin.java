package net.skycore.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.skycore.client.hud.HotbarHud;
import net.skycore.client.util.SpoofElytraAsChestplateHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudHideVanillaHotbarMixin {
   @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
   private void skycore$hideVanillaHotbar(CallbackInfo callbackInfo) {
      if (HotbarHud.shouldReplaceVanilla()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"))
   private void skycore$moveHotbarDecorationsStart(GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
      guiGraphicsExtractor.pose().pushMatrix();
      guiGraphicsExtractor.pose().translate(HotbarHud.vanillaHudOffsetX(), HotbarHud.vanillaHudOffsetY());
   }

   @Inject(method = "extractHotbarAndDecorations", at = @At("RETURN"))
   private void skycore$moveHotbarDecorationsEnd(GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
      guiGraphicsExtractor.pose().popMatrix();
   }

   @Redirect(method = "extractItemHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedSlot()I"))
   private int skycore$visualHotbarSelection(Inventory inventory) {
      int visualSlot = SpoofElytraAsChestplateHelper.getVisualSlot();
      return visualSlot >= 0 && visualSlot <= 8 ? visualSlot : inventory.getSelectedSlot();
   }

   @Redirect(
      method = "extractItemHotbar",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;")
   )
   private ItemStack skycore$visualHotbarStack(Inventory inventory, int value) {
      ItemStack item = inventory.getItem(value);
      return SpoofElytraAsChestplateHelper.getHotbarSpoofStack(value, item);
   }

   @Redirect(
      method = "tick()V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedItem()Lnet/minecraft/world/item/ItemStack;")
   )
   private ItemStack skycore$visualSelectedForHighlight(Inventory inventory) {
      if (SpoofElytraAsChestplateHelper.shouldSuppressTooltip()) {
         int visualSlot = SpoofElytraAsChestplateHelper.getVisualSlot();
         if (visualSlot >= 0 && visualSlot <= 8) {
            return SpoofElytraAsChestplateHelper.getHotbarSpoofStack(visualSlot, inventory.getItem(visualSlot));
         }
      }

      return inventory.getSelectedItem();
   }

   @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
   private void skycore$hideSelectedItemName(CallbackInfo callbackInfo) {
      if (SpoofElytraAsChestplateHelper.shouldSuppressTooltip()) {
         callbackInfo.cancel();
      }
   }
}
