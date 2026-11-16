package deluxe.client.ui.hud;

import deluxe.client.render.FontText;
import deluxe.client.render.ItemRenderCache;

import deluxe.client.util.Animation;

import deluxe.client.util.Easings;
import deluxe.client.render.FontFace;
import deluxe.client.util.ItemDrawOptions;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

public final class InventoryHud {
   private static final Animation animation = new Animation();

   public static void method1258(MinecraftClient minecraftClient, float local1723_1, float local1723_2) {
      if (minecraftClient.player != null) {
         boolean local1723_3 = false;

         for (int index = 9; index < 36 && !local1723_3; index++) {
            local1723_3 = !minecraftClient.player.getInventory().getStack(index).isEmpty();
         }

         animation.update();
         animation.animateTo(!local1723_3 && !HudRenderContext.method4342() ? 0.0 : 1.0, 0.4, Easings.EASE_OUT_QUART);
         float local1723_4 = animation.floatValue();
         if (!(local1723_4 <= 0.001F)) {
            float local1723_5 = Math.max(16.0F + FontText.method1396(FontFace.SEMI_BOLD, "Inventory", 6.0F) + 4.0F, 111.0F);
            float local1723_6 = 51.0F;
            float[] floatArray = HudManager.method3390("inventory", local1723_1, local1723_2);
            HudManager.method3418("inventory", floatArray[0], floatArray[1], local1723_5, local1723_6);
            HudRenderContext.method4341(floatArray[0] + local1723_5 / 2.0F, floatArray[1] + local1723_6 / 2.0F, local1723_4);
            HudStyle.method4824(floatArray[0], floatArray[1], local1723_5, local1723_4, HudIcons.INVENTORY, "Inventory");
            float local1723_8 = floatArray[0] + (local1723_5 - 111.0F) / 2.0F;
            float local1723_9 = floatArray[1] + 13.0F + 2.0F;

            for (int index2 = 0; index2 < 3; index2++) {
               float local1723_11 = local1723_9 + index2 * 12.5F;
               HudStyle.method4821(local1723_8, local1723_11, 111.0F, 11.0F, local1723_4);
               float local1723_12 = local1723_11 + 5.5F;

               for (int index3 = 0; index3 < 8; index3++) {
                  HudStyle.method4810(local1723_8 + index3 * 12.5F + 11.0F + 0.75F - 0.25F, local1723_12, local1723_4, 1.5F);
               }

               for (int index4 = 0; index4 < 9; index4++) {
                  ItemStack stack = minecraftClient.player.getInventory().getStack(9 + index2 * 9 + index4);
                  if (!stack.isEmpty()) {
                     float local1723_15 = Math.round((local1723_8 + index4 * 12.5F + 1.5F) * 2.0F) / 2.0F;
                     float local1723_16 = Math.round((local1723_11 + 1.5F) * 2.0F) / 2.0F;
                     ItemRenderCache.method2336(stack, local1723_15, local1723_16, 8.0F, ItemDrawOptions.decorated(local1723_4));
                  }
               }
            }

            HudRenderContext.method4337();
         }
      }
   }
}
