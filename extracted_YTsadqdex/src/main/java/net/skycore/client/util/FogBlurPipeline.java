package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class FogBlurPipeline {
   private static final BindGroupLayout FOG_BLUR_LAYOUT = BindGroupLayout.builder()
      .withSampler("SceneSampler")
      .withSampler("BlurSampler")
      .withSampler("DepthSampler")
      .withUniform("FogBlurUniforms", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline FOG_BLUR = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/fog_blur"))
      .withVertexShader(Identifier.parse("skycore:post/blurs/kawase_common"))
      .withFragmentShader(Identifier.parse("skycore:post/fog/fog_blur"))
      .withBindGroupLayout(FOG_BLUR_LAYOUT)
      .withColorTargetState(ColorTargetState.DEFAULT)
      .withDepthStencilState(Optional.empty())
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();
   public static final String FOG_BLUR_UBO = "FogBlurUniforms";

   private FogBlurPipeline() {
   }
}
