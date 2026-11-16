package deluxe.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public final class ScreenClosingTransition {
   private static final List<ScreenClosingTransition.Nested0386> field04573 = new ArrayList<>();

   private ScreenClosingTransition() {
   }

   public static boolean method1457() {
      return field04573.isEmpty();
   }

   public static void method1458(ScreenClosingTransition.Nested0386 local2137_0) {
      if (local2137_0 != null && !field04573.contains(local2137_0)) {
         field04573.add(local2137_0);
      }
   }

   public static void method1459(DrawContext drawContext) {
      if (!field04573.isEmpty()) {
         for (ScreenClosingTransition.Nested0386 local2138_2 : new ArrayList<>(field04573)) {
            if (!local2138_2.renderClosing(drawContext)) {
               field04573.remove(local2138_2);
            }
         }
      }
   }

   public static void method1460(Class<?> local2139_0) {
      field04573.removeIf(local2139_0::isInstance);
   }

   public static void method1461(ScreenClosingTransition.Nested0386 local2140_0) {
      field04573.remove(local2140_0);
   }

   public interface Nested0386 {
      boolean renderClosing(DrawContext drawContext);
   }
}
