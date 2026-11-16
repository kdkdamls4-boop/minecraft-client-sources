package net.skycore.client.mixin;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.skycore.client.util.ResolvePrimaryTextureKeyHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverCustomSwordMixin {
   @Inject(method = "appendItemLayers", at = @At("HEAD"), cancellable = true)
   private void skycore$customSword(ItemStackRenderState itemStackRenderState, ItemStack itemStack, ItemDisplayContext itemDisplayContext, Level level, ItemOwner itemOwner, int value, CallbackInfo callbackInfo) {
      if (ResolvePrimaryTextureKeyHelper.shouldReplace(itemStack)) {
         ResolvePrimaryTextureKeyHelper.Argument argument = ResolvePrimaryTextureKeyHelper.selected(itemDisplayContext);
         if (argument != null) {
            LayerRenderState layerRenderState = itemStackRenderState.newLayer();
            if (ResolvePrimaryTextureKeyHelper.INSTANCE.setupLayer(layerRenderState, argument)) {
               itemStackRenderState.setAnimated();
               callbackInfo.cancel();
            }
         }
      }
   }
}
