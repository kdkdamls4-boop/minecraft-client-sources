package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class MotionBlurPipelines {
   private static final BindGroupLayout BLEND_LAYOUT = BindGroupLayout.builder()
      .withSampler("CurrentSampler")
      .withSampler("HistorySampler")
      .withUniform("MotionBlurParams", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline MOTION_BLUR = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/motion_blur"))
      .withVertexShader(Identifier.parse("skycore:post/blurs/kawase_common"))
      .withFragmentShader(Identifier.parse("skycore:post/motion_blur"))
      .withBindGroupLayout(MotionBlurPipelines.BLEND_LAYOUT)
      .withColorTargetState(ColorTargetState.DEFAULT)
      .withDepthStencilState(Optional.empty())
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   private static final BindGroupLayout BLIT_LAYOUT = BindGroupLayout.builder().withSampler("CurrentSampler").build();
   public static final RenderPipeline MOTION_BLUR_BLIT = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/motion_blur_blit"))
      .withVertexShader(Identifier.parse("skycore:post/blurs/kawase_common"))
      .withFragmentShader(Identifier.parse("skycore:post/motion_blur_blit"))
      .withBindGroupLayout(MotionBlurPipelines.BLIT_LAYOUT)
      .withColorTargetState(ColorTargetState.DEFAULT)
      .withDepthStencilState(Optional.empty())
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   public static final String MOTION_BLUR_UBO = "MotionBlurParams";

   private MotionBlurPipelines() {
   }
}
