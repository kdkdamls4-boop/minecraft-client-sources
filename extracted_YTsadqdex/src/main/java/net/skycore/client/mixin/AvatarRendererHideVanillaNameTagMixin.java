package net.skycore.client.mixin;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.skycore.client.module.render.NameTagsModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererHideVanillaNameTagMixin {
   @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("HEAD"), cancellable = true)
   private void skycore$hideVanillaNameTag(Avatar avatar, double value, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      if (avatar instanceof Player player && NameTagsModule.shouldHideVanillaTag(player)) {
         callbackInfoReturnable.setReturnValue(false);
      }
   }
}
