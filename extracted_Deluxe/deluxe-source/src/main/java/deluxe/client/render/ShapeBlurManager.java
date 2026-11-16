package deluxe.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.Window;
import org.lwjgl.system.MemoryStack;

public enum ShapeBlurManager {
   INSTANCE;

   private static final float[] field13919 = new float[]{1.1F, 1.0F, 1.4F};
   private static final int[] field13920 = new int[]{2, 3, 4};
   private final SimpleFramebuffer[] field13927 = new SimpleFramebuffer[3];
   private final boolean[] field13928 = new boolean[3];
   private final boolean[] field13929 = new boolean[3];
   private SimpleFramebuffer[] field13930 = new SimpleFramebuffer[0];
   private final List<SimpleFramebuffer> field13931 = new ArrayList<>();
   private final List<GpuTextureView> field13932 = new ArrayList<>();
   private final List<Integer> field13933 = new ArrayList<>();
   private final List<float[]> field13934 = new ArrayList<>();
   private final List<ShapeBlurManager.Nested0516> field13935 = new ArrayList<>();
   private final int[] field13936 = new int[4];
   private final float[] field13937 = new float[4];
   private int field13938;
   private int field13939;
   private long field13940;
   private SimpleFramebuffer field13941;
   private GpuBuffer field13942;
   private long field13943;

   public void method4045() {
      long local6803_1 = System.nanoTime();
      if (this.field13938 >= this.field13939 || local6803_1 - this.field13940 > 3000000000L) {
         this.field13939 = this.field13938;
         this.field13940 = local6803_1;
      }

      while (this.field13931.size() > this.field13939) {
         int local6803_3 = this.field13931.size() - 1;
         this.field13931.remove(local6803_3).delete();
         this.field13932.remove(local6803_3);
         this.field13933.remove(local6803_3);
         this.field13934.remove(local6803_3);
         this.field13935.remove(local6803_3);
      }

      this.field13938 = 0;
   }

   public void method4046() {
      long local6804_1 = System.nanoTime();
      if (local6804_1 - this.field13943 >= 16666666L) {
         this.field13943 = local6804_1;
         this.method4047();
      }
   }

   private void method4047() {
      Framebuffer framebuffer = method4065();
      if (framebuffer != null) {
         boolean local6805_2 = false;

         for (boolean local6805_6 : this.field13928) {
            local6805_2 |= local6805_6;
         }

         if (!local6805_2) {
            Arrays.fill(this.field13929, false);
         } else {
            Window window = MinecraftClient.getInstance().getWindow();
            int local6805_4 = Math.max(1, window.getFramebufferWidth() / 2);
            int local6805_5 = Math.max(1, window.getFramebufferHeight() / 2);
            GpuTextureView colorAttachmentView = framebuffer.getColorAttachmentView();

            try (ShapeBlurPass.Nested0426 local6805_7 = ShapeBlurPass.method2280()) {
               for (int index = 0; index < 3; index++) {
                  this.field13929[index] = this.field13928[index];
                  this.field13928[index] = false;
                  if (this.field13929[index]) {
                     this.field13927[index] = method4066(this.field13927[index], "deluxe_shape_blur_l" + index, local6805_4, local6805_5);
                     this.method4049(
                        colorAttachmentView, method4048(index), field13919[index], field13919[index] * 2.0F, this.field13927[index], null, 0.0F
                     );
                  }
               }
            }
         }
      }
   }

   private static int method4048(int local6806_0) {
      int local6806_1 = 31 - Integer.numberOfLeadingZeros(Math.max(1, 2));
      return Math.max(1, field13920[local6806_0] + 1 - local6806_1);
   }

   private void method4049(
      GpuTextureView gpuTextureView, int local6807_2, float local6807_3, float local6807_4, SimpleFramebuffer simpleFramebuffer, float[] floatArray, float local6807_7
   ) {
      this.method4053(local6807_2, simpleFramebuffer.textureWidth, simpleFramebuffer.textureHeight);
      GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      GpuTextureView gpuTextureView2 = gpuTextureView;
      float local6807_10 = local6807_3 + 1.0F;
      float local6807_11 = local6807_3 * 0.5F + 1.0F;

      for (int index = 0; index < local6807_2; index++) {
         int[] intArray = null;
         if (floatArray != null) {
            float local6807_14 = method4050(local6807_7, local6807_10, local6807_2 - 1);

            for (int index2 = local6807_2 - 1; index2 > index; index2--) {
               local6807_14 += local6807_11 * (1 << index2);
            }

            intArray = this.method4052(this.field13930[index], simpleFramebuffer, floatArray, local6807_14);
         }

         ShapeBlurPass.method2277(
            ShapePipelines.BLUR_DOWN, this.field13930[index], gpuTextureView2, gpuSampler, index == 0 ? local6807_4 : local6807_3, intArray
         );
         gpuTextureView2 = this.field13930[index].getColorAttachmentView();
      }

      for (int index3 = local6807_2 - 2; index3 >= 0; index3--) {
         int[] intArray2 = floatArray == null
            ? null
            : this.method4052(this.field13930[index3], simpleFramebuffer, floatArray, method4050(local6807_7, local6807_10, index3));
         ShapeBlurPass.method2277(
            ShapePipelines.BLUR_UP,
            this.field13930[index3],
            this.field13930[index3 + 1].getColorAttachmentView(),
            gpuSampler,
            local6807_3,
            intArray2
         );
      }

      int[] intArray3 = floatArray == null ? null : this.method4052(simpleFramebuffer, simpleFramebuffer, floatArray, local6807_7);
      ShapeBlurPass.method2277(
         ShapePipelines.BLUR_UP, simpleFramebuffer, this.field13930[0].getColorAttachmentView(), gpuSampler, local6807_3, intArray3
      );
   }

   private static float method4050(float local6808_0, float local6808_1, int local6808_2) {
      return local6808_0 + local6808_1 * ((1 << local6808_2 + 2) - 2);
   }

   private static float method4051(float local6809_0, int local6809_1, float local6809_2, float local6809_3) {
      float local6809_4 = method4050(local6809_0, local6809_2 + 1.0F, local6809_1 - 1);

      for (int index = local6809_1 - 1; index > 0; index--) {
         local6809_4 += (local6809_2 * 0.5F + 1.0F) * (1 << index);
      }

      return local6809_4 + local6809_3 * 0.5F + 1.0F;
   }

   private int[] method4052(Framebuffer framebuffer, SimpleFramebuffer simpleFramebuffer, float[] floatArray, float local6810_4) {
      int local6810_5 = Math.max(1, framebuffer.textureWidth);
      int local6810_6 = Math.max(1, framebuffer.textureHeight);
      float local6810_7 = local6810_4 / Math.max(1, simpleFramebuffer.textureWidth);
      float local6810_8 = local6810_4 / Math.max(1, simpleFramebuffer.textureHeight);
      int local6810_9 = Math.max(0, (int)Math.floor((floatArray[0] - local6810_7) * local6810_5) - 1);
      int local6810_10 = Math.max(0, (int)Math.floor((floatArray[1] - local6810_8) * local6810_6) - 1);
      int local6810_11 = Math.min(local6810_5, (int)Math.ceil((floatArray[2] + local6810_7) * local6810_5) + 1);
      int local6810_12 = Math.min(local6810_6, (int)Math.ceil((floatArray[3] + local6810_8) * local6810_6) + 1);
      int[] intArray = this.field13936;
      intArray[0] = Math.min(local6810_9, local6810_5 - 1);
      intArray[1] = Math.min(local6810_10, local6810_6 - 1);
      intArray[2] = Math.max(1, local6810_11 - intArray[0]);
      intArray[3] = Math.max(1, local6810_12 - intArray[1]);
      return intArray;
   }

   private void method4053(int local6811_1, int local6811_2, int local6811_3) {
      if (this.field13930.length < local6811_1) {
         this.field13930 = Arrays.copyOf(this.field13930, local6811_1);
      }

      for (int index = 0; index < local6811_1; index++) {
         this.field13930[index] = method4066(
            this.field13930[index],
            "deluxe_shape_blur_mip" + index,
            Math.max(1, local6811_2 >> index + 1),
            Math.max(1, local6811_3 >> index + 1)
         );
      }
   }

   public int method4054(int local6812_1, float local6812_2, float local6812_3, float local6812_4, float local6812_5, float local6812_6) {
      Window window = MinecraftClient.getInstance().getWindow();
      int local6812_8 = Math.max(1, window.getFramebufferWidth() / 2);
      int local6812_9 = Math.max(1, window.getFramebufferHeight() / 2);
      int local6812_10 = this.field13938++;

      while (this.field13931.size() <= local6812_10) {
         this.field13931.add(new SimpleFramebuffer("deluxe_shape_blur_2d_res_" + this.field13931.size(), local6812_8, local6812_9, false));
         this.field13932.add(null);
         this.field13933.add(0);
         this.field13934.add(new float[5]);
         this.field13935.add(new ShapeBlurManager.Nested0516());
      }

      SimpleFramebuffer simpleFramebuffer = this.field13931.get(local6812_10);
      if (simpleFramebuffer.textureWidth != local6812_8 || simpleFramebuffer.textureHeight != local6812_9) {
         simpleFramebuffer.resize(local6812_8, local6812_9);
         this.field13935.get(local6812_10).field13944 = false;
      }

      this.field13932.set(local6812_10, simpleFramebuffer.getColorAttachmentView());
      this.field13933.set(local6812_10, method4064(local6812_1));
      float[] floatArray = this.field13934.get(local6812_10);
      floatArray[0] = local6812_2;
      floatArray[1] = local6812_3;
      floatArray[2] = local6812_4;
      floatArray[3] = local6812_5;
      floatArray[4] = local6812_6;
      return local6812_10;
   }

   private TextureSetup method4055(int local6813_1) {
      if (local6813_1 >= 0 && local6813_1 < this.field13932.size()) {
         GpuTextureView gpuTextureView = this.field13932.get(local6813_1);
         return gpuTextureView != null && !gpuTextureView.isClosed() ? TextureSetup.of(gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR)) : null;
      } else {
         return null;
      }
   }

   public int method4056(GpuTextureView gpuTextureView) {
      if (gpuTextureView == null) {
         return -1;
      }

      for (int index = 0; index < this.field13932.size(); index++) {
         if (this.field13932.get(index) == gpuTextureView) {
            return index;
         }
      }

      return -1;
   }

   public boolean method4057(int local6815_1) {
      return local6815_1 >= 0 && local6815_1 < this.field13931.size() && this.method4058(local6815_1, System.nanoTime());
   }

   private boolean method4058(int local6816_1, long local6816_2) {
      ShapeBlurManager.Nested0516 local6816_4 = this.field13935.get(local6816_1);
      if (!local6816_4.field13944) {
         return true;
      } else {
         float[] floatArray = this.field13934.get(local6816_1);
         if (local6816_4.field13947 == this.field13933.get(local6816_1) && !method4059(local6816_4.field13948, floatArray)) {
            boolean local6816_6 = !Arrays.equals(local6816_4.field13948, floatArray);
            return local6816_6 && !local6816_4.method4068(floatArray, this.field13931.get(local6816_1))
               ? true
               : local6816_2 - local6816_4.field13946 >= (local6816_6 ? 6944444L : 16666666L);
         } else {
            return true;
         }
      }
   }

   private static boolean method4059(float[] floatArray, float[] floatArray2) {
      if (floatArray != null && floatArray2 != null) {
         float local6817_2 = Math.max(0.0F, Math.min(floatArray[2], floatArray2[2]) - Math.max(floatArray[0], floatArray2[0]));
         float local6817_3 = Math.max(0.0F, Math.min(floatArray[3], floatArray2[3]) - Math.max(floatArray[1], floatArray2[1]));
         float local6817_4 = local6817_2 * local6817_3;
         float local6817_5 = Math.max(0.0F, floatArray[2] - floatArray[0]) * Math.max(0.0F, floatArray[3] - floatArray[1]);
         float local6817_6 = Math.max(0.0F, floatArray2[2] - floatArray2[0]) * Math.max(0.0F, floatArray2[3] - floatArray2[1]);
         float local6817_7 = local6817_5 + local6817_6 - local6817_4;
         return local6817_7 > 1.0E-9F && local6817_4 / local6817_7 < 0.75F;
      } else {
         return floatArray != floatArray2;
      }
   }

   public void method4060(int local6818_1, Framebuffer framebuffer) {
      if (local6818_1 >= 0
         && local6818_1 < this.field13931.size()
         && framebuffer != null
         && framebuffer.getColorAttachmentView() != null
         && !framebuffer.getColorAttachmentView().isClosed()) {
         SimpleFramebuffer simpleFramebuffer = this.field13931.get(local6818_1);
         long local6818_4 = System.nanoTime();
         if (this.method4058(local6818_1, local6818_4)) {
            float[] floatArray = this.field13934.get(local6818_1);
            int local6818_7 = this.field13933.get(local6818_1);
            boolean local6818_8 = this.method4061(framebuffer, simpleFramebuffer, floatArray, local6818_7);
            ShapeBlurManager.Nested0516 local6818_9 = this.field13935.get(local6818_1);
            local6818_9.field13944 = true;
            local6818_9.field13946 = local6818_4;
            local6818_9.field13947 = local6818_7;
            System.arraycopy(floatArray, 0, local6818_9.field13948, 0, 5);
            local6818_9.field13945 = local6818_8;
         }
      }
   }

   private boolean method4061(Framebuffer framebuffer, SimpleFramebuffer simpleFramebuffer, float[] floatArray, int local6819_4) {
      GpuTextureView gpuTextureView = framebuffer.getColorAttachmentView();
      float local6819_6 = field13919[local6819_4] * 2.0F;
      boolean local6819_7 = false;

      try (ShapeBlurPass.Nested0426 local6819_8 = ShapeBlurPass.method2280()) {
         float[] floatArray2 = null;
         float local6819_10 = 9.0F;
         if (floatArray != null && floatArray[2] > floatArray[0] && floatArray[3] > floatArray[1]) {
            local6819_6 = field13919[local6819_4];
            floatArray2 = this.field13937;
            System.arraycopy(floatArray, 0, floatArray2, 0, 4);
            local6819_7 = true;
            this.field13941 = method4066(this.field13941, "deluxe_shape_blur_clamp", simpleFramebuffer.textureWidth, simpleFramebuffer.textureHeight);
            int[] intArray = this.method4052(
               this.field13941, simpleFramebuffer, floatArray2, method4051(local6819_10, method4048(local6819_4), field13919[local6819_4], local6819_6)
            );
            ShapeBlurPass.method2275(
               ShapePipelines.CLAMP_COPY,
               this.field13941,
               gpuTextureView,
               RenderSystem.getSamplerCache().get(FilterMode.LINEAR),
               this.method4062(floatArray),
               intArray
            );
            gpuTextureView = this.field13941.getColorAttachmentView();
         }

         this.method4049(gpuTextureView, method4048(local6819_4), field13919[local6819_4], local6819_6, simpleFramebuffer, floatArray2, local6819_10);
      }

      return local6819_7;
   }

   private GpuBuffer method4062(float[] floatArray) {
      if (this.field13942 == null || this.field13942.isClosed()) {
         this.field13942 = RenderSystem.getDevice().createBuffer(() -> "deluxe_shape_blur_rect", 136, 32L);
      }

      MemoryStack memoryStack = MemoryStack.stackPush();

      try {
         ByteBuffer byteBuffer = memoryStack.calloc(32);
         byteBuffer.putFloat(0, floatArray[0]);
         byteBuffer.putFloat(4, floatArray[1]);
         byteBuffer.putFloat(8, floatArray[2]);
         byteBuffer.putFloat(12, floatArray[3]);
         byteBuffer.putFloat(16, floatArray[4]);
         byteBuffer.position(0);
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field13942.slice(0L, 32L), byteBuffer);
      } catch (Throwable throwable) {
         if (memoryStack != null) {
            try {
               memoryStack.close();
            } catch (Throwable throwable2) {
               throwable.addSuppressed(throwable2);
            }
         }

         throw throwable;
      }

      if (memoryStack != null) {
         memoryStack.close();
      }

      return this.field13942;
   }

   public TextureSetup method4063(
      int local6821_1, boolean local6821_2, float local6821_3, float local6821_4, float local6821_5, float local6821_6, float local6821_7
   ) {
      int local6821_8 = method4064(local6821_1);
      if (local6821_2) {
         int local6821_9 = this.method4054(local6821_8, local6821_3, local6821_4, local6821_5, local6821_6, local6821_7);
         TextureSetup textureSetup = this.method4055(local6821_9);
         if (textureSetup != null) {
            return textureSetup;
         }
      }

      this.field13928[local6821_8] = true;
      if (!this.field13929[local6821_8]) {
         return null;
      }

      SimpleFramebuffer simpleFramebuffer = this.field13927[local6821_8];
      return simpleFramebuffer != null && simpleFramebuffer.getColorAttachmentView() != null && !simpleFramebuffer.getColorAttachmentView().isClosed()
         ? TextureSetup.of(simpleFramebuffer.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR))
         : null;
   }

   private static int method4064(int local6822_0) {
      return local6822_0 < 0 ? 0 : Math.min(local6822_0, 2);
   }

   private static Framebuffer method4065() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient == null) {
         return null;
      } else {
         Framebuffer framebuffer = minecraftClient.getFramebuffer();
         if (framebuffer != null && framebuffer.getColorAttachment() != null && framebuffer.getColorAttachment().getFormat() == TextureFormat.RGBA8) {
            return framebuffer.getColorAttachmentView() != null && !framebuffer.getColorAttachmentView().isClosed() ? framebuffer : null;
         } else {
            return null;
         }
      }
   }

   private static SimpleFramebuffer method4066(SimpleFramebuffer simpleFramebuffer, String local6824_1, int local6824_2, int local6824_3) {
      if (simpleFramebuffer == null) {
         return new SimpleFramebuffer(local6824_1, local6824_2, local6824_3, false);
      }

      if (simpleFramebuffer.textureWidth != local6824_2 || simpleFramebuffer.textureHeight != local6824_3) {
         simpleFramebuffer.resize(local6824_2, local6824_3);
      }

      return simpleFramebuffer;
   }

   private static final class Nested0516 {
      private boolean field13944;
      private boolean field13945;
      private long field13946;
      private int field13947;
      private final float[] field13948 = new float[5];

      private boolean method4068(float[] floatArray, SimpleFramebuffer simpleFramebuffer) {
         if (!this.field13945) {
            return true;
         }

         float local6829_3 = 6.0F / Math.max(1, simpleFramebuffer.textureWidth);
         float local6829_4 = 6.0F / Math.max(1, simpleFramebuffer.textureHeight);
         return floatArray[0] >= this.field13948[0] - local6829_3
            && floatArray[1] >= this.field13948[1] - local6829_4
            && floatArray[2] <= this.field13948[2] + local6829_3
            && floatArray[3] <= this.field13948[3] + local6829_4;
      }
   }
}
