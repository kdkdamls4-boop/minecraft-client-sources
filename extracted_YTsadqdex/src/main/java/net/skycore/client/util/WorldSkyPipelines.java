package net.skycore.client.util;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class WorldSkyPipelines {
   private static final BindGroupLayout WORLD_SKY_CLOUDS_LAYOUT = BindGroupLayout.builder()
      .withSampler("DepthSampler")
      .withUniform("WorldSkyUniforms", UniformType.UNIFORM_BUFFER)
      .build();
   private static final BindGroupLayout WORLD_SKY_LAYOUT = BindGroupLayout.builder()
      .withSampler("DepthSampler")
      .withSampler("CloudSampler")
      .withUniform("WorldSkyUniforms", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline WORLD_SKY_VOLUMETRIC_CLOUDS = worldPostPipeline(
      "sky_volumetric_clouds", "skycore:world/sky/volumetric_clouds", WorldSkyPipelines.WORLD_SKY_LAYOUT
   );
   public static final RenderPipeline WORLD_SKY_SIYANIE = worldPostPipeline("sky_siyanie", "skycore:world/sky/siyanie", WORLD_SKY_CLOUDS_LAYOUT);
   public static final GpuFormat SKY_CLOUDS_FORMAT = GpuFormat.RGBA16_FLOAT;
   public static final RenderPipeline WORLD_SKY_CLOUDS_PLASMA = skyCloudsPipeline("sky_clouds_plasma", "skycore:world/sky/clouds_plasma");
   public static final RenderPipeline WORLD_SKY_DEEP_SPACE = worldPostPipeline(
      "sky_deep_space", "skycore:world/sky/deep_space", WorldSkyPipelines.WORLD_SKY_LAYOUT
   );
   public static final RenderPipeline WORLD_SKY_CLOUDS_VOLUMETRIC = skyCloudsPipeline("sky_clouds_volumetric", "skycore:world/sky/clouds_volumetric");
   public static final RenderPipeline WORLD_SKY_NEBULA = worldPostPipeline("sky_nebula", "skycore:world/sky/nebula", WORLD_SKY_LAYOUT);
   public static final RenderPipeline WORLD_SKY_CLOUDS_DEEP_SPACE = skyCloudsPipeline("sky_clouds_deep_space", "skycore:world/sky/clouds_deep_space");
   private static final BindGroupLayout SATURATION_LAYOUT = BindGroupLayout.builder()
      .withSampler("SceneSampler")
      .withUniform("SaturationUniforms", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline WORLD_SKY_PLASMA = worldPostPipeline("sky_plasma", "skycore:world/sky/plasma", WORLD_SKY_LAYOUT);
   public static final RenderPipeline WORLD_SKY_CLOUDS_NEBULA = skyCloudsPipeline("sky_clouds_nebula", "skycore:world/sky/clouds_nebula");
   public static final RenderPipeline WORLD_SATURATION = worldPostPipeline("saturation", "skycore:post/world_saturation", SATURATION_LAYOUT);
   public static final GpuFormat EFFECT_FORMAT = GpuFormat.RGBA8_UNORM;

   private WorldSkyPipelines() {
   }

   public static RenderPipeline[] all() {
      RenderPipeline[] renderPipelineValues = new RenderPipeline[]{
         SkyPanoramaRenderer.PIPELINE,
         WorldSkyRenderer.pipelines()[0],
         WorldSkyRenderer.pipelines()[1],
         WorldSkyRenderer.pipelines()[2],
         RenderSkyHelper.BLOCK,
         RenderSkyHelper.PLASMA,
         RenderSkyHelper.SUMMER,
         null,
         null,
         null,
         null,
         null,
         null,
         null,
         null,
         null,
         null
      };
      renderPipelineValues[7] = WORLD_SKY_CLOUDS_DEEP_SPACE;
      renderPipelineValues[8] = WORLD_SKY_CLOUDS_NEBULA;
      renderPipelineValues[9] = WORLD_SKY_CLOUDS_PLASMA;
      renderPipelineValues[10] = WORLD_SKY_CLOUDS_VOLUMETRIC;
      renderPipelineValues[11] = WORLD_SKY_DEEP_SPACE;
      renderPipelineValues[12] = WORLD_SKY_NEBULA;
      renderPipelineValues[13] = WORLD_SKY_PLASMA;
      renderPipelineValues[14] = WORLD_SKY_VOLUMETRIC_CLOUDS;
      renderPipelineValues[15] = WORLD_SKY_SIYANIE;
      renderPipelineValues[16] = WORLD_SATURATION;
      return renderPipelineValues;
   }

   private static RenderPipeline worldPostPipeline(String text, String text2, BindGroupLayout bindGroupLayout) {
      Builder builder = RenderPipeline.builder();
      Identifier texture = Identifier.parse("skycore:pipeline/world/" + text);
      Builder builder2 = builder.withLocation(texture);
      Identifier texture2 = Identifier.parse("skycore:post/blurs/kawase_common");
      Builder builder3 = builder2.withVertexShader(texture2);
      Identifier texture3 = Identifier.parse(text2);
      Builder builder4 = builder3.withFragmentShader(texture3).withBindGroupLayout(bindGroupLayout);
      Builder builder5 = builder4.withColorTargetState(ColorTargetState.DEFAULT).withDepthStencilState(Optional.empty());
      Builder builder6 = builder5.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX);
      Builder builder7 = builder6.withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false);
      return builder7.build();
   }

   private static RenderPipeline skyCloudsPipeline(String text, String text2) {
      Builder builder = RenderPipeline.builder();
      Identifier texture = Identifier.parse("skycore:pipeline/world/" + text);
      Builder builder2 = builder.withLocation(texture);
      Identifier texture2 = Identifier.parse("skycore:post/blurs/kawase_common");
      Builder builder3 = builder2.withVertexShader(texture2);
      Builder builder4 = builder3.withFragmentShader(Identifier.parse(text2));
      Builder builder5 = builder4.withBindGroupLayout(WORLD_SKY_CLOUDS_LAYOUT);
      Builder builder6 = builder5.withColorTargetState(new ColorTargetState(Optional.empty(), SKY_CLOUDS_FORMAT, 15));
      Optional<com.mojang.blaze3d.pipeline.DepthStencilState> optional = Optional.empty();
      Builder builder7 = builder6.withDepthStencilState(optional);
      return builder7.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false).build();
   }
}
