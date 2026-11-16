package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.skycore.client.config.FriendManager;
import net.skycore.client.module.render.CapeModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WingsLayer.class)
public abstract class WingsLayerCapeTextureOnElytraMixin {
   private static final Identifier CAPE_ON_ELYTRA = Identifier.parse("skycore:textures/cape.png");

   @Inject(
      method = "getPlayerElytraTexture(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)Lnet/minecraft/resources/Identifier;",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void skycore$capeTextureOnElytra(HumanoidRenderState humanoidRenderState, CallbackInfoReturnable<Identifier> callbackInfoReturnable) {
      if (CapeModule.INSTANCE.isEnabled() && humanoidRenderState instanceof AvatarRenderState avatarRenderState) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null) {
            if (instance.level.getEntity(avatarRenderState.id) instanceof AbstractClientPlayer player) {
               if (skycore$capeApplies(player)) {
                  callbackInfoReturnable.setReturnValue(CAPE_ON_ELYTRA);
               }
            }
         }
      }
   }

   private static boolean skycore$capeApplies(AbstractClientPlayer player) {
      Minecraft instance = Minecraft.getInstance();
      return player == instance.player || FriendManager.INSTANCE.isFriend(player.getGameProfile().name());
   }
}
