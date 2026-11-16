package net.skycore.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.skycore.client.module.render.HandsModule;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererHandsBeforeRenderMixin {
   @Inject(
      method = "renderItemInHand",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lnet/minecraft/client/renderer/SubmitNodeStorage;)V",
         shift = Shift.BEFORE
      )
   )
   private void skycore$handsBeforeRender(CameraRenderState cameraRenderState, float coordinate, Matrix4fc matrix4fc, CallbackInfo callbackInfo) {
      HandsModule.INSTANCE.renderer().beforeHands();
   }

   @Inject(
      method = "renderItemInHand",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lnet/minecraft/client/renderer/SubmitNodeStorage;)V",
         shift = Shift.AFTER
      )
   )
   private void skycore$handsAfterRender(CameraRenderState cameraRenderState, float coordinate, Matrix4fc matrix4fc, CallbackInfo callbackInfo) {
      HandsModule.INSTANCE.renderer().afterHands();
   }
}
