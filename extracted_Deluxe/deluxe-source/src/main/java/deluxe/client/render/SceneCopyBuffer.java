package deluxe.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;

public final class SceneCopyBuffer {
   private static SimpleFramebuffer field13612;
   private static long field13613;

   public static GpuTextureView method3920(Framebuffer framebuffer) {
      if (framebuffer != null && framebuffer.getColorAttachment() != null && framebuffer.textureWidth > 0 && framebuffer.textureHeight > 0) {
         if (field13612 == null) {
            field13612 = new SimpleFramebuffer("deluxe_scene_copy", framebuffer.textureWidth, framebuffer.textureHeight, false);
         }

         if (field13612.textureWidth != framebuffer.textureWidth || field13612.textureHeight != framebuffer.textureHeight) {
            field13612.resize(framebuffer.textureWidth, framebuffer.textureHeight);
         }

         if (field13612.getColorAttachment() == null) {
            return null;
         }

         RenderSystem.getDevice()
            .createCommandEncoder()
            .copyTextureToTexture(
               framebuffer.getColorAttachment(), field13612.getColorAttachment(), 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight
            );
         field13613 = System.currentTimeMillis();
         return field13612.getColorAttachmentView();
      } else {
         return null;
      }
   }

   public static void method3921() {
      if (field13612 != null) {
         field13612.delete();
         field13612 = null;
      }
   }

   private SceneCopyBuffer() {
   }

   public static void method3922() {
      if (field13612 != null && System.currentTimeMillis() - field13613 > 5000L) {
         method3921();
      }
   }
}
