package net.skycore.client.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.Optional;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class GuiPipelines {
   public static final RenderPipeline MATTE_BLUR_RECT = builder("matte_blur_rect", "blur_rect", true).build();
   public static final RenderPipeline BLUR_RECT = builder("blur_rect", "blur_rect", true).build();
   private static final BindGroupLayout GUI_BLUR_LAYOUT = BindGroupLayout.builder()
      .withSampler("CurrentInput")
      .withUniform("GuiKawaseUniforms", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline GUI_BLUR_UP = blurBuilder("gui_blur_up", "gui_kawase_up").build();
   public static final RenderPipeline COLOR_GRID = builder("color_grid", "color_grid", true).build();
   public static final RenderPipeline TEXTURE = builder("texture", "texture", true).build();
   public static final RenderPipeline GUI_BLUR_DOWN = blurBuilder("gui_blur_down", "gui_kawase_down").build();
   public static final RenderPipeline GLASS_SHADOW = builder("glass_shadow", "rect", false).build();
   public static final RenderPipeline GUI_MATTE_BLUR_UP = blurBuilder("gui_matte_blur_up", "matte_kawase_up").build();
   public static final RenderPipeline TEXTURE_TRIANGLES = triangleBuilder("texture_triangles", "texture", true).build();
   public static final RenderPipeline GUI_MATTE_BLUR_DOWN = blurBuilder("gui_matte_blur_down", "matte_kawase_down").build();
   public static final RenderPipeline RECT = builder("rect", "rect", false).build();
   private static final RenderPipeline[] ITEM_DOWNSCALE = new RenderPipeline[]{
      itemDownscalePipeline(1),
      itemDownscalePipeline(2),
      itemDownscalePipeline(3),
      itemDownscalePipeline(4),
      itemDownscalePipeline(5),
      itemDownscalePipeline(6),
      itemDownscalePipeline(7),
      itemDownscalePipeline(8)
   };
   public static final RenderPipeline RING = builder("ring", "ring", false).build();
   public static final RenderPipeline TEXT = builder("text", "text", true).build();
   public static final RenderPipeline MENU_BACKGROUND = builder("menu_background", "menu_background", false).build();

   private static Builder blurBuilder(String text, String text2) {
      Snippet[] snippetValues = new Snippet[0];
      Builder builder = RenderPipeline.builder(snippetValues).withLocation(Identifier.parse("skycore:gui/" + text));
      Identifier texture = Identifier.parse("skycore:post/blurs/kawase_common");
      Builder builder2 = builder.withVertexShader(texture);
      String text3 = "skycore:post/blurs/" + text2;
      Identifier texture2 = Identifier.parse(text3);
      Builder builder3 = builder2.withFragmentShader(texture2).withBindGroupLayout(GUI_BLUR_LAYOUT);
      Builder builder4 = builder3.withColorTargetState(ColorTargetState.DEFAULT);
      Optional<com.mojang.blaze3d.pipeline.DepthStencilState> optional = Optional.empty();
      Builder builder5 = builder4.withDepthStencilState(optional);
      Builder builder6 = builder5.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX);
      return builder6.withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false);
   }

   private static Builder builder(String text, String text2, boolean condition) {
      return uiBuilder(text, text2, condition, PrimitiveTopology.QUADS);
   }

   public static RenderPipeline[] itemDownscaleVariants() {
      return (RenderPipeline[])ITEM_DOWNSCALE.clone();
   }

   public static RenderPipeline itemDownscale(int value) {
      return ITEM_DOWNSCALE[Math.clamp(value, 1, ITEM_DOWNSCALE.length) - 1];
   }

   private GuiPipelines() {
   }

   private static Builder triangleBuilder(String text, String text2, boolean condition) {
      return uiBuilder(text, text2, condition, PrimitiveTopology.TRIANGLES);
   }

   private static RenderPipeline itemDownscalePipeline(int value) {
      Snippet[] snippetValues = new Snippet[]{net.skycore.client.mixin.RenderPipelinesSnippetAccessor.skycore$getGuiTexturedSnippet()};
      Builder builder = RenderPipeline.builder(snippetValues);
      String text = "skycore:gui/item_downscale_" + value;
      Identifier texture = Identifier.parse(text);
      Builder builder2 = builder.withLocation(texture);
      Identifier texture2 = Identifier.parse("skycore:core/item_downscale");
      Builder builder3 = builder2.withVertexShader(texture2);
      Identifier texture3 = Identifier.parse("skycore:core/item_downscale");
      Builder builder4 = builder3.withFragmentShader(texture3).withShaderDefine("TEXEL_SIZE", value);
      ColorTargetState colorTargetState = new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA);
      return builder4.withColorTargetState(colorTargetState).build();
   }

   private static Builder uiBuilder(String text, String text2, boolean condition, PrimitiveTopology primitiveTopology) {
      Builder builder = RenderPipeline.builder(net.skycore.client.mixin.RenderPipelinesSnippetAccessor.skycore$getGuiSnippet());
      String text3 = "skycore:gui/" + text;
      Identifier texture = Identifier.parse(text3);
      Builder builder2 = builder.withLocation(texture);
      Identifier texture2 = Identifier.parse("skycore:core/" + text2);
      Builder builder3 = builder2.withVertexShader(texture2);
      String text4 = "skycore:core/" + text2;
      Builder builder4 = builder3.withFragmentShader(Identifier.parse(text4));
      ColorTargetState colorTargetState = new ColorTargetState(BlendFunction.TRANSLUCENT);
      Builder builder5 = builder4.withColorTargetState(colorTargetState);
      DepthStencilState depthStencilState = new DepthStencilState(CompareOp.ALWAYS_PASS, false);
      Builder builder6 = builder5.withDepthStencilState(depthStencilState).withVertexBinding(0, UiVertexFormats.UI).withPrimitiveTopology(primitiveTopology);
      Builder builder7 = builder6.withCull(false);
      if (condition) {
         builder7.withBindGroupLayout(BindGroupLayouts.SAMPLER0);
      }

      return builder7;
   }
}
