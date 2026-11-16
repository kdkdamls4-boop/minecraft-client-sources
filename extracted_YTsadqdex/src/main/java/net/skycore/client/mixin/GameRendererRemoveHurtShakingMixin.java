package net.skycore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.skycore.client.module.render.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererRemoveHurtShakingMixin {
   @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
   private void skycore$removeHurtShaking(CameraRenderState cameraRenderState, PoseStack poseStack, CallbackInfo callbackInfo) {
      if (NoRenderModule.shouldRemoveShaking()) {
         callbackInfo.cancel();
      }
   }
}
