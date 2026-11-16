package deluxe.mixin;

import deluxe.client.module.render.BlockOverlayModule;
import deluxe.client.module.render.CustomWorldModule;
import deluxe.client.render.PlayerOutlineRenderer;
import deluxe.client.module.render.TagsModule;
import deluxe.client.module.render.TargetEspModule;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin2 {
   @Shadow
   private Framebuffer entityOutlineFramebuffer;
   private boolean playersMaskFilled;

   @Shadow
   protected abstract boolean canDrawEntityOutlines();

   @Inject(method = "renderSky(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/client/render/Camera;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"), cancellable = true)
   private void replaceVanillaSky(FrameGraphBuilder frameGraphBuilder, Camera camera, GpuBufferSlice gpuBufferSlice, CallbackInfo callbackInfo) {
      if (CustomWorldModule.method2204().method2185()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "renderTargetBlockOutline(Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/util/math/MatrixStack;ZLnet/minecraft/client/render/state/WorldRenderState;)V", at = @At("HEAD"), cancellable = true)
   private void skipVanillaOutlineForBlockOverlay(
      Immediate immediate, MatrixStack matrixStack, boolean local8220_3, WorldRenderState worldRenderState, CallbackInfo callbackInfo
   ) {
      if (BlockOverlayModule.method3643().isEnabled()) {
         callbackInfo.cancel();
      }
   }

   @ModifyExpressionValue(method = "render(Lnet/minecraft/client/util/memory/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/state/WorldRenderState;hasOutline:Z"))
   private boolean skipVanillaGlowPostChain(boolean newPlayersMaskFilled) {
      if (TagsModule.method4106().method4095()) {
         this.playersMaskFilled = newPlayersMaskFilled;
         return false;
      } else {
         return newPlayersMaskFilled;
      }
   }

   @Inject(method = "drawEntityOutlinesFramebuffer()V", at = @At("HEAD"), cancellable = true)
   private void drawPlayersShaderInsteadOfGlowBlit(CallbackInfo callbackInfo) {
      if (!TagsModule.method4106().method4095()) {
         PlayerOutlineRenderer.method4386().method4375();
      } else {
         callbackInfo.cancel();
         PlayerOutlineRenderer.method4386().method4376(this.entityOutlineFramebuffer);
         if (this.playersMaskFilled && TagsModule.method4106().field13990.isChecked()) {
            PlayerOutlineRenderer.method4386().method4391(this.entityOutlineFramebuffer);
         }

         TargetEspModule.method1936().method1909();
      }
   }
}
