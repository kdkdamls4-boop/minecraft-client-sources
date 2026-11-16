package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.Optional;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;

public final class AtmospherePipelines {
   public static final RenderPipeline RAIN_STREAK = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/atmosphere_rain_streak"))
      .withVertexShader(Identifier.parse("skycore:core/rain_streak"))
      .withFragmentShader(Identifier.parse("skycore:core/rain_streak"))
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   public static final String FOG_UBO = "AtmosphereUniforms";
   public static final String RAIN_GROUND_UBO = "RainGroundUniforms";
   private static final BindGroupLayout FOG_LAYOUT = BindGroupLayout.builder()
      .withSampler("SkyEnvironmentSampler")
      .withSampler("SceneSampler")
      .withSampler("DepthSampler")
      .withSampler("BlurSampler")
      .withUniform("AtmosphereUniforms", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline FOG = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/atmosphere_fog"))
      .withVertexShader(Identifier.parse("skycore:post/blurs/kawase_common"))
      .withFragmentShader(Identifier.parse("skycore:post/atmosphere/atmospheric_fog"))
      .withBindGroupLayout(FOG_LAYOUT)
      .withColorTargetState(ColorTargetState.DEFAULT)
      .withDepthStencilState(Optional.empty())
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private static final BindGroupLayout RAIN_GROUND_LAYOUT = BindGroupLayout.builder()
      .withSampler("SkyEnvironmentSampler")
      .withSampler("SceneSampler")
      .withSampler("DepthSampler")
      .withUniform("RainGroundUniforms", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline RAIN_GROUND = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/atmosphere_rain_ground"))
      .withVertexShader(Identifier.parse("skycore:post/blurs/kawase_common"))
      .withFragmentShader(Identifier.parse("skycore:post/atmosphere/rain_ground"))
      .withBindGroupLayout(AtmospherePipelines.RAIN_GROUND_LAYOUT)
      .withColorTargetState(ColorTargetState.DEFAULT)
      .withDepthStencilState(Optional.empty())
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();

   private AtmospherePipelines() {
   }
}
