package net.skycore.client.mixin;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.skycore.client.util.CosmeticsRenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererAddCosmeticsLayerMixin extends LivingEntityRenderer<Avatar, AvatarRenderState, PlayerModel> {
   protected AvatarRendererAddCosmeticsLayerMixin(Context context, PlayerModel playerModel, float coordinate) {
      super(context, playerModel, coordinate);
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   private void skycore$addCosmeticsLayer(Context context, boolean condition, CallbackInfo callbackInfo) {
      this.addLayer(new CosmeticsRenderLayer(this));
   }
}
