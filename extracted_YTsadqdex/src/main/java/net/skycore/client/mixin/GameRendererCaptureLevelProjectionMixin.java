package net.skycore.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.skycore.client.util.CaptureLevelProjectionHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GameRenderer.class)
public abstract class GameRendererCaptureLevelProjectionMixin {
   @ModifyArg(
      method = "renderLevel",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"
      )
   )
   private Matrix4f skycore$captureLevelProjection(Matrix4f matrix) {
      CaptureLevelProjectionHelper.captureLevelProjection(matrix);
      return matrix;
   }
}
