package net.skycore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.render.DuplisheModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererDuplisheHitMixin {
   @Inject(method = "bobHurt", at = @At("TAIL"))
   private void skycore$duplisheHit(CameraRenderState cameraRenderState, PoseStack poseStack, CallbackInfo callbackInfo) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.getCameraEntity() == instance.player) {
         SkycoreClient.modules().get(DuplisheModule.class).ifPresent(duplisheModule -> poseStack.mulPose(Axis.ZP.rotationDegrees(duplisheModule.peckAmount() * 5.0F)));
      }
   }
}
