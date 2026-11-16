package deluxe.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.awt.image.BufferedImage;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;

public final class FontTexture implements AutoCloseable {
   private static int field12379;
   private final int field12380;
   private final NativeImage field12381;
   private final NativeImageBackedTexture field12382;
   private final TextureSetup field12383;
   private int field12384 = 1;
   private int field12385 = 1;
   private int field12386;
   private int field12387 = Integer.MAX_VALUE;
   private int field12388 = Integer.MAX_VALUE;
   private int field12389 = Integer.MIN_VALUE;
   private int field12390 = Integer.MIN_VALUE;

   TextureSetup method3529() {
      return this.field12383;
   }

   public FontTexture.Nested0490 method3530(int local5774_1, int local5774_2) {
      int local5774_3 = local5774_1 + 2;
      int local5774_4 = local5774_2 + 2;
      if (local5774_3 <= this.field12380 && local5774_4 <= this.field12380) {
         if (this.field12384 + local5774_3 > this.field12380) {
            this.field12384 = 1;
            this.field12385 = this.field12385 + this.field12386;
            this.field12386 = 0;
         }

         if (this.field12385 + local5774_4 > this.field12380) {
            return null;
         }

         int local5774_5 = this.field12384;
         int local5774_6 = this.field12385;
         this.field12384 += local5774_3;
         this.field12386 = Math.max(this.field12386, local5774_4);
         return new FontTexture.Nested0490(
            this,
            local5774_5,
            local5774_6,
            (float)local5774_5 / this.field12380,
            (float)local5774_6 / this.field12380,
            (float)(local5774_5 + local5774_1) / this.field12380,
            (float)(local5774_6 + local5774_2) / this.field12380
         );
      } else {
         return null;
      }
   }

   @Override
   public void close() {
      this.field12382.close();
   }

   public void method3531(BufferedImage bufferedImage, int local5776_2, int local5776_3) {
      int width = bufferedImage.getWidth();
      int height = bufferedImage.getHeight();

      for (int index = 0; index < height; index++) {
         for (int index2 = 0; index2 < width; index2++) {
            this.field12381.setColor(local5776_2 + index2, local5776_3 + index, method3532(bufferedImage.getRGB(index2, index)));
         }
      }

      this.field12387 = Math.min(this.field12387, local5776_2);
      this.field12388 = Math.min(this.field12388, local5776_3);
      this.field12389 = Math.max(this.field12389, local5776_2 + width);
      this.field12390 = Math.max(this.field12390, local5776_3 + height);
   }

   public FontTexture(int local5777_1) {
      this.field12380 = local5777_1;
      this.field12381 = new NativeImage(local5777_1, local5777_1, true);
      this.field12382 = new NativeImageBackedTexture(() -> "deluxe_font_atlas_" + field12379++, this.field12381);
      this.field12383 = TextureSetup.of(this.field12382.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
   }

   private static int method3532(int local5778_0) {
      int local5778_1 = local5778_0 >>> 24 & 0xFF;
      int local5778_2 = local5778_0 >>> 16 & 0xFF;
      int local5778_3 = local5778_0 >>> 8 & 0xFF;
      int local5778_4 = local5778_0 & 0xFF;
      return local5778_1 << 24 | local5778_4 << 16 | local5778_3 << 8 | local5778_2;
   }

   public void method3534() {
      if (this.field12389 > this.field12387 && this.field12390 > this.field12388) {
         if (this.field12382.getGlTexture() != null) {
            int local5780_1 = Math.max(0, this.field12387);
            int local5780_2 = Math.max(0, this.field12388);
            int local5780_3 = Math.min(this.field12380, this.field12389) - local5780_1;
            int local5780_4 = Math.min(this.field12380, this.field12390) - local5780_2;
            if (local5780_3 > 0 && local5780_4 > 0) {
               RenderSystem.getDevice()
                  .createCommandEncoder()
                  .writeToTexture(
                     this.field12382.getGlTexture(), this.field12381, 0, 0, local5780_1, local5780_2, local5780_3, local5780_4, local5780_1, local5780_2
                  );
            }

            this.field12387 = Integer.MAX_VALUE;
            this.field12388 = Integer.MAX_VALUE;
            this.field12389 = Integer.MIN_VALUE;
            this.field12390 = Integer.MIN_VALUE;
         }
      }
   }

   public record Nested0490(FontTexture page, int x, int y, float u0, float v0, float u1, float v1) {
   }
}
