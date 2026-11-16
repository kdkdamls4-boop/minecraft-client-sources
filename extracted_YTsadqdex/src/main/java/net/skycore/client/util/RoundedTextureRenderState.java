package net.skycore.client.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fc;

public final class RoundedTextureRenderState extends ScissorAreaHelper {
   private final float u0;
   private final float cornerRadius;
   private final float y;
   private final SvgTextureCache texture;
   private final int color;
   private final float height;
   private final float v0;
   private final float width;
   private final float x;
   private final float deviceScale;
   private final float u1;
   private final float v1;
   private final Identifier managedTextureId;

   public RoundedTextureRenderState(
      Matrix3x2fc matrix3x2fc,
      Identifier texture2,
      boolean texture3,
      float x2,
      float y2,
      float width2,
      float height2,
      float u02,
      float v02,
      float u12,
      float v12,
      int color2,
      float cornerRadius2,
      ScreenRectangle screenRectangle
   ) {
      super(matrix3x2fc, screenRectangle, x2, y2, width2, height2);
      this.texture = texture3 ? null : SvgTextureCache.load(texture2);
      this.managedTextureId = texture3 ? texture2 : null;
      this.x = x2;
      this.y = y2;
      this.width = width2;
      this.height = height2;
      this.u0 = u02;
      this.v0 = v02;
      this.u1 = u12;
      this.v1 = v12;
      this.color = color2;
      this.cornerRadius = cornerRadius2;
      float computedValue = Math.max((float)Math.hypot(matrix3x2fc.m00(), matrix3x2fc.m01()), (float)Math.hypot(matrix3x2fc.m10(), matrix3x2fc.m11()));
      this.deviceScale = Math.max(1, MinecraftContext.mc.getWindow().getGuiScale()) * Math.max(computedValue, 0.01F);
   }

   @Override
   public RenderPipeline pipeline() {
      return GuiPipelines.TEXTURE;
   }

   @Override
   public TextureSetup textureSetup() {
      if (this.texture != null) {
         float computedValue = Math.abs(this.u1 - this.u0);
         float maximum = Math.max(computedValue, 0.01F);
         float computedValue2 = Math.abs(this.v1 - this.v0);
         float maximum2 = Math.max(computedValue2, 0.01F);
         float computedValue3 = this.width * this.deviceScale / maximum;
         float computedValue4 = this.height * this.deviceScale / maximum2;
         return this.texture.textureSetup(computedValue3, computedValue4);
      } else {
         TextureManager textureManager = MinecraftContext.mc.getTextureManager();
         GpuTextureView textureView = textureManager.getTexture(this.managedTextureId).getTextureView();
         SamplerCache samplerCache = RenderSystem.getSamplerCache();
         return TextureSetup.singleTexture(textureView, samplerCache.getClampToEdge(FilterMode.NEAREST, false));
      }
   }

   @Override
   public void buildVertices(VertexConsumer vertexConsumer) {
      float computedValue = this.width * 0.5F;
      float computedValue2 = this.height * 0.5F;
      int value = PackSizeHelper.packSize(this.width);
      int value2 = PackSizeHelper.packSize(this.height);
      float intermediate = -computedValue;
      float computedValue3 = -computedValue2;
      this.addVertex(vertexConsumer, this.x, this.y, this.u0, this.v0, intermediate, computedValue3, value, value2);
      float height2 = this.y + this.height;
      intermediate = -computedValue;
      this.addVertex(vertexConsumer, this.x, height2, this.u0, this.v1, intermediate, computedValue2, value, value2);
      float x2 = this.x + this.width;
      height2 = this.y + this.height;
      this.addVertex(vertexConsumer, x2, height2, this.u1, this.v1, computedValue, computedValue2, value, value2);
      float x3 = this.x + this.width;
      float computedValue4 = -computedValue2;
      this.addVertex(vertexConsumer, x3, this.y, this.u1, this.v0, computedValue, computedValue4, value, value2);
   }

   private void addVertex(VertexConsumer vertexConsumer, float value, float value2, float color2, float color3, float value3, float value4, int value5, int value6) {
      float radius = this.transformX(value, value2);
      float radius2 = this.transformY(value, value2);
      VertexConsumer vertexConsumer2 = vertexConsumer.addVertex(radius, radius2, Math.max(this.cornerRadius, 0.0F)).setColor(this.color).setUv(color2, color3);
      vertexConsumer2.setUv1(PackSizeHelper.packSignedSize(value3), PackSizeHelper.packSignedSize(value4)).setUv2(value5, value6).setNormal(0.0F, 0.0F, 1.0F).setLineWidth(0.0F);
   }

   public RoundedTextureRenderState(Matrix3x2fc matrix3x2fc, Identifier texture2, float value, float value2, float value3, float value4, int value5, ScreenRectangle screenRectangle) {
      this(matrix3x2fc, texture2, false, value, value2, value3, value4, 0.0F, 0.0F, 1.0F, 1.0F, value5, 0.0F, screenRectangle);
   }
}
