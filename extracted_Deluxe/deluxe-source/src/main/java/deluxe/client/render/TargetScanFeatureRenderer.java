package deluxe.client.render;

import deluxe.client.DeluxeClient;
import deluxe.client.module.render.TargetEspModule;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.function.Function;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public final class TargetScanFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   private static final RenderPipeline field09335 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(DeluxeClient.id("pipeline/target_scan_surface"))
      .withVertexShader(DeluxeClient.id("core/target_scan_surface"))
      .withFragmentShader(DeluxeClient.id("core/target_scan_surface"))
      .withSampler("Sampler0")
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(true)
      .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS)
      .build();
   private static final Function<Identifier, RenderLayer> field09336 = Util.memoize(
      identifier -> RenderLayer.of("target_scan_surface", RenderSetup.builder(field09335).texture("Sampler0", identifier).expectedBufferSize(32768).build())
   );

   private void emitModelMesh(MatrixStack matrixStack, VertexConsumer vertexConsumer) {
      this.getContextModel().head.render(matrixStack, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV, -1);
      this.getContextModel().body.render(matrixStack, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV, -1);
      this.getContextModel().rightArm.render(matrixStack, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV, -1);
      this.getContextModel().leftArm.render(matrixStack, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV, -1);
      this.getContextModel().rightLeg.render(matrixStack, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV, -1);
      this.getContextModel().leftLeg.render(matrixStack, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV, -1);
   }

   public void render(
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int local4258_3,
      PlayerEntityRenderState playerEntityRenderState,
      float local4258_5,
      float local4258_6
   ) {
      if (MinecraftClient.getInstance().world != null && playerEntityRenderState.skinTextures != null) {
         if (MinecraftClient.getInstance().world.getEntityById(playerEntityRenderState.id) instanceof PlayerEntity playerEntity) {
            TargetEspModule.Nested0418 local4258_8 = TargetEspModule.method1936().method1911(playerEntity);
            if (local4258_8 != null && !(local4258_8.alpha() <= 0.001F)) {
               orderedRenderCommandQueue.getBatchingQueue(20)
                  .submitCustom(matrixStack, field09336.apply(playerEntityRenderState.skinTextures.body().texturePath()), (entry, vertexConsumer) -> {
                     MatrixStack matrixStack2 = new MatrixStack();
                     matrixStack2.peek().copy(entry);
                     matrixStack2.scale(1.008F - local4258_8.hit() * 0.018F, 1.008F - local4258_8.hit() * 0.018F, 1.008F - local4258_8.hit() * 0.018F);
                     this.getContextModel().setAngles(playerEntityRenderState);
                     TargetScanFeatureRenderer.Nested0451 vertexConsumer2 = new TargetScanFeatureRenderer.Nested0451();
                     this.emitModelMesh(matrixStack2, vertexConsumer2);
                     if (vertexConsumer2.validSpan()) {
                        this.emitModelMesh(matrixStack2, new TargetScanFeatureRenderer.Nested0450(vertexConsumer, vertexConsumer2, local4258_8));
                     }
                  });
            }
         }
      }
   }

   public TargetScanFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> local4260_1) {
      super(local4260_1);
   }

   private static final class Nested0450 implements VertexConsumer {
      private VertexConsumer field09355;
      private TargetScanFeatureRenderer.Nested0451 field09356;
      private int field09357;
      private int field09358;
      private int field09359;
      private int field09360;
      private int field09361;
      private int field09362;
      private int field09363;
      private float field09364;
      private float field09365;

      private static float spanPC(float local4264_0, float local4264_1, float local4264_2) {
         return MathHelper.clamp((local4264_0 - local4264_1) / Math.max(1.0E-4F, local4264_2 - local4264_1), 0.0F, 1.0F);
      }

      private Nested0450(VertexConsumer vertexConsumer, TargetScanFeatureRenderer.Nested0451 local4265_2, TargetEspModule.Nested0418 local4265_3) {
         this.field09355 = vertexConsumer;
         this.field09356 = local4265_2;
         this.field09357 = local4265_3.primaryColor() >>> 16 & 0xFF;
         this.field09358 = local4265_3.primaryColor() >>> 8 & 0xFF;
         this.field09359 = local4265_3.primaryColor() & 0xFF;
         this.field09360 = (local4265_3.secondaryColor() >>> 16 & 0xFF) << 8 | Math.round(MathHelper.clamp(local4265_3.phase(), 0.0F, 1.0F) * 255.0F);
         this.field09361 = (local4265_3.secondaryColor() >>> 8 & 0xFF) << 8 | Math.round(MathHelper.clamp(local4265_3.alpha(), 0.0F, 1.0F) * 255.0F);
         this.field09362 = (local4265_3.secondaryColor() & 0xFF) << 8 | Math.round(MathHelper.clamp(local4265_3.glow(), 0.0F, 2.0F) * 127.5F);
         this.field09363 = Math.round(MathHelper.clamp(local4265_3.hit(), 0.0F, 1.0F) * 255.0F) << 8;
      }

      @Override
      public VertexConsumer color(int local4266_1, int local4266_2, int local4266_3, int local4266_4) {
         return this.setScanColor();
      }

      @Override
      public VertexConsumer normal(float local4267_1, float local4267_2, float local4267_3) {
         this.field09355.normal(local4267_1, local4267_2, local4267_3);
         return this;
      }

      @Override
      public VertexConsumer color(int local4268_1) {
         return this.setScanColor();
      }

      @Override
      public VertexConsumer light(int local4269_1, int local4269_2) {
         this.field09355.light(this.field09362, this.field09363 | Math.round(spanPC(this.field09364, this.field09356.field09373, this.field09356.__) * 255.0F));
         return this;
      }

      @Override
      public VertexConsumer overlay(int local4270_1, int local4270_2) {
         this.field09355.overlay(this.field09360, this.field09361);
         return this;
      }

      @Override
      public VertexConsumer vertex(float local4271_1, float local4271_2, float local4271_3) {
         this.field09364 = local4271_1;
         this.field09365 = local4271_2;
         this.field09355.vertex(local4271_1, local4271_2, local4271_3);
         return this;
      }

      @Override
      public VertexConsumer lineWidth(float local4272_1) {
         this.field09355.lineWidth(local4272_1);
         return this;
      }

      @Override
      public VertexConsumer texture(float local4273_1, float local4273_2) {
         this.field09355.texture(local4273_1, local4273_2);
         return this;
      }

      private VertexConsumer setScanColor() {
         this.field09355
            .color(
               this.field09357,
               this.field09358,
               this.field09359,
               Math.round(spanPC(this.field09365, this.field09356.field09374, this.field09356.field09375) * 255.0F)
            );
         return this;
      }
   }

   private static final class Nested0451 implements VertexConsumer {
      private float field09373 = Float.POSITIVE_INFINITY;
      private float field09374 = Float.POSITIVE_INFINITY;
      private float __ = Float.NEGATIVE_INFINITY;
      private float field09375 = Float.NEGATIVE_INFINITY;

      @Override
      public VertexConsumer overlay(int local4275_1, int local4275_2) {
         return this;
      }

      @Override
      public VertexConsumer normal(float local4276_1, float local4276_2, float local4276_3) {
         return this;
      }

      @Override
      public VertexConsumer vertex(float local4277_1, float local4277_2, float local4277_3) {
         this.field09373 = Math.min(this.field09373, local4277_1);
         this.field09374 = Math.min(this.field09374, local4277_2);
         this.__ = Math.max(this.__, local4277_1);
         this.field09375 = Math.max(this.field09375, local4277_2);
         return this;
      }

      @Override
      public VertexConsumer texture(float local4278_1, float local4278_2) {
         return this;
      }

      @Override
      public VertexConsumer light(int local4279_1, int local4279_2) {
         return this;
      }

      @Override
      public VertexConsumer color(int local4280_1, int local4280_2, int local4280_3, int local4280_4) {
         return this;
      }

      private boolean validSpan() {
         return Float.isFinite(this.field09373)
            && Float.isFinite(this.field09374)
            && this.__ - this.field09373 > 1.0E-4F
            && this.field09375 - this.field09374 > 1.0E-4F;
      }

      @Override
      public VertexConsumer lineWidth(float local4283_1) {
         return this;
      }

      @Override
      public VertexConsumer color(int local4284_1) {
         return this;
      }
   }
}
