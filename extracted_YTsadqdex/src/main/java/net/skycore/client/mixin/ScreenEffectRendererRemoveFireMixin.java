package net.skycore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.skycore.client.module.render.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererRemoveFireMixin {
   @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
   private static void skycore$removeFire(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, TextureAtlasSprite textureAtlasSprite, CallbackInfo callbackInfo) {
      if (NoRenderModule.shouldRemoveFireOverlay()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
   private void skycore$removeTotemOverlay(ItemStack itemStack, RandomSource randomSource, CallbackInfo callbackInfo) {
      if (itemStack.has(DataComponents.DEATH_PROTECTION) && NoRenderModule.shouldRemoveTotemOverlay()) {
         callbackInfo.cancel();
      }
   }
}
