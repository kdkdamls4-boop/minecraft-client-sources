package deluxe.client.ui.hud;

import deluxe.client.render.FontText;
import deluxe.client.render.ItemRenderCache;

import deluxe.client.util.Animation;

import deluxe.client.util.Easings;
import deluxe.client.render.FontFace;
import deluxe.client.util.ItemDrawOptions;
import deluxe.client.module.player.UseTrackerModule;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class ArrowsHud {
   private static final Animation animation = new Animation();
   private static ItemStack field01544 = Items.ARROW.getDefaultStack();
   private static int field01545;

   private ArrowsHud() {
   }

   public static void method0836(float local1128_0, float local1128_1) {
      UseTrackerModule useTrackerModule = UseTrackerModule.method1800();
      boolean local1128_3 = useTrackerModule.method1802() > 0;
      if (local1128_3) {
         field01544 = useTrackerModule.method1812();
         field01545 = useTrackerModule.method1802();
      } else if (HudRenderContext.method4342()) {
         field01544 = Items.ARROW.getDefaultStack();
         field01545 = 3;
      }

      animation.update();
      animation.animateTo(!local1128_3 && !HudRenderContext.method4342() ? 0.0 : 1.0, 0.4, Easings.EASE_OUT_QUART);
      float local1128_4 = animation.floatValue();
      String local1128_5 = String.valueOf(field01545);
      float local1128_6 = 13.0F + FontText.method1396(FontFace.SEMI_BOLD, local1128_5, 7.0F);
      float local1128_7 = 10.0F;
      float[] floatArray = HudManager.method3390("arrows", local1128_0 / 2.0F - local1128_6 / 2.0F, local1128_1 / 2.0F + 18.0F);
      HudManager.method3418("arrows", floatArray[0], floatArray[1], local1128_6, local1128_7);
      if (!(local1128_4 <= 0.001F)) {
         HudRenderContext.method4341(floatArray[0] + local1128_6 / 2.0F, floatArray[1] + local1128_7 / 2.0F, local1128_4);
         ItemRenderCache.method2336(
            field01544,
            Math.round(floatArray[0] * 2.0F) / 2.0F,
            Math.round(floatArray[1] * 2.0F) / 2.0F,
            10.0F,
            ItemDrawOptions.noDecorations(local1128_4)
         );
         FontText.method1397(
            FontFace.SEMI_BOLD, local1128_5, floatArray[0] + 13.0F, floatArray[1] + 1.5F, 7.0F, HudStyle.method4820(local1128_4)
         );
         HudRenderContext.method4337();
      }
   }
}
