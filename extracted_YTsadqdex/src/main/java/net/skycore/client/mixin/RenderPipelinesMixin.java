package net.skycore.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.skycore.client.util.AddQuadHelper;
import net.skycore.client.util.TargetParticleRenderer;
import net.skycore.client.util.WorldParticleRenderer;
import net.skycore.client.util.GuiPipelines;
import net.skycore.client.util.AtmospherePipelines;
import net.skycore.client.util.FocusBlurPipeline;
import net.skycore.client.util.FogBlurPipeline;
import net.skycore.client.util.MotionBlurPipelines;
import net.skycore.client.util.LoadMeshHelper;
import net.skycore.client.util.TargetEffectPipelines;
import net.skycore.client.util.ThunderPipelines;
import net.skycore.client.util.AuraMarkerRenderer;
import net.skycore.client.util.BrushstrokeTrailRenderer;
import net.skycore.client.util.RenderLinesHelper;
import net.skycore.client.util.SampleSurfaceHelper;
import net.skycore.client.util.TargetScanRenderer;
import net.skycore.client.util.WorldSkyPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPipelines.class)
public abstract class RenderPipelinesMixin {
   @Shadow
   private static RenderPipeline register(RenderPipeline renderPipeline) {
      throw new AssertionError();
   }

   @Inject(method = "<clinit>", at = @At("TAIL"))
   private static void registerSkycorePipelines(CallbackInfo callbackInfo) {
      register(GuiPipelines.RECT);
      register(GuiPipelines.RING);
      register(GuiPipelines.TEXT);
      register(GuiPipelines.TEXTURE);
      register(GuiPipelines.GLASS_SHADOW);
      register(GuiPipelines.BLUR_RECT);
      register(GuiPipelines.MATTE_BLUR_RECT);
      register(GuiPipelines.COLOR_GRID);
      register(GuiPipelines.MENU_BACKGROUND);
      register(GuiPipelines.GUI_BLUR_DOWN);
      register(GuiPipelines.GUI_BLUR_UP);
      register(GuiPipelines.GUI_MATTE_BLUR_DOWN);
      register(GuiPipelines.GUI_MATTE_BLUR_UP);

      for (RenderPipeline renderPipeline : GuiPipelines.itemDownscaleVariants()) {
         register(renderPipeline);
      }

      register(AuraMarkerRenderer.pipeline());

      for (RenderPipeline renderPipeline2 : TargetScanRenderer.pipelines()) {
         register(renderPipeline2);
      }

      for (RenderPipeline renderPipeline3 : TargetEffectPipelines.all()) {
         register(renderPipeline3);
      }

      for (RenderPipeline renderPipeline4 : LoadMeshHelper.pipelines()) {
         register(renderPipeline4);
      }

      for (RenderPipeline renderPipeline5 : TargetParticleRenderer.pipelines()) {
         register(renderPipeline5);
      }

      for (RenderPipeline renderPipeline6 : WorldParticleRenderer.pipelines()) {
         register(renderPipeline6);
      }

      for (RenderPipeline renderPipeline7 : WorldSkyPipelines.all()) {
         register(renderPipeline7);
      }

      register(AddQuadHelper.pipeline());
      register(SampleSurfaceHelper.pipeline());
      register(BrushstrokeTrailRenderer.pipeline());

      for (RenderPipeline renderPipeline8 : ThunderPipelines.pipelines()) {
         register(renderPipeline8);
      }

      register(RenderLinesHelper.pipeline());
      register(FogBlurPipeline.FOG_BLUR);
      register(FocusBlurPipeline.FOCUS_BLUR);
      register(AtmospherePipelines.FOG);
      register(AtmospherePipelines.RAIN_GROUND);
      register(AtmospherePipelines.RAIN_STREAK);
      register(MotionBlurPipelines.MOTION_BLUR);
      register(MotionBlurPipelines.MOTION_BLUR_BLIT);
   }
}
