package net.skycore.client.util;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;

public final class CaptureHelper {
   private static final Map<GuiItemRenderState, Float> ALPHAS = Collections.synchronizedMap(new WeakHashMap<>());
   private static final ThreadLocal<Float> ACTIVE_ALPHA = new ThreadLocal<>();

   private CaptureHelper() {
   }

   private static float clamp(float value) {
      float minimum = Math.min(1.0F, value);
      return Math.max(0.0F, minimum);
   }

   public static void capture(GuiItemRenderState guiItemRenderState) {
      Float value = ACTIVE_ALPHA.get();
      if (guiItemRenderState != null && value != null) {
         if (value < 0.999F) {
            ALPHAS.put(guiItemRenderState, value);
         }
      }
   }

   public static float consume(GuiItemRenderState guiItemRenderState) {
      if (guiItemRenderState == null) {
         return 1.0F;
      }

      Float value = ALPHAS.remove(guiItemRenderState);
      float result;
      if (value == null) {
         result = 1.0F;
      } else {
         float value2 = value;
         result = clamp(value2);
      }

      return result;
   }

   // $VF: Could not verify finally blocks. A semaphore variable has been added to preserve control flow.
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public static void withAlpha(float value, Runnable runnable) {
      Float value2 = ACTIVE_ALPHA.get();
      ACTIVE_ALPHA.set(clamp(value));
      boolean condition = false /* VF: Semaphore variable */;

      try {
         condition = true;
         runnable.run();
         condition = false;
      } finally {
         if (condition) {
            if (value2 == null) {
               ACTIVE_ALPHA.remove();
            } else {
               ACTIVE_ALPHA.set(value2);
            }
         }
      }

      if (value2 == null) {
         ACTIVE_ALPHA.remove();
      } else {
         ACTIVE_ALPHA.set(value2);
      }
   }
}
