package deluxe.client.render;

import deluxe.client.MinecraftAccess;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap.Type;

public final class RoofHeightMap implements MinecraftAccess {
   public static final int field11652 = 64;
   public static final int field11653 = 128;
   public static int field11654 = Integer.MIN_VALUE;
   public static int field11655;
   public static int field11656;
   private static NativeImage field11657;
   private static GpuTexture field11658;
   private static GpuTextureView field11659;
   private static long field11660;

   private RoofHeightMap() {
   }

   public static void method3309() {
      if (mc.world != null) {
         if (field11658 == null || field11658.isClosed()) {
            field11657 = new NativeImage(64, 64, false);
            field11658 = RenderSystem.getDevice().createTexture(() -> "deluxe_roof_map", 5, TextureFormat.RGBA8, 64, 64, 1, 1);
            field11659 = RenderSystem.getDevice().createTextureView(field11658);
            field11654 = Integer.MIN_VALUE;
         }

         int local5406_0 = (MathHelper.floor(mc.gameRenderer.getCamera().getCameraPos().x) & -8) - 32;
         int local5406_1 = (MathHelper.floor(mc.gameRenderer.getCamera().getCameraPos().z) & -8) - 32;
         int local5406_2 = MathHelper.floor(mc.gameRenderer.getCamera().getCameraPos().y) & -8;
         if (local5406_0 != field11654 || local5406_1 != field11655 || local5406_2 != field11656 || System.currentTimeMillis() - field11660 > 500L) {
            for (int index = 0; index < 64; index++) {
               for (int index2 = 0; index2 < 64; index2++) {
                  int local5406_5 = MathHelper.clamp(
                     mc.world.getTopY(Type.MOTION_BLOCKING, local5406_0 + index2, local5406_1 + index) - local5406_2 + 128, 0, 255
                  );
                  field11657.setColor(index2, index, 0xFF000000 | local5406_5 << 16 | local5406_5 << 8 | local5406_5);
               }
            }

            RenderSystem.getDevice().createCommandEncoder().writeToTexture(field11658, field11657);
            field11654 = local5406_0;
            field11655 = local5406_1;
            field11656 = local5406_2;
            field11660 = System.currentTimeMillis();
         }
      }
   }

   public static GpuTextureView method3310() {
      return field11659;
   }
}
