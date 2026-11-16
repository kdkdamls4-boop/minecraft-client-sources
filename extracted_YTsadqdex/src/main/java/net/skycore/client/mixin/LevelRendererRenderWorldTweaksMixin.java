package net.skycore.client.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.render.AtmosphereModule;
import net.skycore.client.module.render.BlockOverlayModule;
import net.skycore.client.module.render.FocusModule;
import net.skycore.client.module.render.FogModule;
import net.skycore.client.module.render.MotionBlurModule;
import net.skycore.client.module.render.ParticlesModule;
import net.skycore.client.module.render.WorldTweaksModule;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererRenderWorldTweaksMixin {
   @Inject(method = "render", at = @At("TAIL"))
   private void skycore$renderWorldTweaks(
      GraphicsResourceAllocator graphicsResourceAllocator,
      DeltaTracker deltaTracker,
      boolean condition,
      CameraRenderState cameraRenderState,
      Matrix4fc matrix4fc,
      GpuBufferSlice gpuBufferSlice,
      Vector4f vector4f,
      boolean condition2,
      CallbackInfo callbackInfo
   ) {
      WorldTweaksModule worldTweaksModule = WorldTweaksModule.enabled();
      if (worldTweaksModule != null) {
         worldTweaksModule.renderWorldEnd(cameraRenderState);
      }

      AtmosphereModule atmosphereModule = AtmosphereModule.enabled();
      if (atmosphereModule != null) {
         atmosphereModule.render(cameraRenderState);
      }

      FogModule fogModule = FogModule.enabled();
      if (fogModule != null && (atmosphereModule == null || !atmosphereModule.ownsFog())) {
         fogModule.blurRenderer().render(cameraRenderState);
      }

      FocusModule focusModule = FocusModule.enabled();
      if (focusModule != null) {
         focusModule.renderer().render(cameraRenderState);
      }

      SkycoreClient.modules().get(MotionBlurModule.class).ifPresent(motionBlurModule -> motionBlurModule.renderer().render());
      ParticlesModule.INSTANCE.render();
      SkycoreClient.modules().get(BlockOverlayModule.class).ifPresent(blockOverlayModule -> blockOverlayModule.render(cameraRenderState));
   }
}
