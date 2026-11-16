package net.skycore.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.skycore.client.util.AcquireViewHelper;
import net.skycore.client.util.GuiPipelines;
import net.skycore.client.util.CaptureHelper;
import net.skycore.client.util.RenderGuiHelper;
import net.skycore.client.util.RunPassHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererResetGlassSplitsMixin {
   @Shadow
   @Final
   private List<?> draws;
   @Unique
   private final IntList skycore$backdropSplits = new IntArrayList();
   @Unique
   private final IntList skycore$backdropKinds = new IntArrayList();
   @Unique
   private int skycore$previousBackdropKind;
   @Unique
   private boolean skycore$openingBackdropCaptured;

   @Inject(method = "prepare", at = @At("HEAD"))
   private void skycore$resetGlassSplits(CallbackInfo callbackInfo) {
      this.skycore$backdropSplits.clear();
      this.skycore$backdropKinds.clear();
      this.skycore$previousBackdropKind = 0;
      this.skycore$openingBackdropCaptured = false;
   }

   @Inject(method = "addElementToMesh", at = @At("HEAD"))
   private void skycore$markGlassRuns(GuiElementRenderState guiElementRenderState, CallbackInfo callbackInfo) {
      int value = skycore$backdropKind(guiElementRenderState.pipeline());
      if (value != 0 && value != this.skycore$previousBackdropKind) {
         this.skycore$backdropSplits.add(this.draws.size());
         this.skycore$backdropKinds.add(value);
      }

      this.skycore$previousBackdropKind = value;
   }

   @WrapOperation(
      method = "draw",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;II)V"
      )
   )
   private void skycore$captureBackdropBeforeGlassRuns(
      GuiRenderer guiRenderer, Supplier<String> supplier, RenderTarget renderTarget, GpuBufferSlice gpuBufferSlice, int value, int value2, Operation<Void> operation
   ) {
      int index = value;

      for (int value3 = 0; value3 < this.skycore$backdropSplits.size(); value3++) {
         int value4 = this.skycore$backdropSplits.getInt(value3);
         if (value4 >= index && value4 < value2) {
            if (value4 > index) {
               operation.call(new Object[]{guiRenderer, supplier, renderTarget, gpuBufferSlice, index, value4});
            }

            if (this.skycore$backdropKinds.getInt(value3) == 2) {
               AcquireViewHelper.captureNow();
            } else if (!RenderGuiHelper.stabilizeOpeningBlur() || !this.skycore$openingBackdropCaptured) {
               RunPassHelper.captureNow();
               this.skycore$openingBackdropCaptured = true;
            }

            index = value4;
         }
      }

      if (index < value2) {
         operation.call(new Object[]{guiRenderer, supplier, renderTarget, gpuBufferSlice, index, value2});
      }
   }

   @ModifyArg(
      method = "submitBlitFromItemAtlas",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/state/gui/BlitRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2fc;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V"
      ),
      index = 11
   )
   private int skycore$fadeItemBlit(int coordinate, @Local(argsOnly = true) GuiItemRenderState guiItemRenderState) {
      float value = CaptureHelper.consume(guiItemRenderState);
      if (value >= 0.999F) {
         return coordinate;
      }

      int roundedValue = Math.round(value * 255.0F);
      return roundedValue << 24 | roundedValue << 16 | roundedValue << 8 | roundedValue;
   }

   @WrapOperation(
      method = "submitBlitFromItemAtlas",
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/systems/SamplerCache;getRepeat(Lcom/mojang/blaze3d/textures/FilterMode;)Lcom/mojang/blaze3d/textures/GpuSampler;"
      )
   )
   private GpuSampler skycore$smoothDownscaledItems(
      SamplerCache samplerCache, FilterMode filterMode, Operation<GpuSampler> operation, @Local(argsOnly = true) GuiItemRenderState guiItemRenderState
   ) {
      return skycore$isDownscaled(guiItemRenderState) ? (GpuSampler)operation.call(new Object[]{samplerCache, FilterMode.LINEAR}) : (GpuSampler)operation.call(new Object[]{samplerCache, filterMode});
   }

   @WrapOperation(
      method = "submitBlitFromItemAtlas",
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/client/renderer/RenderPipelines;GUI_TEXTURED_PREMULTIPLIED_ALPHA:Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
      )
   )
   private RenderPipeline skycore$bicubicDownscalePipeline(Operation<RenderPipeline> operation, @Local(argsOnly = true) GuiItemRenderState guiItemRenderState) {
      return skycore$isDownscaled(guiItemRenderState)
         ? GuiPipelines.itemDownscale(Minecraft.getInstance().getWindow().getGuiScale())
         : (RenderPipeline)operation.call(new Object[0]);
   }

   @Unique
   private static int skycore$backdropKind(RenderPipeline renderPipeline) {
      if (renderPipeline == GuiPipelines.MATTE_BLUR_RECT) {
         return 2;
      } else {
         return renderPipeline != GuiPipelines.BLUR_RECT && renderPipeline != GuiPipelines.GLASS_SHADOW ? 0 : 1;
      }
   }

   @Unique
   private static boolean skycore$isDownscaled(GuiItemRenderState guiItemRenderState) {
      return guiItemRenderState.pose().m00 < 0.999F;
   }
}
