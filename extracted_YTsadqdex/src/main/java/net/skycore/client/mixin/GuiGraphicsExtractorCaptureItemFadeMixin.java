package net.skycore.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.skycore.client.util.CaptureHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorCaptureItemFadeMixin {
   @WrapOperation(
      method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addItem(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;)V"
      )
   )
   private void skycore$captureItemFade(GuiRenderState guiRenderState, GuiItemRenderState guiItemRenderState, Operation<Void> operation) {
      CaptureHelper.capture(guiItemRenderState);
      operation.call(new Object[]{guiRenderState, guiItemRenderState});
   }
}
