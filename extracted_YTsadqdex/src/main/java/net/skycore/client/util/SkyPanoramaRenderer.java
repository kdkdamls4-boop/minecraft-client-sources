package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.skycore.client.module.render.WorldTweaksModule;
import org.lwjgl.system.MemoryStack;

public final class SkyPanoramaRenderer implements AutoCloseable {
   private GpuBuffer uniforms;
   public static final RenderPipeline PIPELINE = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/sky_environment"))
      .withVertexShader(Identifier.parse("skycore:post/blurs/kawase_common"))
      .withFragmentShader(Identifier.parse("skycore:world/sky/environment"))
      .withBindGroupLayout(BindGroupLayout.builder().withUniform("SkyEnvironmentUniforms", UniformType.UNIFORM_BUFFER).build())
      .withColorTargetState(ColorTargetState.DEFAULT)
      .withDepthStencilState(Optional.empty())
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private TextureTarget panorama;

   public GpuTextureView render(WorldTweaksModule worldTweaksModule) {
      int elapsed = mode(worldTweaksModule.skyEffect());
      if (elapsed < 0) {
         return null;
      }

      if (this.panorama == null) {
         this.panorama = new TextureTarget("Sky reflection panorama", 256, 128, false, WorldSkyPipelines.EFFECT_FORMAT);
      }

      if (this.uniforms == null) {
         this.uniforms = RenderSystem.getDevice().createBuffer(() -> {
            return "Sky environment uniforms";
         }, 136, 96L);
      }

      int value = worldTweaksModule.primarySkyColor();
      int value2 = worldTweaksModule.secondarySkyColor();
      RenderSkyHelper.Settings settings = worldTweaksModule.sourceSkySettings();

      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, 96)
            .putVec4(WriteHexByteHelper.red(value) / 255.0F, WriteHexByteHelper.green(value) / 255.0F, WriteHexByteHelper.blue(value) / 255.0F, 1.0F)
            .putVec4(WriteHexByteHelper.red(value2) / 255.0F, WriteHexByteHelper.green(value2) / 255.0F, WriteHexByteHelper.blue(value2) / 255.0F, 1.0F)
            .putVec4(elapsed == 4 ? RenderSkyOverlayHelper.siyanieTime() : RenderSkyOverlayHelper.shaderTime(), worldTweaksModule.skyIntensity(), worldTweaksModule.skySpeed(), elapsed)
            .putVec4(worldTweaksModule.siyanieSoftness(), worldTweaksModule.siyanieCoverage(), settings.scale(), settings.night() ? 1.0F : 0.0F)
            .putVec4(settings.speed(), settings.outline(), settings.glow(), settings.fill())
            .putVec4((float)(System.currentTimeMillis() % 100000L) / 1000.0F, settings.alpha(), 0.0F, 0.0F)
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.uniforms.slice(), byteBuffer);
      }

      try (RenderPass device = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
            return "Sky reflection panorama";
         }, this.panorama.getColorTextureView(), Optional.empty())) {
         device.setPipeline(PIPELINE);
         device.setUniform("SkyEnvironmentUniforms", this.uniforms);
         device.setVertexBuffer(0, EnsureHelper.buffer().slice());
         device.draw(EnsureHelper.vertexCount(), 1, 0, 0);
      }

      return this.panorama.getColorTextureView();
   }

   @Override
   public void close() {
      if (this.panorama != null) {
         this.panorama.destroyBuffers();
         this.panorama = null;
      }

      if (this.uniforms != null) {
         this.uniforms.close();
         this.uniforms = null;
      }
   }

   public static int mode(String text) {
      return switch (text) {
         case "Deep Space" -> 0;
         case "Nebula" -> 1;
         case "Plasma" -> 2;
         case "Clouds" -> 3;
         case "Aurora" -> 4;
         case "SkyCore Plasma" -> 5;
         case "SkyCore Summer" -> 6;
         default -> -1;
      };
   }
}
