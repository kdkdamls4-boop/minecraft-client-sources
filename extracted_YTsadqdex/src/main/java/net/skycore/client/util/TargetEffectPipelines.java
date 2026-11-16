package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class TargetEffectPipelines {
   private static final BindGroupLayout FIRE_LAYOUT = BindGroupLayout.builder()
      .withSampler("SceneSampler")
      .withSampler("BlurSampler")
      .withSampler("MaskSampler")
      .withUniform("HandFireData", UniformType.UNIFORM_BUFFER)
      .build();
   private static final BindGroupLayout MASK_LAYOUT = BindGroupLayout.builder()
      .withSampler("BeforeSampler")
      .withSampler("AfterSampler")
      .withSampler("DepthBeforeSampler")
      .withSampler("DepthAfterSampler")
      .withUniform("MaskData", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline MASK = pipeline("soul_mask", TargetEffectPipelines.MASK_LAYOUT);
   public static final RenderPipeline FIRE = pipeline("soul_fire", FIRE_LAYOUT);
   private static final BindGroupLayout TRAIL_LAYOUT = BindGroupLayout.builder()
      .withSampler("PrevTrailSampler")
      .withSampler("SceneSampler")
      .withSampler("MaskSampler")
      .withUniform("HandTrailData", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline TRAIL = pipeline("soul_trail", TargetEffectPipelines.TRAIL_LAYOUT);
   private static final BindGroupLayout BLUR_LAYOUT = BindGroupLayout.builder()
      .withSampler("Sampler0")
      .withUniform("HandBlurData", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline BLUR = pipeline("soul_blur", TargetEffectPipelines.BLUR_LAYOUT);

   public static List<RenderPipeline> all() {
      return List.of(MASK, TRAIL, BLUR, FIRE);
   }

   private static RenderPipeline pipeline(String text, BindGroupLayout bindGroupLayout) {
      Builder builder = RenderPipeline.builder();
      builder = builder.withLocation(Identifier.parse("skycore:pipeline/hands/" + text));
      builder = builder.withVertexShader(Identifier.parse("skycore:post/blurs/kawase_common"));
      builder = builder.withFragmentShader(Identifier.parse("skycore:post/hands/" + text));
      builder = builder.withBindGroupLayout(bindGroupLayout);
      builder = builder.withColorTargetState(ColorTargetState.DEFAULT);
      Optional<com.mojang.blaze3d.pipeline.DepthStencilState> optional = Optional.empty();
      builder = builder.withDepthStencilState(optional);
      builder = builder.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX);
      return builder.withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false).build();
   }

   private TargetEffectPipelines() {
   }
}
