package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class FocusBlurPipeline {
   private static final BindGroupLayout FOCUS_BLUR_LAYOUT = BindGroupLayout.builder()
      .withSampler("SceneSampler")
      .withSampler("BlurSampler")
      .withSampler("DepthSampler")
      .withUniform("FocusBlurUniforms", UniformType.UNIFORM_BUFFER)
      .build();
   public static final String FOCUS_BLUR_UBO = "FocusBlurUniforms";
   public static final RenderPipeline FOCUS_BLUR = RenderPipeline.builder()
      .withLocation(Identifier.parse("skycore:pipeline/focus_blur"))
      .withVertexShader(Identifier.parse("skycore:post/blurs/kawase_common"))
      .withFragmentShader(Identifier.parse("skycore:post/focus/focus_blur"))
      .withBindGroupLayout(FOCUS_BLUR_LAYOUT)
      .withColorTargetState(ColorTargetState.DEFAULT)
      .withDepthStencilState(Optional.empty())
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withCull(false)
      .build();

   private FocusBlurPipeline() {
   }
}
