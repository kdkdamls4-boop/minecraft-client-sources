package deluxe.mixin;

import deluxe.client.render.ArcDividerRenderer;
import deluxe.client.render.BatchedBlurRenderer;
import deluxe.client.render.BlobRenderer;
import deluxe.client.render.CircleRenderer;
import deluxe.client.render.CornerBracketRenderer;
import deluxe.client.render.FontRegistry;
import deluxe.client.render.GlassRenderer;
import deluxe.client.render.GlowCompositeRenderer;
import deluxe.client.render.ImageRenderer;
import deluxe.client.render.LineRenderer;
import deluxe.client.render.MenuGridRenderer;
import deluxe.client.render.MsdfTextRenderer;
import deluxe.client.render.Outline360Renderer;
import deluxe.client.render.OutlineDefaultRenderer;
import deluxe.client.render.OutlineGlassRenderer;
import deluxe.client.render.PickerRenderer;
import deluxe.client.render.ThemePaletteBuffer;
import deluxe.client.render.ItemRenderCache;
import deluxe.client.render.PipelineWarmup;
import deluxe.client.render.GradientSplitBuffer;
import deluxe.client.render.ShapeBlurManager;
import deluxe.client.render.ShapePipelines;
import deluxe.client.render.RectDefaultRenderer;
import deluxe.client.render.RectHalfIconRenderer;
import deluxe.client.render.RippleRenderer;
import deluxe.client.render.ShapeRenderer;
import deluxe.client.render.ShimmerRenderer;
import deluxe.client.render.SoftGlowRenderer;
import ru.dreamix.fabricloader.VMBridge;
import deluxe.client.render.WarpTextRenderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import net.minecraft.client.gui.render.state.TexturedQuadGuiElementRenderState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.texture.TextureSetup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
   @Shadow
   @Final
   private List<?> preparations;
   @Shadow
   @Final
   private List<?> draws;
   @Shadow
   private int blurLayer;
   private final IntArrayList shapeBlurMarkers = new IntArrayList();
   private final IntArrayList shapeBlurMarkerSlots = new IntArrayList();
   private RenderPass currentRenderPass;
   private boolean blurDrawActive;
   private boolean glassDrawActive;
   private boolean shapeDrawActive;
   private boolean glowDrawActive;
   private boolean softGlowDrawActive;
   private boolean glassOutlineDrawActive;
   private boolean circleDrawActive;
   private boolean bracketDrawActive;
   private boolean blobDrawActive;
   private boolean arcDrawActive;
   private boolean pickerDrawActive;
   private boolean rectangleDrawActive;
   private boolean halfIconRectangleDrawActive;
   private boolean outlineDrawActive;
   private boolean outline360DrawActive;
   private boolean imageDrawActive;
   private boolean lineDrawActive;
   private boolean itemDrawActive;
   private boolean rippleDrawActive;
   private boolean shimmerDrawActive;
   private boolean menuGridDrawActive;
   private boolean warpTextDrawActive;

   @Invoker("render")
   abstract void deluxe$executeDrawRange(
      Supplier<String> var1, Framebuffer framebuffer, GpuBufferSlice gpuBufferSlice, GpuBufferSlice gpuBufferSlice2, GpuBuffer gpuBuffer, IndexType indexType, int var7, int var8
   );

   @Inject(method = "endBuffer(Lnet/minecraft/client/render/BufferBuilder;Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/texture/TextureSetup;Lnet/minecraft/client/gui/ScreenRect;)V", at = @At("HEAD"))
   private void trackShapeBlurMarker(
      BufferBuilder bufferBuilder, RenderPipeline renderPipeline, TextureSetup textureSetup, ScreenRect screenRect, CallbackInfo callbackInfo
   ) {
      BatchedBlurRenderer.method4504().method4514(renderPipeline, textureSetup, this.preparations.size(), this.blurLayer);
      if (ShapePipelines.usesBlurTexture(renderPipeline)) {
         this.shapeBlurMarkers.add(this.preparations.size());
         this.shapeBlurMarkerSlots.add(textureSetup == null ? -1 : ShapeBlurManager.INSTANCE.method4056(textureSetup.texure0()));
      }
   }

   @Redirect(
      method = "renderPreparedDraws(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Ljava/util/function/Supplier;Lnet/minecraft/client/gl/Framebuffer;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V"
      )
   )
   private void splitShapeBlurRanges(
      GuiRenderer guiRenderer,
      Supplier<String> local8150_2,
      Framebuffer framebuffer,
      GpuBufferSlice gpuBufferSlice,
      GpuBufferSlice gpuBufferSlice2,
      GpuBuffer gpuBuffer,
      IndexType indexType,
      int local8150_8,
      int local8150_9
   ) {
      if (!this.shapeBlurMarkers.isEmpty() && local8150_8 < local8150_9) {
         int local8150_10 = local8150_8;

         for (int index = 0; index < this.shapeBlurMarkers.size(); index++) {
            int local8150_12 = this.shapeBlurMarkers.getInt(index);
            if (local8150_12 >= local8150_8 && local8150_12 < local8150_9) {
               int local8150_13 = this.shapeBlurMarkerSlots.getInt(index);
               if (local8150_13 >= 0 && ShapeBlurManager.INSTANCE.method4057(local8150_13)) {
                  if (local8150_12 > local8150_10) {
                     this.deluxe$executeDrawRange(local8150_2, framebuffer, gpuBufferSlice, gpuBufferSlice2, gpuBuffer, indexType, local8150_10, local8150_12);
                  }

                  ShapeBlurManager.INSTANCE.method4060(local8150_13, framebuffer);
                  local8150_10 = local8150_12;
               }
            }
         }

         if (local8150_10 < local8150_9) {
            this.deluxe$executeDrawRange(local8150_2, framebuffer, gpuBufferSlice, gpuBufferSlice2, gpuBuffer, indexType, local8150_10, local8150_9);
         }
      } else {
         this.deluxe$executeDrawRange(local8150_2, framebuffer, gpuBufferSlice, gpuBufferSlice2, gpuBuffer, indexType, local8150_8, local8150_9);
      }
   }

   @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"))
   private void beginBlurFrame(CallbackInfo callbackInfo) {
      int local8151_2 = MinecraftClient.getInstance().getOverlay() == null ? 1 : 8;

      while (local8151_2 > 0 && this.warmNextUnit()) {
         local8151_2--;
      }

      BatchedBlurRenderer.method4504().method4495();
      GlassRenderer.method2703().method2702();
      ShapeRenderer.method3903().method3904();
      GlowCompositeRenderer.method1853().method1813();
      SoftGlowRenderer.method2483().method2485();
      OutlineGlassRenderer.method2432().method2426();
      CircleRenderer.method4292().method4305();
      CornerBracketRenderer.method4283().method4272();
      BlobRenderer.method3064().method3052();
      ArcDividerRenderer.method3734().method3744();
      PickerRenderer.method0930().method0919();
      RectDefaultRenderer.method4243().method4250();
      RectHalfIconRenderer.method3933().method3944();
      OutlineDefaultRenderer.method1479().method1470();
      Outline360Renderer.method2731().method2723();
      ImageRenderer.method3174().method3170();
      LineRenderer.method1280().method1270();
      RippleRenderer.method4355().method4352();
      ShimmerRenderer.method3145().method3155();
      MenuGridRenderer.method2408().method2404();
      WarpTextRenderer.method0813().method0806();
      MsdfTextRenderer.method2389().method2364();
      ItemRenderCache.method2351();
   }

   private boolean warmNextUnit() {
      if (FontRegistry.warmNextFont()) {
         return true;
      } else {
         return MsdfTextRenderer.method2389().method2362() ? true : PipelineWarmup.warmNextPipeline();
      }
   }

   @Inject(method = "prepare()V", at = @At("HEAD"))
   private void preparePendingBlurResources(CallbackInfo callbackInfo) {
      this.shapeBlurMarkers.clear();
      this.shapeBlurMarkerSlots.clear();
      GradientSplitBuffer.method3126();
      BatchedBlurRenderer.method4504().method4481();
      GlowCompositeRenderer.method1853().method1838();
   }

   @Inject(method = "prepare()V", at = @At("RETURN"))
   private void prepareRenderUniforms(CallbackInfo callbackInfo) {
      ThemePaletteBuffer.method2143();
      BatchedBlurRenderer.method4504().method4487();
      GlassRenderer.method2703().method2708();
      ShapeRenderer.method3903().method3900();
      GlowCompositeRenderer.method1853().method1852();
      SoftGlowRenderer.method2483().method2491();
      OutlineGlassRenderer.method2432().method2420();
      CircleRenderer.method4292().method4302();
      CornerBracketRenderer.method4283().method4280();
      BlobRenderer.method3064().method3048();
      ArcDividerRenderer.method3734().method3742();
      PickerRenderer.method0930().method0931();
      RectDefaultRenderer.method4243().method4248();
      RectHalfIconRenderer.method3933().method3925();
      OutlineDefaultRenderer.method1479().method1482();
      Outline360Renderer.method2731().method2736();
      ImageRenderer.method3174().method3168();
      LineRenderer.method1280().method1283();
      RippleRenderer.method4355().method4350();
      ShimmerRenderer.method3145().method3141();
      MenuGridRenderer.method2408().method2409();
      WarpTextRenderer.method0813().method0814();
      ItemRenderCache.method2355();
   }

   @Inject(method = "renderPreparedDraws(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"))
   private void prepareBlurCapture(CallbackInfo callbackInfo) {
      BatchedBlurRenderer.method4504().method4503(this.blurLayer, this.draws.size());
      ShapeBlurManager.INSTANCE.method4046();
   }

   @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("RETURN"))
   private void clearSpecialItemTints(CallbackInfo callbackInfo) {
      ItemRenderCache.method2340();
      ShapeBlurManager.INSTANCE.method4045();
   }

   @Redirect(
      method = "prepareItem(Lnet/minecraft/client/gui/render/state/ItemGuiElementRenderState;FFII)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;addSimpleElementToCurrentLayer(Lnet/minecraft/client/gui/render/state/TexturedQuadGuiElementRenderState;)V"
      )
   )
   private void tintItemAtlasBlit(
      GuiRenderState guiRenderState,
      TexturedQuadGuiElementRenderState texturedQuadGuiElementRenderState,
      ItemGuiElementRenderState itemGuiElementRenderState,
      float local8157_4,
      float local8157_5,
      int local8157_6,
      int local8157_7
   ) {
      guiRenderState.addSimpleElementToCurrentLayer(
         ItemRenderCache.method2354(itemGuiElementRenderState) == -1
            ? texturedQuadGuiElementRenderState
            : new TexturedQuadGuiElementRenderState(
               texturedQuadGuiElementRenderState.pipeline(),
               texturedQuadGuiElementRenderState.textureSetup(),
               texturedQuadGuiElementRenderState.pose(),
               texturedQuadGuiElementRenderState.x1(),
               texturedQuadGuiElementRenderState.y1(),
               texturedQuadGuiElementRenderState.x2(),
               texturedQuadGuiElementRenderState.y2(),
               texturedQuadGuiElementRenderState.u1(),
               texturedQuadGuiElementRenderState.u2(),
               texturedQuadGuiElementRenderState.v1(),
               texturedQuadGuiElementRenderState.v2(),
               ItemRenderCache.method2354(itemGuiElementRenderState),
               texturedQuadGuiElementRenderState.scissorArea(),
               texturedQuadGuiElementRenderState.bounds()
            )
      );
   }

   @Redirect(
      method = "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V")
   )
   private void trackPipeline(RenderPass newCurrentRenderPass, RenderPipeline renderPipeline) {
      if (!renderPipeline.getLocation().getNamespace().equals("deluxe")) {
         this.currentRenderPass = null;
         newCurrentRenderPass.setPipeline(renderPipeline);
      } else {
         this.currentRenderPass = newCurrentRenderPass;
         this.blurDrawActive = BatchedBlurRenderer.method4504().method4510(renderPipeline);
         this.glassDrawActive = GlassRenderer.method2703().method2713(renderPipeline);
         this.shapeDrawActive = ShapeRenderer.method3903().method3909(renderPipeline);
         this.glowDrawActive = GlowCompositeRenderer.method1853().method1854(renderPipeline);
         this.softGlowDrawActive = SoftGlowRenderer.method2483().method2484(renderPipeline);
         this.glassOutlineDrawActive = OutlineGlassRenderer.method2432().method2430(renderPipeline);
         this.circleDrawActive = CircleRenderer.method4292().method4289(renderPipeline);
         this.bracketDrawActive = CornerBracketRenderer.method4283().method4282(renderPipeline);
         this.blobDrawActive = BlobRenderer.method3064().method3065(renderPipeline);
         this.arcDrawActive = ArcDividerRenderer.method3734().method3748(renderPipeline);
         this.pickerDrawActive = PickerRenderer.method0930().method0916(renderPipeline);
         this.rectangleDrawActive = RectDefaultRenderer.method4243().method4255(renderPipeline);
         this.halfIconRectangleDrawActive = RectHalfIconRenderer.method3933().method3939(renderPipeline);
         this.outlineDrawActive = OutlineDefaultRenderer.method1479().method1469(renderPipeline);
         this.outline360DrawActive = Outline360Renderer.method2731().method2725(renderPipeline);
         this.imageDrawActive = ImageRenderer.method3174().method3162(renderPipeline);
         this.lineDrawActive = LineRenderer.method1280().method1282(renderPipeline);
         this.rippleDrawActive = RippleRenderer.method4355().method4356(renderPipeline);
         this.shimmerDrawActive = ShimmerRenderer.method3145().method3144(renderPipeline);
         this.menuGridDrawActive = MenuGridRenderer.method2408().method2402(renderPipeline);
         this.warpTextDrawActive = WarpTextRenderer.method0813().method0803(renderPipeline);
         this.itemDrawActive = ItemRenderCache.method2352(renderPipeline);
         newCurrentRenderPass.setPipeline(renderPipeline);
      }
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;drawIndexed(IIII)V", shift = Shift.BEFORE))
   private void bindBlurParams(CallbackInfo callbackInfo) {
      if (this.blurDrawActive && this.currentRenderPass != null) {
         BatchedBlurRenderer.method4504().method4497(this.currentRenderPass);
      }

      if (this.glassDrawActive && this.currentRenderPass != null) {
         GlassRenderer.method2703().method2699(this.currentRenderPass);
      }

      if (this.shapeDrawActive && this.currentRenderPass != null) {
         ShapeRenderer.method3903().method3912(this.currentRenderPass);
      }

      if (this.glowDrawActive && this.currentRenderPass != null) {
         GlowCompositeRenderer.method1853().method1857(this.currentRenderPass);
      }

      if (this.softGlowDrawActive && this.currentRenderPass != null) {
         SoftGlowRenderer.method2483().method2490(this.currentRenderPass);
      }

      if (this.glassOutlineDrawActive && this.currentRenderPass != null) {
         OutlineGlassRenderer.method2432().method2415(this.currentRenderPass);
      }

      if (this.circleDrawActive && this.currentRenderPass != null) {
         CircleRenderer.method4292().method4296(this.currentRenderPass);
      }

      if (this.bracketDrawActive && this.currentRenderPass != null) {
         CornerBracketRenderer.method4283().method4273(this.currentRenderPass);
      }

      if (this.blobDrawActive && this.currentRenderPass != null) {
         BlobRenderer.method3064().method3058(this.currentRenderPass);
      }

      if (this.arcDrawActive && this.currentRenderPass != null) {
         ArcDividerRenderer.method3734().method3738(this.currentRenderPass);
      }

      if (this.pickerDrawActive && this.currentRenderPass != null) {
         PickerRenderer.method0930().method0918(this.currentRenderPass);
      }

      if (this.rectangleDrawActive && this.currentRenderPass != null) {
         RectDefaultRenderer.method4243().method4260(this.currentRenderPass);
      }

      if (this.halfIconRectangleDrawActive && this.currentRenderPass != null) {
         RectHalfIconRenderer.method3933().method3937(this.currentRenderPass);
      }

      if (this.outlineDrawActive && this.currentRenderPass != null) {
         OutlineDefaultRenderer.method1479().method1463(this.currentRenderPass);
      }

      if (this.outline360DrawActive && this.currentRenderPass != null) {
         Outline360Renderer.method2731().method2724(this.currentRenderPass);
      }

      if (this.imageDrawActive && this.currentRenderPass != null) {
         ImageRenderer.method3174().method3185(this.currentRenderPass);
      }

      if (this.lineDrawActive && this.currentRenderPass != null) {
         LineRenderer.method1280().method1269(this.currentRenderPass);
      }

      if (this.rippleDrawActive && this.currentRenderPass != null) {
         RippleRenderer.method4355().method4359(this.currentRenderPass);
      }

      if (this.shimmerDrawActive && this.currentRenderPass != null) {
         ShimmerRenderer.method3145().method3147(this.currentRenderPass);
      }

      if (this.menuGridDrawActive && this.currentRenderPass != null) {
         MenuGridRenderer.method2408().method2396(this.currentRenderPass);
      }

      if (this.warpTextDrawActive && this.currentRenderPass != null) {
         WarpTextRenderer.method0813().method0804(this.currentRenderPass);
      }

      if (this.itemDrawActive && this.currentRenderPass != null) {
         ItemRenderCache.method2358(this.currentRenderPass);
      }
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V", at = @At("RETURN"))
   private void clearTrackedPipeline(CallbackInfo callbackInfo) {
      this.currentRenderPass = null;
      this.blurDrawActive = false;
      this.glassDrawActive = false;
      this.shapeDrawActive = false;
      this.glowDrawActive = false;
      this.softGlowDrawActive = false;
      this.glassOutlineDrawActive = false;
      this.circleDrawActive = false;
      this.bracketDrawActive = false;
      this.blobDrawActive = false;
      this.arcDrawActive = false;
      this.pickerDrawActive = false;
      this.rectangleDrawActive = false;
      this.halfIconRectangleDrawActive = false;
      this.outlineDrawActive = false;
      this.outline360DrawActive = false;
      this.imageDrawActive = false;
      this.lineDrawActive = false;
      this.itemDrawActive = false;
      this.rippleDrawActive = false;
      this.shimmerDrawActive = false;
      this.menuGridDrawActive = false;
      this.warpTextDrawActive = false;
   }

   static {
      VMBridge.identifyClass(GuiRendererMixin.class, "ukilqNNd");
   }
}
