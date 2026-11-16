package net.skycore.client.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fc;

public final class TexturedTriangleRenderState extends ScissorAreaHelper {
   private final int color;
   private final Identifier texture;
   private final float[] vertices;

   @Override
   public RenderPipeline pipeline() {
      return GuiPipelines.TEXTURE_TRIANGLES;
   }

   @Override
   public void buildVertices(VertexConsumer vertexConsumer) {
      int vertexIndex = 0;

      while (true) {
         int intermediate = vertexIndex + 3;
         if (intermediate >= this.vertices.length) {
            return;
         }

         float value = this.vertices[vertexIndex];
         float computedValue = this.vertices[vertexIndex + 1];
         float computedValue2 = this.vertices[vertexIndex + 2];
         float computedValue3 = this.vertices[vertexIndex + 3];
         float value2 = this.transformX(value, computedValue);
         float value3 = this.transformY(value, computedValue);
         VertexConsumer vertexConsumer2 = vertexConsumer.addVertex(value2, value3, 0.0F).setColor(this.color);
         VertexConsumer vertexConsumer3 = vertexConsumer2.setUv(computedValue2, computedValue3);
         VertexConsumer vertexConsumer4 = vertexConsumer3.setUv1(0, 0);
         VertexConsumer vertexConsumer5 = vertexConsumer4.setUv2(0, 0);
         VertexConsumer vertexConsumer6 = vertexConsumer5.setNormal(0.0F, 0.0F, 1.0F);
         vertexConsumer6.setLineWidth(0.0F);
         vertexIndex += 4;
      }
   }

   public TexturedTriangleRenderState(Matrix3x2fc matrix3x2fc, Identifier texture2, float[] vertices2, int color2, float value, float value2, float value3, float value4, ScreenRectangle screenRectangle) {
      super(matrix3x2fc, screenRectangle, value, value2, value3, value4);
      this.texture = texture2;
      this.vertices = vertices2;
      this.color = color2;
   }

   @Override
   public TextureSetup textureSetup() {
      TextureManager textureManager = MinecraftContext.mc.getTextureManager();
      AbstractTexture texture2 = textureManager.getTexture(this.texture);
      GpuTextureView textureView = texture2.getTextureView();
      SamplerCache samplerCache = RenderSystem.getSamplerCache();
      GpuSampler clampToEdge = samplerCache.getClampToEdge(FilterMode.NEAREST, false);
      return TextureSetup.singleTexture(textureView, clampToEdge);
   }
}
