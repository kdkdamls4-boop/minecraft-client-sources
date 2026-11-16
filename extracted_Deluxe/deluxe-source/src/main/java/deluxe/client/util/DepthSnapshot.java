package deluxe.client.util;

import deluxe.client.MinecraftAccess;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gl.Framebuffer;

public final class DepthSnapshot implements MinecraftAccess {
   private static GpuTexture field14456;
   private static GpuTextureView field14457;
   private static boolean field14458;
   private static long field14459;

   public static GpuTextureView method4228() {
      return field14458 && field14457 != null && !field14457.isClosed() ? field14457 : mc.getFramebuffer().getDepthAttachmentView();
   }

   public static void method4229() {
      if (!field14458) {
         Framebuffer framebuffer = mc.getFramebuffer();
         if (RenderSystem.getDevice() != null
            && framebuffer != null
            && framebuffer.getDepthAttachment() != null
            && framebuffer.textureWidth > 0
            && framebuffer.textureHeight > 0) {
            GpuTexture depthAttachment = framebuffer.getDepthAttachment();
            if (field14456 != null
               && (
                  field14456.isClosed()
                     || field14456.getWidth(0) != framebuffer.textureWidth
                     || field14456.getHeight(0) != framebuffer.textureHeight
                     || field14456.getFormat() != depthAttachment.getFormat()
               )) {
               method4231();
            }

            if (field14456 == null) {
               field14456 = RenderSystem.getDevice()
                  .createTexture(() -> "deluxe_depth_snapshot", 15, depthAttachment.getFormat(), framebuffer.textureWidth, framebuffer.textureHeight, 1, 1);
               field14457 = RenderSystem.getDevice().createTextureView(field14456);
            }

            RenderSystem.getDevice()
               .createCommandEncoder()
               .copyTextureToTexture(depthAttachment, field14456, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
            field14458 = true;
            field14459 = System.currentTimeMillis();
         }
      }
   }

   private DepthSnapshot() {
   }

   public static void method4230() {
      Framebuffer framebuffer = mc.getFramebuffer();
      if (RenderSystem.getDevice() != null && framebuffer != null && framebuffer.getDepthAttachment() != null) {
         method4229();
         RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(framebuffer.getDepthAttachment(), 1.0);
      }
   }

   public static void method4231() {
      if (field14457 != null) {
         field14457.close();
         field14457 = null;
      }

      if (field14456 != null) {
         field14456.close();
         field14456 = null;
      }

      field14458 = false;
   }

   public static void method4233() {
      field14458 = false;
      if (field14456 != null && System.currentTimeMillis() - field14459 > 5000L) {
         method4231();
      }
   }
}
