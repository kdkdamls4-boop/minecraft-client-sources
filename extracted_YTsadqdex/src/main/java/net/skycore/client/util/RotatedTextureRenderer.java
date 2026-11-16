package net.skycore.client.util;

import net.minecraft.resources.Identifier;

public final class RotatedTextureRenderer {
   public static void draw(float coordinate, float coordinate2, float coordinate3, Identifier texture, float radius, int color) {
      BeginFrameHelper.TextureBuilder textureBuilder = BeginFrameHelper.texture(coordinate, coordinate2, coordinate3, coordinate3, texture);
      BeginFrameHelper.TextureBuilder textureBuilder2 = textureBuilder.managed();
      BeginFrameHelper.TextureBuilder textureBuilder3 = textureBuilder2.uv(0.125F, 0.125F, 0.25F, 0.25F);
      BeginFrameHelper.TextureBuilder textureBuilder4 = textureBuilder3.radius(radius).color(color);
      textureBuilder4.draw();
      BeginFrameHelper.TextureBuilder textureBuilder5 = BeginFrameHelper.texture(coordinate, coordinate2, coordinate3, coordinate3, texture);
      BeginFrameHelper.TextureBuilder textureBuilder6 = textureBuilder5.managed();
      BeginFrameHelper.TextureBuilder textureBuilder7 = textureBuilder6.uv(0.625F, 0.125F, 0.75F, 0.25F);
      BeginFrameHelper.TextureBuilder textureBuilder8 = textureBuilder7.radius(radius).color(color);
      textureBuilder8.draw();
   }

   private RotatedTextureRenderer() {
   }
}
